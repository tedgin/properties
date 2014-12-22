# properties

Built on top of
[weavejester's environ library](https://github.com/weavejester/environ), this
library unifies property determination into a single interface that supports,
but does not require, type checking and validation. It currently supports
retrieving configuration values from command line arguments, environment
variables, JVM system properties, and Java system properties.


## example.clj

```clojure
(ns example
  (:require [properties.core :as props])
  (:import [clojure.lang BigInt]
           [java.net URL]))

(defprotocol example-properties
  "Here is an example of declaring a set of property values."

  (^{:property "string-1" :default "default value" :validator #(< (count %) 64)})
   ^String fully-annotated-string [_]
   "This example shows all of the metadata that can be used to with a property.
    :property provides the name of field in the properties source. :default
    provides a default value if the property cannot be found in another source.
    :validator provides a predicate for validating the property value. ^String
    indicates that the property will be interpreted as a string.")

  (unannotated-string [_]
   "This example shows that none of the metadata are required. If :property
    isn't provided, no source other than the provided metadata and their
    implicit values will be used. If :default isn't provided, an implicit
    default will be used. For strings, the implicit default is the empty string
    (\"\"). If :validator isn't provided, no validation will be performed. If a
    type hint (tag) isn't provided, the property will be considered a string.")

  (^BigInt integer [_]
   "This is an example of an integer property declaration. The implicit default
    value of an integer is zero (0).")

  (^Boolean bool [_]
   "This is an example of a Boolean property declaration. The implicit default
    value of a Boolean is false.")

  (^URL url [_]
   "This is an example of a URL (java.net.URL) property declaration. The
    implicit default value of a URL is nil."))

(defn ->default
  "This function instantiates example-properties with each property has its
   default value."
  []
  (props/mk-default example-properties))

(defn ->from-environ
  "Underneath, this library uses environ to extract the properties that come
   from the environment variables and JVM system properties. This function
   instantiates example-properties using only these two sources."
  []
  (props/mk-properties example-properties))

(defn ->from-environ-and-cmd-line
  [argv]
  "To override the environ extracted properties with values provided on the
   command line, pass the command line argument vector into mk-properties as
   the :argv keyword parameter."
  (props/mk-properties example-properties :argv argv))

(defn ->from-environ-and-properties
  "To override the environ extracted properties with values provided by another
   source other than the command line, pass the source into mk-properties as the
   :source keyword parameter. The source may be a map, a java.util.Properties
   object, or anything clojure.java.io/reader can parse into a map."
  [src]
  (props/mk-properties example-properties :source src))

(defn ->from-everywhere
  "Of course both the command line and another source may be used to override
   the values provided by environ. In this case, the command line arguments take
   precedence."
  [src argv]
  (props/mk-properties example-properties :source src :argv argv))

(defn ->from-source-only
  "The values extracted from environ need not be used. This function uses values
   taken from a source other than environ or the command line."
  [src]
  (props/mk-from-source example-properties src))

(defn ->with-prefix
  "Properties coming from a source often have a prefix used to namespace the set
   of properties. This prefix can be used as a filter and will resolve to the
   base property name defined by the :prefix metadata value in the property
   definition. For example, if the prefix is `ns-1` the property defined in the
   source as `ns-1-url` would map to the `url` property, and the source property
   `ns-2-bool` would be ignored. The :prefix keyword parameter can be used to
   provide this prefix."
  [prefix]
  (props/mk-properties example-properties :prefix prefix))
```


## Property Names

The convention used by environ is used: All property names are case-insensitive,
with hyphens (`-`), periods (`.`), and underscores (`_`) being treated as
equivalent. For example, `PropertiesExample.my_prop` and
`propertiesexample-my-prop` would refer to the same source property.


## Property Source Precedence

The precedence of the property is determined as follows:

1. command line arguments (_highest_)
1. other source like Java properties file
1. JVM system properties
1. environment variables
1. .lein-env file in project directory
1. defaults (_lowest_)


## Custom Type Support

Custom types may be used with this library. To make a custom type usable by this
library, four multimethods need to be implemented for the type. The methods are
declared in the `properties.type-support` namespace.

The `implicit-default` multimethod returns the default value to use for the
custom type if no `:default` metadata value is provided as part of the
declaration of a property of this custom type. If this multimethod is not
implemented for the type, the implicit default value for properties of this type
will be `nil`.

The `type?` multimethod acts as a predicate to see if a given value has the
custom type. If this multimethod is not implemented for a type, `isa?` will be
used.

The `from-str` multimethod constructs a value of a custom type from its string
representation, the representation found in a Java properties file. If this
multimethod is not implemented for a type, `read-string` will be used.

The `as-code` multimethod prepares a value of a custom type for runtime code
generation. If this multimethod is not implemented for a type, the value will be
returned unaltered.

Here is an example of adding support for the `Byte` class.

```clojure
(ns example-2
  (:require [properties.type-support :as types]
            [properties.core :as props]))

(defmethod types/implicit-default Byte [_]
  (byte 0))

(defmethod types/from-str Byte [_ value]
  (Byte. value))

(defprotocol custom-property-type
  (^{:property "byte"}
   ^Byte byte-prop [_]))

(defn mk
  [src]
  (props/mk-properties custom-property-type :source src))
```
