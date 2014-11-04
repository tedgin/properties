(ns properties.mk-properties-test
  (:use [midje.sweet]
        [properties.core]))


(defprotocol environ-property
  (^{:property "env-string"}
   project-string [_])

  (^{:property "java.version"}
   sys-string [_])

  (^{:property "PATH"}
   env-string [_]))


(facts "environ integration works"
  (let [obj (mk-properties environ-property)]
    (fact "a property can be retrieved from the project.clj"
      (project-string obj) => "environ")
    (fact "a property can be retrieved from a system property"
      (sys-string obj) => (System/getProperty "java.version"))
    (fact "a property can be retrieved from an environment variable"
      (env-string obj) => (System/getenv "PATH"))))


; TODO test command line argument support
