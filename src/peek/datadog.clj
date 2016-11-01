(ns peek.datadog
  (:import [java.net DatagramSocket
            DatagramPacket
            InetSocketAddress])
  (:require [clojure.string :refer [join]]
            [environ.core :refer [env]]))

(def datadog-host
  (or (env :dogstatsd-host) "127.0.0.1"))

(def datadog-port
  (or (env :dogstatsd-port) "8125"))

(def socket
  (DatagramSocket. 0))

(defn send-udp
  [packet]
  (let [payload (.getBytes packet)
        length (min (alength payload) 512)
        address (InetSocketAddress. datadog-host datadog-port)
        packet (DatagramPacket. payload length address)]
    (.send socket packet)))

(defn sampled-send
  [packet sample-rate]
  (when (<= (rand) sample-rate)
    (send-udp packet)))

(defn tag-string
  "Creates a Datadog-formatted string out of `tag-map`, a map of user-defined
   tags.  The keys can be either keywords or strings and the values
   can be anything that is string-representable.  If `tag-map` is empty, returns 
   empty string."
  [tag-map]
  (let [tags (map (fn [[k v]] (str (name k) ":" v)) tag-map)]
    (when-not (empty? tags)
      (str "#" (clojure.string/join "," tags)))))

(defn build-packet
  [type key value tag-map sample-rate]
  (let [tags (tag-string tag-map)
        kv (str key ":" value)
        rate (str "@" sample-rate)]
    (join "|" (filter (complement empty?) [kv type rate tags]))))

(defn submit
  "Builds a UDP payload for the metric and submits it over UDP to the
   DogstatsD collector."
  [type key value tags rate]
  (let [packet (build-packet type key value tags rate)]
    (sampled-send packet rate)))

(def datagram-keys
  {:date_happened "d"
   :hostname "h"
   :aggregation_key "k"
   :priority "p"
   :source_type_name "s"
   :alert_type "t"})

(defn event-opts
  [opt-map]
  (let [non-tags (dissoc opt-map :tags)]
    (clojure.string/join "|"
      (map (fn [[k v]] (str (get datagram-keys k) ":" v)) non-tags))))

(defn event-header
  [title text]
  (let [title-len (count title)
        text-len (count text)
        joined (str title-len "," text-len)]
    (str "_e{" joined "}:" title "|" text)))

(defn event-packet
  [title text opt-map]
  (let [header (event-header title text)
        opts (event-opts opt-map)
        tags (tag-string (:tags opt-map))]
    (clojure.string/join "|"
      (filter (complement empty?) [header opts tags]))))

(defn submit-event
  [title text opts]
  (let [packet (event-packet title text opts)]
    (send-udp packet)))
