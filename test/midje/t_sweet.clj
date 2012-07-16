(ns midje.t-sweet
  (:use midje.sweet
        midje.util
        midje.test-util)
  (:require midje.internal-ideas.t-fakes))

(fact "all of Midje's public, API-facing vars have docstrings"
  (map str (remove (comp :doc meta) (vals (ns-publics 'midje.sweet)))) => []
  (map str (remove (comp :doc meta) (vals (ns-publics 'midje.semi-sweet)))) => []
  (map str (remove (comp :doc meta) (vals (ns-publics 'midje.unprocessed)))) => []
  (map str (remove (comp :doc meta) (vals (ns-publics 'midje.util)))) => [])

(after-silently ; failing
 (fact (+ 1 1) => 3)
 (fact @reported => (one-of bad-result)))

(after-silently ; succeeding
 (facts
   (+ 10 10) => 20
   (+ 20 20) => 40)
 (fact @reported => (two-of pass)))


(after-silently ; combination, including a future fact
 (facts
   (+ 1 1) => 3
   (+ 1 "1") =future=> "2"
   (+ 1 1) => 2)
 (fact @reported => (just bad-result
                          (contains {:type :future-fact
                                     :description [nil "(+ 1 \"1\")"]})
                          pass)))

(defn number [] )
(defn two-numbers [] 
  (+ (number) (number)))


(letfn [(throws-arrow-exception? [captured-throwable]
          (= "Right side of =throws=> should extend Throwable." (.getMessage (.throwable captured-throwable))))]

  (after-silently ;; =throws=> gives sensible error when called without a Throwable
    (fact
      (two-numbers) => nil
      (provided
        (number) =throws=> [1]))
  
    (fact @reported => (just (contains {:type :mock-expected-result-failure
                                        :actual throws-arrow-exception? } )))))


(facts "this is a doc string"
  (+ 10 10) => 20
  "this is another one"
  (+ 20 20) => 40)

(unfinished g)
(defn f [n] (g n))
(defn call2 [n m]
  (+ (g n) (g m)))
  
;; Some examples of prerequisites
(after-silently
 (fact (f 1) => 33
   (provided (g 1) => 33))
 (fact @reported => (one-of pass)))

(after-silently
 (facts 
   (f 1) => 313
   (provided
     (g 1) => 313)
    
   (f 22) => 500
   (provided 
     (g 22) => 500))
 (fact @reported => (two-of pass)))

(after-silently 
 (facts 
   (call2 1 2) => 30
   (provided 
     (g 1) => 10
     (g 2) => 20))
 (fact @reported => (one-of pass)))

;; facts can see their environment
(let [outer-value 2]
  (fact
    (let [inner-value 3]
      (call2 outer-value inner-value) => 23
      (provided (g outer-value) => (* 10 outer-value)
                (g inner-value) => inner-value))))


(defn always-one [x] 1)
(defn g-caller [x] (g x))

; Metaconstants
(fact (always-one ...anything...) => 1)
(fact (g-caller ...something...) => ...g-value...
  (provided (g ...something...) => ...g-value...))

(fact "key-value pairs can be passed to override normal behavior"
  (always-one 3) => 3 :expected-result 1)

(defn a-fun [n] n)
; http://github.com/marick/Midje/issues/#issue/5
(doseq [v (range 5)]
  (fact
    (str "It should be the identity for value " v) ;; doc string needn't be constant
    (a-fun v) => v))

; http://github.com/marick/Midje/issues/#issue/2
(fact
  (expect (always-one 5) => 1
          (not-called a-fun)))

(binding [midje.semi-sweet/*include-midje-checks* false]
  (load "sweet_compile_out"))

  
;; It doesn't matter which namespace the => is in
(after-silently 
 (fact (+ 1 1) midje.semi-sweet/=> 3)
 (fact @reported => (one-of bad-result)))

(after-silently 
 (fact (+ 1 1) midje.sweet/=> 3)
 (fact @reported => (one-of bad-result)))

(after-silently
 (fact (+ 1 2) =not=> 599)
 (fact @reported => (one-of pass)))

(after-silently
 (fact (+ 1 2) =not=> 3)
 (fact @reported => (one-of inappropriate-equality)))

(after-silently
 (fact (+ 1 2) =not=> even?)
 (fact @reported => (one-of pass)))

(after-silently
 (fact (+ 1 2) =not=> odd?)
 (fact @reported => (one-of inappropriate-checker)))

;; fact and future-fact descriptions nest themselves when reported

(after-silently
  (facts "A"
    (fact "B"
      (+ 1 2) => 1))
  (fact @reported => (one-of (contains {:description ["A" "B"]} ))))

(after-silently
  (facts "level 1"
    (fact "level 2"
      (fact "level 3"
        (throw (Exception. "BOOM")) => anything)))
  (fact @reported => (one-of (contains {:description ["level 1" "level 2" "level 3"]} )))) 

(after-silently
  (facts "about mathemtics"
    (future-fact "do in future"
      nil => 1))
  (fact @reported => (one-of (contains {:description ["about mathemtics" "do in future"]} ))))

;; Background prerequisites
(unfinished check-f check-g check-h)
(defn ander [n]
  (and (check-f n) (check-g n) (check-h n)))

(after-silently
 (against-background [(check-f 1) => true, (check-g 1) => true, (check-h 1) => true]
   (facts
     (ander 1) => truthy
     (ander 1) => falsey (provided (check-f 1) => false)
     (ander 1) => falsey (provided (check-g 1) => false)
     (ander 1) => falsey (provided (check-h 1) => false)))
 (fact @reported => (four-of pass)))


(unfinished h g i j)


(defn f [n] (+ (g (h n)) (i (h n))))

(fact
 (f 1) => 3
 (provided
   (g (h 1)) => 2
   (i (h 1)) => 1))
  
(defn nesty [n] (g (h (i (j n)))))
(fact
 (nesty 1) => 3
 (provided
   (g (h (i (j 1)))) => 3))

(defn second-arg [a b] (+ a (f a (g b))))
(fact
 (second-arg 1 2) => 9
 (provided
   (f 1 (g 2)) => 8))


(unfinished scope-to-fact)
(defn g [x] (scope-to-fact))

(facts
  (against-background (scope-to-fact) => 5)
  (g 1) => 5
  (let [result (g 1)] result => 5))    ;; This used to fail

(against-background [(scope-to-fact) => 5]
  (facts
    (g 1) => 5
    (let [result (g 1)] result => 5)))    ;; This used to fail

(background (scope-to-fact) => 5)
(facts 
  (g 1) => 5
  (let [result (g 1)] result => 5)    ;; This used to fail
)

(background (scope-to-fact) => "outer")

(fact "fakes can be overridden"
  (against-background (scope-to-fact) => "middle")
  (g 1) => "middle"
  (g 1) => "inner"
  (provided
    (scope-to-fact) => "inner"))



(unfinished called)

(after-silently 
 (fact
   (called 1) => 1
   (provided
     (called 1) => 1 :times 2))
 (fact @reported => (contains (contains {:type :mock-incorrect-call-count
                                         :failures (contains (contains {:actual-count 1}))}))))
  
(after-silently
 (fact
   (called 1) => 1
   (provided
     (called 1) => 1 :times (range 2 8)))
 (fact @reported => (contains (contains {:type :mock-incorrect-call-count
                                         :failures (contains (contains {:actual-count 1}))}))))
 

(after-silently
 (fact
   (do (called 1) (called 1) (called 1)) => 1
   (provided
     (called 1) => 1 :times even?))
 (fact @reported => (contains (contains {:type :mock-incorrect-call-count
                                         :failures (contains (contains {:actual-count 3}))}))))
  
(fact
  (do (called 1) (called 1)) => 1
  (provided
    (called 1) => 1))




;; Possibly the most common case
(after-silently
 (fact
   (called 45) => 3
   (provided
     (called irrelevant) => 1 :times 0))
 (fact @reported => (contains (contains {:type :mock-incorrect-call-count
                                         :failures (contains (contains {:actual-count 1}))}))))
    

(def #^:dynamic *fact-retval* (fact
                               (+ 1 1) => 2
                               "some random return value"))
(fact "fact returns true on success"
   *fact-retval* => true)


(def #^:dynamic  *fact-retval* (fact
                                (+ 1 1) => 2
                                (midje.ideas.reporting.report/note-failure-in-fact)
                                "some random return value"))
(fact "fact returns false on failure"
  *fact-retval* => false)


(def #^:dynamic *fact-retval* (fact
                               (+ 1 1) => 2
                               "some random return value"))
(fact "a fact's return value is not affected by previous failures"
  *fact-retval* => true)

(defn a [])
(defn b [] (a))

(fact "prerequisites can throw throwables"
  (b) => (throws Exception)
  (provided 
    (a) =throws=> (Exception.)))


;; Tool creators can hook into the maps generated by the Midje compilation process

(unfinished foo)
(defn-call-countable noop-fn [& args] :do-nothing)
(binding [midje.semi-sweet/*expect-checking-fn* noop-fn]
  (fact :ignored => :ignored
    (provided
      (foo) => :bar)))
(fact @noop-fn-count => 1)


;; In prerequisites, functions can be referred to by vars as well as symbols

;;; These functions are duplicated in t_fakes.clj, since the whole
;;; point of allowing vars is to refer to private vars in another
;;; namespace. To make sure there's no mistakes, these local versions
;;; are so tagged.
;;; 
(defn var-inc-local [x] (inc x))
(defn var-inc-user-local [x] (* x (var-inc-local x)))
(defn var-twice-local []
  (var-inc-local (var-inc-local 2)))

(fact "can fake private remote-namespace functions using vars"
  (#'midje.internal-ideas.t-fakes/var-inc-user 2) => 400
  (provided
    (#'midje.internal-ideas.t-fakes/var-inc 2) => 200))

(fact "and can fake local functions using vars"
  (#'var-inc-user-local 2) => 400
  (provided
    (#'var-inc-local 2) => 200))

(fact "default prerequisites work with vars"
  (binding [midje.config/*allow-default-prerequisites* true]
    (#'midje.internal-ideas.t-fakes/var-twice) => 201
    (provided
      (#'midje.internal-ideas.t-fakes/var-inc 2) => 200)))

;;; Unfolded prerequisites

(fact "vars also work with unfolded prerequisites"
  (var-twice-local) => 201
  (provided
   (var-inc-local (var-inc-local 2))  => 201))

(defn here-and-there []
  (var-inc-local (#'midje.internal-ideas.t-fakes/var-inc 2)))
  
(fact "vars also work with unfolded prerequisites"
  (here-and-there) => 201
  (provided
   (var-inc-local (#'midje.internal-ideas.t-fakes/var-inc 2)) => 201))

(defn there-and-here []
  (#'midje.internal-ideas.t-fakes/var-inc (var-inc-local 2)))
  
(fact "vars also work with unfolded prerequisites"
  (there-and-here) => 201
  (provided
   (#'midje.internal-ideas.t-fakes/var-inc (var-inc-local 2)) => 201))

(defn over-there-over-there-spread-the-word-to-beware []
  (#'midje.internal-ideas.t-fakes/var-inc
   (#'midje.internal-ideas.t-fakes/var-inc 2)))
  
(fact "vars also work with unfolded prerequisites"
  (over-there-over-there-spread-the-word-to-beware) => 201
  (provided
   (#'midje.internal-ideas.t-fakes/var-inc
    (#'midje.internal-ideas.t-fakes/var-inc 2)) => 201))

(after-silently
 (future-fact "exceptions do not blow up"
   "foo" => odd?)
 (future-fact @reported => (just bad-result)))
   
