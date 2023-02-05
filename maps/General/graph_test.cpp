#include "library/cpp/testing/unittest/env.h"
#include "library/cpp/testing/unittest/registar.h"

#include "edge_data_checker.h"
#include "graph_data_checker.h"
#include "lanes_checker.h"
#include "manoeuvre_annotations_checker.h"
#include "packer.h"
#include "test_cost_calculator.h"
#include "test_graph.h"
#include "test_util.h"
#include "topology_checker.h"

#include <contrib/libs/flatbuffers/include/flatbuffers/flatbuffers.h>

#include <maps/libs/locale/include/convert.h>

namespace coverage = maps::coverage5;

Y_UNIT_TEST_SUITE(OfflineDataPacker) {
    Y_UNIT_TEST(Graph) {
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
        GraphTopologyChecker topologyChecker {drivingData};

        topologyChecker();
        GraphDataChecker{&topologyChecker, drivingData}();
        EdgeDataChecker{&topologyChecker, drivingData, &roadGraph}();
        LanesChecker{&topologyChecker, &roadGraph, drivingData}();
        ManoeuvreAnnotationsChecker{&topologyChecker, &roadGraph, drivingData}();
    }
}
