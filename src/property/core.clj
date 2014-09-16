(ns property.core
  (:require [clojure.java.io :as io])
  (:import [java.net URL]))


(defn- url?
  [value]
  (try
    (io/as-url value)
    true
    (catch Throwable _
      false)))


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
  (if-let [tag (:tag fn-sig)]
    tag
    'String))


(defn- validate-value
  [value prop-type]
  (if (correct-type? value prop-type)
    value
    (throw (RuntimeException. (str value "does not have type" prop-type)))))


(defn- determine-default
  [fn-sig cfg-map]
  (let [cfg-val     (get cfg-map (:property fn-sig))
        default-val (:default fn-sig)
        prop-type   (determine-type fn-sig)]
    (cond
      cfg-val     (validate-value cfg-val prop-type)
      default-val (validate-value default-val prop-type)
      :else       (implicit-default prop-type))))


(defn- prep-fn
  [sig cfg-map]
  (let [fname  (:name sig)
        constr (ctor-fn (determine-type sig))
        value  (determine-default sig cfg-map)]
    `((~fname [_#] (~constr ~value)))))


(defn ->from-map
  [protocol cfg-map]
  (let [protoSym (.sym (:var protocol))
        prefix   `(reify ~protoSym)
        fn-defs  (map #(prep-fn % cfg-map) (vals (:sigs protocol)))]
    (eval (apply concat prefix fn-defs))))


(defn ->default
  "Instantiates an object that implements the given protocol where each function returns its
   default value.

   Params:
     protocol - the protocol mapping

   Returns:
     It returns the object."
  [protocol]
  (->from-map protocol {}))
