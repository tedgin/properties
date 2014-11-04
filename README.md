# properties

This library is designed to make working with java properties files a little easier.

## Generated Code

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

 * implicit defaults
 * defaults
 * environment variables
 * property file
 * command line arguments
