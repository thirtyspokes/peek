# peek

A Clojure interface to DogStatsD (a StatsD-like metrics collection system created by DataDog).

## Usage

To learn more about the DogStatsD protocol itself, check out the [official documentation at Datadog](http://docs.datadoghq.com/guides/dogstatsd/).

### Configuration

This library can be used without any configuration - if none is supplied, then it will default to sending metrics to 127.0.0.1:8125.  To send your metrics to a different location, use `peek.core/init`:

```clojure
;; Somewhere in your app startup:
(ns your-app.core
  (:require [peek.core :as stats]))

(stats/init {:host "127.0.0.2" :port 3000})
```

`peek.core/init` takes an optional `:prefix` key.  If this is supplied, the string you provide will be prefixed to all of the metrics that are emitted:

```clojure
(stats/increment! "logins.failed") ;; key is "logins.failed"

(stats/init {:prefix "service-name"}
(stats/increment! "logins.failed") ;; key is "service-name.logins.failed"
```

### Recording Metrics

All of the DogStatsD metrics are supported (except for service checks).  All of the metric functions take a key, the value to be recorded (with the exception of counters, for which the value is optional and defaults to 1 if not supplied), and optional named parameters:

- :tags, a map of keywords or strings to values that will be appended to the metric and can be used as filters in Datadog.
- :sample, a decimal number between 0.0 and 1.0 that will determine how often the metric will actually be recorded.

All of the metric functions run their body in a go block, meaning that they execute on a separate thread from the call site and return `nil` immediately (with the exception of the `time!` macro, as you will see below).

```clojure
(ns your-app.core
  (:require [peek.core :as stats]))

;; Increment the logins.failed counter by 1, with the tags region:US:
(stats/increment! "logins.failed" :tags {:region "US"})

;; Increment the same counter by 10 instead:
(stats/increment! "logins.failed" :delta 10 :tags {:region "US"})

;; Set the current value of a gauge.
(stats/gauge! "connections.open" 10 :tags {:system "mysql"})

;; Set a value for a histogram.
(stats/histogram! "profile.rendering" 55)

;; Add an email to a set of uniques, tagged referral:true.
(stats/datadog-set! "visitors.unique" "example@mail.com" :tags {:referral "true"})

;; Submit an event with the title "Deployment", a text description, and
;; the alert_type of "success".  Note that for events, the last parameter
;; is an actual options map instead of named params.
(stats/event! "deployment" "A deployment has finished successfully" {:alert_type "success"})
```

For recording timings, a `time!` macro is provided that will execute the Clojure code passed to it and return the result, while also emitting a timing metric as a side effect.

```clojure
;; In this example your code will execute and return rows as normal, and the execution time
;; in milliseconds will be recorded as a side effect.
(stats/time! "database.queries.select_all"
  (j/query mysql-db ["SELECT * FROM table"]))

;; The time! macro can also support optional tags and sample rate, but they must
;; be supplied in that order - so if you want a non-default sample rate but no tags,
;; you must supply an empty map for the tag parameter.

;; This timing will be tagged pipeline:api and sampled at a rate of 0.75.
(stats/time! "controllers.actions.index" {:pipeline "api"} 0.75
  ( ... your code to handle the controller action ...))
```

## Installation

In your project.clj: 

```
[thirtyspokes/peek "1.0.0"]
```

Or if your desires are *unconventional*:

```
<dependency>
  <groupId>thirtyspokes</groupId>
  <artifactId>peek</artifactId>
  <version>1.0.0</version>
</dependency>
```

## License

Copyright © 2016 Ray Ashman Jr.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
