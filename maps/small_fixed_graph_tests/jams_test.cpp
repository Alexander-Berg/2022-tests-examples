#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/edge_persistent_index/packer/lib/include/persistent_index_builder.h>
#include <maps/libs/leptidea/bin/create_data/lib/graph_data_patch_fb_serializer.h>
#include <maps/libs/leptidea/bin/create_data/lib/graph_data_serializer.h>
#include <maps/libs/leptidea/bin/create_topology/lib/topology_builder.h>
#include <maps/libs/leptidea/include/graph_data_patch_fb.h>
#include <maps/libs/leptidea/tests/lib/test_data_generation.h>
#include <maps/libs/leptidea/tests/lib/test_leptideas.h>
#include <maps/libs/leptidea/tests/lib/test_utils.h>

#include <yandex/maps/jams/router/jams.h>
#include <yandex/maps/jams/router/closures.h>

#include <util/generic/buffer.h>

#include <cstdlib>
#include <sstream>

using namespace leptidea7;
using namespace maps::road_graph::literals;

namespace mjr = maps::jams::router;

double getDuration(const MultilevelPath& path) {
    return path.weight().duration().value();
}

/*
 *                 4(A)
 *                 ^\
 *                 | \
 *              e3 |  \ e5
 *             l=8 |   \l=8
 *                 |    \
 *   e0      e1    |     \    e4      e6
 *   l=2     l=24  |      V   l=3     l=1
 * 0------>1------>2------>3------>5------>6
 *     S              e2            T
 *                    l=12
 *
 *   ST (Optimal Path), l=40
 */

struct JamsTests: TTestBase, SmallGraph {
    JamsTests(): SmallGraph(
        TopologyDescription{{0, 1}, {2, 3, 4}, {5, 6}},
        Edges{
            {0, 1}, {1, 2}, {2, 3}, {2, 4}, {3, 5}, {4, 3}, {5, 6}},
        edgesData(),
        {},
        {},
        {})
    {
        const auto edgesNumber = edgesData().size();

        maps::road_graph::PersistentIndexBuilder indexBuilder("Unit test graph");
        indexBuilder.reserve(edgesNumber);

        // Build collision for all edges and get randomized long ids on build()
        for (size_t i = 0; i < edgesNumber; ++i) {
            indexBuilder.setEdgePersistentId(
                maps::road_graph::EdgeId(i),
                maps::road_graph::LongEdgeId(0));
        }

        indexPtr = std::make_unique<maps::road_graph::PersistentIndex>(
            indexBuilder.build());
    }

    static std::vector<rg::MutableEdgeData> edgesData() {
        std::vector<rg::MutableEdgeData> edgesData(
            7, buildEdgeData(1 /*speed*/, false /*toll*/, rg::AccessId::Automobile));
        setLengths(&edgesData, {2, 24, 12, 8, 3, 8, 1});
        return edgesData;
    }

    PatchedGraphDataHolder createPatchedGraphData(
            const mjr::Jams& jams, mjr::Closures closures) {
        const std::string JAMS_FILENAME = "unit_test_jams.pb";
        jams.save(JAMS_FILENAME);

        const std::string CLOSURES_FILENAME = "unit_test_closures.pb";
        closures.save(CLOSURES_FILENAME);

        MutableGraphData patchedGraphData = buildPatchedGraphData(
            l6aData(),
            &index(),
            {JAMS_FILENAME}, // read jams only
            "dummy_external_jams_path",
            CLOSURES_FILENAME, // read closures
            false, // preserve turn penalties
            Penalty(), // penalty for entering a closure
            Penalty(), // penalty for manoeuvre closure
            Penalty(), // penalty for access pass
            1, // threadsNumber
            false, // ignoreEdgesSlowdown
            0, // closuresTimeBias
            roadGraph()->builtForAccessIdMask(),
            nullptr // tzData
            );

        return PatchedGraphDataHolder(
            serializedGraphData(),
            GraphDataPatchFbSerializer(std::move(patchedGraphData)).serializeToBuffer(),
            topology());
    }

    MultilevelPath findOptimalPath(const GraphData& graphData) const {
        const PositionOnEdge sourcePosition{
            0_e, EdgePosition{.5f}};
        const Source source{sourcePosition, {}};
        const Target target{6_e, {}};

        const auto alternatives =
            leptidea()->findAlternatives({source}, {target}, graphData, PathFindOptions{});

        UNIT_ASSERT_EQUAL(alternatives.size(), 1);
        return alternatives.front();
    }

