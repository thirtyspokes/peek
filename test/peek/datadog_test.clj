(ns peek.datadog-test
  (:use midje.sweet)
  (:require [clojure.test :refer :all]
            [peek.datadog :refer :all]))

(facts "`tag-string` produces datadog tags from a map"
  (tag-string {:a 1 :b "3"}) => "#a:1,b:3"
  (tag-string {:a 3}) => "#a:3"
  (tag-string {"a" 1 "b" "3"}) => "#a:1,b:3"
  (tag-string {}) => ""
  (tag-string nil) => "")
