;; cider-connect localhost 8202 (nrepl)
;; (shadow.cljs.devtools.api/node-repl :dev)
;; (require 'fnrc.core)
;; (fnrc.core/foo)
;; :cljs/quit

(ns fnrc.core
  (:require
   [clojure.repl :as r]
   [cljs.nodejs :as node]
   ["protobufjs" :as proto]))

(enable-console-print!)

(def schema (atom nil))

(defn load-schema []
  (->
   (proto/load (str js/__dirname "/src/main/fnrc/proto.proto"))
   (.then #(reset! schema %))
   (.catch #(reset! schema (.toString %)))))

(defn load-schema-cb []
  (proto/load (str js/__dirname "/src/main/fnrc/proto.proto")
              (fn [err root]
                (if err
                  (reset! schema (.toString err))
                  (reset! schema root)))))

(defn lookup-type [type]
  (-> @schema (.lookupType type)))

(defn verify [type m]
  (-> (lookup-type type) (.verify m)))

(defn is-valid? [type m]
  (not (verify type m)))

(defn create [mtype m]
  (-> mtype (.create m)))

(defn to-buffer [mtype message]
  (-> mtype (.encode message) (.finish)))

(defn to-object [type message]
  (let [mtype (-> @schema (.lookupType type))]
    (-> mtype (.toObject message))))

(defn map-to-js [m]
  (apply js-obj (apply concat (seq m))))

(defn serialize [type m]
  (let [mtype (-> @schema (.lookupType type))
        message (create mtype (map-to-js m))
        buffer (to-buffer mtype message)]
    buffer))

(defn deserialize [type message]
  (let [mtype (-> @schema (.lookupType type))
        buffer (Buffer/from message "binary")
        m (-> mtype (.decode buffer))]
    m))

(defn test-event []
  (->>
   {"metricF" 32
    "time" 44}
   (serialize "Event")
   (deserialize "Event")))

(def test1 #js {:metricF "fake" :bar false})

(defn test-verify []
  (verify "Event" test1))

(defn ^:export foo []
  (js/console.log "HIYAaaa")
  (println "FOO"))

(defn init []
  (println "Hello world"))
