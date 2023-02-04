#include "utils.h"
#include "../../router_result.h"
#include "../../alternatives_selector/alternative_info.h"

#include <library/cpp/testing/unittest/env.h>

using maps::geolib3::Point2;
using maps::routing::PathSegment;

RouterResult makeRoute() {
    const Route route {
        {
            PathSegment(maps::road_graph::EdgeId(0), 1, 1, 1, 0, 0)
        },
        {
            RequestPointWithPosition(0, RequestPoint(Point2(0, 0))),
            RequestPointWithPosition(1, RequestPoint(Point2(0, 0)))
        }
    };
    RouterResult result(
        VehicleParameters(VehicleType::CAR), Avoid(), route, &ROAD_GRAPH);
    result.traits.set_has_tolls(false);
    result.traits.set_has_boat_ferries(false);
    result.traits.set_has_ford_crossings(false);
    result.traits.set_requires_access_pass(false);
    result.traits.set_crosses_borders(false);
    result.traits.set_future_blocked(false);
    result.traits.set_dead_jam(false);
    result.traits.set_blocked(false);
    result.traits.set_has_rugged_roads(false);
    result.traits.set_has_unpaved_roads(false);
    result.traits.set_has_in_poor_condition_roads(false);
    result.traits.set_has_vehicle_restrictions(false);
    return result;
}

JamSegment createJamSegment(JamType jamType) {
    JamSegment jamSegment;
    jamSegment.jamType = jamType;
    return jamSegment;
}
