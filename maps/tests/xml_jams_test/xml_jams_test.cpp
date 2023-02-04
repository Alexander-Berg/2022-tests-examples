#include "../common.h"

#include <maps/analyzer/libs/tie_here_jams/include/xml_jams.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <string>
#include <unordered_set>

namespace tie_here_jams = maps::analyzer::tie_here_jams;

using EdgeId = maps::jams::router::Jams::EdgeId;

Y_UNIT_TEST_SUITE(XmlJamsTests) {
    Y_UNIT_TEST_F(XmlJamsConverts, GeoIdFixture) {
        maps::road_graph::Graph graph{ROAD_GRAPH_DATA_PATH};
        maps::succinct_rtree::Rtree edgesRTree{EDGES_RTREE_PATH, graph};
        maps::road_graph::PersistentIndex persistentIndex{EDGES_PERSISTENT_INDEX_PATH};

        const maps::jams::router::Jams etalonJams(persistentIndex, RESULT_JAMS_FILE);
        const auto checkJams = [&](const auto& jams) {
            EXPECT_EQ(jams.timestamp(), 1571656893);
            EXPECT_EQ(jams.graphVersion(), "3.0.0-0");

            EXPECT_EQ(jams.jamsCount(), etalonJams.jamsCount());

            for (const auto& [edgeId, edgeJam]: jams.jams()) {
                const auto etalonJam = etalonJams.jam(edgeId);
                EXPECT_TRUE(etalonJam);
                EXPECT_TRUE(edgeJam.region() != 0);
                EXPECT_DOUBLE_EQ(edgeJam.speed(), etalonJam->speed());
            }
        };

        tie_here_jams::ConvertTask task(
            JAMS_XML_FILE, "log_name", graph, edgesRTree, persistentIndex, getGeoId()
        );
        task.read(STORAGE_FILE);
        task.convertXmlJams();

        const auto jamsAll = task.getJams();
        checkJams(jamsAll);
    }

    Y_UNIT_TEST_F(XmlJamsBans, GeoIdFixture) {
        maps::road_graph::Graph graph{ROAD_GRAPH_DATA_PATH};
        maps::succinct_rtree::Rtree edgesRTree{EDGES_RTREE_PATH, graph};
        maps::road_graph::PersistentIndex persistentIndex{EDGES_PERSISTENT_INDEX_PATH};

        tie_here_jams::ConvertTask task(
            JAMS_XML_FILE, "log_name", graph, edgesRTree, persistentIndex, getGeoId()
        );
        task.read(STORAGE_FILE);
        task.convertXmlJams();

        auto jams = task.getJams();

        const auto affectedEdges = jams.affectedEdges();
        std::unordered_set<EdgeId> affectedEdgesSet(affectedEdges.begin(), affectedEdges.end());

        const auto jamsCount = jams.jamsCount();

        std::ifstream banFile(BAN_FILE);
        const auto bannedPlines = tie_here_jams::parseBanList(banFile);
        std::size_t unmatchedPlines = 0;
        std::size_t totalBannedEdges = 0;
        for (const auto& pline: bannedPlines) {
            const auto bannedEdges = tie_here_jams::setDefaultSpeeds(jams, pline, graph, edgesRTree, persistentIndex, getGeoId());
            tie_here_jams::setDefaultSpeeds(jams, pline, graph, edgesRTree, persistentIndex, getGeoId());
            if (!bannedEdges) {
                ++unmatchedPlines;
            } else {
                totalBannedEdges += bannedEdges;
            }
        }

        EXPECT_TRUE(unmatchedPlines == 0);

        std::size_t overwrittenEdges = 0;
        std::size_t defaultSpeedsEdges = 0;

        for (const auto& [edgeId, edgeJam]: jams.jams()) {
            const auto defaultSpeed = graph.edgeData(maps::road_graph::EdgeId(edgeId)).speed();
            const auto jamSpeed = edgeJam.speed();
            if (defaultSpeed == jamSpeed) {
                ++defaultSpeedsEdges;
                if (affectedEdgesSet.find(edgeId) != affectedEdgesSet.end()) {
                    ++overwrittenEdges;
                }
            }
        }

        EXPECT_EQ(defaultSpeedsEdges, totalBannedEdges);
        EXPECT_EQ(overwrittenEdges, jamsCount + totalBannedEdges - jams.jamsCount());
    }
}
