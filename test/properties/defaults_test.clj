(ns properties.defaults-test
  (:use [midje.sweet]
        [properties.core])
  (:require [clojure.java.io :as io])
  (:import [java.net URL]))


(defprotocol ImplicitType
  (implicit-str [_]))

(fact "a property without type hint is assumed to be a string"
  (implicit-str (->default ImplicitType)) => string?)


(defprotocol ImplicitDefaults
  (^String implicit-default-string [_])
  (^boolean implicit-default-bool [_])
  (^int implicit-default-int [_])
  (^URL implicit-default-url [_]))

(facts "about implicit default values"
  (let [props (->default ImplicitDefaults)]
    (fact "the implicit default for a string property is the empty string"
      (implicit-default-string props) => "")
    (fact "the implicit default for a boolean property is false"
      (implicit-default-bool props) => false)
    (fact "the implicit default for an int property is 0"
      (implicit-default-int props) => 0)
    (fact "the implicit default for a URL is nil"
      (implicit-default-url props) => nil)))


(defprotocol GoodDefaults
  (^{:default "good"}
   ^String good-string [_])

  (^{:default true}
   ^boolean good-bool [_])

  (^{:default 42}
   ^int good-int [_])

  (^{:default (io/as-url "http://localhost")}
   ^URL good-url [_]))

(facts "default values are correct"
  (let [props (->default GoodDefaults)]
    (fact "string has correct default"
      (good-string props) => "good")
    (fact "boolean has correct default"
      (good-bool props) => true)
    (fact "int has correct default"
      (good-int props) => 42)
    (fact "URL has correct default"
      (good-url props) => (io/as-url "http://localhost"))))


(defprotocol BadStringDefault
  (^{:default :bad}
   ^String bad-string [_]))

(fact
  "a protocol with a string function having a default value that isn't a string will throw an exception on instantiation."
  (->default BadStringDefault) => (throws Throwable))


(defprotocol BadBoolDefault
  (^{:default :bad}
   ^boolean bad-boolean [_]))

(fact
  "a protocol with a boolean function having a default value that isn't a boolean will throw an exception on instantiation."
  (->default BadBoolDefault) => (throws Throwable))


(defprotocol BadIntDefault
  (^{:default :bad}
   ^int bad-int [_]))

(fact
  "a protocol with an int function having a default value that isn't an int will throw an exception on instantiation."
  (->default BadIntDefault) => (throws Throwable))


(defprotocol BadURLDefault
  (^{:default :bad}
   ^URL bad-url [_]))

(fact
  "a protocol with a URL function having a default value that isn't a URL will throw an exception on instantiation."
  (->default BadURLDefault) => (throws Throwable))