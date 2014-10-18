(ns properties.core
  (:require [clojure.java.io :as io]
            [properties.type-support :as types])
  (:import [clojure.lang IPersistentMap]
           [java.io Reader]
           [java.net URL]
           [java.util Properties]))


; String support

(defmethod types/implicit-default String  [_] "")

(defmethod types/type? String [_ value] (string? value))


; Boolean support

(defmethod types/implicit-default Boolean [_] false)

(defmethod types/type? Boolean [_ value] (isa? Boolean (type value)))

(defmethod types/from-str-fn Boolean [_] 'boolean)


; Integer support

(defmethod types/implicit-default Integer [_] 0)

(defmethod types/type? Integer [_ value] (integer? value))

(defmethod types/from-str-fn Integer [_] 'Integer/parseInt)


; URL support

(defmethod types/type? URL [_ value]
  (boolean
    (try
      (io/as-url value)
      (catch Throwable _))))

(defmethod types/from-str-fn URL [_] 'clojure.java.io/as-url)

(defmethod types/as-code URL [url] (str url))


(defn- determine-type
  [fn-sig]
  (let [type-sym (:tag fn-sig)]
    (cond
      (nil? type-sym)       String
      (= type-sym 'boolean) Boolean
      (= type-sym 'int)     Integer
      :else                 (ns-resolve *ns* type-sym))))


(defn- validate-value
  [value prop-type]
  (let [bad #(throw (RuntimeException. (str value " does not have type " prop-type)))]
    (if (string? value)
      (try
        ((types/from-str-fn prop-type) value)
        (catch Throwable _
          (bad)))
      (when-not (types/type? prop-type value)
        (bad)))
    value))


(defn- property
  [fn-sig]
  (when-let [p (:property fn-sig)] (name p)))


(defn- determine-code-value
  [fn-sig prop-type cfg-map]
  (let [cfg-val     (eval (get cfg-map (property fn-sig)))
        default-val (eval (:default fn-sig))]
    (cond
      cfg-val     (validate-value cfg-val prop-type)
      default-val (validate-value default-val prop-type)
      :else       (types/implicit-default prop-type))))


(defn- ctor-fn
  [prop-type value]
  (when (string? value)
    (types/from-str-fn prop-type)))


(defn- prep-fn
  [sig cfg-map]
  (let [fname (:name sig)
        ftype (determine-type sig)
        value (types/as-code (determine-code-value sig ftype cfg-map))]
    (if-let [constr (ctor-fn ftype value)]
      `((~fname [_#] (~constr ~value)))
      `((~fname [_#] ~value)))))


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
