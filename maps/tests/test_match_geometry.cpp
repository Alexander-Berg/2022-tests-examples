#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/graphmatching/include/match_geometry.h>

#include <maps/analyzer/libs/shortest_path/include/types.h>
#include <maps/libs/geolib/include/polyline.h>
#include <maps/libs/common/include/exception.h>
#include <maps/analyzer/libs/common/include/tnode_helpers.h>

#include <library/cpp/yson/node/node_io.h>
#include <util/stream/file.h>


namespace mag = maps::analyzer::graphmatching;
using maps::geolib3::Polyline2;
using maps::analyzer::shortest_path::LongSegmentId;
using maps::road_graph::LongEdgeId;
using maps::road_graph::SegmentIndex;


const TString TEST_DATA = ArcadiaSourceRoot() + "/maps/analyzer/libs/graphmatching/tests/data/geometries.yson";
constexpr auto MmapSimple = EMappingMode::Standard;


struct MatchGeometryTest : public ::testing::Test {
    MatchGeometryTest()
        : roadGraph(BinaryPath("maps/data/test/graph3/road_graph.fb"), MmapSimple)
        , persistentIndex(BinaryPath("maps/data/test/graph3/edges_persistent_index.fb"), MmapSimple)
        , rtree(BinaryPath("maps/data/test/graph3/rtree.fb"), roadGraph)
    {}

    maps::road_graph::Graph roadGraph;
    maps::road_graph::PersistentIndex persistentIndex;
    maps::succinct_rtree::Rtree rtree;
};


TEST_F(MatchGeometryTest, MatchGeometry) {
    TFileInput input(TEST_DATA);
    auto node = NYT::NodeFromYsonStream(&input, ::NYson::EYsonType::ListFragment);

    for (const auto& row: node.AsList()) {
        auto pline = Polyline2{maps::analyzer::parseGeometry(row)};
        auto canon = maps::analyzer::parseRoute(row);

        auto result = mag::matchGeometry(
            pline,
            roadGraph,
            rtree,
            persistentIndex,
            0.2
        );

        ASSERT_TRUE(result.solid());
        EXPECT_EQ(result.parts[0].segments, canon);
    }
}
