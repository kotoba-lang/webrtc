(ns kotoba.webrtc.export-test
  (:require [clojure.test :refer [deftest is]]
            [kotoba.webrtc.session :as session]
            [kotoba.webrtc.stats :as stats]
            [kotoba.webrtc.export :as ex]))

(deftest sessions-csv-export
  (let [s (:session (session/apply-event (session/create-session "c1" "alice" "bob") {:type :create-offer :sdp "x"}))
        csv (ex/sessions->csv [s])]
    (is (re-find #"call_id,local_id,remote_id,state,ice_candidates" csv))
    (is (re-find #"c1,alice,bob,have-local-offer,0" csv))))

(deftest stats-csv-export
  (let [st (stats/stats-sample {:packets-sent 100 :packets-lost 1 :jitter-ms 5 :round-trip-ms 40})
        csv (ex/stats->csv [st])]
    (is (re-find #"quality,packets_sent,packets_lost,jitter_ms,round_trip_ms" csv))
    (is (re-find #"good,100,1,5,40" csv))))

(deftest sessions-json-export
  (let [s (:session (session/apply-event (session/create-session "c1" "alice" "bob") {:type :create-offer :sdp "x"}))
        j (ex/sessions->json [s])]
    (is (re-find #"\"state\":\"have-local-offer\"" j))))

(deftest stats-json-export
  (let [st (stats/stats-sample {:packets-sent 100 :packets-lost 1 :jitter-ms 5 :round-trip-ms 40})
        j (ex/stats->json [st])]
    (is (re-find #"\"quality\":\"good\"" j))))
