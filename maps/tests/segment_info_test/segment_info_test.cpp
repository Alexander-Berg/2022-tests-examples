#include "test_tools.h"

#include <library/cpp/testing/gtest/gtest.h>

#include <maps/analyzer/libs/data/include/segment_info.h>
#include <maps/analyzer/libs/common/include/types.h>

#include <string>

namespace mad = maps::analyzer::data;
using maps::analyzer::shortest_path::LongSegmentId;
using maps::road_graph::EdgeId;
using maps::road_graph::LongEdgeId;
using maps::road_graph::SegmentId;
using maps::road_graph::SegmentIndex;

TEST(SegmentInfoTests, SegmentInfo) {
    mad::SegmentInfo s;

    std::string version = "#$%";
    s.setGraphVersion(version);
    EXPECT_EQ(s.graphVersion(), version);

    SegmentId segmentId{EdgeId{5}, SegmentIndex{7}};
    s.setSegmentId(segmentId);
    EXPECT_EQ(s.segmentId(), segmentId);

    auto persId = s.persistentEdgeId();
    EXPECT_TRUE(!persId);
    EXPECT_THROW(s.persistentSegmentId(), maps::Exception);
    s.setPersistentEdgeId(LongEdgeId(12345));
    persId = s.persistentEdgeId();
    EXPECT_TRUE(persId && persId->value() == 12345);
    EXPECT_EQ(
        s.persistentSegmentId(),
        (LongSegmentId{LongEdgeId(12345), SegmentIndex{7}})
    );

    double segmentSpeed = 16.0;
    s.setAverageSpeed(segmentSpeed);
    EXPECT_EQ(s.averageSpeed(), segmentSpeed);

    size_t usersNumber = 9;
    s.setUsersNumber(usersNumber);
    EXPECT_EQ(s.usersNumber(), usersNumber);

    EXPECT_EQ(s.signalsNumber(), 0ul);
    mad::GpsSignal s1, s2;
    s1.setClid("1");
    s2.setClid("2");
    s.addSignal(s1);
    s.addSignal(s2);
    EXPECT_EQ(s.signalsNumber(), 2ul);
    EXPECT_EQ(s.signal(0).debugString(), s1.debugString());
    EXPECT_EQ(s.signal(1).debugString(), s2.debugString());
}
