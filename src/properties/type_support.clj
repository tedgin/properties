(ns properties.type-support)


(defmulti implicit-default
  "This multimethod returns the implicit default value for a given type. If the type has no method,
   the multimethod returns nil.

   Parameters:
     prop-type - the type to find the implicit default for

   Returns:
     It returns the implicit default value for the given type. If there isn't one, it returns nil."
  (fn [prop-type] prop-type))

(defmethod implicit-default :default [_] nil)


(defmulti ^Boolean type?
  "This multimethod acts as a predicate to detect if a given value has a given type. If the type
   has no method, the result will check the type of the value to see if it is a subtype of the
   dispatch type.

   Parameters:
     prop-type - the type the value should have
     value     - the value being tested.

   Returns:
     It returns true, if the value is of the correct type, otherwise false."
  (fn [prop-type value] prop-type))

(defmethod type? :default [prop-type value] (isa? (type value) prop-type))


(defmulti from-str
  "Given a type, this multimethod converts a string to a value of that type. If the type has no
   method, the result the identity function will be returned.

   Parameters:
     prop-type - the type used to dispatch on
     str-val   - the string to parse

   Returns:
     It returns the value from the string."
  (fn [prop-type ^String str-val] prop-type))

(defmethod from-str :default [_ str-val] str-val)


(defmulti as-code
  "Given a value, this multimethod will prepare this value for runtime code generation. The
   multimethod dispatches on the type of the value. If the type has no method, the value will be
   returned unaltered.

   Parameters:
     value - the value to encode

   Returns:
     The encoded value is returned."
  (fn [value] (type value)))

(defmethod as-code :default [value] value)
