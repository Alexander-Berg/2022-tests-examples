#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/shortest_path/include/concat.h>
#include <maps/analyzer/libs/shortest_path/include/shortest_paths_finder.h>
#include <maps/libs/road_graph/include/graph.h>

#include <string>
#include <limits>
#include <memory>


using namespace maps::analyzer::shortest_path;
using maps::road_graph::EdgeId;
using maps::road_graph::SegmentId;
using maps::road_graph::SegmentIndex;
using maps::road_graph::SegmentPart;

struct PathTest : public ::testing::Test {
    PathTest():
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

    void checkEqualTraces(const PathTrace& lhs, const PathTrace& rhs) {
        auto lIter = iteratePath(lhs, roadGraph);
        auto rIter = iteratePath(rhs, roadGraph);

        while (lIter != SegmentPartIterator{} && rIter != SegmentPartIterator{}) {
            auto l = *lIter++;
            auto r = *rIter++;
            EXPECT_EQ(l, r);
        }
        EXPECT_TRUE((lIter == SegmentPartIterator{} && rIter == SegmentPartIterator{}));
    }

    SegmentPart seg(std::uint32_t edgeId, std::size_t index, double from, double to) {
        return SegmentPart{{EdgeId{edgeId}, SegmentIndex{index}}, from, to};
    }

    PointOnGraph pt(std::uint32_t edgeId, std::size_t index, double pos) {
        return PointOnGraph{{EdgeId{edgeId}, SegmentIndex{index}}, pos};
    }

    maps::road_graph::Graph roadGraph;
};




TEST_F(PathTest, UnifyEmptyPathTrace) {
    PathTrace trace{};
    auto utrace = unifiedPathTrace(trace, roadGraph);
    EXPECT_TRUE(utrace.unified());
    EXPECT_TRUE(utrace.empty());
}

TEST_F(PathTest, UnifyOneEdgePathTrace) {
    PathTrace trace{
        .edges = {EdgeId{0}},
    };
    EXPECT_FALSE(trace.unified());
    auto utrace = unifiedPathTrace(trace, roadGraph);
    EXPECT_TRUE(utrace.unified());
    checkEqualTraces(trace, utrace);
}

TEST_F(PathTest, UnifyTwoEdgePathTrace) {
    PathTrace trace{
        .edges = {EdgeId{0}, EdgeId{1}},
    };
    EXPECT_FALSE(trace.unified());
    auto utrace = unifiedPathTrace(trace, roadGraph);
    EXPECT_TRUE(utrace.unified());
    EXPECT_NO_THROW(utrace.source());
    EXPECT_NO_THROW(utrace.target());
    checkEqualTraces(trace, utrace);
}

TEST_F(PathTest, UnifyTargetOnlyPathTrace) {
    PathTrace trace{
        .toTarget = {
            seg(0, 0, 0.5, 1.0),
            seg(0, 1, 0.0, 0.5),
        }
    };
    EXPECT_FALSE(trace.unified());
    auto utrace = unifiedPathTrace(trace, roadGraph);
    EXPECT_TRUE(utrace.unified());
    EXPECT_NO_THROW(utrace.source());
    EXPECT_NO_THROW(utrace.target());
    checkEqualTraces(trace, utrace);
}

TEST_F(PathTest, UnifySourceAndEdgePathTrace) {
    PathTrace trace{
        .fromSource = {
            seg(0, 0, 0.5, 1.0),
            seg(0, 1, 0.0, 1.0),
        },
        .edges = {EdgeId{1}},
    };
    EXPECT_FALSE(trace.unified());
    auto utrace = unifiedPathTrace(trace, roadGraph);
    EXPECT_TRUE(utrace.unified());
    EXPECT_NO_THROW(utrace.source());
    EXPECT_NO_THROW(utrace.target());
    checkEqualTraces(trace, utrace);
}

TEST_F(PathTest, UnifyUnifiedPathTrace) {
    PathTrace trace{
        .fromSource = {
            seg(0, 0, 0.5, 1.0),
            seg(0, 1, 0.0, 1.0),
        },
        .edges = {EdgeId{1}},
        .toTarget = {
            seg(2, 0, 0.0, 1.0),
            seg(2, 1, 0.0, 0.5),
        }
    };
    EXPECT_TRUE(trace.unified());
    auto utrace = unifiedPathTrace(trace, roadGraph);
    EXPECT_TRUE(utrace.unified());
    EXPECT_NO_THROW(utrace.source());
    EXPECT_NO_THROW(utrace.target());
    checkEqualTraces(trace, utrace);
}

