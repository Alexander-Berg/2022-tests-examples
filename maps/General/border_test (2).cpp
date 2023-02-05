#include "library/cpp/testing/unittest/env.h"
#include "library/cpp/testing/unittest/registar.h"

#include "packer.h"
#include "test_cost_calculator.h"
#include "test_graph.h"
#include "test_util.h"
#include "edge_expanded_graph.h"

#include <unordered_set>
#include <unordered_map>
#include <vector>

#include <maps/libs/locale/include/convert.h>

namespace coverage = maps::coverage5;

namespace {

void verifyBorderToBorderWeights(
        const TestEdgeExpandedGraph& expandedGraph,
        const od::Border& border) {
    for (EdgeId i = 0; i != border.inEdgeIds()->size(); ++i) {
        for (EdgeId j = 0; j != border.outEdgeIds()->size(); ++j) {
            UNIT_ASSERT(weightsAreClose(
                decodeWeight(border.weights()->Get(
                    i * border.outEdgeIds()->size() + j)),
                findPath(
                    expandedGraph,
                    border.inEdgeIds()->Get(i),
                    border.outEdgeIds()->Get(j)).weight,
                // Allow for some loss of precision when adding compressed
                // weights
                2));
        }
    }
}

void verifyEdgeToFromBorderWeights(
        const TestEdgeExpandedGraph& expandedGraph,
        const od::Border& border,
        const fb::Vector<uint16_t>& borderWeights,
        bool toBorder) {
    for (Id edgeId = 0; edgeId != expandedGraph.verticesNumber(); ++edgeId) {
        const auto borderWeight = decodeWeight(borderWeights.Get(edgeId));
        int32_t closestWeight = std::numeric_limits<int32_t>::max();
        const auto* edgeIds = toBorder ?
            border.outEdgeIds() : border.inEdgeIds();
        for (const auto borderEdgeId: *edgeIds) {
            const auto source = toBorder ? edgeId : borderEdgeId;
            const auto target = toBorder ? borderEdgeId : edgeId;
            const auto weight = findPath(expandedGraph, source, target).weight;
            UNIT_ASSERT(
                borderWeight < weight ||
                weightsAreClose(borderWeight, weight, 2));

            closestWeight = std::min(closestWeight, weight);
        }

        // Allow for some loss of precision when adding compressed
        // weights
        UNIT_ASSERT(weightsAreClose(borderWeight, closestWeight, 2));
    }
}

} // namespace

Y_UNIT_TEST_SUITE(OfflineBorder) {
    Y_UNIT_TEST(BorderTest) {
        buildSmallGraph();
        maps::road_graph::Graph roadGraph(TEST_ROAD_GRAPH_PATH);
        maps::road_graph::PersistentIndex persistentIdx(TEST_PERSISTENT_INDEX_PATH);
        maps::succinct_rtree::Rtree rtree(TEST_RTREE_PATH, roadGraph);
        coverage::Coverage coverage(TEST_COVERAGE_DIR);

        flatbuffers::FlatBufferBuilder fbBuilder;

        TestCostCalculatorImpl costCalculator(&roadGraph, 0.);

        Packer(
            &rtree,
            &roadGraph,
            maps::locale::to<maps::locale::Locale>("ru_RU"),
            &persistentIdx,
            &costCalculator,
            BinaryPath("maps/data/test/geobase/geodata5.bin"),
            &fbBuilder).pack(
                coverage[TEST_COVERAGE_LAYER], {TEST_REGION_ID});

        auto drivingData = od::GetDrivingData(fbBuilder.GetBufferPointer());
        TestEdgeExpandedGraph testGraph(drivingData->edgeExpandedGraph());

        verifyBorderToBorderWeights(testGraph, *drivingData->border());
        verifyEdgeToFromBorderWeights(
            testGraph,
            *drivingData->border(),
            *drivingData->graph()->weightsToBorder(),
            /* toBorder = */ true);
        verifyEdgeToFromBorderWeights(
            testGraph,
            *drivingData->border(),
            *drivingData->graph()->weightsFromBorder(),
            /* toBorder = */ false);
    }
}
