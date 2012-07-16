(ns behaviors.t-error-handling-line-numbers
  (:use [midje sweet test-util]
        [midje.error-handling.validation-errors]))

;; Different kinds of errors in prerequisites

(defn this-file [line]
  (contains {:position (just [#"t_error_handling_line_numbers.clj", line])}))

(unfinished f)
(after-silently
 (fact (f) => 3 (provided ...movie... => (exactly odd?)))
 (fact @reported => (just (this-file 12))))

(after-silently
 (expect (f) => 3 (fake ...movie... => (exactly odd?)))
 (fact @reported => (just (this-file 16))))

(after-silently
 (fake ...movie... => 3)
 (fact @reported => (just (this-file 20))))

;; Different kinds of errors in facts.

(after-silently 
 (fact (f) =>)
 (fact @reported => (just (this-file 26))))
 
