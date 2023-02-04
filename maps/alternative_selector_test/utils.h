#pragma once

#include "../../alternatives_selector/alternative_info.h"
#include "../../router_result.h"

#include <library/cpp/testing/unittest/env.h>

using maps::navigator_stopwatch5::JamType;
using maps::navigator_stopwatch5::JamSegment;

const maps::road_graph::Graph ROAD_GRAPH(
    BinaryPath("maps/data/test/graph3/road_graph.fb"));

RouterResult makeRoute();

JamSegment createJamSegment(JamType jamType);
