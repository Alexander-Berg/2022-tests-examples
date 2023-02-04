#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <maps/analyzer/tools/mapbox_quality/lib/merger.h>

namespace mq = maps::analyzer::tools::mapbox_quality;

const std::string ROAD_GRAPH_FILE = BinaryPath("maps/data/test/graph4/road_graph.fb");

void checkMergedJams(const mq::MergedJams& lhs, const mq::MergedJams& rhs) {
    EXPECT_EQ(lhs.size(), rhs.size());

    for (const auto& [k, v]: lhs) {
        const auto it = rhs.find(k);
        EXPECT_NE(it, rhs.end());
        EXPECT_NEAR(v.time, it->second.time, 1e-3);
        EXPECT_NEAR(v.coverage, it->second.coverage, 1e-3);
    }
}

Y_UNIT_TEST_SUITE(test_merger)
{
    Y_UNIT_TEST(test_coverage)
    {
        EXPECT_DOUBLE_EQ(mq::coverage({}), 0.);

        EXPECT_DOUBLE_EQ(mq::coverage({
            {0, 0}
        }), 0.);

        EXPECT_DOUBLE_EQ(mq::coverage({
            {0, 1}
        }), 1.);

        EXPECT_DOUBLE_EQ(mq::coverage({
            {0., 0.1},
            {0.5, 0.6}
        }), 0.2);

        EXPECT_DOUBLE_EQ(mq::coverage({
            {0., 0.1},
            {0.1, 0.3}
        }), 0.3);

        EXPECT_DOUBLE_EQ(mq::coverage({
            {0.5, 0.6},
            {0.5, 1.}
        }), 0.5);

        EXPECT_DOUBLE_EQ(mq::coverage({
            {0.1, 0.3},
            {0.2, 0.5},
            {0.5, 1.},
        }), 0.9);

        EXPECT_DOUBLE_EQ(mq::coverage({
            {0., 0.5},
            {0.2, 0.6},
            {0.6, 1.},
        }), 1.);
    }

    Y_UNIT_TEST(test_merge_jams)
    {
        maps::road_graph::Graph graph{ROAD_GRAPH_FILE};
        checkMergedJams(
            mq::mergeJams(graph, {}),
            mq::MergedJams{}
        );

        const auto seg = [](auto eId, auto segInd) {
            return maps::road_graph::SegmentId{
                maps::road_graph::EdgeId{eId},
                maps::road_graph::SegmentIndex{segInd}
            };
        };

        checkMergedJams(
            mq::mergeJams(graph, {
                {seg(636874u, 0u), {{0., 0.7, 26.71}}},
                {seg(269564u, 0u), {{0., .5, 21.83}, {0.5, 1., 43.65}}},
                {seg(269542u, 0u), {}}

            }),
            {
                {seg(636874u, 0u), {2., 0.7}},
                {seg(269564u, 0u), {1.5, 1.}}
            }
        );

        checkMergedJams(
            mq::mergeJams(graph, {
                {seg(636874u, 0u), {{0., 0.7, 26.71}}},
                {seg(269564u, 0u), {{0., .5, 21.83}, {0.5, 1., 43.65}}}

            }, /* minCoverage = */ 0.9),
            {
                {seg(269564u, 0u), {1.5, 1.}}
            }
        );
    }
}
