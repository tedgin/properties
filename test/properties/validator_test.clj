(ns properties.validator-test
  (:use [midje.sweet]
        [properties.core]))


(defn- primary-color?
  [color]
  (contains? #{"red" "green" "blue"} color))


(defprotocol validatable

  (^{:property :favorite-color :default "blue" :validator primary-color?}
   ^String favorite-color [_]))


(fact "validation passes when it should"
  (mk-properties validatable) => irrelevant)


(fact "valiation fails when it should"
  (mk-properties validatable :source {:favorite-color "violet"}) => (throws Throwable))
