; SPDX-License-Identifier: EPL-2.0
(ns nomos.topology.schema
  "Malli schema for the nomos synthesis topology.

  The topology describes, as a plain Clojure value, the full signal chain
  from sample-rate DSP inside a kairos-grid patch up through the kairos
  CLAP session graph and nomos-rt modulation layer. A control tree declares
  the musical interface that nous uses to operate the patch.

  ## Layers

    Patch        — kairos-grid internal signal graph (modules + cables)
    Topology     — kairos session graph (CLAP nodes + audio routes + modulations)
    ControlTree  — named handles connecting nous to the synthesis (controls + signals)
    Session      — top-level value combining Topology + ControlTree

  ## Example session

    {:topology
     {:nodes [{:id    :voice
                :type  :kairos-grid
                :patch {:modules [{:id :env  :type \"env\"}
                                   {:id :osc  :type \"plaits\"
                                    :params {:harmonics 0.5 :timbre 0.5
                                             :morph 0.5  :level  1.0}}
                                   {:id :filt :type \"svf\"
                                    :params {:cutoff 0.35 :q 0.1}}
                                   {:id :out  :type \"audio-out\"}]
                         :cables  [[:env 4 :osc 0]
                                    [:env 5 :osc 4]
                                    [:osc 0 :filt 0]
                                    [:filt 0 :out 0]
                                    [:filt 0 :out 1]]}}
               {:id   :space
                :type \"clap/com.fabfilter/pro-r-3\"
                :params {:decay 1.2}}]
      :routes      [{:from [:voice 0] :to [:space 0]}
                     {:from [:space 0] :to :master/left}
                     {:from [:space 1] :to :master/right}]
      :modulations [{:source [:voice \"signal/envelope\"]
                      :target [:space \"decay\"]
                      :amount 0.4
                      :curve  :exp}]}
     :control-tree
     {:controls {:pitch  {:target [:voice \"osc/note\"]
                           :range  [0 127]
                           :type   :midi-note}
                  :timbre {:target [:voice \"osc/harmonics\"]
                            :range  [0 1]
                            :cc     74}
                  :colour {:target [:voice \"filt/cutoff\"]
                            :range  [0 1]
                            :cc     71}}
      :signals  {:envelope {:source [:voice \"signal/envelope\"]}
                  :gate     {:source [:voice \"signal/gate\"]}}}}")

;; ---------------------------------------------------------------------------
;; Primitives
;; ---------------------------------------------------------------------------

(def NodeId
  "Stable identifier for a kairos session node (CLAP plugin instance)."
  :keyword)

(def ModuleId
  "Stable identifier for a module within a kairos-grid patch."
  :keyword)

(def PortRef
  "A port reference within a patch — integer index or named keyword."
  [:or :int :keyword])

(def PortName
  "Qualified param or tap port name as known to the engine schema.
  Param names are scoped to their module: \"osc/harmonics\", \"filt/cutoff\".
  Tap names follow the tap namespace: \"signal/envelope\", \"signal/gate\"."
  :string)

(def ParamRef
  "A reference to a named param port on a session node: [node-id param-name]."
  [:tuple NodeId PortName])

(def TapRef
  "A reference to a named tap on a session node: [node-id tap-name]."
  [:tuple NodeId PortName])

(def NomosSrc
  "A modulation source owned by nomos-rt rather than a session node.
  Namespaced keyword: :nomos-rt/lfo-1, :nomos-rt/clock-phase, etc."
  :keyword)

(def ModSource
  "Any modulation source: a tap on a session node, or a nomos-rt source."
  [:or TapRef NomosSrc])

;; ---------------------------------------------------------------------------
;; Patch — kairos-grid internal signal graph
;; ---------------------------------------------------------------------------

(def Cable
  "A directed connection inside a kairos-grid patch.
  [from-module-id from-port to-module-id to-port]
  Port refs are integer indices (v1); named keyword ports in a future version."
  [:tuple ModuleId PortRef ModuleId PortRef])

(def Module
  "A module instance inside a kairos-grid patch.
  :id     — user-assigned keyword; scopes param port names (\"osc/harmonics\")
  :type   — registry key matching a registered module factory (\"plaits\", \"svf\",
             \"alembic/my-patch\", etc.)
  :params — initial values for named param ports; keys are unqualified keywords
             that are prefixed with :id at engine build time"
  [:map {:closed false}
   [:id     ModuleId]
   [:type   :string]
   [:params {:optional true} [:map-of :keyword number?]]])

(def Patch
  "A kairos-grid signal graph: an ordered list of module instances and the
  cables that connect their ports."
  [:map
   [:modules [:vector Module]]
   [:cables  [:vector Cable]]])

;; ---------------------------------------------------------------------------
;; Session topology — kairos CLAP session graph
;; ---------------------------------------------------------------------------

(def RouteEndpoint
  "An endpoint for an audio route: either [node-id port] or a named master
  output like :master/left or :master/right."
  [:or
   [:tuple NodeId PortRef]
   :keyword])

(def Route
  "A directed audio connection between two session nodes, or from a node to
  the master output."
  [:map
   [:from RouteEndpoint]
   [:to   RouteEndpoint]])

(def Curve
  "Modulation response curve applied to the normalised [0,1] amount."
  [:enum :linear :exp :log :s-curve])

(def Modulation
  "A binding from a modulation source to a named param target.
  :source — a tap on a session node or a nomos-rt modulation source
  :target — a named param port on any session node
  :amount — scale factor in [0, 1]; default 1.0 when omitted
  :curve  — response curve; default :linear when omitted"
  [:map
   [:source ModSource]
   [:target ParamRef]
   [:amount {:optional true} [:double {:min 0.0 :max 1.0}]]
   [:curve  {:optional true} Curve]])

(def Node
  "A CLAP plugin instance in the kairos session graph.
  :id     — stable session-scoped identifier
  :type   — :kairos-grid for grid nodes; string CLAP plugin-id for others
  :params — initial parameter values (CLAP params, not patch-bus params)
  :patch  — kairos-grid internal graph; only valid when :type is :kairos-grid"
  [:map {:closed false}
   [:id     NodeId]
   [:type   [:or :keyword :string]]
   [:params {:optional true} [:map-of :keyword number?]]
   [:patch  {:optional true} Patch]])

(def Topology
  "The kairos session graph: nodes, audio routes between them, and
  nomos-rt modulation bindings."
  [:map
   [:nodes       [:vector Node]]
   [:routes      {:optional true} [:vector Route]]
   [:modulations {:optional true} [:vector Modulation]]])

;; ---------------------------------------------------------------------------
;; Control tree — musical interface between nous and the synthesis
;; ---------------------------------------------------------------------------

(def ControlType
  "Semantic type hint for a control binding.
  :continuous — a continuous float parameter (default)
  :midi-note  — MIDI note number 0–127
  :gate       — boolean on/off
  :trigger    — momentary pulse"
  [:enum :continuous :midi-note :gate :trigger])

(def ControlBinding
  "A named handle by which nous drives a synthesis parameter.
  :target — the param port this control writes to
  :range  — [min max] clamp applied before writing; default [0 1]
  :type   — semantic type hint
  :cc     — optional MIDI CC number for hardware controller mapping"
  [:map {:closed false}
   [:target ParamRef]
   [:range  {:optional true} [:tuple number? number?]]
   [:type   {:optional true} ControlType]
   [:cc     {:optional true} [:int {:min 0 :max 127}]]])

(def SignalBinding
  "A named observable that nous can read from the synthesis.
  Backed by a tap-bus source; useful for reactive composition and
  nomos-rt modulation routing."
  [:map
   [:source ModSource]])

(def ControlTree
  "The musical interface of a session.
  :controls — named inputs (nous → synthesis via param-bus)
  :signals  — named outputs (synthesis → nous via tap-bus)"
  [:map
   [:controls {:optional true} [:map-of :keyword ControlBinding]]
   [:signals  {:optional true} [:map-of :keyword SignalBinding]]])

;; ---------------------------------------------------------------------------
;; Session — top-level value
;; ---------------------------------------------------------------------------

(def Session
  "A complete nomos synthesis session: topology + control tree.
  This is the primary artifact. alembic generates it from defpatch!;
  nous manipulates it; kairos interprets the topology; kairos-grid
  receives its :patch subtree via the patch-bus extension."
  [:map
   [:topology     Topology]
   [:control-tree {:optional true} ControlTree]])
