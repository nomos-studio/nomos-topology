; SPDX-License-Identifier: EPL-2.0
(ns nomos.topology.core
  "Construction helpers and validation for nomos synthesis topology values.

  All helpers produce plain Clojure maps/vectors that satisfy the corresponding
  Malli schemas in nomos.topology.schema. No macros, no records — just data."
  (:require [malli.core          :as m]
            [malli.error         :as me]
            [nomos.topology.schema :as schema]))

;; ---------------------------------------------------------------------------
;; Validation
;; ---------------------------------------------------------------------------

(defn valid?
  "Return true if `v` satisfies schema `s`."
  [s v]
  (m/validate s v))

(defn explain
  "Return a humanised error map for `v` against schema `s`, or nil if valid."
  [s v]
  (when-let [err (m/explain s v)]
    (me/humanize err)))

(defn valid-session?  [v] (valid? schema/Session  v))
(defn valid-topology? [v] (valid? schema/Topology v))
(defn valid-patch?    [v] (valid? schema/Patch    v))
(defn valid-node?     [v] (valid? schema/Node     v))
(defn valid-module?   [v] (valid? schema/Module   v))

;; ---------------------------------------------------------------------------
;; Patch — kairos-grid internal graph
;; ---------------------------------------------------------------------------

(defn module
  "Construct a module descriptor.
    (module :osc \"plaits\")
    (module :osc \"plaits\" {:harmonics 0.5 :timbre 0.5 :level 1.0})"
  ([id type]
   {:id id :type type})
  ([id type params]
   {:id id :type type :params params}))

(defn cable
  "Construct a cable descriptor connecting from-module port to to-module port.
    (cable :osc 0 :filt 0)"
  [from-id from-port to-id to-port]
  [from-id from-port to-id to-port])

(defn patch
  "Construct a patch descriptor from a sequence of modules and cables."
  [modules cables]
  {:modules (vec modules)
   :cables  (vec cables)})

;; ---------------------------------------------------------------------------
;; Session topology
;; ---------------------------------------------------------------------------

(defn node
  "Construct a session node descriptor.
    (node :reverb \"clap/com.fabfilter/pro-r-3\")
    (node :reverb \"clap/com.fabfilter/pro-r-3\" {:params {:decay 1.2}})"
  ([id type]
   {:id id :type type})
  ([id type opts]
   (merge {:id id :type type} opts)))

(defn grid-node
  "Construct a :kairos-grid node embedding a patch.
    (grid-node :voice (patch [...] [...]))"
  [id patch]
  {:id id :type :kairos-grid :patch patch})

(defn route
  "Construct an audio route between two endpoints.
    (route [:voice 0] [:space 0])
    (route [:space 0] :master/left)"
  [from to]
  {:from from :to to})

(defn modulation
  "Construct a modulation binding from source to target param.
    (modulation [:voice \"signal/envelope\"] [:space \"decay\"] {:amount 0.4 :curve :exp})
    (modulation :nomos-rt/lfo-1 [:voice \"osc/harmonics\"])"
  ([source target]
   {:source source :target target})
  ([source target {:keys [amount curve]}]
   (cond-> {:source source :target target}
     (some? amount) (assoc :amount amount)
     (some? curve)  (assoc :curve  curve))))

(defn topology
  "Construct a topology map.
    (topology nodes)
    (topology nodes routes)
    (topology nodes routes modulations)"
  ([nodes]
   {:nodes (vec nodes)})
  ([nodes routes]
   {:nodes  (vec nodes)
    :routes (vec routes)})
  ([nodes routes modulations]
   {:nodes        (vec nodes)
    :routes       (vec routes)
    :modulations  (vec modulations)}))

;; ---------------------------------------------------------------------------
;; Control tree
;; ---------------------------------------------------------------------------

(defn control
  "Construct a control binding from a param target and optional metadata.
    (control [:voice \"osc/note\"] {:range [0 127] :type :midi-note :cc 74})"
  ([target]
   {:target target})
  ([target {:keys [range type cc]}]
   (cond-> {:target target}
     (some? range) (assoc :range range)
     (some? type)  (assoc :type  type)
     (some? cc)    (assoc :cc    cc))))

(defn signal
  "Construct a signal binding from a tap source.
    (signal [:voice \"signal/envelope\"])"
  [source]
  {:source source})

(defn control-tree
  "Construct a control tree from named controls and/or signals.
    (control-tree {:controls {:pitch (control [:voice \"osc/note\"] {...})}
                   :signals  {:env   (signal [:voice \"signal/envelope\"])}})"
  [{:keys [controls signals]}]
  (cond-> {}
    (seq controls) (assoc :controls controls)
    (seq signals)  (assoc :signals  signals)))

;; ---------------------------------------------------------------------------
;; Session
;; ---------------------------------------------------------------------------

(defn session
  "Construct a top-level session value.
    (session (topology nodes routes modulations) (control-tree {...}))"
  ([topology]
   {:topology topology})
  ([topology control-tree]
   {:topology     topology
    :control-tree control-tree}))
