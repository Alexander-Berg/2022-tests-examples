#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/leptidea/tests/lib/test_data_generation.h>
#include <maps/libs/leptidea/tests/lib/test_leptideas.h>
#include <maps/libs/leptidea/tests/lib/test_utils.h>

#include <maps/libs/leptidea/include/leptidea.h>
#include <maps/libs/leptidea/include/types.h>

#include <maps/libs/leptidea/lib/three_stage_search.h>

using namespace leptidea7;
using namespace maps::road_graph::literals;

namespace {

/*
*                        e11
*                 9=============>10
*                 ^               |
*                 |               | e12
*              e4 |               |
*    e0      e1   |  e3      e5   v  e6
* 0------>1------>2------>3------>4------>11
*         |                       ^
*      e2 |                       |
*         |                       | e10
*         v  e7      e8      e9   |
*         5======>6======>7======>8
*
*
* ------> courtyard edge (8th category)
* ======> non courtyard edge (7th category)
*/

using AltEdges = std::vector<EdgeId>;

SmallGraph makeGraphWithAlternatives() {
    TopologyDescription topologyDescription{
        {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11}};

    Edges edges{
        {0, 1}, {1, 2}, {1, 5}, {2, 3}, {2, 9}, {3, 4}, {4, 11},
        {5, 6}, {6, 7}, {7, 8}, {8, 4}, {9, 10}, {10, 4}};

    std::vector<rg::MutableEdgeData> edgesData(edges.size(),
        buildEdgeData(1 /*speed*/, false /*toll*/, rg::AccessId::Truck));

    const std::unordered_set<size_t> courtyardEdges = {0, 1, 2, 3, 4, 5, 6, 10, 12};
    for (size_t edgeId = 0; edgeId < edgesData.size(); ++edgeId) {
        edgesData[edgeId].category = courtyardEdges.contains(edgeId) ? 8 : 7;
    }

    edgesData[11].length = 2;

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

Alternatives findAlternatives(EdgeId sourceEdgeId, EdgeId targetEdgeId) {
    const auto graph = makeGraphWithAlternatives();

    PositionOnEdge sourcePosition{sourceEdgeId, EdgePosition{0}};
    Source source{sourcePosition, Weight{}};
    Target target{targetEdgeId, Weight{}};
    AlternativesConfig altConfig {
        .alternativesNumber = 100,
        .searchPredicate = [&](EdgeId edgeId) {
            return graph.roadGraph()->edgeData(edgeId).category() < 8;
        }
    };

    const auto checkedLeptidea = makeFastLeptidea(graph.topology());
    PathFindOptions options;
    options.desiredConstraints(Constraints{RoadTrait::AllowedForTruck});
    auto alternatives = findAlternativesByThreeStageSearch(
        {source}, {target}, *graph.l6aData(), options, altConfig,
        *checkedLeptidea);

    return alternatives;
}

double getLength(const MultilevelPath& alternative) {
    const auto& start = alternative.pathEdges.front().arrivalWeight;
    const auto& finish = alternative.pathEdges.back().arrivalWeight;
    return (finish - start).length().value();
}

} // namespace

Y_UNIT_TEST_SUITE(ThreeStageSearch) {

Y_UNIT_TEST(found_alternative_through_non_courtyard)
{
    const auto alternatives = findAlternatives(0_e, 6_e);
    const AltEdges expectedAlternativeEdges = {0_e, 2_e, 7_e, 8_e, 9_e, 10_e, 6_e};

    UNIT_ASSERT_EQUAL(alternatives.size(), 1);
    UNIT_ASSERT_EQUAL(getAltEdges(alternatives[0]), expectedAlternativeEdges);
    UNIT_ASSERT_DOUBLES_EQUAL(getLength(alternatives[0]), 6, 1e-4);
}

Y_UNIT_TEST(found_alternative_through_one_non_courtyard_edge)
{
    const auto alternatives = findAlternatives(1_e, 6_e);
    const AltEdges expectedAlternativeEdges = {1_e, 4_e, 11_e, 12_e, 6_e};

    UNIT_ASSERT_EQUAL(alternatives.size(), 1);
    UNIT_ASSERT_EQUAL(getAltEdges(alternatives[0]), expectedAlternativeEdges);
    UNIT_ASSERT_DOUBLES_EQUAL(getLength(alternatives[0]), 5, 1e-4);
}

Y_UNIT_TEST(no_alternative_through_non_courtyard)
{
    const auto alternatives = findAlternatives(3_e, 6_e);

    UNIT_ASSERT(alternatives.empty());
}

}
