#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/leptidea/include/types.h>

#include <maps/libs/leptidea/bin/create_data/lib/levels_data_builder.h>
#include <maps/libs/leptidea/tests/lib/test_data_generation.h>
#include <maps/libs/leptidea/tests/lib/test_leptideas.h>
#include <maps/libs/leptidea/tests/lib/query_generation.h>

#include <util/random/random.h>
#include <util/random/shuffle.h>

#include <optional>

namespace {

Edges createRandomEdges(size_t numVertices) {
    Edges edges;

    if (numVertices <= 1) {
        return edges;
    }

    for (size_t vertexFrom = 0; vertexFrom < numVertices; ++vertexFrom) {
        Edges outgoingEdges;

        for (size_t vertexTo = 0; vertexTo < numVertices; ++vertexTo) {
            if (vertexFrom != vertexTo) {
                outgoingEdges.emplace_back(vertexFrom, vertexTo);
            }
        }

        Shuffle(outgoingEdges.begin(), outgoingEdges.end());
        edges.insert(
            edges.end(),
            outgoingEdges.begin(),
            outgoingEdges.begin() + 1 + RandomNumber(numVertices - 1));
    }

    std::sort(edges.begin(), edges.end());
    return edges;
}

std::vector<rg::MutableVehicleConstraint> createRandomEdgesVehicleConstraints(size_t numEdges) {
    auto createVehicleConstraints = [] (
            float weightLimit,
            float axleWeightLimit,
            float maxWeightLimit,
            float heightLimit,
            float widthLimit,
            float lengthLimit,
            float payloadLimit,
            uint8_t ecoClass,
            bool trailerNotAllowed) {
        return maps::road_graph::MutableVehicleConstraint {
            maps::road_graph::AccessId::Automobile
                | maps::road_graph::AccessId::Taxi
                | maps::road_graph::AccessId::Truck,
            "" /* universalId */,
            "" /* passId */,
            weightLimit,
            axleWeightLimit,
            maxWeightLimit,
            heightLimit,
            widthLimit,
            lengthLimit,
            payloadLimit,
            ecoClass,
            trailerNotAllowed,
            true /* strict */
        };
    };

    std::vector<float> constraints = {
        leptidea7::Constraints::MAX_LIMIT_VALUE * 0.25f,
        leptidea7::Constraints::MAX_LIMIT_VALUE * 0.5f,
        leptidea7::Constraints::MAX_LIMIT_VALUE * 0.75f,
        leptidea7::Constraints::MAX_LIMIT_VALUE};

    std::vector<rg::MutableVehicleConstraint> res(numEdges);
    std::mt19937 gen;
    std::uniform_int_distribution<> dis(0, 3);
    std::uniform_int_distribution<> ecoClassDis(
        0, leptidea7::Constraints::BEST_ECO_CLASS);
    std::uniform_int_distribution<> trailerNotAllowedDis(
        0, 1);

    for (size_t index = 0; index < numEdges; ++index) {
        if (dis(gen) == 0) {
            res[index] = createVehicleConstraints(
                constraints[dis(gen)] /* weightLimit */,
                constraints[dis(gen)] /* axleWeightLimit */,
                constraints[dis(gen)] /* maxWeightLimit */,
                constraints[dis(gen)] /* heightLimit */,
                constraints[dis(gen)] /* widthLimit */,
                constraints[dis(gen)] /* lengthLimit */,
                constraints[dis(gen)] /* payloadLimit */,
                ecoClassDis(gen),
                static_cast<bool>(trailerNotAllowedDis(gen))
            );
        } else {
            res[index] = createVehicleConstraints(
                leptidea7::Constraints::MAX_LIMIT_VALUE /* weightLimit */,
                leptidea7::Constraints::MAX_LIMIT_VALUE /* axleWeightLimit */,
                leptidea7::Constraints::MAX_LIMIT_VALUE /* maxWeightLimit */,
                leptidea7::Constraints::MAX_LIMIT_VALUE /* heightLimit */,
                leptidea7::Constraints::MAX_LIMIT_VALUE /* widthLimit */,
                leptidea7::Constraints::MAX_LIMIT_VALUE /* lengthLimit */,
                leptidea7::Constraints::MAX_LIMIT_VALUE /* payloadLimit */,
                leptidea7::Constraints::maxValue().minEcoClass(),
                leptidea7::Constraints::maxValue().trailerNotAllowed()
            );
        }
    }

    return res;
}

} // namespace

