(ns properties.core
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [environ.core :as env]
            [properties.type-support :as types])
  (:import [clojure.lang BigInt IPersistentMap]
           [java.io Reader]
           [java.math BigInteger]
           [java.net URL]
           [java.util Properties]))


; String support

(defmethod types/implicit-default String  [_] "")

(defmethod types/type? String [_ value] (string? value))


; Boolean support

(defmethod types/implicit-default Boolean [_] false)

(defmethod types/from-str Boolean [_ str-val] (boolean str-val))


; BigInt support

(defmethod types/implicit-default BigInt [_] 0)

(defmethod types/type? BigInt [_ value] (integer? value))

(defmethod types/from-str BigInt [_ str-val] (BigInt/fromBigInteger (BigInteger. str-val)))


; URL support

(defmethod types/from-str URL [_ str-val] (io/as-url str-val))

(defmethod types/as-code URL [url] `(io/as-url ~(str url)))


(defn- map-properties
  [op props]
  (apply hash-map (mapcat op props)))


(defn- determine-type
  [fn-sig]
  (let [type-sym (:tag fn-sig)]
    (cond
      (nil? type-sym)       String
      (= type-sym 'boolean) Boolean
      :else                 (ns-resolve *ns* type-sym))))


(defn- resolve-value
  [sig value prop-type]
  (let [bad #(throw (RuntimeException. (str value " for " (:name sig) " does not have type " prop-type)))]
    (try
      (cond
        (types/type? prop-type value) value
        (string? value)               (types/from-str prop-type value)
        :else                         (bad))
      (catch Throwable _
        (bad)))))


(defn- canonical-name
  [prop-name]
  (when prop-name
    (-> prop-name
      name
      str/lower-case
      (str/replace #"[\._]" "-")
      keyword)))


(defn- property
  [fn-sig]
  (canonical-name (:property fn-sig)))


(defn- determine-code-value
  [sig cfg-map]
  (let [prop-type   (determine-type sig)
        cfg-val     (eval (get cfg-map (property sig)))
        default-val (eval (:default sig))]
    (cond
      cfg-val     (resolve-value sig cfg-val prop-type)
      default-val (resolve-value sig default-val prop-type)
      :else       (types/implicit-default prop-type))))


(defn- sanitize-validator-call
  [validator-sym value]
  (try
    (eval `(~validator-sym ~value))
    (catch Throwable t
      (throw (RuntimeException. (str validator-sym " must be a no-throw predicate that accepts a "
                                     "single argument of type " (type value))
                                t)))))


(defn- validate-value
  [sig value]
  (when-let [validator-sym (:validator sig)]
    (when-not (sanitize-validator-call validator-sym value)
      (throw (RuntimeException. (str value " failed property " (:name sig) " validation.")))))
  value)


(defn- prep-fn
  [sig cfg-map]
  (let [fname (:name sig)
        value (->> (determine-code-value sig cfg-map)
                (validate-value sig)
                types/as-code)]
    `((~fname [_#] ~value))))


(defn- mk
  [protocol cfg-map]
  (let [protoSym (.sym (:var protocol))
        fn-defs  (map #(prep-fn % cfg-map) (vals (:sigs protocol)))]
    (eval (apply concat `(reify ~protoSym) fn-defs))))


(defn- remove-prefix
  [prefix [prop value]]
  (if-not prefix
    [prop value]
    (let [prefix  (name (canonical-name prefix))
          pattern (re-pattern (str "^" prefix "-(.*)$"))]
      (when-let [[_ base] (re-matches pattern (name prop))]
        [(keyword base) value]))))


(defmulti ^:private resolve-source type)

(defmethod resolve-source :default
  [source]
  (with-open [rdr (io/reader source)]
    (into {} (doto (Properties.) (.load rdr)))))

(defmethod resolve-source IPersistentMap
  [properties]
  properties)

(defmethod resolve-source nil
  [_]
  {})

