(ns property.core
  (:require [clojure.java.io :as io])
  (:import [clojure.lang IPersistentMap]
           [java.io Reader]
           [java.net URL]
           [java.util Properties]))


(defn- url?
  [value]
  (try
    (io/as-url value)
    true
    (catch Throwable _ false)))


(defn- to-code
  [prop-type value]
  (when-not (nil? value)
    (case prop-type
      URL (str value)
          value)))


(defn- correct-type?
  [value prop-type]
  (case prop-type
    boolean (isa? Boolean (type value))
    int     (integer? value)
    String  (string? value)
    URL     (url? value)
            true))


(defn- ctor-fn
  [prop-type]
  (case prop-type
    boolean `identity
    int     `identity
    String  `identity
    URL     `io/as-url
            `identity))


(defn- implicit-default
  [prop-type]
  (case prop-type
    boolean false
    int     0
    String  ""
            nil))


(defn- determine-type
  [fn-sig]
  (get fn-sig :tag 'String))


(defn- validate-value
  [value prop-type]
  (if (correct-type? value prop-type)
    value
    (throw (RuntimeException. (str value "does not have type" prop-type)))))


(defn- property
  [fn-sig]
  (when-let [p (:property fn-sig)] (name p)))


(defn- determine-code-value
  [fn-sig cfg-map]
  (let [prop-type   (determine-type fn-sig)
        cfg-val     (get cfg-map (property fn-sig))
        default-val (:default fn-sig)
        valid-val   (cond
                      cfg-val     (validate-value cfg-val prop-type)
                      default-val (validate-value default-val prop-type)
                      :else       (implicit-default prop-type))]
    (to-code prop-type valid-val)))


(defn- prep-fn
  [sig cfg-map]
  (let [fname  (:name sig)
        constr (ctor-fn (determine-type sig))
        value  (determine-code-value sig cfg-map)]
    `((~fname [_#] (~constr ~value)))))


(defn- protocol-from-map
  [protocol cfg-map]
  (let [protoSym (.sym (:var protocol))
        prefix   `(reify ~protoSym)
        fn-defs  (map #(prep-fn % cfg-map) (vals (:sigs protocol)))]
    (eval (apply concat prefix fn-defs))))


(defn- protocol-from-properties
  [protocol props]
  (protocol-from-map protocol (into {} props)))


(defn ->default
  "Instantiates a properties object that implements the given protocol where each function returns
   its default value.

   Params:
     protocol - the protocol mapping

   Returns:
     It returns the properties object."
  [protocol]
  (protocol-from-map protocol {}))


(defmulti ->from
  "Instantiates a properties object that implements the given protocol where the given source is
   referenced for the function return values. If the function has a :property metadata, the source
   is inspected for that property. If found, the function will use the corresponding value.
   Otherwise, it will use the default value.

   Params:
     protocol - the protocol mapping
     source   - the source of the property values. It may be a map, a java.util.Properties object
                or anything clojure.java.io/reader can resolve.

   Returns:
     It returns the properties object."
  (fn [protocol source] (type source)))

(defmethod ->from IPersistentMap
  [protocol properties]
  (letfn [(str-key [[k v]] [(name k) v])]
    (protocol-from-map protocol (apply hash-map (mapcat str-key properties)))))

(defmethod ->from Properties
  [protocol properties]
  (protocol-from-properties protocol properties))

(defmethod ->from :default
  [protocol source]
  (with-open [rdr (io/reader source)]
    (protocol-from-properties protocol (doto (Properties.) (.load rdr)))))
