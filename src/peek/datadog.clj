(ns peek.datadog
  (:import [java.net DatagramSocket
            DatagramPacket
            InetSocketAddress])
  (:require [clojure.string :refer [join]]
            [environ.core :refer [env]]))

(def config
  ^{:doc "Holds the host/port configuration for the collector."}
  (atom {}))

(defn init
  "Sets up the configuration for the metrics to be collected.  Accepts
   an options map with the following keys:

   :host - The IP address of your DogStatsD collector.
   :port - The port (an Integer) your DogStatsD collector is listening on.
   :prefix - A string to be preprended to the keys of all metrics that are
    emitted.

   Defaults:
   :host \"127.0.0.1\", :port 8125, :prefix \"\""
  [& {:keys [host port prefix] :or {host "127.0.0.1" port 8125 prefix nil}}]
  (swap! config assoc :host host :port port :prefix prefix))

(defn dd-host
  "Returns the host from configuration, or the default."
  []
  (:host @config "127.0.0.1"))

(defn dd-port
  "Returns the port from configuration, or the default."
  []
  (if-let [port (:port @config)]
    (if (string? port)
      (Integer/parseInt port)
      port)
    8125))

(defn dd-prefix
  "Returns the prefix from the configuration, or empty string."
  []
  (if-let [prefix (:prefix @config)]
    (str prefix ".")
    ""))

(def socket
  (DatagramSocket. 0))

(defn send-udp
  "Sends a single UDP packet to the configured host and port containing
   the supplied `payload`."
  [packet]
  (let [payload (.getBytes packet)
        length (min (alength payload) 512)
        address (InetSocketAddress. (dd-host) (dd-port))
        packet (DatagramPacket. payload length address)]
    (.send socket packet)))

(defn sampled-send
  "Will send a UDP packet containing `payload` at a rate roughly determined
   by `sample-rate` - e.g., at 1.0 will always send, and at 0.25 roughly one
   out of four packets will be sent."
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
  "Constructs a metrics datagram for DogStatsD."
  [type key value tag-map sample-rate]
  (let [tags (tag-string tag-map)
        kv (str (dd-prefix) key ":" value)
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
  "Constructs the optional portion of an event datagram."
  [opt-map]
  (let [non-tags (dissoc opt-map :tags)]
    (join "|" (map (fn [[k v]] (str (get datagram-keys k) ":" v)) non-tags))))

(defn event-header
  "Constructs the required portion of an event datagram."
  [title text]
  (let [title-len (count title)
        text-len (count text)
        joined (str title-len "," text-len)]
    (str "_e{" joined "}:" title "|" text)))

(defn event-packet
  "Constructs a full datagram for a DogStatsD event."
  [title text opt-map]
  (let [header (event-header title text)
        opts (event-opts opt-map)
        tags (tag-string (:tags opt-map))]
    (join "|" (filter (complement empty?) [header opts tags]))))

(defn submit-event
  "Creates an event datagram and submits it to the collector over UDP."
  [title text opts]
  (let [packet (event-packet title text opts)]
    (send-udp packet)))
