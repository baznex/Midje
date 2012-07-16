(ns ^{:doc "Checkers are for checking results of expectations, or checking 
            that appropriate arguments are passed to prerequisites"} 
  midje.checkers
  (:use [midje.util.thread-safe-var-nesting :only [var-root]]))

(letfn [(republish [namespace symbols]
          (require namespace)
          (doseq [sym symbols]
            (let [^clojure.lang.Var var ((ns-publics namespace) sym)]
              (let [sym (with-meta sym (assoc (meta var) :ns *ns*))]
                (if (.hasRoot var)
                  (intern *ns* sym (var-root var))
                  (intern *ns* sym)))))) ]

  (republish 'midje.checkers.defining
             '[defchecker checker as-checker])
  (republish 'midje.checkers.chatty
             '[chatty-checker])
  (republish 'midje.checkers.simple
             '[truthy falsey TRUTHY FALSEY anything irrelevant exactly throws roughly])
  (republish 'midje.checkers.combining
             '[every-checker some-checker])
  (republish 'midje.checkers.collection
             '[has has-suffix has-prefix just contains n-of
              one-of two-of three-of four-of five-of six-of seven-of eight-of nine-of ten-of])
  (republish 'midje.checkers.deprecated
             '[map-containing only-maps-containing maps-containing in-any-order]))