TEST_F(PathTest, ConcatPaths) {
    // left paths both ends in one point
    PathTrace shortLeft{
        .fromSource = {seg(2, 0, 0.3, 0.6)},
    };
    PathTrace longLeft{
        .fromSource = {seg(0, 0, 0.2, 1.0), seg(0, 1, 0.0, 1.0)},
        .edges = {EdgeId{1}},
        .toTarget = {seg(2, 0, 0.0, 0.6)},
    };
    // right paths both starts from one point
    PathTrace shortRight{
        .fromSource = {seg(2, 0, 0.6, 0.9)},
    };
    PathTrace longRight{
        .fromSource = {seg(2, 0, 0.6, 1.0), seg(2, 1, 0.0, 1.0)},
        .edges = {EdgeId{3}},
        .toTarget = {seg(4, 0, 0.0, 0.5)},
    };

    const auto checkConcat = [&](const PathTrace& lhs, const PathTrace& rhs) {
        EXPECT_NO_THROW(lhs + rhs);
        const auto s = lhs + rhs;
        EXPECT_EQ(s.source(), lhs.source());
        EXPECT_EQ(s.target(), rhs.target());
        EXPECT_TRUE(s.unified());
    };

    checkConcat(shortLeft, shortRight);
    checkConcat(shortLeft, longRight);
    checkConcat(longLeft, shortRight);
    checkConcat(longLeft, longRight);

    EXPECT_THROW(shortRight + shortLeft, maps::Exception);
    EXPECT_THROW(shortRight + longLeft, maps::Exception);
    EXPECT_THROW(longRight + shortLeft, maps::Exception);
    EXPECT_THROW(longRight + longLeft, maps::Exception);
}

TEST_F(PathTest, ConcatPathThroughSegment) {
    PathTrace lhs{
        .fromSource = {seg(0, 0, 0.0, 1.0)},
    };
    PathTrace rhs{
        .fromSource = {seg(0, 1, 0.0, 1.0)},
    };
    EXPECT_NO_THROW(lhs + rhs);
    const auto s = lhs + rhs;
    EXPECT_EQ(s.source(), lhs.source());
    EXPECT_EQ(s.target(), rhs.target());
    EXPECT_TRUE(s.unified());
}

TEST_F(PathTest, ConcatPathTracesThroughVertex) {
    // edge 0 -> edge 3
    PathTrace lhs{
        .fromSource = {seg(0, 0, 0.3, 1.0)},
    };
    PathTrace rhs{
        .fromSource = {seg(3, 0, 0.0, 0.3)},
    };

    EXPECT_THROW(concatPathTraces(lhs, rhs), maps::Exception);
    EXPECT_NO_THROW(concatPathTraces(lhs, rhs, roadGraph));

    PathTrace empty{};
    EXPECT_NO_THROW(concatPathTraces(lhs, empty, roadGraph));
    EXPECT_NO_THROW(concatPathTraces(empty, lhs, roadGraph));
}

TEST_F(PathTest, ConcatPathTracesThroughVertexFail) {
    // edge 0 -/> edge 10
    PathTrace lhs{
        .fromSource = {seg(0, 0, 0.3, 1.0)},
    };
    PathTrace rhs{
        .fromSource = {seg(10, 0, 0.0, 0.3)},
    };

    EXPECT_THROW(concatPathTraces(lhs, rhs), maps::Exception);
    EXPECT_THROW(concatPathTraces(lhs, rhs, roadGraph), maps::Exception);
}

TEST_F(PathTest, ConcatPathTracesThroughVertexFailSegmentsNumber) {
    // edge 10 (7 segments) -> edge 85356
    PathTrace lhsFail{
        .fromSource = {seg(10, 5, 0.3, 1.0)},
    };
    PathTrace lhsGood{
        .fromSource = {seg(10, 6, 0.3, 1.0)},
    };
    PathTrace rhs{
        .fromSource = {seg(85356, 0, 0.0, 0.3)},
    };

    EXPECT_THROW(concatPathTraces(lhsFail, rhs, roadGraph), maps::Exception);
    EXPECT_NO_THROW(concatPathTraces(lhsGood, rhs, roadGraph));
}

TEST_F(PathTest, ConcatPathsThroughVertex) {
    // edge 0 -> edge 3
    Path lhs{
        .source = pt(0, 0, 0.3),
        .target = pt(0, 0, 1.0),
        .info = PathInfo::from(seg(0, 0, 0.3, 1.0), roadGraph),
        .trace = {{.fromSource = {seg(0, 0, 0.3, 1.0)}}},
    };
    Path rhs{
        .source = pt(3, 0, 0.0),
        .target = pt(3, 0, 0.3),
        .info = PathInfo::from(seg(3, 0, 0.0, 0.3), roadGraph),
        .trace = {{.fromSource = {seg(3, 0, 0.0, 0.3)}}},
    };

    EXPECT_THROW(concatPaths(lhs, rhs), maps::Exception);
    EXPECT_NO_THROW(concatPaths(lhs, rhs, roadGraph));
}
