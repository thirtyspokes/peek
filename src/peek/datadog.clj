(ns peek.datadog
  (:import [java.net DatagramSocket
            DatagramPacket
            InetSocketAddress])
  (:require [clojure.string :refer [join]]))

(def socket
  (DatagramSocket. 0))

(defn send-udp
  "Send a short textual message over a DatagramSocket to the specified
  host and port. If the string is over 512 bytes long, it will be
  truncated."
  [packet]
  (let [payload (.getBytes packet)
        length (min (alength payload) 512)
        address (InetSocketAddress. "127.0.0.1" 8125)
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

(defn submit-counter
  "Creates a UDP packet for a Datadoc counter and send it to the collector."
  [key delta tags sample-rate]
  (let [packet (build-packet "c" key delta tags sample-rate)]
    (sampled-send packet sample-rate)))

(defn submit-timing
  "Creates a UDP packet for a Datadog timing and sends it to the collector."
  [key timing tags sample-rate]
  (let [packet (build-packet "ms" key timing tags sample-rate)]
    (sampled-send packet sample-rate)))
