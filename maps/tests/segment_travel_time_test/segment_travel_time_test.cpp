#include "test_tools.h"

#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/data/include/segment_travel_time.h>
#include <maps/libs/road_graph/include/types.h>
#include <maps/libs/edge_persistent_index/include/types.h>

#include <boost/date_time/posix_time/posix_time.hpp>

#include <string>


namespace ma = maps::analyzer;
namespace mad = maps::analyzer::data;
namespace pt = boost::posix_time;

TEST(SegmentTravelTimeTests, SegmentInfo) {
    mad::SegmentTravelTime s;
    EXPECT_TRUE(!s.enterTime());
    EXPECT_TRUE(!s.standingStartTime());
    EXPECT_TRUE(!s.travelTime());
    EXPECT_TRUE(!s.travelTimeBase());
    EXPECT_TRUE(!s.persistentEdgeId());
    EXPECT_TRUE(!s.manoeuvreId());

    maps::road_graph::SegmentId segmentId{maps::road_graph::EdgeId(5), maps::road_graph::SegmentIndex(7)};
    s.setSegmentId(segmentId);
    EXPECT_TRUE(s.segmentId() == segmentId);

    using maps::road_graph::LongEdgeId;
    auto persId = s.persistentEdgeId();
    EXPECT_TRUE(!persId);
    s.setPersistentEdgeId(LongEdgeId(12345));
    persId = s.persistentEdgeId();
    EXPECT_TRUE(persId && persId->value() == 12345);

    EXPECT_TRUE(s.segmentId() == segmentId);

    const auto manoeuvreId = ma::manoeuvres::ManoeuvreId(123654);
    s.setManoeuvreId(manoeuvreId);
    EXPECT_EQ(s.manoeuvreId(), manoeuvreId);

    double travelTime = 16.0;
    s.setTravelTime(travelTime);
    EXPECT_EQ(s.travelTime(), travelTime);

    double travelTimeBase = 8.0;
    s.setTravelTimeBase(travelTimeBase);
    EXPECT_EQ(s.travelTimeBase(), travelTimeBase);

    pt::ptime time = maps::fromTimestamp(3910);
    s.setEnterTime(time);
    EXPECT_EQ(s.enterTime(), time);

    pt::ptime standingStartTime = maps::fromTimestamp(1234);
    s.setStandingStartTime(standingStartTime);
    EXPECT_EQ(s.standingStartTime(), standingStartTime);

    size_t regionId = 50;
    s.setRegionId(regionId);
    EXPECT_EQ(s.regionId(), regionId);

    ma::VehicleId vehicleId("14", "13");
    s.setVehicleId(vehicleId);
    EXPECT_EQ(s.vehicleId(), vehicleId);

    pt::ptime time0 = maps::fromTimestamp(39100);
    pt::ptime time1 = maps::fromTimestamp(391000);
    s.pushSignalTime(time0);
    s.pushSignalTime(time1);
    EXPECT_EQ(s.signalsSize(), 2ul);
    EXPECT_EQ(s.signalTimeAt(0), time0);
    EXPECT_EQ(s.signalTimeAt(1), time1);
}

TEST(SegmentTravelTimeTests, SegmentInfoCollection) {
    mad::SegmentTravelTime s;
    mad::SegmentTravelTimeCollection c;

    std::string version = "#$%";
    c.setGraphVersion(version);
    EXPECT_EQ(c.graphVersion(), version);

    c.push(s);
    c.push(s);
    EXPECT_EQ(c.size(), 2ul);
}
