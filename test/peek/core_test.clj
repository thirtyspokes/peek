(ns peek.core-test
  (:use midje.sweet)
  (:require [peek.core :refer :all]
            [clojure.test :refer :all]
            [clojure.core.async :refer [>!! <!! chan]]
            [peek.datadog :refer [sampled-send send-udp submit]]))

(let [result (atom nil)]    
  (with-redefs [sampled-send (fn [packet sample-rate] (reset! result packet))]
    (fact "time! returns the evaluated body and also emits a packet"
      (time! "test" (+ 1 1)) => 2
      @result => truthy)))

(deftest increment-test
  "API methods use options and defaults to build packets"
  (let [ch (chan)]
    (with-redefs [sampled-send (fn [packet rate] (>!! ch packet))
                  send-udp (fn [packet] (>!! ch packet))]
      (do (increment! "test" :delta 5 :sample 0.75 :tags {:system "test"})
          (is (= (<!! ch) "test:5|c|@0.75|#system:test")))
      (do (increment! "test" :sample 0.75)
          (is (= (<!! ch) "test:1|c|@0.75")))
      (do (increment! "test")
          (is (= (<!! ch) "test:1|c|@1.0")))
      (do (timing! "test" 100 :tags {:system "test"} :sample 0.75)
          (is (= (<!! ch) "test:100|ms|@0.75|#system:test")))
      (do (timing! "test" 100)
          (is (= (<!! ch) "test:100|ms|@1.0")))
      (do (gauge! "test" 5 :sample 0.5)
          (is (= (<!! ch) "test:5|g|@0.5")))
      (do (gauge! "test" 5)
          (is (= (<!! ch) "test:5|g|@1.0")))
      (do (datadog-set! "test" 1 :tags {:system "test"})
          (is (= (<!! ch) "test:1|s|@1.0|#system:test")))
      (do (datadog-set! "test" 1)
          (is (= (<!! ch) "test:1|s|@1.0")))
      (do (histogram! "test" 10 :tags {:system "test"} :sample 0.25)
          (is (= (<!! ch) "test:10|h|@0.25|#system:test")))
      (do (histogram! "test" 10)
          (is (= (<!! ch) "test:10|h|@1.0")))
      (do (event! "title" "test" {:tags {:system "test"} :hostname "test"})
          (is (= (<!! ch) "_e{5,4}:title|test|h:test|#system:test")))
      (do (event! "title" "test")
          (is (= (<!! ch) "_e{5,4}:title|test"))))))
