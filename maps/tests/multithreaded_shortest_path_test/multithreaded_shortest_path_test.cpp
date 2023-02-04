#include <library/cpp/testing/gtest/gtest.h>
#include <library/cpp/testing/unittest/env.h>

#include <maps/analyzer/libs/shortest_path/impl/shortest_path.h>
#include <maps/analyzer/libs/shortest_path/include/path.h>
#include <maps/libs/road_graph/include/graph.h>

#include <memory>
#include <set>
#include <thread>
#include <random>


using namespace maps::road_graph;
using namespace maps::analyzer::shortest_path;


const std::string PATH_TO_ROAD_GRAPH = BinaryPath("maps/data/test/graph3/road_graph.fb");
constexpr std::size_t NUM_TESTS = 10;


struct ShortestPathTest {

    ShortestPathTest() : graph(PATH_TO_ROAD_GRAPH) { }

    void findShortestPath()
    {
        std::default_random_engine generator;
        std::uniform_int_distribution<int> distribution(0, graph.verticesNumber().value() - 1);
        const auto gen = [&]() { return distribution(generator); };

        for(std::size_t iter = 0; iter < NUM_TESTS; ++iter) {
            VertexId source = VertexId(gen());
            VertexId target = VertexId(gen());
            const auto path = findEdgesPath(
                graph,
                source,
                target,
                300000.0
            );
        }
    }

    Graph graph;
};

TEST(MultiThreadedShortest, FindShortestPath) {

    std::vector<std::thread> threads;

    constexpr std::size_t numThreads = 8;

    for (std::size_t i = 0; i < numThreads; ++i) {
        threads.emplace_back(
            &ShortestPathTest::findShortestPath,
            std::make_unique<ShortestPathTest>()
        );
    }

    for (auto& th: threads) {
        th.join();
    }
}