Y_UNIT_TEST_SUITE(RandomGraph) {

Y_UNIT_TEST(random_graph_correctness_tests) {
    ResetRandomState();

    // TODO: smart choose parameters using arity, level, num vertices in topo
    const size_t NUM_GRAPHS = 30;
    const size_t NUM_QUERIES_PER_GRAPH = 100;
    const size_t MATRIX_SIZE = 10;
    for (size_t nestingLevel = 0; nestingLevel <= 3; ++nestingLevel) {
        for (size_t maxArity = 2; maxArity <= 3ul; ++maxArity) {
            auto getArity = [&] { return 1 + RandomNumber(maxArity); };
            for (size_t numGraphs = 0; numGraphs < NUM_GRAPHS; ++numGraphs) {
                const auto topo = genTopology(0, nestingLevel, getArity);
                const auto edges =
                    createRandomEdges(topo.vertices().size());
                const SmallGraph smallGraph(topo, edges, {});

                const EndpointsGenerator endpointsGenerator{
                    1, 1e5f, smallGraph.roadGraph()};
                RandomQueriesGenerator queriesGenerator{
                    endpointsGenerator,
                    std::numeric_limits<double>::infinity()};
                if (edges.empty()) {
                    continue;
                }
                auto fastLeptidea = makeLeptidea(smallGraph.topology());
                auto correctLeptidea =
                    makeCorrectLeptidea(smallGraph.topology());

                std::vector<Sources> matrixSources;
                std::vector<std::vector<PositionOnEdge>> matrixTargets;

                for (size_t qId = 0; qId < NUM_QUERIES_PER_GRAPH; ++qId) {
                    const auto query = queriesGenerator();
                    std::optional<double> error = comparePaths(
                        fastLeptidea.get(),
                        correctLeptidea.get(),
                        *smallGraph.l6aData(),
                        *smallGraph.roadGraph(),
                        query);
                    UNIT_ASSERT(!error || error < 1e-3);

                    matrixSources.emplace_back(query.sources);
                    matrixTargets.emplace_back();
                    for (const auto& target: query.targets) {
                        matrixTargets.back().emplace_back(target.edgeId, EdgePosition{0});
                    }
                }

                ASSERT(MATRIX_SIZE < NUM_QUERIES_PER_GRAPH);
                matrixSources.resize(MATRIX_SIZE);
                matrixTargets.resize(MATRIX_SIZE);

                auto matrix1 = fastLeptidea->findOptimalWeightsMatrix(
                    matrixSources,
                    matrixTargets,
                    *smallGraph.l6aData(),
                    PathFindOptions{}
                );

                auto matrix2 = correctLeptidea->findOptimalWeightsMatrix(
                    matrixSources,
                    matrixTargets,
                    *smallGraph.l6aData(),
                    PathFindOptions{}
                );

                UNIT_ASSERT_EQUAL(matrix1.size(), matrix2.size());
                for (auto i: makeRange(matrix1.size())) {
                    UNIT_ASSERT_EQUAL(matrix1[i].size(), matrix2[i].size());
                    for (auto j: makeRange(matrix1[i].size())) {
                        const Weight& w1 = matrix1[i][j];
                        const Weight& w2 = matrix2[i][j];
                        UNIT_ASSERT_DOUBLES_EQUAL(w1.duration().value(), w2.duration().value(), 5e-2);
                        UNIT_ASSERT_DOUBLES_EQUAL(w1.length().value(), w2.length().value(), 5e-1);
                        UNIT_ASSERT_DOUBLES_EQUAL(w1.penalty().value(), w2.penalty().value(), 5e-2);
                    }
                }
            }
        }
    }
}

Y_UNIT_TEST(random_graph_correctness_tests_with_constraints) {
    ResetRandomState();

    // TODO: smart choose parameters using arity, level, num vertices in topo
    const size_t NUM_GRAPHS = 30;
    const size_t NUM_QUERIES_PER_GRAPH = 200;
    const size_t MATRIX_SIZE = 10;
    for (size_t nestingLevel = 0; nestingLevel <= 3; ++nestingLevel) {
        for (size_t maxArity = 2; maxArity <= 3ul; ++maxArity) {
            auto getArity = [&] { return 1 + RandomNumber(maxArity); };
            for (size_t numGraphs = 0; numGraphs < NUM_GRAPHS; ++numGraphs) {
                const auto topo = genTopology(0, nestingLevel, getArity);
                const auto edges =
                    createRandomEdges(topo.vertices().size());
                const auto edgesVehicleConstraints =
                    createRandomEdgesVehicleConstraints(edges.size());

                const SmallGraph smallGraph(topo, edges, edgesVehicleConstraints);

                const EndpointsGenerator endpointsGenerator{
                    1, 1e5f, smallGraph.roadGraph()};
                RandomQueriesGenerator queriesGenerator{
                    endpointsGenerator,
                    std::numeric_limits<double>::infinity()};
                if (edges.empty()) {
                    continue;
                }
                auto fastLeptidea = makeLeptidea(smallGraph.topology());
                auto correctLeptidea =
                    makeCorrectLeptidea(smallGraph.topology());

                std::vector<Sources> matrixSources;
                std::vector<std::vector<PositionOnEdge>> matrixTargets;

                for (size_t qId = 0; qId < NUM_QUERIES_PER_GRAPH; ++qId) {
                    auto query = queriesGenerator();

                    query = Query{
                        std::move(query.sources),
                        std::move(query.targets),
                        query.pathFindOptions
                    };

                    std::optional<double> error = comparePaths(
                        fastLeptidea.get(),
                        correctLeptidea.get(),
                        *smallGraph.l6aData(),
                        *smallGraph.roadGraph(),
                        query);

                    UNIT_ASSERT(!error || error < 1e-3);

                    matrixSources.emplace_back(query.sources);
                    matrixTargets.emplace_back();
                    for (const auto& target: query.targets) {
                        matrixTargets.back().emplace_back(target.edgeId, EdgePosition{0});
                    }
                }

                ASSERT(MATRIX_SIZE < NUM_QUERIES_PER_GRAPH);
                matrixSources.resize(MATRIX_SIZE);
                matrixTargets.resize(MATRIX_SIZE);

                PathFindOptions options;
                options.desiredConstraints(Constraints{leptidea7::RoadTrait::AllowedForTruck});
                const auto matrix1 = fastLeptidea->findOptimalWeightsMatrix(
                    matrixSources,
                    matrixTargets,
                    *smallGraph.l6aData(),
                    options
                );

                const auto matrix2 = correctLeptidea->findOptimalWeightsMatrix(
                    matrixSources,
                    matrixTargets,
                    *smallGraph.l6aData(),
                    options
                );

                UNIT_ASSERT_EQUAL(matrix1.size(), matrix2.size());
                for (auto i: makeRange(matrix1.size())) {
                    UNIT_ASSERT_EQUAL(matrix1[i].size(), matrix2[i].size());
                    for (auto j: makeRange(matrix1[i].size())) {
                        const Weight& w1 = matrix1[i][j];
                        const Weight& w2 = matrix2[i][j];
                        UNIT_ASSERT_DOUBLES_EQUAL(w1.duration().value(), w2.duration().value(), 5e-2);
                        UNIT_ASSERT_DOUBLES_EQUAL(w1.length().value(), w2.length().value(), 5e-1);
                        UNIT_ASSERT_DOUBLES_EQUAL(w1.penalty().value(), w2.penalty().value(), 5e-2);
                    }
                }
            }
        }
    }
}


}
