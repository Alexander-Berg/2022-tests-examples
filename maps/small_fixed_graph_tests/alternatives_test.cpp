#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/leptidea/tests/lib/test_data_generation.h>
#include <maps/libs/leptidea/tests/lib/test_leptideas.h>
#include <maps/libs/leptidea/tests/lib/test_utils.h>

#include <maps/libs/leptidea/include/leptidea.h>
#include <maps/libs/leptidea/include/types.h>

using namespace leptidea7;

/*
 *
 *
 *                                 9(A)
 *                                 ^\
 *                                 | \
 *                              e6 |  \ e11
 *                             l=25|   \l=25
 *                                 |    \
 *    e0      e1     e2      e3    |     \    e7      e8      e9      e10
 *   l=12     l=7    l=8     l=15  |      V  l=10    l=13    l=11     l=5
 * 0------>1------>2------>3------>4======>5------>6------>7------>8------>11
 *    S                     \         e5           ^                T
 *                           \       l=30         /
 *                            \                  /
 *                             \                /
 *                              \              /
 *                               \            /
 *                             e4 \          /e12
 *                            l=55 \        / l=50
 *                                  \      /
 *                                   \    /
 *                                    \  /
 *                                     V/
 *                                     10(B)
 *
 *   ---> is toll-free edge, ===> is toll edge
 *
 *   ST (Optimal Path), l=100
 *   SAT (alternative 'a', toll-free optimal path), l=120, sharing=70
 *   SBT (alternative 'b'), l=150, sharing=45
 */

using AltEdges = std::vector<EdgeId>;

const auto ST = makeVector<EdgeId>({0, 1, 2, 3, 5, 7, 8, 9, 10});
const auto SAT = makeVector<EdgeId>({0, 1, 2, 3, 6, 11, 7, 8, 9, 10});
const auto SBT = makeVector<EdgeId>({0, 1, 2, 4, 12, 8, 9, 10});

SmallGraph makeGraphWithAlternatives() {
    TopologyDescription topologyDescription{{0, 1}, {2, 3, 4, 5, 6, 7}, {8, 11}, {9}, {10}};

    Edges edges{
        {0, 1}, {1, 2}, {2, 3}, {3, 4}, {3, 10}, {4, 5}, {4, 9},
        {5, 6}, {6, 7}, {7, 8}, {8, 11}, {9, 5}, {10, 6}};

    std::vector<rg::MutableEdgeData> edgesData(13,
        buildEdgeData(1 /*speed*/, false /*toll*/, rg::AccessId::Automobile));

    edgesData[5].isToll = true;
    edgesData[5].endsWithTollPost = true;

    setLengths(&edgesData, {12, 7, 8, 15, 55, 30, 25, 10, 13, 11, 5, 25, 50});

    return SmallGraph(topologyDescription, edges, edgesData, {}, {}, {});
}

AltEdges getAltEdges(const MultilevelPath& alternative) {
    AltEdges altEdges;
    altEdges.reserve(alternative.pathEdges.size());
    for (const auto& pathEdge: alternative.pathEdges) {
        altEdges.emplace_back(pathEdge.edgeId);
    }
    return altEdges;
}

void checkIfShortestPathIsST(const MultilevelPath& shortestPath) {
    const auto shortestPathLength =
        shortestPath.pathEdges.back().arrivalWeight.length();
    UNIT_ASSERT_DOUBLES_EQUAL(shortestPathLength.value(), 100.0, 1e-4);
    UNIT_ASSERT_EQUAL(getAltEdges(shortestPath), ST);
}

