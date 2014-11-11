# properties

This library is intended to unify property determination into a single interface
that supports but does not require type checking and validation. It currently
supports retrieving configuration values from command line arguments,
environment variables, JVM system properties, and Java system properties.


## example.clj

```clojure
(ns example
  :require [properties.core :as prop]
  :import  [clojure.lang BigInt]
           [java.net URL])


(defprotocol example-properties
  "Here is an example of declaring a set of property values."

  (^{:property "string-1" :default "default value" :validator #(< (count %) 64)})
   ^String fully-annotated-string [_]
   "This example shows all of the metadata that can be used to with a property.
    :property provides the name of field in the properties source. :default
    provides a default value if the property cannot be found in another source.
    :validator provides a predicate for validating the property value. ^String
    indicates that the property will be interpreted as a string.")

  (^{:property "string-2"}
   string [_]
   "This example shows that most of the metadata don't need to be provided. If
    :default isn't provided, an implicit default will be used. For strings, the
    implicit default is the empty string, \"\". If :validator isn't provided, no
    validation will be performed.  If a type hint (tag) isn't provided, the
    property will be stored in a string.

    The next example demonstrates that none of the metadata are required. If
    :property isn't provided, no source other than the provided metadata and
    their implicit values will be used.")

  (unannotated-string [_])

  (^BigInt integer [_]
   "This is an example of an integer property declaration. The implicit default
    value of an integer is zero (0).")

  (^Boolean bool [_]
   "This is an example of a Boolean property declaration. The implicit default
    value of a Boolean is false.")

  (^URL url [_]
   "This is an example of a URL (java.net/URL) property declaration. The
    implicit default value of a URL is nil."))


(defn ->default
  "This function instantiates example-properties with each property has its
   default value."
  []
  (props/mk-default example-properties))


(defn ->from-environ
  "Underneath, this library uses weavejester's environ library to extract the
   properties that come from the environment variables and JVM system
   properties. This function instantiates example-properties using only these
   two sources."
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
   :source keyword parameter. The source may be a map, a java.util/Properties
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
```

*TODO complete documentation*

## Defaults
It supports default values, so that the properties file only needs the values that have been customized. Here is an example of a default.

```clojure
(defprotocol MyProperties

 (^{:property "subsystem.property" :default 5}
  ^BigInt subsystem-property [_]))
```

### Implicit Defaults

*TODO complete section*

## Property Precedence

 * LOWEST
 * implicit defaults
 * defaults
 * environment variables
 * property file
 * command line arguments
 * HIGHEST
