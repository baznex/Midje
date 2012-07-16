(ns midje.checkers.util
  (:use [midje.util.form-utils :only [classic-map?]]
        [midje.util.object-utils :only [name-object]]))

(defn named-as-call
  "Adds a string name that looks like a function call to
   a function's metadata under :name"
  [name expected function]
  (name-object function (format "(%s %s)" name expected)))