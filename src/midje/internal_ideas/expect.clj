(ns ^{:doc "Mostly functions for identifying semi-sweet expects, and for converting 
            midje.sweet arrow forms into semi-sweet expcet forms."}
  midje.internal-ideas.expect
  (:use [midje.util.treelike :only [tree-variant]]
        [midje.util.namespace :only [matches-symbols-in-semi-sweet-or-sweet-ns?]]
        [midje.util.form-utils :only [first-named?]]
        [midje.util.zip :only [skip-to-rightmost-leaf n-times remove-moving-right]]
        [midje.ideas.arrows :only [start-of-checking-arrow-sequence? arrow-sequence-overrides]]
        [midje.internal-ideas.file-position :only [arrow-line-number]])
  (:require [clojure.zip :as zip]))
  

(defmulti expect? tree-variant)

(defmethod expect? :zipper [loc]
  (and (zip/branch? loc)
       (matches-symbols-in-semi-sweet-or-sweet-ns? '(expect) (zip/down loc))))

(defmethod expect? :form [form]
  (first-named? form "expect"))



;; Moving around

(defn up-to-full-expect-form
  "From anywhere (recursively) within an expect form, move so that
   loc is at the full form (so that zip/down is 'expect)." 
  [loc]
  (if (expect? loc)
    loc
    (recur (zip/up loc))))



(defn tack-on__then__at-same-location [[form & more-forms] loc]
  (assert (expect? loc))
  (if form
    (recur more-forms (zip/append-child loc form))	  
    (up-to-full-expect-form loc)))

(defn tack-on__then__at-rightmost-expect-leaf [forms loc]
  (let [tack (fn [loc] (tack-on__then__at-same-location forms loc))]
    (-> loc tack zip/down skip-to-rightmost-leaf)))


(defn wrap-with-expect__then__at-rightmost-expect-leaf [loc]
  (assert (start-of-checking-arrow-sequence? loc))
  (let [right-hand (-> loc zip/right zip/right)
        arrow-sequence (-> loc zip/right zip/node)
        additions (arrow-sequence-overrides (zip/rights right-hand))
        line-number (arrow-line-number (zip/right loc))
        edited-loc (zip/edit loc
                      (fn [loc]
                        (vary-meta
                          `(midje.semi-sweet/expect ~loc ~arrow-sequence ~(zip/node right-hand) ~@additions)
                          assoc :line line-number)))]
    (->> edited-loc
      zip/right
      (n-times (inc (count additions)) remove-moving-right)
      zip/remove)))