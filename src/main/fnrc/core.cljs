;; cider-connect localhost 8202 (nrepl)
;; (shadow.cljs.devtools.api/node-repl :dev)
;; (require 'fnrc.core)
;; (fnrc.core/foo)

(ns fnrc.core
  (:require
   [clojure.repl :as r]
   [cljs.nodejs :as node]
   ["protobufjs" :as proto]))

(defn ^:export foo []
  (js/console.log "HIYA")
  (println "FOO"))

(defn init []
  (println "Hello world"))
