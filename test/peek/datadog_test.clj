(ns peek.datadog-test
  (:use midje.sweet)
  (:require [peek.datadog :refer :all]))

(facts "`tag-string` produces datadog tags from a map"
  (tag-string {:a 1 :b "3"}) => "#a:1,b:3"
  (tag-string {:a 3}) => "#a:3"
  (tag-string {"a" 1 "b" "3"}) => "#a:1,b:3"
  (tag-string {}) => nil
  (tag-string nil) => nil)

(facts "`build-packet` builds datagrams"
  (build-packet "c" "counter" 1 {:a "a"} 0.25) => "counter:1|c|@0.25|#a:a"
  (build-packet "ms" "timing" 1 {:a "a"} 0.3) => "timing:1|ms|@0.3|#a:a"
  (build-packet "g" "gauge" 1 {:a "a"} 0.5) => "gauge:1|g|@0.5|#a:a"
  (build-packet "s" "set" 1 {:a "a"} 0.75) => "set:1|s|@0.75|#a:a"
  (build-packet "h" "histo" 1 {:a "a"} 1.0) => "histo:1|h|@1.0|#a:a")

(facts "`event-opts` builds the options portion of an event"
  (event-opts {:aggregation_key "a" :alert_type "t"}) => "k:a|t:t"
  (event-opts {}) => "")

(facts "`event-header` builds the header portion of an event"
  (event-header "test" "testing") => "_e{4,7}:test|testing")

(facts "`event-packet` builds a full event datagram"
  (event-packet "test" "testing" {:alert_type "t"}) => "_e{4,7}:test|testing|t:t"
  (event-packet "a" "bc" {}) => "_e{1,2}:a|bc")
