(defproject properties "0.0.0"
  :description  "a DRY java properties wrapper with defaults and validation support"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [environ "1.0.0"]]
  :plugins      [[lein-environ "1.0.0"]]
  :profiles     {:dev {:dependencies [[midje "1.6.3"]]
                       :env          {:env-string "environ"}}})
