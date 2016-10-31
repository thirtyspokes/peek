(ns peek.core
  (:require [clojure.core.async :refer [go]]
            [peek.datadog :as d]))

(defn gauge!
  "Records the current `value` of the gauge identified by `key`.  Executes
   asynchronously and immediately returns `nil`.

   Optinally accepts an options map with keys:

   :tags - A map of tags to append to this measurement.
   :sample - A rate between (0.0 and 1.0) at which to sample this gauge.

   Defaults:
   :tags {}, :sample 1.0"
  [key value & {:keys [tags sample] :or {tags {} sample 1.0}}]
  (go (d/submit "g" key value tags sample))
  nil)

(defn timing!
  "Records a timing of `value` milliseconds for the metric identified
   by `key`.  The body is executed in a go block so this function will
   immediately return `nil`.

   Optionally accepts an options map with keys:

   :tags - A map of tags to append to this measurement.
   :sample - A rate between (0.0 and 1.0) at which to sample this timing.

   Defaults:
   :tags {}, :sample 1.0"
  [key timing & {:keys [tags sample] :or {tags {} sample 1.0}}]
  (go (d/submit "ms" key timing tags sample))
  nil)

(defn increment!
  "Increments the counter specified by `key`.  Executes 
   asynchronously and immediately returns `nil`.

   Optionally accepts an options map with keys:
   
   :delta - The amount by which to increment the counter.
   :tags - A map of tags to append to this measurement.
   :sample - A rate (between 0.0 and 1.0) at which to sample this measurement.

   Defaults:
   :delta 1, :tags {}, :sample 1.0"
  [key & {:keys [delta tags sample] :or {delta 1 tags {} sample 1.0}}]
  (go (d/submit "c" key delta tags sample))
  nil)

(defn- withtime
  "Allows multi-arity usage of `time!`."
  ([key form]
   (withtime key {} 1.0 form))
  ([key tags form]
   (withtime key tags 1.0 form))
  ([key tags sample-rate form]
   `(let [start# (System/currentTimeMillis)
          res# (eval (quote ~form))]
      (d/submit-timing
        (quote ~key)
        (- (System/currentTimeMillis) start#)
        (quote ~tags)
        (quote ~sample-rate))
      res#)))

(defmacro time!
  "Wraps `form` in a Datadog timing - starts the timer, evaluates the code in 
   `form`, submits the execution time as a timing packet, and then returns the
  result of evaluating `form`.

  `time!` requires at minimum a `key` for the timing to be recorded:

  (time! \"login.successful\" ( ... your code to be instrumented ... )

  In addition to the `key`, you may also optionally provide `tags` and a `sample-rate`
  as in the other metric functions, but they must be in that order, e.g. if you wish
  to not supply tags but use a non-default sample rate:

  (time! \"login.successful\" {} 0.25 ( ... your code to be instrumented ... )"
  [& args]
  (apply withtime args))
