;; -*- indent-tabs-mode: nil -*-

(ns ^{:doc "A way to create multiple facts with the same template, but different data points."}
  midje.ideas.tabular
  (:use 
    [clojure.string :only [join]]
    [midje.error-handling.validation-errors :only [valid-let simple-report-validation-error validate]]
    [midje.internal-ideas.fact-context :only [within-fact-context]]
    [midje.internal-ideas.file-position :only [form-with-copied-line-numbers
                                               form-position]] ; for deprecation
    [midje.internal-ideas.report :only [midje-position-string]] ; for deprecation
    [midje.util.form-utils :only [translate-zipper]]
    [midje.util.zip :only [skip-to-rightmost-leaf]]
    [midje.internal-ideas.expect :only [expect?]]
    [midje.ideas.arrows :only [above-arrow-sequence__add-key-value__at-arrow]]
    [midje.ideas.metaconstants :only [metaconstant-symbol?]]
    [utilize.map :only [ordered-zipmap]])
(:require [midje.util.unify :as unify]))

(def #^:private deprecation-hack:file-position (atom ""))

(defn- remove-pipes+where [table]
  (when (and false (#{:where 'where} (first table)))
    (println "The `where` syntactic sugar for tabular facts is deprecated and will be removed in Midje 1.4."
      @deprecation-hack:file-position))

  (when (and false (some #(= % '|) table))
    (println "The `|` syntactic sugar for tabular facts is deprecated and will be removed in Midje 1.4."
      @deprecation-hack:file-position))

  (letfn [(strip-off-where [x] (if (#{:where 'where} (first x)) (rest x) x))]
    (->> table strip-off-where (remove #(= % '|)))))

(defn- headings-rows+values [table locals]
  (letfn [(table-variable? [s]
            (and (symbol? s)
              (not (metaconstant-symbol? s))
              (not (resolve s))
              (not ((set locals) s))))] 
    (split-with table-variable? (remove-pipes+where table))))

(defn- ^{:testable true } table-binding-maps [table locals]
  (let [[headings-row values] (headings-rows+values table locals)
        value-rows (partition (count headings-row) values)]
    (map (partial ordered-zipmap headings-row) value-rows)))

(defn- format-binding-map [binding-map] 
  (let [formatted-entries (for [[k v] binding-map]
                            (str (pr-str k) " " (pr-str v)))]
    (str "[" (join "\n                           " formatted-entries) "]")))

(defn- ^{:testable true } add-binding-note
  [expect-containing-form ordered-binding-map]
  (translate-zipper expect-containing-form
    expect?
    (fn [loc] (skip-to-rightmost-leaf
                (above-arrow-sequence__add-key-value__at-arrow
                  :binding-note (format-binding-map ordered-binding-map) loc)))))

(defn tabular* [locals form]
  (letfn [(macroexpander-for [fact-form]
            (comp macroexpand
              (partial form-with-copied-line-numbers fact-form)
              (partial unify/substitute fact-form)))]

    (valid-let [[description? fact-form table] (validate form locals)
                _ (swap! deprecation-hack:file-position
                         (constantly (midje-position-string (form-position fact-form))))
                ordered-binding-maps (table-binding-maps table locals)
                expect-forms (map (macroexpander-for fact-form) ordered-binding-maps)
                expect-forms-with-binding-notes (map add-binding-note
        expect-forms
        ordered-binding-maps)]
      `(within-fact-context ~description?
         ~@expect-forms-with-binding-notes))))

(defmethod validate "tabular" [[_tabular_ & form] locals]
  (let [[[description? & _] [fact-form & table]] (split-with string? form)
        [headings-row values] (headings-rows+values table locals)]
    (cond (empty? table)
          (simple-report-validation-error form
            "There's no table. (Misparenthesized form?)")
      
          (empty? values)
          (simple-report-validation-error form
            "It looks like the table has headings, but no values:")
      
          (empty? headings-row)
          (simple-report-validation-error form
            "It looks like the table has no headings, or perhaps you"
            "tried to use a non-literal string for the doc-string?:")
      
          :else 
          [description? fact-form table])))