#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/shortest_path/include/shortest_paths_finder.h>
#include <maps/analyzer/libs/shortest_path/impl/shortest_path.h>
#include <maps/analyzer/libs/shortest_path/impl/util.h>

#include <algorithm>
#include <ctime>
#include <vector>


using namespace maps::road_graph;
using namespace maps::analyzer::shortest_path;

const std::string PATH_TO_ROAD_GRAPH = BinaryPath("maps/data/test/graph3/road_graph.fb");

struct ShortestPathPrecalcTest : public ::testing::Test {
    ShortestPathPrecalcTest(): graph(PATH_TO_ROAD_GRAPH) {}

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

    maps::road_graph::Graph graph;
};


TEST_F(ShortestPathPrecalcTest, CheckAdjacent) {
    std::srand(std::time(0));

    int N = 100;
    for(int i = 0; i < N; ++i) {
        const EdgeId sourceEdgeId = graph.base(EdgeId(rand() % graph.edgesNumber().value()));
        SegmentIndex sourceSegmentIndex = SegmentIndex(0); //TODO RANDOM
        SegmentId sourceSegmentId = SegmentId{sourceEdgeId, sourceSegmentIndex};

        VertexId sourceEdgeTargetVertexId = graph.edge(sourceEdgeId).target;
        auto sourceEdgeAdjacentRange = graph.outEdges(sourceEdgeTargetVertexId);

        if(sourceEdgeAdjacentRange.empty()) {
            continue;
        }

        EdgeId targetEdgeIdAdj = (*sourceEdgeAdjacentRange.begin()).id;

        SegmentIndex targetSegmentIndexAdj = SegmentIndex(0);
        SegmentId targetSegmentIdAdj = SegmentId{targetEdgeIdAdj, targetSegmentIndexAdj};

        PointOnGraph sourcePt = PointOnGraph{sourceSegmentId, 0.0};
        PointOnGraph targetPtAdj = PointOnGraph{targetSegmentIdAdj, 0.0};

        const auto forbidden = isForbiddenTurn(graph, sourceEdgeId, targetEdgeIdAdj);

        double maxDist = 1000.0;

        ShortestPathsFinder spFinder(graph);
        std::optional<Path> pathWithoutCache = spFinder.find(sourcePt, targetPtAdj, maxDist);
        ASSERT_TRUE(pathWithoutCache.has_value());
        EXPECT_EQ(pathWithoutCache->info.forbiddenTurns > 0, forbidden);
    }
}

TEST_F(ShortestPathPrecalcTest, CompareOnlineRandom) {
    std::srand(std::time(0));

    int N = 100;

    for(int i = 0; i < N; ++i) {
        VertexId sourceVertexId = VertexId(rand() % graph.verticesNumber().value());
        VertexId targetVertexId = VertexId(rand() % graph.verticesNumber().value());

        if (graph.outEdges(sourceVertexId).empty() || graph.outEdges(targetVertexId).empty()) {
            continue;
        }

        EdgeId sourceEdgeId = graph.base((*graph.outEdges(sourceVertexId).begin()).id);
        EdgeId targetEdgeId = graph.base((*graph.outEdges(targetVertexId).begin()).id);

        srand(time(0));
        double maxDist = 1000.0;
        PointOnGraph sourcePointOnGraph = {{sourceEdgeId, SegmentIndex(0)}, 1.0};
        PointOnGraph targetPointOnGraph = {{targetEdgeId, SegmentIndex(0)}, 0.0};

        sourceVertexId = graph.edge(sourcePointOnGraph.segmentId.edgeId).target;

        const auto pathWithoutCache = findEdgesPath(
            graph,
            sourceVertexId,
            targetVertexId,
            maxDist
        );

        ShortestPathsFinder spFinder(graph);
        auto foundPath = spFinder.find(sourcePointOnGraph, targetPointOnGraph, maxDist);
        if (!foundPath.has_value()) {
            // no path found, ensure `path` is empty too
            EXPECT_TRUE(pathWithoutCache.empty());
        }
        else {
            // path found, restore and ensure result equals to `path` edges
            spFinder.restore(*foundPath);
            ASSERT_TRUE(foundPath->trace.has_value());
            std::vector<EdgeId> foundPathIdsList = foundPath->trace->edges;

            ASSERT_EQ(pathWithoutCache.size(), foundPathIdsList.size());

            for(size_t i = 0; i < pathWithoutCache.size(); ++i) {
                EXPECT_EQ(pathWithoutCache[i], foundPathIdsList[i]);
            }

            // restore one more time and ensure nothing affected
            spFinder.restore(*foundPath);
            EXPECT_EQ(pathWithoutCache.size(), foundPath->trace->edges.size());
        }
    }
}

TEST_F(ShortestPathPrecalcTest, ShortestPathFind) {
    std::srand(42);

    const int N = 1000;
    const double maxDist = 3000.0;

    for(int i = 0; i < N; ++i) {
        const EdgeId edgeId = EdgeId(rand() % graph.edgesNumber().value());
        const auto edge = graph.edge(edgeId);
        const auto edgeData = graph.edgeData(edgeId);

        // On the same edge
        const PointOnGraph targetPoint = {{edgeId, SegmentIndex(edgeData.geometry().segmentsNumber() - 1)}, 1.};
        const PointOnGraph sourcePoint = {{edgeId, SegmentIndex(0)}, 0.};
        ShortestPathsFinder spFinder(graph);
        std::optional<Path> foundPath = spFinder.find(sourcePoint, targetPoint, maxDist);
        ASSERT_TRUE(foundPath.has_value());
        spFinder.restore(*foundPath);
        EXPECT_TRUE(foundPath->trace.has_value());

        // On the adjacent edges
        const VertexId targetVertexId = edge.target;
        if (graph.outEdges(targetVertexId).empty()) {
            continue;
        }
        const EdgeId outEdgeId = (*graph.outEdges(targetVertexId).begin()).id;

        const auto forbidden = isForbiddenTurn(graph, edgeId, outEdgeId);

        const auto outEdgeData = graph.edgeData(outEdgeId);
        const PointOnGraph outEdgePoint = {{outEdgeId, SegmentIndex(outEdgeData.geometry().segmentsNumber() - 1)}, 1.};
        std::optional<Path> pathWithAdjacent = spFinder.find(sourcePoint, outEdgePoint, maxDist);
        ASSERT_TRUE(pathWithAdjacent.has_value());
        EXPECT_EQ(pathWithAdjacent->info.forbiddenTurns > 0, forbidden);
        spFinder.restore(*pathWithAdjacent);
        EXPECT_TRUE(pathWithAdjacent->trace.has_value());
    }
}
