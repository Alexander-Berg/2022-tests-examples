#include <library/cpp/testing/unittest/registar.h>

#include "graph_5_vertices_6_edges_fixture.h"
#include <maps/libs/leptidea/tests/lib/test_utils.h>

struct Graph5V6ETest : public TTestBase, Graph5V6EFixture {
    UNIT_TEST_SUITE(Graph5V6ETest);
    UNIT_TEST(checkShortestShortcut);
    UNIT_TEST_SUITE_END();

    void checkShortestShortcut() {
        auto findShortcut = [&](
            EdgeId sourceEdgeId,
            EdgeId targetEdgeId,
            RoadTraits desiredRoadTraits)
        {
            const GraphComponentId component = l6aData()
                ->topology()
                .targetComponentAtLevel(sourceEdgeId, LevelId(1));

            return l6aData()
                ->shortcuts(component)
                .optimalShortcutBetween(
                    l6aData()->topology().inEdgeIndex(sourceEdgeId, component),
                    l6aData()->topology().outEdgeIndex(targetEdgeId, component),
                    Constraints{desiredRoadTraits})
                .decodePath();
        };

        const auto shortcutWithTolls = makeVector<EdgeId>({5, 0, 2, 3});
        UNIT_ASSERT_EQUAL(
            findShortcut(EdgeId(5), EdgeId(3), traits),
            shortcutWithTolls);

        const auto tollFreeShortcut = makeVector<EdgeId>({5, 1, 4, 3});
        UNIT_ASSERT_EQUAL(
            findShortcut(EdgeId(5), EdgeId(3), tollFreeTraits),
            tollFreeShortcut);

        const auto directTransition = makeVector<EdgeId>({3, 5});
        UNIT_ASSERT_EQUAL(
            findShortcut(EdgeId(3), EdgeId(5), tollFreeTraits),
            directTransition);
        }
};

UNIT_TEST_SUITE_REGISTRATION(Graph5V6ETest);
