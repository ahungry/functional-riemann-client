;; cider-connect localhost 8202 (nrepl)
;; (shadow.cljs.devtools.api/node-repl :dev)
;; (require 'fnrc.core)
;; (fnrc.core/foo)
;; :cljs/quit

(ns fnrc.core
  (:use-macros
   [cljs.core.async.macros :only [go]])
  (:require
   [clojure.repl :as r]
   [clojure.test :as t]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.spec.test.alpha :as stest]
   [cljs.nodejs :as node]
   [cljs.core.async :refer [<! timeout]]
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

(defn obj->clj
  [obj]
  (if (goog.isObject obj)
    (-> (fn [result key]
          (let [v (goog.object/get obj key)]
            (if (= "function" (goog/typeOf v))
              result
              (assoc result key (obj->clj v)))))
        (reduce {} (.getKeys goog/object obj)))
    obj))

(defn serialize [type obj]
  (let [mtype (-> @schema (.lookupType type))
        message (create mtype obj)
        buffer (to-buffer mtype message)]
    buffer))

(defn deserialize [type message]
  (let [mtype (-> @schema (.lookupType type))
        buffer (Buffer/from message "binary")
        m (-> mtype (.decode buffer))]
    m))

(def serialize-event (partial fnrc.core/serialize "Event"))
(def deserialize-event (partial fnrc.core/deserialize "Event"))

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
  (js/Promise.
   (fn [resolve reject]
     (->
      (load-schema)
      (.then resolve)))))

(defn init []
  (println "Hello world"))

(t/deftest a-test
  (t/testing "FIXME, I fail."
    (t/is (= 2 1))))