    mjr::Jams makeTestJams() const {
        mjr::Jams jams(index());
        return jams;
    }

    mjr::Closures makeTestClosures() const {
        mjr::Closures closures(index(), time_t{0});
        return closures;
    }


    const maps::road_graph::PersistentIndex& index() const {
        return *indexPtr;
    }

    std::unique_ptr<maps::road_graph::PersistentIndex> indexPtr;

    UNIT_TEST_SUITE(JamsTests);
    UNIT_TEST(noJamsTest);
    UNIT_TEST(emptyJamsTest);
    UNIT_TEST(oneStaticJamChangesPathWeightTest);
    UNIT_TEST(oneStaticJamSlowdownChangesPathTest);
    UNIT_TEST(oneStaticJamChangesPathTopologyTest);
    UNIT_TEST(ignoringJamSlowdownTest);
    UNIT_TEST(manyJamsTest);
    UNIT_TEST_SUITE_END();

    void noJamsTest() {
        const auto path = findOptimalPath(*l6aData());
        UNIT_ASSERT_DOUBLES_EQUAL(getDuration(path), 40, 1e-3);
    }

    void emptyJamsTest() {
        const PatchedGraphDataHolder dataHolder = createPatchedGraphData(
            makeTestJams(), makeTestClosures());
        const auto path = findOptimalPath(*dataHolder);
        UNIT_ASSERT_DOUBLES_EQUAL(getDuration(path), 40, 1e-3);
    }

    // Jams change shortest path weight, but its topology is the same.
    void oneStaticJamChangesPathWeightTest() {
        auto jams = makeTestJams();
        jams.addJam(1_e, 2.);

        const PatchedGraphDataHolder dataHolder = createPatchedGraphData(
            jams, makeTestClosures());
        const auto path = findOptimalPath(*dataHolder);
        UNIT_ASSERT_DOUBLES_EQUAL(getDuration(path), 28, 1e-3);
    }

    void oneStaticJamSlowdownChangesPathTest() {
        auto jams = makeTestJams();
        jams.addJam(2_e, 0.5);

        const PatchedGraphDataHolder dataHolder = createPatchedGraphData(
            jams, makeTestClosures());
        const auto path = findOptimalPath(*dataHolder);
        UNIT_ASSERT_DOUBLES_EQUAL(getDuration(path), 44, 0.01);
    }

    void ignoringJamSlowdownTest() {
        auto jams = makeTestJams();
        jams.addJam(2_e, 0.95);

        const PatchedGraphDataHolder dataHolder = createPatchedGraphData(
            jams, makeTestClosures());
        const auto path = findOptimalPath(*dataHolder);
        UNIT_ASSERT_DOUBLES_EQUAL(getDuration(path), 40, 1e-3);
    }

    // Different shortest path caused by jams.
    void oneStaticJamChangesPathTopologyTest() {
        auto jams = makeTestJams();
        jams.addJam(3_e, 4.);

        const PatchedGraphDataHolder dataHolder = createPatchedGraphData(
            jams, makeTestClosures());
        const auto path = findOptimalPath(*dataHolder);
        UNIT_ASSERT_DOUBLES_EQUAL(getDuration(path), 38, 1e-3);
    }

    /*
     *                 4(A)
     *                 ^\
     *                 | \
     *              e3 |  \ e5
     *             t=2 |   \t=4
     *                 |    \
     *    e0      e1   |     \    e4      e6
     *    t=4     t=3  |      V   t=1     t=5
     * 0------>1------>2------>3------>5------>6
     *     S              e2            T
     *                    t=4
     */
    void manyJamsTest() {
        auto jams = makeTestJams();
        jams.addJam(0_e, 0.5);
        jams.addJam(1_e, 8);
        jams.addJam(2_e, 3);
        jams.addJam(3_e, 4);
        jams.addJam(4_e, 3);
        jams.addJam(5_e, 8);
        jams.addJam(6_e, 0.5);

        const PatchedGraphDataHolder dataHolder = createPatchedGraphData(
            jams, makeTestClosures());
        const auto path = findOptimalPath(*dataHolder);
        UNIT_ASSERT_DOUBLES_EQUAL(getDuration(path), 9, 1e-3);
    }
};

UNIT_TEST_SUITE_REGISTRATION(JamsTests);
