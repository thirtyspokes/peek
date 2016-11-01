(ns peek.core-test
  (:use midje.sweet)
  (:require [peek.core :refer :all]
            [peek.datadog :refer [sampled-send]]))

(let [result (atom nil)]    
  (with-redefs [sampled-send (fn [packet sample-rate] (reset! result packet))]
    (fact "time! returns the evaluated body and also emits a packet"
      (time! "test" (+ 1 1)) => 2
      @result => truthy)))
