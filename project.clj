(defproject properties "0.0.0"
  :description  "A clojure library that unifies property determination into a single interface that 
                 supports, but does not require, type checking and validation."
  :license      {:name "Public Domain"}
  :url          "https://github.com/tedgin/properties"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [environ "1.0.0"]]
  :plugins      [[lein-environ "1.0.0"]]
  :profiles     {:dev {:dependencies [[midje "1.6.3"]]
                       :env          {:env-string "environ"}}})
