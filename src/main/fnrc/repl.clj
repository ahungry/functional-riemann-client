(ns repl.core
  (:require
   [shadow.cljs.devtools.api :as shadow ]))

;; use :cljs/quit to get back here
(defn go []
  (shadow/node-repl :dev)
  (require 'fnrc.core)
  (fnrc.core/foo))