Alternatives findAlternatives(
        float maxSlowdown,
        float maxSharing,
        RoadTraits desiredRoadTraits = RoadTrait::AllowedForAuto) {
    const auto graph = makeGraphWithAlternatives();

    PositionOnEdge sourcePosition{EdgeId(0), EdgePosition{0.5}};
    Source source{sourcePosition, Weight{}};
    Target target{EdgeId(10), Weight{}};
    AlternativesConfig altConfig {
        .alternativesNumber = 100,
        .maxSharing = maxSharing,
        .maxSlowdown = maxSlowdown
    };

    const auto checkedLeptidea = makeFastLeptidea(graph.topology());
    PathFindOptions options;
    options.desiredConstraints(Constraints{desiredRoadTraits});
    auto alternatives = checkedLeptidea->findAlternatives(
        {source}, {target}, *graph.l6aData(), options, altConfig);
    UNIT_ASSERT(!alternatives.empty());
    if (desiredRoadTraits == RoadTrait::AllowedForAuto) {
        checkIfShortestPathIsST(alternatives.front());
    }

    return alternatives;
}

std::ostream& operator<<(
        std::ostream& o, const std::vector<EdgeId>& edges) {
    o << "{ ";
    for (const auto& e: edges) {
        o << e << " ";
    }
    return o << "}";
}

double getLength(const MultilevelPath& alternative) {
    const auto& start = alternative.pathEdges.front().arrivalWeight;
    const auto& finish = alternative.pathEdges.back().arrivalWeight;
    return (finish - start).length().value();
}


Y_UNIT_TEST_SUITE(Alternatives) {

Y_UNIT_TEST(no_alternatives_1_test)
{
    const auto alternatives = findAlternatives(0.19, 0.99);
    UNIT_ASSERT_EQUAL(alternatives.size(), 1);
}

Y_UNIT_TEST(no_alternatives_2_test)
{
    const auto alternatives = findAlternatives(100, 0.44);
    UNIT_ASSERT_EQUAL(alternatives.size(), 1);
}

Y_UNIT_TEST(one_alternative_a_test)
{
    const auto alternatives = findAlternatives(0.21, 0.99);
    UNIT_ASSERT_EQUAL(alternatives.size(), 2);
    UNIT_ASSERT_EQUAL(getAltEdges(alternatives[1]), SAT);
    UNIT_ASSERT_DOUBLES_EQUAL(getLength(alternatives[1]), 120, 1e-4);
}

Y_UNIT_TEST(one_alternative_b_test)
{
    const auto alternatives = findAlternatives(100, 0.46);
    UNIT_ASSERT_EQUAL(alternatives.size(), 2);
    UNIT_ASSERT_EQUAL(getAltEdges(alternatives[1]), SBT);
    UNIT_ASSERT_DOUBLES_EQUAL(getLength(alternatives[1]), 150, 1e-4);
}

Y_UNIT_TEST(both_alternatives_test)
{
    const auto alternatives = findAlternatives(0.51, 0.71);
    UNIT_ASSERT_EQUAL(alternatives.size(), 3);
    UNIT_ASSERT_EQUAL(getAltEdges(alternatives[1]), SAT);
    UNIT_ASSERT_DOUBLES_EQUAL(getLength(alternatives[1]), 120, 1e-4);
    UNIT_ASSERT_EQUAL(getAltEdges(alternatives[2]), SBT);
    UNIT_ASSERT_DOUBLES_EQUAL(getLength(alternatives[2]), 150, 1e-4);
}

Y_UNIT_TEST(toll_free_alternative_test)
{
    const auto alternatives = findAlternatives(
        0.51, 0.71, RoadTrait::TollFree|RoadTrait::AllowedForAuto);
    UNIT_ASSERT_EQUAL(alternatives.size(), 2);
    UNIT_ASSERT_EQUAL(getAltEdges(alternatives[0]), SAT);
    UNIT_ASSERT_DOUBLES_EQUAL(getLength(alternatives[0]), 120, 1e-4);
    UNIT_ASSERT_EQUAL(getAltEdges(alternatives[1]), SBT);
    UNIT_ASSERT_DOUBLES_EQUAL(getLength(alternatives[1]), 150, 1e-4);
}

}
