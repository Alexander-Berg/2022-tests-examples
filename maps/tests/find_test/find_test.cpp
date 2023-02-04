#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/shortest_path/include/shortest_paths_finder.h>
#include <maps/libs/road_graph/include/graph.h>

#include <string>
#include <limits>
#include <memory>

using namespace maps::analyzer::shortest_path;

struct FindTest : public ::testing::Test {
    FindTest():
        roadGraph(BinaryPath("maps/data/test/graph3/road_graph.fb"))
    {}

    std::optional<Path> find(ShortestPathsFinder& spFinder, std::pair<std::uint64_t, std::uint64_t> edges, double maxDistance = std::numeric_limits<double>::infinity()) {
        auto [src, dst] = edges;

        return spFinder.find(
            PointOnGraph{maps::road_graph::SegmentId{maps::road_graph::EdgeId(src), maps::road_graph::SegmentIndex(0)}, 0.0},
            PointOnGraph{maps::road_graph::SegmentId{maps::road_graph::EdgeId(dst), maps::road_graph::SegmentIndex(0)}, 0.0},
            maxDistance
        );
    }

    // test cases
    std::pair<std::uint64_t, std::uint64_t> testCase5km = {204019, 203774};
    std::pair<std::uint64_t, std::uint64_t> testCase2_5km = {204019, 153410};
    std::pair<std::uint64_t, std::uint64_t> testCase400m = {204019, 90789};
    std::pair<std::uint64_t, std::uint64_t> testCaseLefortTunnel = {54682, 99822};

    maps::road_graph::Graph roadGraph;
};


TEST_F(FindTest, TestFindWithCache) {
    ShortestPathsFinder spFinder(roadGraph, std::make_shared<ShortestPathsLRUCache>(1024, 1024, 3000.0)); // 1024 cache size + 3km max distance

    // over max distance, should not find
    EXPECT_TRUE(!find(spFinder, testCase5km).has_value());
    // should find
    EXPECT_TRUE(find(spFinder, testCase2_5km).has_value());
    EXPECT_TRUE(find(spFinder, testCase400m).has_value());
    // explicit max distance less than path length, should not find
    EXPECT_TRUE(!find(spFinder, testCase2_5km, 1000.0).has_value());
    EXPECT_TRUE(!find(spFinder, testCase400m, 100.0).has_value());
}

TEST_F(FindTest, TestFindWithNoCache) {
    ShortestPathsFinder spFinder(roadGraph, false);

    // should find
    EXPECT_TRUE(find(spFinder, testCase5km).has_value());
    EXPECT_TRUE(find(spFinder, testCase2_5km).has_value());
    EXPECT_TRUE(find(spFinder, testCase400m).has_value());
    EXPECT_TRUE(find(spFinder, testCase5km, 6000.0).has_value());
    EXPECT_TRUE(find(spFinder, testCase2_5km, 3000.0).has_value());
    EXPECT_TRUE(find(spFinder, testCase400m, 1000.0).has_value());
    // explicit max distance less than path length, should not find
    EXPECT_TRUE(!find(spFinder, testCase5km, 4000.0).has_value());
    EXPECT_TRUE(!find(spFinder, testCase2_5km, 2000.0).has_value());
    EXPECT_TRUE(!find(spFinder, testCase400m, 200.0).has_value());
}

TEST_F(FindTest, TestFindTunnel) {
    ShortestPathsFinder spFinder(roadGraph);

    // should find lefortovo tunnel even though max distance equal to 1km
    EXPECT_TRUE(find(spFinder, testCaseLefortTunnel).has_value());
}
