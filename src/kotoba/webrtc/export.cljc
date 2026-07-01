(ns kotoba.webrtc.export
  "Operator-facing export for a video-call/WebRTC host runtime.

  Renders session and call-quality stats records to CSV and JSON for audit
  and downstream reporting. Pure data → text: no network."
  (:require [clojure.string :as str]
            [kotoba.webrtc.stats :as stats]))

(defn- csv-cell [v]
  (let [s (str (if (nil? v) "" v))]
    (if (re-find #"[\",\n]" s)
      (str "\"" (str/replace s "\"" "\"\"") "\"")
      s)))

(defn- csv-row [vals] (str/join "," (map csv-cell vals)))

(defn- json-str [v]
  (-> (str (if (nil? v) "" v))
      (str/replace "\\" "\\\\")
      (str/replace "\"" "\\\"")
      (str/replace "\n" "\\n")))

(defn sessions->csv [sessions]
  (str/join "\n"
    (cons (csv-row ["call_id" "local_id" "remote_id" "state" "ice_candidates"])
          (for [s sessions]
            (csv-row [(:webrtc.session/call-id s)
                      (or (:webrtc.session/local-id s) "")
                      (or (:webrtc.session/remote-id s) "")
                      (name (:webrtc.session/state s))
                      (count (:webrtc.session/ice-candidates s))])))))

(defn stats->csv [samples]
  (str/join "\n"
    (cons (csv-row ["quality" "packets_sent" "packets_lost" "jitter_ms" "round_trip_ms"])
          (for [s samples]
            (csv-row [(name (stats/classify s))
                      (:webrtc.stats/packets-sent s)
                      (:webrtc.stats/packets-lost s)
                      (:webrtc.stats/jitter-ms s)
                      (:webrtc.stats/round-trip-ms s)])))))

(defn sessions->json [sessions]
  (str "["
       (str/join ","
                 (for [s sessions]
                   (str "{\"call_id\":\"" (json-str (:webrtc.session/call-id s)) "\","
                        "\"local_id\":\"" (json-str (:webrtc.session/local-id s)) "\","
                        "\"remote_id\":\"" (json-str (:webrtc.session/remote-id s)) "\","
                        "\"state\":\"" (name (:webrtc.session/state s)) "\","
                        "\"ice_candidates\":" (count (:webrtc.session/ice-candidates s)) "}")))
       "]"))

(defn stats->json [samples]
  (str "["
       (str/join ","
                 (for [s samples]
                   (str "{\"quality\":\"" (name (stats/classify s)) "\","
                        "\"packets_sent\":" (:webrtc.stats/packets-sent s) ","
                        "\"packets_lost\":" (:webrtc.stats/packets-lost s) ","
                        "\"jitter_ms\":" (:webrtc.stats/jitter-ms s) ","
                        "\"round_trip_ms\":" (:webrtc.stats/round-trip-ms s) "}")))
       "]"))
