#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/leptidea/tests/lib/test_data_generation.h>
#include <maps/libs/leptidea/tests/lib/query_generation.h>
#include <maps/libs/leptidea/tests/lib/test_utils.h>

#include <random>

Weight runChainGraphTest(
        const SmallGraph& graph,
        const Source& source,
        EdgeId targetEdge) {
    const Query query{
        {source},
        {{targetEdge, Weight::zero()}},
        PathFindOptions().desiredConstraints(Constraints{RoadTrait::AllowedForAuto})};
    const auto path = findOptimalPath(
        graph.leptidea().get(), *graph.l6aData(), query);
    UNIT_ASSERT(path);
    const auto& pathEdges = path->pathEdges;
    const auto edgeFrom = source.positionOnEdge.edgeId.value();
    UNIT_ASSERT_DOUBLES_EQUAL(
        pathEdges.back().arrivalWeight.duration().value(),
        targetEdge.value() -
        (edgeFrom + source.positionOnEdge.position.value()),
        pathEdges.back().arrivalWeight.duration().value() * 1e-2);
    std::vector<EdgeId> correctPath;
    for (size_t e = edgeFrom; e <= targetEdge.value(); ++e) {
        correctPath.emplace_back(e);
    }
    UNIT_ASSERT_EQUAL(edgeIds(pathEdges), correctPath);

    return pathEdges.back().arrivalWeight;
}

void runWeightsTest(
        const SmallGraph& graph,
        const Source& source,
        EdgeId targetEdge,
        Weight correctWeight) {

    const auto target = PositionOnEdge{targetEdge, EdgePosition{0}};

    auto leptidea = graph.leptidea();

    auto solm = leptidea->findOptimalWeightsMatrix(
        {{source}},
        {{target}},
        *graph.l6aData(),
        PathFindOptions()
    );

    UNIT_ASSERT_EQUAL(solm.size(), 1);
    UNIT_ASSERT_EQUAL(solm[0].size(), 1);
    UNIT_ASSERT_DOUBLES_EQUAL(
        correctWeight.duration().value(),
        solm[0][0].duration().value(),
        1e-2
    );
}

Edges connectAdjacentVertices(size_t numVertices) {
    Edges edges;
    for (size_t i = 0; i + 1 < numVertices; ++i) {
        edges.emplace_back(i, i + 1);
    }
    std::sort(edges.begin(), edges.end());
    return edges;
}

Y_UNIT_TEST_SUITE(ChainGraphRandomPartition) {

Y_UNIT_TEST(chain_graph_random_partition_tests) {
    std::mt19937 engine(1343);
    auto randIntUpTo = [&engine](size_t maxValue) {
        return std::uniform_int_distribution<size_t>(0, maxValue)(engine);
    };

    const size_t NUM_TESTS = 100;
    auto randomPosition = [&] {
        const size_t denominator = 1 + randIntUpTo(4); // [1...5]
        const size_t numerator = randIntUpTo(denominator);
        return EdgePosition{1.0f * numerator / denominator};
    };

    for (size_t nestingLevel = 0; nestingLevel <= 5; ++nestingLevel) {
        for (size_t maxArity = 1; maxArity <= 3ul; ++maxArity) {
            auto getArity = [&] { return 1 + randIntUpTo(maxArity - 1); };
            for (size_t testId = 0; testId < NUM_TESTS; ++testId) {
                const auto topo = genTopology(0, nestingLevel, getArity);
                const auto edges =
                    connectAdjacentVertices(topo.vertices().size());
                const SmallGraph smallGraph(topo, edges, {});
                if (edges.size() < 2) {
                    continue;
                }
                const size_t from = randIntUpTo(edges.size() - 2);
                const size_t to =
                    from + 1 + randIntUpTo(edges.size() - 1 - (from + 1));
                const Source source{
                    {EdgeId(from), randomPosition()},
                    Weight::zero()};
                const auto target = EdgeId(to);
                auto pathWeight = runChainGraphTest(smallGraph, source, target);
                runWeightsTest(smallGraph, source, target, pathWeight);
            }
        }
    }
}

}
