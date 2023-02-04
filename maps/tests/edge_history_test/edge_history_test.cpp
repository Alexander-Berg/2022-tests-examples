#include "../test_tools.h"

#include <boost/date_time/posix_time/posix_time.hpp>
#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/config.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/edge_history.h>
#include <maps/libs/road_graph/include/types.h>

#include <unordered_set>

namespace pt = boost::posix_time;


Y_UNIT_TEST_SUITE(EdgeHistoryTest)
{
    Y_UNIT_TEST(IsStanding)
    {
        std::unordered_set<std::string> uuids;
        const auto noUuidsDefaultFilter = UuidFilter(uuids, JamsType::DEFAULT, true);
        Config config(makeSegmentshandlerConfig("segmentshandler.conf"));
        EdgeHistory edgeHistory;
        using maps::road_graph::EdgeId;
        using maps::road_graph::SegmentIndex;
        const maps::road_graph::SegmentId seg_zeroth = seg(0, 0);
        const maps::road_graph::SegmentId seg_first = seg(0, 1);
        const maps::road_graph::SegmentId seg_second = seg(0, 2);

        const auto vehicleId = ma::VehicleId("a", "a");
        const auto standingTime = maps::nowUtc();
        const auto step = pt::seconds(30);
        //empty
        UNIT_ASSERT(!edgeHistory.isStanding(standingTime, config, noUuidsDefaultFilter));

        UNIT_ASSERT(edgeHistory.add(createTravelTime(
            seg_zeroth, standingTime, ma::VehicleId("a", "a"), 1.0, 1.0
        )));
        //only travel time segment
        UNIT_ASSERT(!edgeHistory.isStanding(standingTime, config, noUuidsDefaultFilter));

        UNIT_ASSERT(edgeHistory.add(createStandingSegment(
            seg_first, standingTime, ma::VehicleId("b", "b")
        )));
        //standing exists but no moving in past
        UNIT_ASSERT(!edgeHistory.isStanding(standingTime, config, noUuidsDefaultFilter));

        UNIT_ASSERT(edgeHistory.add(createTravelTime(
            seg_first, standingTime - step, ma::VehicleId("a", "a"), 1.0, 1.0
        )));
        //moving in past was added but standing was deleted as unconfirmed
        UNIT_ASSERT(!edgeHistory.isStanding(standingTime, config, noUuidsDefaultFilter));

        UNIT_ASSERT(edgeHistory.add(createStandingSegment(
            seg_first, standingTime, ma::VehicleId("b", "b")
        )));
        // ok
        UNIT_ASSERT(edgeHistory.isStanding(standingTime, config, noUuidsDefaultFilter));

        UNIT_ASSERT(edgeHistory.add(createTravelTime(
            seg_second, standingTime + step, ma::VehicleId("c", "c"), 1.0, 1.0
        )));
        // ok, passing on other segment
        UNIT_ASSERT(edgeHistory.isStanding(standingTime, config, noUuidsDefaultFilter));

        UNIT_ASSERT(edgeHistory.add(createTravelTime(
            seg_first, standingTime + step, ma::VehicleId("d", "d"), 1.0, 1.0
        )));
        // because of passing
        UNIT_ASSERT(!edgeHistory.isStanding(standingTime, config, noUuidsDefaultFilter));
    }
}