(defmethod resolve-source Properties
  [properties]
  (into {} properties))


(defn- canonical-property
  [[prop-name prop-val]]
  [(canonical-name prop-name) prop-val])


(defn- get-source-props
  [source prefix]
  (letfn [(fmt-prop [prop] (remove-prefix prefix (canonical-property prop)))]
    (map-properties fmt-prop (resolve-source source))))


(defn mk-default
  "Instantiates a properties object that implements the given protocol where each function returns
   its default value.

   Params:
     protocol - the protocol mapping

   Returns:
     It returns the properties object.

   Throws:
     It will throw an exception if any of the default property values cannot be coerced to the
     property type."
  [protocol]
  (mk protocol {}))


(defn mk-from-source
  "Instantiates a properties object that implements the given protocol where the given property
   source is referenced to determine the property values. If the function has :property metadata,
   the source is inspected for that property. If found, the function will use the corresponding
   value. Otherwise, it will use the default value.

   The environment variables and command line arguments are not considered when determining the
   values of the properties.

   Params:
     protocol - the protocol mapping
     source   - a provided source for properties, usually a properties files. It may be a map, a
                `java.util.Properties` object, or anything `clojure.java.io/reader` can resolve.
     prefix   - (OPTIONAL) If this parameter is provided, the source will be filtered for
                properties whose names begin with `<prefix>.`. The dotted prefix will then be
                removed from the property names.

   Returns:
     It returns the properties object.

   Throws:
     It will throw an exception if any of the resolved property values cannot be coerced to the
     property type."
  [protocol source & {:keys [prefix]}]
  (mk protocol (get-source-props source prefix)))


(defn- prefix-name
  [prefix prop-name]
  (if prefix
    (str (name prefix) "." prop-name)
    prop-name))


(defn- lookup-env-prop
  [prefix prop-name]
  (when prop-name
    (let [env-name (canonical-name (prefix-name prefix prop-name))
          value    (env/env env-name)]
      (when value
        (remove-prefix prefix [env-name value])))))


(defn- get-env-props
  [protocol prefix]
  (let [sigs (vals (:sigs protocol))]
    (map-properties #(lookup-env-prop prefix (:property %)) sigs)))


(defn- resolve-arg-prop
  [arg]
  (let [[prop-name prop-val] (str/split arg #"=" 2)]
    (when prop-val
      [(canonical-name prop-name) prop-val])))


(defn- get-cmd-line-props
  [cl-args]
  (map-properties resolve-arg-prop cl-args))


(defn mk-properties
  "Instantiates a properties object that implements the given protocol where JVM system properties,
   environment variables, the given property source, and command line arguments are referenced to
   determine the property values. If a function declared in the protocol has :property metadata, the
   value of the property is found by looking in the potential sources in the following order.

     1) command line arguments
     2) the property source
     3) environment variables
     4) JVM system properties

   The first value found is the value that will be used. If no value is found, the default for the
   property will be used.

   Params:
     protocol - the protocol mapping
     source   - (OPTIONAL) a provided source for properties, usually a properties files. It may be a
                map, a `java.util.Properties` object, or anything `clojure.java.io/reader` can
                resolve.
     argv     - (OPTIONAL) The command line arguments as passed into `-main`.
     prefix   - (OPTIONAL) If this parameter is provided, the JVM system properties, environment
                variables and the source will be filtered for properties whose names begin with
                `<prefix>.`. The dotted prefix will then be removed from the property names.

   Returns:
     It returns the properties object.

   Throws:
     It will throw an exception if any of the resolved property values cannot be coerced to the
     property type."
  [protocol & {:keys [source argv prefix]}]
  (let [env-props      (get-env-props protocol prefix)
        src-props      (get-source-props source prefix)
        cmd-line-props (get-cmd-line-props argv)
        resolved-props (merge env-props src-props cmd-line-props)]
    (mk protocol resolved-props)))
