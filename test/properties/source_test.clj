(ns properties.source-test
  (:use [midje.sweet]
        [properties.core])
  (:require [clojure.java.io :as io])
  (:import [clojure.lang BigInt]
           [java.net URL]
           [java.util Properties]))


(defprotocol property-str
  (^{:property "string"}
   ^String str-property-string [_])

  (^{:property "boolean"}
   ^boolean str-property-boolean [_])

  (^{:property "int"}
   ^BigInt str-property-int [_])

  (^{:property "url"}
   ^URL str-property-url [_]))

(facts "a source map can be used to instantiate a property object where keys are keywords"
  (let [src {:string  "string"
             :boolean true
             :int     42
             :url     (io/as-url "http://localhost")}
        obj (mk-from-source property-str src)]
    (fact "a string property can be retrieved from a source map"
      (str-property-string obj) => (:string src))
    (fact "a boolean property can be retrieved from a source map"
      (str-property-boolean obj) => (:boolean src))
    (fact "an int property can be retrieved from a source map"
      (str-property-int obj) => (:int src))
    (fact "a URL property can be retrieved from a source map"
      (str-property-url obj) => (:url src))))

(facts "a source map can be used to instantiate a property object where keys are strings"
  (let [src {"string"  "string"
             "boolean" true
             "int"     42
             "url"     (io/as-url "http://localhost")}
        obj (mk-from-source property-str src)]
    (fact "a string property can be retrieved from a source map"
      (str-property-string obj) => (get src "string"))
    (fact "a boolean property can be retrieved from a source map"
      (str-property-boolean obj) => (get src "boolean"))
    (fact "an int property can be retrieved from a source map"
      (str-property-int obj) => (get src "int"))
    (fact "a URL property can be retrieved from a source map"
      (str-property-url obj) => (get src "url"))))


(defprotocol property-key
  (^{:property :string}
   ^String key-property-string [_])

  (^{:property :boolean}
   ^boolean key-property-boolean [_])

  (^{:property :int}
   ^BigInt key-property-int [_])

  (^{:property :url}
   ^URL key-property-url [_]))

(facts "a source map can be used to instantiate a property object where properties are keywords"
  (let [src {:string  "string"
             :boolean true
             :int     42
             :url     (io/as-url "http://localhost")}
        obj (mk-from-source property-key src)]
    (fact "a string property can be retrieved from a source map"
      (key-property-string obj) => (:string src))
    (fact "a boolean property can be retrieved from a source map"
      (key-property-boolean obj) => (:boolean src))
    (fact "an int property can be retrieved from a source map"
      (key-property-int obj) => (:int src))
    (fact "a URL property can be retrieved from a source map"
      (key-property-url obj) => (:url src))))


(defprotocol properties
  (^{:property "string"}
   ^String property-string [_])
  (^{:property "boolean"}
   ^boolean property-boolean [_])
  (^{:property "int"}
   ^BigInt property-int [_])
  (^{:property "url"}
   ^URL property-url [_]))

(facts "a java.util.Properties object can be used to instantiate a property object"
  (let [src (doto (Properties.)
              (.setProperty "string" "string")
              (.setProperty "boolean" "true")
              (.setProperty "int" "4")
              (.setProperty "url" "http://localhost"))
        obj (mk-from-source properties src)]
    (fact "a string value is correct"
      (property-string obj) => "string")
    (fact "a boolean value is correct"
      (property-boolean obj) => true)
    (fact "an int value is correct"
      (property-int obj) => 4)
    (fact "a URL value is correct"
      (property-url obj) => (io/as-url "http://localhost"))))

(fact "a property object can be instantiated from something clojure.java.io/reader can resolve"
  (let [src (char-array "string = string")
        obj (mk-from-source properties src)]
    (property-string obj) => "string"))


(defprotocol mixed
  (^{:property "source" :default "default"}
   ^String source [_])

  (^{:property "no-source" :default "default"}
   ^String no-source [_])

  (^{:default "default"}
   ^String no-property [_]))

(facts "defaulting is handled corrected with source"
  (let [src {:source      "source"
             :no-property "source"
             :other       "other"}
        obj (mk-from-source mixed src)]
    (fact "source is preferred to default"
      (source obj) => "source")
    (fact "default is used when source missing property"
      (no-source obj) => "default")
    (fact "default is used when function has no associated property"
      (no-property obj) => "default")))


(defprotocol base-properties
  (^{:property :prefixed :default "default"}
   prefixed [_])

  (^{:property :unprefixed :default "default"}
   unprefixed [_]))

(facts "prefixes are correctly removed"
  (let [src {:prefix.prefixed "source" :unprefixed "source"}
        obj (mk-from-source base-properties src :prefix)]
    (fact "a prefixed property name will have the prefix removed"
      (prefixed obj) => "source")
    (fact "an unprefixed property will use the default"
      (unprefixed obj) => "default")))
