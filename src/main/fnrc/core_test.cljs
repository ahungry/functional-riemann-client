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

;; https://stackoverflow.com/questions/40697841/howto-include-clojure-specd-functions-in-a-test-suite
(defmacro defspec-test
  ([name sym-or-syms] `(defspec-test ~name ~sym-or-syms nil))
  ([name sym-or-syms opts]
   (when t/*load-tests*
     `(def ~(vary-meta name assoc
                       :test `(fn []
                                (let [check-results# (clojure.spec.test.alpha/check ~sym-or-syms ~opts)
                                      checks-passed?# (every? nil? (map :failure check-results#))]
                                  (if checks-passed?#
                                    (t/do-report {:type    :pass
                                                  :message (str "Generative tests pass for "
                                                                (str/join ", " (map :sym check-results#)))})
                                    (doseq [failed-check# (filter :failure check-results#)
                                            :let [r# (clojure.spec.test.alpha/abbrev-result failed-check#)
                                                  failure# (:failure r#)]]
                                      (t/do-report
                                       {:type     :fail
                                        :message  (with-out-str (clojure.spec.alpha/explain-out failure#))
                                        :expected (->> r# :spec rest (apply hash-map) :ret)
                                        :actual   (if (instance? Throwable failure#)
                                                    failure#
                                                    (:clojure.spec.test.alpha/val failure#))})))
                                  checks-passed?#)))
        (fn [] (t/test-var (var ~name)))))))

;; (defspec-test test-addition [blub.core/addition])

(defn generate-promise-tests []
  (-> (core/load-schema)
      (.then
       #(deftest load-schema-test []
          (testing "Schema loaded."
            (is (= "Root" (.toString %))))))))

(def stub-event #js {:time 44 :metricF 32})

(deftest a-test []
  (testing "Serial and Deserialize works"
    (let [res (->> stub-event
                   core/serialize-event
                   core/deserialize-event
                   (core/to-object "Event")
                   js->clj)]
      (is (= 44 (int (get res "time" )))))))

(defn -main []
  (go
    (generate-promise-tests)
    (<! (timeout 100))
    (run-tests)))
