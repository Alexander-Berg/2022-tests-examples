#include "../common.h"

#include <maps/analyzer/libs/tie_here_jams/include/convert_task_processor.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/system/fs.h>
#include <util/folder/path.h>

#include <string>
#include <unordered_set>

namespace tie_here_jams = maps::analyzer::tie_here_jams;

Y_UNIT_TEST_SUITE(ConvertTaskProcessorTest) {
    Y_UNIT_TEST_F(ConvertTaskProcess, GeoIdFixture) {
        maps::road_graph::Graph graph{ROAD_GRAPH_DATA_PATH};
        maps::succinct_rtree::Rtree edgesRTree{EDGES_RTREE_PATH, graph};
        maps::road_graph::PersistentIndex persistentIndex{EDGES_PERSISTENT_INDEX_PATH};

        tie_here_jams::ConvertTaskProcessor processor(persistentIndex);

        tie_here_jams::ConvertTask task(
            JAMS_XML_FILE, "log_name", graph, edgesRTree, persistentIndex, getGeoId()
        );

        processor(task, STORAGE_FILE);
        auto convertedJams = processor.convertedJams();

        const maps::jams::router::Jams etalonJams(persistentIndex, RESULT_JAMS_FILE);

        EXPECT_EQ(convertedJams.timestamp(), etalonJams.timestamp());
        EXPECT_EQ(convertedJams.graphVersion(), etalonJams.graphVersion());

        EXPECT_EQ(convertedJams.jamsCount(), etalonJams.jamsCount());

        for (const auto& [edgeId, edgeJam]: convertedJams.jams()) {
            const auto etalonJam = etalonJams.jam(edgeId);
            EXPECT_TRUE(etalonJam);
            EXPECT_TRUE(edgeJam.region() != 0);
            EXPECT_DOUBLE_EQ(edgeJam.speed(), etalonJam->speed());
        }
    }
}
