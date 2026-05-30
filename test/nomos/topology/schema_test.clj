; SPDX-License-Identifier: EPL-2.0
(ns nomos.topology.schema-test
  (:require [clojure.test              :refer [deftest is testing]]
            [malli.core                :as m]
            [nomos.topology.schema     :as schema]
            [nomos.topology.core       :as topo]))

;; ---------------------------------------------------------------------------
;; Primitive schemas
;; ---------------------------------------------------------------------------

(deftest port-ref
  (testing "integer port"
    (is (m/validate schema/PortRef 0)))
  (testing "named keyword port"
    (is (m/validate schema/PortRef :voice_note)))
  (testing "string is not a PortRef"
    (is (not (m/validate schema/PortRef "0")))))

(deftest param-ref
  (testing "valid param ref"
    (is (m/validate schema/ParamRef [:voice "osc/harmonics"])))
  (testing "must be a tuple"
    (is (not (m/validate schema/ParamRef {:node :voice :param "osc/harmonics"})))))

(deftest mod-source
  (testing "tap ref source"
    (is (m/validate schema/ModSource [:voice "signal/envelope"])))
  (testing "nomos-rt keyword source"
    (is (m/validate schema/ModSource :nomos-rt/lfo-1)))
  (testing "bare string is not a source"
    (is (not (m/validate schema/ModSource "signal/envelope")))))

;; ---------------------------------------------------------------------------
;; Patch — kairos-grid internal graph
;; ---------------------------------------------------------------------------

(deftest cable-schema
  (testing "integer-indexed cable"
    (is (m/validate schema/Cable [:osc 0 :filt 0])))
  (testing "named-port cable"
    (is (m/validate schema/Cable [:env :voice_note :osc :note])))
  (testing "wrong arity"
    (is (not (m/validate schema/Cable [:osc 0 :filt]))))
  (testing "non-keyword module id"
    (is (not (m/validate schema/Cable ["osc" 0 :filt 0])))))

(deftest module-schema
  (testing "minimal module"
    (is (m/validate schema/Module {:id :osc :type "plaits"})))
  (testing "module with params"
    (is (m/validate schema/Module
                    {:id :osc :type "plaits"
                     :params {:harmonics 0.5 :timbre 0.5 :morph 0.5 :level 1.0}})))
  (testing "alembic module type"
    (is (m/validate schema/Module {:id :res :type "alembic/my-resonator"})))
  (testing "missing :type"
    (is (not (m/validate schema/Module {:id :osc}))))
  (testing "missing :id"
    (is (not (m/validate schema/Module {:type "plaits"})))))

(deftest patch-schema
  (let [mi-voice {:modules [{:id :env  :type "env"}
                              {:id :osc  :type "plaits"
                               :params {:harmonics 0.5 :timbre 0.5
                                        :morph 0.5  :level  1.0}}
                              {:id :filt :type "svf"
                               :params {:cutoff 0.35 :q 0.1}}
                              {:id :out  :type "audio-out"}]
                  :cables  [[:env 4 :osc 0]
                              [:env 5 :osc 4]
                              [:osc 0 :filt 0]
                              [:filt 0 :out 0]
                              [:filt 0 :out 1]]}]
    (testing "full MI voice patch"
      (is (m/validate schema/Patch mi-voice)))
    (testing "empty patch"
      (is (m/validate schema/Patch {:modules [] :cables []})))
    (testing "missing :cables"
      (is (not (m/validate schema/Patch {:modules []}))))))

;; ---------------------------------------------------------------------------
;; Topology — kairos session graph
;; ---------------------------------------------------------------------------

(deftest route-schema
  (testing "node-to-node route"
    (is (m/validate schema/Route {:from [:voice 0] :to [:space 0]})))
  (testing "node-to-master route"
    (is (m/validate schema/Route {:from [:space 0] :to :master/left})))
  (testing "named ports"
    (is (m/validate schema/Route {:from [:voice :out] :to [:space :in]}))))

