; SPDX-License-Identifier: EPL-2.0
(defproject nomos-topology "0.1.0"
  :description "nomos-topology — synthesis topology schema for the nomos-studio ecosystem.
  Defines the data shapes for kairos-grid patches, kairos session graphs,
  nomos-rt modulation bindings, and the control tree that connects synthesis
  to the nous compositional surface."
  :url "https://github.com/nomos-studio/nomos-topology"
  :license {:name "EPL-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.12.2"]
                 [metosin/malli "0.16.4"]]
  :source-paths ["src"]
  :test-paths   ["test"]
  :target-path  "target/%s"
  :profiles {:dev {:dependencies []}})
