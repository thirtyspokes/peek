(defproject thirtyspokes/peek "1.0.0T"
  :description "Clojure client for DogStatsD"
  :url "https://github.com/thirtyspokes/peek"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/core.async "0.2.395"]]
  :profiles {:dev {:dependencies [[midje "1.8.3"]]}})
