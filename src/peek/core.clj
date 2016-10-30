(ns peek.core
  (:require [clojure.core.async :refer [go]]
            [peek.datadog :as d]))

(defn timing!
  "Records a timing of `value` milliseconds for the metric identified
   by `key`.  The body is executed in a go block so this function will
   immediately return `nil`.

   Optionally accepts an options map with keys:

   :tags - A map of tags to append to this measurement.
   :sample - A rate between (0.0 and 1.0) and which to sample this timing.

   Defaults:
   :tags {}, :sample 1.0"
  [key timing & {:keys [tags sample] :or {tags {} sample 1.0}}]
  (go (d/submit-timing key timing tags sample))
  nil)

(defn increment!
  "Increments the counter specified by `key`.  The body is executed
   in a go block so this function will immediately return `nil`.

   Optionally accepts an options map with keys:
   
   :delta - The amount by which to increment the counter.
   :tags - A map of tags to append to this measurement.
   :sample - A rate (between 0.0 and 1.0) at which to sample this measurement.

   Defaults:
   :delta 1, :tags {}, :sample 1.0"
  [key & {:keys [delta tags sample] :or {delta 1 tags {} sample 1.0}}]
  (go (d/submit-counter key delta tags sample))
  nil)

(defmacro time!
  "Wraps `form` in a Datadog timing - starts the timer, executes the code in 
   `form`, submits the execution time as a timing packet, and then returns the
  result of `form`."
  [key tags sample-rate form]
  `(let [start# (System/currentTimeMillis)
         res# (eval (quote ~form))]
     (d/submit-timing
       (quote ~key)
       (- (System/currentTimeMillis) start#)
       (quote ~tags)
       (quote ~sample-rate))
     res#))
