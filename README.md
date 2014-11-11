# properties

This library is intended to unify property determination into a single interface
that supports but does not require type checking and validation. It currently
supports retrieving configuration values from Java properties files, Java system
properties and environment variables.


## example

```clojure
(defprotocol example-properties

  (^{:property "string-1" :default "default value" :validator #(< (count %) 64)})
   ^String fully-annotated-string [_]
   "This example shows all of the metadata that can be used to with a property.
    :property provides the name of field in the properties source. :default
    provides a default value if the property cannot be found in another source.
    :validator provides a predicate for validating the property value. ^String
    indicates that the property will be interpreted as a string. Finally, this
    paragraph forms the documentation for the property value.")

  (^{:property "string-2"}
   string [_]
   "This example shows that most of the metadata don't need to be provided. If
    :default isn't provided, an implicit default will be used. For strings, the
    implicit default is the empty string, \"\". If :validator isn't provided, no
    validation will be performed.  If a type hint (tag) isn't provided, the
    property will be stored in a string.

    The next example demonstrates that none of the metadata are required. If
    :property isn't provided, no source other than the provided metadata and
    their implicit values will be used. The doc string isn't required either.")

  (minimal-string [_])

  (^BigInt integer [_]
   "This is an example of an integer property declaration. The implicit default
    value of an integer is zero (0).")

  (^Boolean boolean [_]
   "This is an example of a Boolean property declaration. The implicit default
    value of a Boolean is false.")

  (^URL url [_]
   "This is an example of a URL (java.net/URL) property declaration. The
    implicit default value of a URL is nil."))
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
