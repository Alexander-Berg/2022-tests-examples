#include <maps/jams/tools/tie-xml-jams/lib/include/xml_jams.h>
#include <maps/analyzer/libs/geoinfo/include/geoid.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/gtest.h>

#include <util/system/fs.h>
#include <util/folder/path.h>

#include <fstream>
#include <string>
#include <unordered_set>

namespace tools = maps::jams::tools;

const std::string TEST_DATA_ROOT = ArcadiaSourceRoot() + "/maps/jams/tools/tie-xml-jams/tests/data/";
const TString COVERAGE_DIR = "coverage5";

const std::string ROAD_GRAPH_DATA_PATH = BinaryPath("maps/data/test/graph3/road_graph.fb");
const std::string EDGES_RTREE_PATH = BinaryPath("maps/data/test/graph3/rtree.fb");
const std::string EDGES_PERSISTENT_INDEX_PATH = BinaryPath("maps/data/test/graph3/edges_persistent_index.fb");

struct GeoIdFixture : public NUnitTest::TBaseFixture {
    GeoIdFixture(): geoId{std::string(BinaryPath("maps/data/test/geoid/geoid.mms.1").c_str())} {}
    maps::geoinfo::GeoId geoId;
};

Y_UNIT_TEST_SUITE(XmlJamsTests) {
    Y_UNIT_TEST(FiltersRegions) {
        using tools::internal::passRegionsFilter;
        EXPECT_TRUE(passRegionsFilter({1, 2, 3, 4}, {2, 8}));
        EXPECT_TRUE(passRegionsFilter({1, 2, 3, 4}, {}));
        EXPECT_TRUE(passRegionsFilter({1, 2, 3, 4}, {5, 6, 4}));
        EXPECT_TRUE(passRegionsFilter({}, {}));
        EXPECT_FALSE(passRegionsFilter({1, 2, 3, 4}, {5, 6, 7}));
        EXPECT_FALSE(passRegionsFilter({1, 2, 3, 4}, {8}));
        EXPECT_FALSE(passRegionsFilter({}, {2}));
    }

    Y_UNIT_TEST_F(XmlJamsConverts, GeoIdFixture) {
        maps::road_graph::Graph graph{ROAD_GRAPH_DATA_PATH};
        maps::succinct_rtree::Rtree edgesRTree{EDGES_RTREE_PATH, graph};
        maps::road_graph::PersistentIndex persistentIndex{EDGES_PERSISTENT_INDEX_PATH};

        const auto loadJams = [&](const std::unordered_set<std::size_t>& regions, std::optional<std::uint32_t> maxCategory = {}) {
            std::ifstream jamsFile(TEST_DATA_ROOT + "jams.xml");
            return tools::convertXmlJams(
                jamsFile,
                graph,
                edgesRTree,
                persistentIndex,
                geoId,
                regions,
                maxCategory
            );
        };

        const maps::jams::router::Jams etalonJams(persistentIndex, TEST_DATA_ROOT + "jams.bin");
        const auto checkJams = [&](const auto& jams) {
            EXPECT_EQ(jams.timestamp(), 1542209400);
            EXPECT_EQ(jams.graphVersion(), "3.0.0-0");
            EXPECT_EQ(jams.jamsCount(), 193);

            EXPECT_EQ(jams.jamsCount(), etalonJams.jamsCount());
            for (const auto& [edgeId, edgeJam]: jams.jams()) {
                const auto etalonJam = etalonJams.jam(edgeId);
                EXPECT_TRUE(etalonJam);
                EXPECT_TRUE(edgeJam.region() != 0);
                EXPECT_DOUBLE_EQ(edgeJam.speed(), etalonJam->speed());
            }
        };

        const auto jamsEmpty = loadJams({100}); // some non-Moscow region, test graph doesn't have it so there should be no jams
        EXPECT_EQ(jamsEmpty.jamsCount(), 0);

        const auto jamsAll = loadJams({});
        checkJams(jamsAll);

        const auto jamsMoscow = loadJams({213});
        checkJams(jamsMoscow);

        const auto jamsRussia = loadJams({1});
        checkJams(jamsRussia);

        constexpr std::uint32_t MAX_CATEGORY = 5;
        const auto categoryJams = loadJams({}, MAX_CATEGORY);
        for (const auto& etalonEdgeId: etalonJams.affectedEdges()) {
            const auto edgeCategory = graph.edgeData(etalonEdgeId).category();
            const auto hasJam = static_cast<bool>(categoryJams.jam(etalonEdgeId));
            EXPECT_EQ(edgeCategory <= MAX_CATEGORY, hasJam);
        }
    }

    Y_UNIT_TEST_F(XmlJamsBans, GeoIdFixture) {
        maps::road_graph::Graph graph{ROAD_GRAPH_DATA_PATH};
        maps::succinct_rtree::Rtree edgesRTree{EDGES_RTREE_PATH, graph};
        maps::road_graph::PersistentIndex persistentIndex{EDGES_PERSISTENT_INDEX_PATH};

        std::ifstream jamsFile(TEST_DATA_ROOT + "jams.xml");
        auto jams = tools::convertXmlJams(jamsFile, graph, edgesRTree, persistentIndex, geoId);
        const auto affectedEdges = jams.affectedEdges();
        std::unordered_set<maps::road_graph::EdgeId> affectedEdgesSet(
            affectedEdges.begin(),
            affectedEdges.end());

        //const auto jamsCount = jams.jamsCount();

        std::ifstream banFile(TEST_DATA_ROOT + "ban.yson");
        const auto bannedPlines = tools::parseBanList(banFile);
        //std::size_t unmatchedPlines = 0;
        //std::size_t totalBannedEdges = 0;
        for (const auto& pline: bannedPlines) {
            //const auto bannedEdges = tools::setDefaultSpeeds(jams, pline, graph, edgesRTree, persistentIndex, {});
            tools::setDefaultSpeeds(jams, pline, graph, edgesRTree, persistentIndex, geoId);
            /*if (!bannedEdges) {
                ++unmatchedPlines;
            } else {
                totalBannedEdges += bannedEdges;
            }*/
        }
        /*
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
        EXPECT_EQ(overwrittenEdges, jamsCount + totalBannedEdges - jams.jamsCount());*/
    }
}