(deftest modulation-schema
  (testing "full modulation binding"
    (is (m/validate schema/Modulation
                    {:source [:voice "signal/envelope"]
                     :target [:space "decay"]
                     :amount 0.4
                     :curve  :exp})))
  (testing "nomos-rt source"
    (is (m/validate schema/Modulation
                    {:source :nomos-rt/lfo-1
                     :target [:voice "osc/harmonics"]
                     :amount 0.2
                     :curve  :linear})))
  (testing "minimal — no amount or curve"
    (is (m/validate schema/Modulation
                    {:source [:voice "signal/gate"]
                     :target [:voice "osc/engine"]})))
  (testing "amount out of range"
    (is (not (m/validate schema/Modulation
                         {:source [:voice "signal/envelope"]
                          :target [:space "decay"]
                          :amount 1.5}))))
  (testing "unknown curve"
    (is (not (m/validate schema/Modulation
                         {:source [:voice "signal/envelope"]
                          :target [:space "decay"]
                          :curve  :quadratic})))))

(deftest node-schema
  (let [patch {:modules [{:id :osc :type "plaits"} {:id :out :type "audio-out"}]
               :cables  [[:osc 0 :out 0]]}]
    (testing "kairos-grid node with patch"
      (is (m/validate schema/Node {:id :voice :type :kairos-grid :patch patch})))
    (testing "CLAP plugin node"
      (is (m/validate schema/Node {:id :reverb :type "clap/com.fabfilter/pro-r-3"
                                   :params {:decay 1.2}})))
    (testing "missing :id"
      (is (not (m/validate schema/Node {:type :kairos-grid :patch patch}))))))

;; ---------------------------------------------------------------------------
;; Control tree
;; ---------------------------------------------------------------------------

(deftest control-tree-schema
  (testing "full control tree"
    (is (m/validate schema/ControlTree
                    {:controls {:pitch  {:target [:voice "osc/note"]
                                          :range  [0 127]
                                          :type   :midi-note}
                                 :timbre {:target [:voice "osc/harmonics"]
                                           :range  [0 1]
                                           :cc     74}
                                 :colour {:target [:voice "filt/cutoff"]
                                           :range  [0 1]
                                           :cc     71}}
                     :signals  {:envelope {:source [:voice "signal/envelope"]}
                                 :gate     {:source [:voice "signal/gate"]}}})))
  (testing "controls only"
    (is (m/validate schema/ControlTree
                    {:controls {:pitch {:target [:voice "osc/note"]}}})))
  (testing "signals only"
    (is (m/validate schema/ControlTree
                    {:signals {:env {:source [:voice "signal/envelope"]}}})))
  (testing "empty control tree"
    (is (m/validate schema/ControlTree {})))
  (testing "nomos-rt signal source"
    (is (m/validate schema/ControlTree
                    {:signals {:clock {:source :nomos-rt/clock-phase}}}))))

;; ---------------------------------------------------------------------------
;; Session — top-level
;; ---------------------------------------------------------------------------

