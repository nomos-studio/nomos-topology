// SPDX-License-Identifier: GPL-3.0-or-later
// nomos/topology/keys.hpp — EDN key name constants for the nomos-topology schema.
//
// Usage with edn-cpp:
//   #include <nomos/topology/keys.hpp>
//   #include <edn/edn.hpp>
//
//   const auto& kw_modules = edn::keyword{nomos::topology::key::modules};
//   const auto* modules_val = map.find(kw_modules);
//
// All keys are the string component of their corresponding EDN keyword:
//   nomos::topology::key::modules  →  :modules  in EDN
//   nomos::topology::key::from     →  :from     in EDN

#pragma once

#include <string_view>

namespace nomos::topology {

// ---------------------------------------------------------------------------
// Map key names  (EDN keyword string components)
// ---------------------------------------------------------------------------

namespace key {

// Session level
constexpr std::string_view topology     = "topology";
constexpr std::string_view control_tree = "control-tree";

// Topology
constexpr std::string_view nodes        = "nodes";
constexpr std::string_view routes       = "routes";
constexpr std::string_view modulations  = "modulations";

// Node / Module shared
constexpr std::string_view id           = "id";
constexpr std::string_view type         = "type";
constexpr std::string_view params       = "params";
constexpr std::string_view patch        = "patch";

// Patch (kairos-grid internal graph)
constexpr std::string_view modules      = "modules";
constexpr std::string_view cables       = "cables";

// Route
constexpr std::string_view from         = "from";
constexpr std::string_view to           = "to";

// Modulation
constexpr std::string_view source       = "source";
constexpr std::string_view target       = "target";
constexpr std::string_view amount       = "amount";
constexpr std::string_view curve        = "curve";

// Control tree
constexpr std::string_view controls     = "controls";
constexpr std::string_view signals      = "signals";
constexpr std::string_view range        = "range";
constexpr std::string_view cc           = "cc";

} // namespace key

// ---------------------------------------------------------------------------
// Well-known node :type values
// ---------------------------------------------------------------------------

namespace node_type {

constexpr std::string_view kairos_grid  = "kairos-grid";

} // namespace node_type

// ---------------------------------------------------------------------------
// Well-known module :type strings (match the module registry keys)
// ---------------------------------------------------------------------------

namespace module_type {

constexpr std::string_view env          = "env";
constexpr std::string_view audio_in     = "audio-in";
constexpr std::string_view audio_out    = "audio-out";
constexpr std::string_view plaits       = "plaits";
constexpr std::string_view svf          = "svf";
constexpr std::string_view one_pole     = "one-pole";

} // namespace module_type

// ---------------------------------------------------------------------------
// Well-known :master/* endpoint keyword names
// ---------------------------------------------------------------------------

namespace master {

constexpr std::string_view left         = "master/left";
constexpr std::string_view right        = "master/right";

} // namespace master

// ---------------------------------------------------------------------------
// Curve values for :curve in Modulation
// ---------------------------------------------------------------------------

namespace curve {

constexpr std::string_view linear       = "linear";
constexpr std::string_view exp          = "exp";
constexpr std::string_view log          = "log";
constexpr std::string_view s_curve      = "s-curve";

} // namespace curve

// ---------------------------------------------------------------------------
// nomos-rt modulation source namespace prefix
// ---------------------------------------------------------------------------

namespace nomos_rt {

constexpr std::string_view ns           = "nomos-rt";  // keyword namespace

} // namespace nomos_rt

} // namespace nomos::topology
