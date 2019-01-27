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
   ["net" :as net]
   ["protobufjs" :as proto]))

(enable-console-print!)

;;  ___     _
;; / __| __| |_  ___ _ __  __ _
;; \__ \/ _| ' \/ -_) '  \/ _` |
;; |___/\__|_||_\___|_|_|_\__,_|

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

;;   ___                  _
;;  / __|___ _ _ ___ __ _| |
;; | (__/ -_) '_/ -_) _` | |
;;  \___\___|_| \___\__,_|_|

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
(def serialize-message (partial fnrc.core/serialize "Msg"))
(def deserialize-message (partial fnrc.core/deserialize "Msg"))
(def serialize-query (partial fnrc.core/serialize "Query"))
(def deserialize-query (partial fnrc.core/deserialize "Query"))
(def serialize-state (partial fnrc.core/serialize "State"))
(def deserialize-state (partial fnrc.core/deserialize "State"))

;;  _
;; | |_ __ _ __
;; |  _/ _| '_ \
;;  \__\__| .__/
;;        |_|
(def socket-state (atom {:state 1
                         :buffer-offset 0
                         :buffer-length (Buffer/alloc 4)
                         :payload-buffer nil
                         :payload-offset 0}))

(defn get-response-length [chunk]
  (+
   (bit-shift-left (get chunk 0) 24)
   (bit-shift-left (get chunk 1) 16)
   (bit-shift-left (get chunk 2) 8)
   (get chunk 3)
   ))

;; https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Operators/Bitwise_Operators
(defn build-packet [payload]
  (let [len (.-length payload)
        packet (Buffer/alloc (+ 4 len))]
    (aset packet 0 (bit-and (unsigned-bit-shift-right len 24) 0xff))
    (aset packet 1 (bit-and (unsigned-bit-shift-right len 16) 0xff))
    (aset packet 2 (bit-and (unsigned-bit-shift-right len 8) 0xff))
    (aset packet 3 (bit-and (unsigned-bit-shift-right len 0) 0xff))
    (-> payload (.copy packet 4 0))
    packet))

(defn get-reified-socket [socket]
  (reify
    Object
    (send [this payload]
      (let [packet (build-packet payload)]
        (-> socket (.write packet))))
    (onMessage [this emit]
      (let [self this]
        (doto socket
          (.on "data"
               (fn [chunk]
                 (let [chunk-offset 0]
                   (while (< chunk-offset (.length chunk))
                     (case (:state @socket-state)
                       1 (println "Case 1")
                       2 (println "Case 2")))))))))))

(defn ^:export get-tcp-socket [{:keys [host port]}]
  (js/Promise.
   (fn [resolve reject]
    (let [socket (net/Socket)]
      (doto socket
        (.connect port host)
        (.setKeepAlive true 0)
        (.setNoDelay true)
        (.setTimeout 500
                     #(doto socket
                        ;; (.emit "error" (js/Error. "Failure to connect"))
                        .destroy
                        (reject "Failure to connect")))
        (.once "connect"
               #(doto socket
                  (.setTimeout 0))
               (let [ri (get-reified-socket socket)]
                 (println (.-send ri))
                 (resolve ri))))
      ;; (get-reified-socket socket)
      ))))

(def socket (atom nil))
(defn ^:export get-socket! []
  (js/Promise.
   (fn [resolve reject]
     (resolve
      (if @socket @socket
          (reset! socket (get-tcp-socket {:host "localhost" :port 5555})))))))

(def stub-send-event
  #js {:time 44
       :metricF 32
       :host "ahungry.com"
       :service "fake"
       :state "ok"
       :tags #js ["uno" "dos"]
       :ttl 15
       })

(def stub-send-msg
  #js {:ok true
       :events #js [stub-send-event]})

(defn ^:export test-send [sock]
  (-> sock (.send (serialize-message stub-send-msg)))
)
;; Just a jibberish export to test it.
(defn ^:export foo []
  (js/Promise.
   (fn [resolve reject]
     (->
      (load-schema)
      (.then resolve)))))

(defn send-with-socket
  "Send a test event to the listening riemann server."
  []
  (-> (get-socket!)
      (.then test-send)
      (.then prn)))

(defn init []
  (println "Hello world"))
