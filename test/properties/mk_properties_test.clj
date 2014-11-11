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

(fact "source is preferred over environ"
  (let [src {:java.version "src"}
        obj (mk-properties environ-property :source src)]
    (sys-string obj) => "src"))

(fact "command line is most preferred"
  (let [src {:java.version "src"}
        obj (mk-properties environ-property :source src :argv ["java.version=cl"])]
    (sys-string obj) => "cl"))


(defprotocol prefixed-property
  (^{:property "version"}
   sys-string2 [_])

  (^{:property "src-base"}
   src-string2 [_])

  (^{:property "java-cl"}
   cl-string2 [_]))

(facts "prefix is handled properly"
  (let [src {:java.src-base "src"}
        obj (mk-properties prefixed-property :source src :argv ["java-cl=cl"] :prefix :java)]
    (fact "prefix is removed from environ property"
      (sys-string2 obj) => (System/getProperty "java.version"))
    (fact "prefix is removed from source property"
      (src-string2 obj) => "src")
    (fact "prefix is not removed from command line property"
      (cl-string2 obj) => "cl")))