(deftest session-schema
  (let [patch {:modules [{:id :env  :type "env"}
                          {:id :osc  :type "plaits"
                           :params {:harmonics 0.5 :level 1.0}}
                          {:id :filt :type "svf"
                           :params {:cutoff 0.35 :q 0.1}}
                          {:id :out  :type "audio-out"}]
               :cables  [[:env 4 :osc 0]
                           [:env 5 :osc 4]
                           [:osc 0 :filt 0]
                           [:filt 0 :out 0]
                           [:filt 0 :out 1]]}]

    (testing "single-node session"
      (is (m/validate schema/Session
                      {:topology {:nodes [{:id :voice :type :kairos-grid
                                            :patch patch}]}})))

    (testing "session with control tree"
      (is (m/validate schema/Session
                      {:topology
                       {:nodes [{:id :voice :type :kairos-grid :patch patch}]}
                       :control-tree
                       {:controls {:pitch  {:target [:voice "osc/note"]
                                             :range  [0 127]
                                             :type   :midi-note}
                                    :timbre {:target [:voice "osc/harmonics"]
                                              :range  [0 1]}}
                        :signals  {:envelope {:source [:voice "signal/envelope"]}}}})))

    (testing "multi-node session with routing and modulation"
      (is (m/validate schema/Session
                      {:topology
                       {:nodes [{:id :voice :type :kairos-grid :patch patch}
                                 {:id :space :type "clap/com.fabfilter/pro-r-3"
                                  :params {:decay 1.2}}]
                        :routes [{:from [:voice 0] :to [:space 0]}
                                  {:from [:voice 1] :to [:space 1]}
                                  {:from [:space 0] :to :master/left}
                                  {:from [:space 1] :to :master/right}]
                        :modulations [{:source [:voice "signal/envelope"]
                                        :target [:space "decay"]
                                        :amount 0.4
                                        :curve  :exp}
                                       {:source :nomos-rt/lfo-1
                                        :target [:voice "osc/harmonics"]
                                        :amount 0.2}]}})))

    (testing "topology required"
      (is (not (m/validate schema/Session
                           {:control-tree {:controls {}}}))))))

;; ---------------------------------------------------------------------------
;; Construction helpers
;; ---------------------------------------------------------------------------

(deftest construction-helpers
  (testing "module"
    (is (= {:id :osc :type "plaits"} (topo/module :osc "plaits")))
    (is (= {:id :osc :type "plaits" :params {:harmonics 0.5}}
           (topo/module :osc "plaits" {:harmonics 0.5})))
    (is (m/validate schema/Module (topo/module :osc "plaits"))))

  (testing "cable"
    (is (= [:osc 0 :filt 0] (topo/cable :osc 0 :filt 0)))
    (is (m/validate schema/Cable (topo/cable :osc 0 :filt 0))))

  (testing "patch"
    (let [p (topo/patch [(topo/module :osc "plaits")
                          (topo/module :out "audio-out")]
                         [(topo/cable :osc 0 :out 0)])]
      (is (m/validate schema/Patch p))))

  (testing "grid-node"
    (let [p (topo/patch [(topo/module :osc "plaits")
                          (topo/module :out "audio-out")]
                         [(topo/cable :osc 0 :out 0)])]
      (is (m/validate schema/Node (topo/grid-node :voice p)))))

  (testing "modulation"
    (is (m/validate schema/Modulation
                    (topo/modulation [:voice "signal/envelope"]
                                     [:space "decay"]
                                     {:amount 0.4 :curve :exp})))
    (is (m/validate schema/Modulation
                    (topo/modulation :nomos-rt/lfo-1 [:voice "osc/harmonics"]))))

  (testing "round-trip session"
    (let [p   (topo/patch [(topo/module :env  "env")
                             (topo/module :osc  "plaits" {:harmonics 0.5 :level 1.0})
                             (topo/module :filt "svf"    {:cutoff 0.35 :q 0.1})
                             (topo/module :out  "audio-out")]
                            [(topo/cable :env 4 :osc 0)
                             (topo/cable :env 5 :osc 4)
                             (topo/cable :osc 0 :filt 0)
                             (topo/cable :filt 0 :out 0)
                             (topo/cable :filt 0 :out 1)])
          top (topo/topology [(topo/grid-node :voice p)])
          ct  (topo/control-tree
               {:controls {:pitch (topo/control [:voice "osc/note"]
                                                {:range [0 127] :type :midi-note})
                             :timbre (topo/control [:voice "osc/harmonics"]
                                                   {:range [0 1] :cc 74})}
                :signals  {:envelope (topo/signal [:voice "signal/envelope"])}})
          s   (topo/session top ct)]
      (is (m/validate schema/Session s)))))
