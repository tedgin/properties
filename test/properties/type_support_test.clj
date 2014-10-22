(ns properties.type-support-test
  (:use [midje.sweet]
        [properties.type-support])
  (:require [clojure.string :as str]
            [properties.core :as prop]))


(defprotocol IsIntPair
  (fst [_])
  (snd [_]))


(deftype IntPair [_1 _2]
  IsIntPair
  (fst [_] _1)
  (snd [_] _2))


(defmethod implicit-default IntPair
  [_]
  (IntPair. 0 0))


(defmethod type? IntPair
  [_ value]
  (isa? (type value) IntPair))


(defmethod from-str IntPair
  [_ str-val]
  (let [[str-1 str-2] (-> str-val
                        str/trim
                        (str/replace #"^\((.*)\)$" "$1")
                        str/trim
                        (str/split #"  *" 2))]
    (IntPair. (Integer/parseInt str-1) (Integer/parseInt str-2))))


(defmethod as-code IntPair
  [pair]
  `(IntPair. ~(fst pair) ~(snd pair)))


(defprotocol CustomType
  (^IntPair implicit [_])

  (^{:default (IntPair. 1 2)}
   ^IntPair default [_])

  (^{:default "(3 4)"}
   ^IntPair str-default [_]))


(facts "new types can be supported"
  (let [obj (prop/->default CustomType)]
    (fact "new type has correct implicit default"
      (let [p (implicit obj)]
        [(fst p) (snd p)] => [0 0]))
    (fact "new type has correct user provided IntPair default"
      (let [p (default obj)]
        [(fst p) (snd p)] => [1 2]))
    (fact "new type has correct user provide string default"
      (let [p (str-default obj)]
        [(fst p) (snd p)] => [3 4]))))
