(ns property.experiment)

(defprotocol Properties

  (^{:default true}
   log-progress? [_]))


