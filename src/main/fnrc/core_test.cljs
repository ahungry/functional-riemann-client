(ns fnrc.core-test
  (:use-macros
   [cljs.core.async.macros :only [go]])
  (:require
   [fnrc.core :as core]
   [clojure.repl :as r]
   [clojure.test :as t :refer [deftest testing is run-tests]]
   [clojure.spec.alpha :as s]
   [clojure.spec.gen.alpha :as gen]
   [clojure.spec.test.alpha :as stest]
   [cljs.nodejs :as node]
   [cljs.core.async :refer [<! timeout]]
   ["protobufjs" :as proto]))

(defn generate-promise-tests []
  (-> (core/load-schema)
      (.then
       #(deftest load-schema-test []
          (testing "Schema loaded."
            (is (= "Root" (.toString %))))))))

(deftest a-test []
  (testing "Some scenario."
    (is (= 2 2))))

(defn -main []
  (go
    (generate-promise-tests)
    (<! (timeout 100))
    (run-tests)))
