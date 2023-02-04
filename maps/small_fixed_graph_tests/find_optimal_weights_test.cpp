#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/leptidea/lib/sparse_graph_dijkstra.h>
#include <maps/libs/leptidea/lib/simple_leptidea.h>
#include "graph_5_vertices_6_edges_fixture.h"

#include <maps/routing/common/include/types.h>

using namespace leptidea7;
using namespace maps::routing;

const PathFindOptions DEFAULT_OPTIONS;
struct Graph5V6ELeptideaWeightsTests : TTestBase, Graph5V6EFixture {

    const Source source{
        PositionOnEdge{
            EdgeId(3),
            EdgePosition{1./2}
        },
        Weight{
            leptidea7::Duration{0.27},
            leptidea7::Penalty{0},
            leptidea7::Length{0.38}
        }
    };

    const PositionOnEdge target1{EdgeId(1), EdgePosition{0.2}};
    const PositionOnEdge target2{EdgeId(3), EdgePosition{0.3}};

    const double baseLength = 0.5 + sqrt(5.);
    const double pathTime1 = baseLength + 0.27 + 0.2;
    const double pathLength1 = baseLength + 0.38 + 0.2;
    const double pathTime2 = pathTime1 + 1. + 0.5 + 0.1;
    const double pathLength2 = pathLength1 + 1. + 1. + 0.1;

    const Weight skew{
        leptidea7::Duration{0.3},
        leptidea7::Penalty(0),
        leptidea7::Length{0}
    };

    static void assertWeight(const Weight& weight, double time, double length) {
        UNIT_ASSERT_DOUBLES_EQUAL(weight.duration().value(), time, 0.05);
        UNIT_ASSERT_DOUBLES_EQUAL(weight.length().value(), length, 0.05);
    }

    static void assertWeight(
        const MultilevelPath& path, double time, double length)
    {
        assertWeight(path.pathEdges.back().arrivalWeight, time, length);
    }

    UNIT_TEST_SUITE(Graph5V6ELeptideaWeightsTests);
    UNIT_TEST(findPaths);
    UNIT_TEST(findWeightsMatrix);
    UNIT_TEST(findWeightsEmpty);
    UNIT_TEST_SUITE_END();

    void findPaths() {
        const auto sol1 = leptidea()->findPathsToAllTargets(
            {source},
            {target1.edgeId, target2.edgeId},
            *l6aData(),
            DEFAULT_OPTIONS
        );

        UNIT_ASSERT(sol1[0]);
        UNIT_ASSERT(sol1[1]);
        UNIT_ASSERT_EQUAL(sol1[0]->pathEdges.size(), 3);
        assertWeight(*sol1[0], pathTime1 - 0.2, pathLength1 - 0.2);
        assertWeight(*sol1[1], pathTime2 - 0.3, pathLength2 - 0.3);

        PathFindOptions options;
        options.maxTargetWeightSkew(skew);

        const auto sol2 = leptidea()->findPathsToAllTargets(
            {source},
            {target1.edgeId, target2.edgeId},
            *l6aData(),
            options
        );

        UNIT_ASSERT(sol2[0]);
        UNIT_ASSERT(!sol2[1]);
        assertWeight(*sol2[0], pathTime1 - 0.2, pathLength1 - 0.2);
    }

    void findWeightsMatrix() {
        const auto sol1 = leptidea()->findOptimalWeightsMatrix(
            {{source}},
            {{target1}, {target2}},
            *l6aData(),
            DEFAULT_OPTIONS
        );

        UNIT_ASSERT_EQUAL(sol1.size(), 1);
        UNIT_ASSERT_EQUAL(sol1[0].size(), 2);
        assertWeight(sol1[0][0], pathTime1, pathLength1);
        assertWeight(sol1[0][1], pathTime2, pathLength2);

        const auto sol2 = leptidea()->findOptimalWeightsMatrix(
            {{source}},
            {{target1, target2}},
            *l6aData(),
            DEFAULT_OPTIONS
        );

        UNIT_ASSERT_EQUAL(sol2.size(), 1);
        UNIT_ASSERT_EQUAL(sol2[0].size(), 1);
        assertWeight(sol2[0][0], pathTime1, pathLength1);

        const std::vector<std::vector<Source>> sources3{
            {source}, {source}};
        const std::vector<std::vector<PositionOnEdge>> targets3{
            {target1}, {target1}, {target1, target2}, {target2}};
        const auto sol3 = leptidea()->findOptimalWeightsMatrix(
            sources3,
            targets3,
            *l6aData(),
            DEFAULT_OPTIONS
        );

        UNIT_ASSERT_EQUAL(sol3.size(), 2);
        for (auto i: makeRange(2)) {
            UNIT_ASSERT_EQUAL(sol3[i].size(), 4);
            assertWeight(sol3[i][0], pathTime1, pathLength1);
            assertWeight(sol3[i][1], pathTime1, pathLength1);
            assertWeight(sol3[i][2], pathTime1, pathLength1);
            assertWeight(sol3[i][3], pathTime2, pathLength2);
        }

        const auto sol4 = leptidea()->findOptimalWeightsMatrix(
            sources3,
            targets3,
            *l6aData(),
            PathFindOptions{}.maxBacktrackLevel(LevelId{1})
        );

        UNIT_ASSERT_EQUAL(sol4.size(), sol3.size());
        for (auto i: makeRange(sol4.size())) {
            UNIT_ASSERT_EQUAL(sol4[i].size(), sol3[i].size());
            for (auto j: makeRange(sol4[i].size())) {
                UNIT_ASSERT_EQUAL(sol4[i][j], sol3[i][j]);
            }
        }
    }

    void findWeightsEmpty() {
        const auto sol1 = leptidea()->findOptimalWeightsMatrix(
            {{source}},
            {{target1}, {}},
            *l6aData(),
            DEFAULT_OPTIONS
        );

        UNIT_ASSERT_EQUAL(sol1.size(), 1);
        UNIT_ASSERT_EQUAL(sol1[0].size(), 2);
        assertWeight(sol1[0][0], pathTime1, pathLength1);
        UNIT_ASSERT_EQUAL(sol1[0][1], Weight::infinity());

        const auto sol2 = leptidea()->findOptimalWeightsMatrix(
            {{}},
            {{target1}, {}},
            *l6aData(),
            DEFAULT_OPTIONS
        );

        UNIT_ASSERT_EQUAL(sol2.size(), 1);
        UNIT_ASSERT_EQUAL(sol2[0].size(), 2);
        UNIT_ASSERT_EQUAL(sol2[0][0], Weight::infinity());
        UNIT_ASSERT_EQUAL(sol2[0][1], Weight::infinity());
    }
};


UNIT_TEST_SUITE_REGISTRATION(Graph5V6ELeptideaWeightsTests);
