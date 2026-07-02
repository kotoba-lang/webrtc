# kotoba-webrtc

[![CI](https://github.com/kotoba-lang/webrtc/actions/workflows/ci.yml/badge.svg)](https://github.com/kotoba-lang/webrtc/actions/workflows/ci.yml)

**WebRTC signaling, session negotiation and room membership in pure
Clojure.** A [kotoba-lang](https://github.com/kotoba-lang) capability
library that models the records a video-call host runtime exchanges: ICE
(RFC 8445) candidates, SDP (RFC 8866) session/media descriptions, a pure
signaling-negotiation state machine, multi-party room membership, and
call-quality stats.

No network, no I/O, no codec. The library models **records, not the wire
format** — the same approach [`kotoba-lang/card`](https://github.com/kotoba-lang/card)
takes for ISO 8583 and [`kotoba-lang/swift`](https://github.com/kotoba-lang/swift)
takes for SWIFT MT: a `PolicyGovernor` or test harness reasons structurally
over EDN, without a parser. Portable `.cljc` across JVM / ClojureScript /
SCI / GraalVM.

## Scope

This library **does not** open a socket, run STUN/TURN, speak to a browser
`RTCPeerConnection`, or carry media. It models negotiation *state* and
*effects to perform* — the host runtime supplies the actual signaling
transport and media pipeline and executes the effects
`kotoba.webrtc.session/apply-event` returns. This is the same separation of
concerns [`kotoba-lang/koe`](https://github.com/kotoba-lang/koe) uses for
voice dialog (ports + a dialog loop, every concrete capability injected by
the host), applied to video-call signaling.

An actual signaling server, TURN/STUN deployment, or browser integration is
a separate decision, classified per
[ADR-2606302300](../../90-docs/adr/2606302300-org-taxonomy-4-orgs.md) step 2
(3-axis + Charter Rider check) — out of scope here.

## Contract

```clojure
(require '[kotoba.webrtc :as webrtc]
         '[kotoba.webrtc.session :as session]
         '[kotoba.webrtc.room :as room]
         '[kotoba.webrtc.stats :as stats])

;; ICE candidate + SDP records
(webrtc/ice-candidate {:foundation "1" :component 1 :transport :udp
                        :priority 2130706431 :address "10.0.0.1"
                        :port 54400 :cand-type :host})
(webrtc/media-description {:kind :video :mid "0" :direction :sendrecv
                            :codecs ["VP8" "H264"]})
(webrtc/sdp-session {:sdp-type :offer :media [...]})

;; signaling negotiation reducer — host executes the returned :effects
(def s0 (session/create-session "call-1" "alice" "bob"))
(session/apply-event s0 {:type :create-offer :sdp "offer-sdp"})
;; => {:session {... :webrtc.session/state :have-local-offer ...}
;;     :effects [[:send-offer "offer-sdp"]]}

;; multi-party room membership
(-> (room/create-room "r1")
    (room/join "alice") (room/join "bob")
    (room/publish-track "alice" "cam")
    (room/subscribe "bob" "alice"))

;; call-quality stats
(stats/classify (stats/stats-sample {:packets-sent 1000 :packets-lost 5
                                      :jitter-ms 10 :round-trip-ms 80}))
;; => :good
```

## Operator console (UI/UX)

A read-only HTML dashboard renders active sessions, room membership and
call-quality stats. Built on [`kotoba-lang/html`](https://github.com/kotoba-lang/html)
(Hiccup→HTML) + [`kotoba-lang/css`](https://github.com/kotoba-lang/css)
(EDN→CSS). Pure data → markup; the console never exposes a write surface.

```clojure
(require '[kotoba.webrtc.ui :as ui])

(ui/dashboard {:sessions [...] :room room :stats [...]})
```

## Export (CSV / JSON)

```clojure
(require '[kotoba.webrtc.export :as ex])

(ex/sessions->csv sessions)
(ex/stats->csv stats)
```

## Test

```sh
clojure -M:test
```

## Why

A video-call host needs to decide, before sending an offer/answer or
forwarding a track, whether a session is in a state where that is valid,
and whether call quality has degraded enough to act on. `kotoba-webrtc` is
the pure-data layer a host runtime (or a `PolicyGovernor`) checks against;
the host owns the actual `RTCPeerConnection`/signaling-transport bindings.

## License

Apache License 2.0.
