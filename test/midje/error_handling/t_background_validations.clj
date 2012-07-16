(ns midje.error-handling.t_background_validations
  (:require [clojure.zip :as zip])
  (:use [midje sweet test-util]
        [midje.internal-ideas.wrapping :only [for-wrapping-target?]]
        [midje.util unify]
        [midje.error-handling validation-errors]
        [midje.error-handling.background-validations]))

(tabular
  (facts "before, after and around validation"
    (fact "valid, then return rest of form"
      (validate (cons ?wrapper `(:facts (do "something")))) => `(:facts (do "something")))
  
    (fact "wrapper's must use either :facts, :contents, or checks as their wrapping targets"
      (validate (cons ?wrapper `(:abc (do "something")))) => validation-error-form?)
    
    (fact "correct form length" 
      (validate (cons ?wrapper `(:facts (do "something") (do "another thing")))) => validation-error-form?
      (validate (list ?wrapper)) => validation-error-form? ))

    ?wrapper
    'before 
    'after  
    'around)

(fact "before gets an optional :after param"
  (validate `(before :contents (do "something") :after (do "another thing"))) =not=> validation-error-form?
  (validate `(before :contents (do "something") :around (do "another thing"))) => validation-error-form?)

(fact "after and around don't get extra params - length should be 3"
  (validate `(after :contents (do "something") :after (do "another thing"))) => validation-error-form?
  (validate `(around :contents (do "something") :after (do "another thing"))) => validation-error-form?)

(facts "against-background validation"

  (fact "valid, then return rest of form"
    (validate `(against-background [(before :contents (do "something")) 
                                    (after :checks (do "something"))]
                 "body")) => `([(before :contents (do "something")) 
                                          (after :checks (do "something"))] "body")
  
    (validate `(against-background (before :contents (do "something")) 
                 "body")) 
    => 
    `( (before :contents (do "something")) 
         "body") )
    
  (fact "invalid if any state-description invalid"
    (validate `(against-background [(before :contents (do "something"))
                                    (after :BAD (do "something"))]
                 "body")) => validation-error-form?
    (validate `(against-background (before :BAD (do "something"))
                 "body")) => validation-error-form? ) 
  
  (fact "invalid when the second in form is not state-descriptions and/or bckground fakes" 
    (validate `(against-background :incorrect-type-here "body")) => validation-error-form? )
  
  (fact "invalid when form has less than 3 elements" 
    (validate `(against-background [(before :contents (do "something"))
                                    (after :BAD (do "something"))])) => validation-error-form? 
    (validate `(against-background (before :contents (do "something")))) => validation-error-form? ))

(facts "background validation"

  (fact "valid, then return rest of form"
    (validate `(background (before :contents (do "something")) 
                           (after :checks (do "something")))) 
    
    => `( (before :contents (do "something")) 
          (after :checks (do "something")))
  
    (validate `(background (before :contents (do "something")))) 
    => 
    `( (before :contents (do "something"))))
    
  (fact "invalid if any state-description invalid"
    (validate `(background (before :contents (do "something"))
                           (after :BAD (do "something")))) => validation-error-form?
    (validate `(background (before :BAD (do "something")))) => validation-error-form? ) )  


;;;; Validation end-to-end facts

(each-causes-validation-error #"second element \(:invalid-wrapping-target\) should be one of: :facts, :contents, or :checks"
  (against-background [(before :invalid-wrapping-target (do "something"))] 
    "body")

  (against-background (before :invalid-wrapping-target (do "something"))
    "body")

  (background (before :invalid-wrapping-target (do "something"))))

(defn f [])
(each-causes-validation-error #"Badly formatted against-background fakes"

  ;; check for vectors w/ no state-descriptions or background fakes
  (against-background [:not-a-state-description-or-fake]
    (fact nil => nil))

  ;; check for vectors w/ one thing that isn't a state-description or background fake
  (against-background [(before :contents (do "something")) (f) => 5 :other-odd-stuff]
    (fact nil => nil)))

(each-causes-validation-error #"Badly formatted background fakes"

  ;; invalid when anything doesn't look like a state-description or background fake
  (background (before :contents (do "something"))
    (:not-a-state-description-or-fake))

  ;; invalid when one thing isn't a state-description or background fake
  (background :invalid-stuff-here))


;; invalid if missing background fakes or state descriptions 
(each-causes-validation-error #"You didn't enter any background fakes or wrappers"
  (against-background []
    (fact nil => nil))

  (background))

;; invalid when list w/ no state-descriptions or background fakes
(after-silently
  (against-background (:not-a-state-description-or-fake)
    (fact nil => nil))

  (fact 
    @reported =future=> (one-of (contains {:type :validation-error}))))

; check for one thing that isn't a state-description or background fake
(causes-validation-error #"at least one background fake or background wrapper"
  (against-background :invalid-stuff-here
    (fact nil => nil)))

;; invalid if missing background fakes or state descriptions 
(causes-validation-error #"need a minimum of three elements to an against-background form"
  (against-background
    (fact nil => nil)))