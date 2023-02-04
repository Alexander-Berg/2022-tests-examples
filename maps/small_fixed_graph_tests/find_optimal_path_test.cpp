#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/leptidea/lib/sparse_graph_dijkstra.h>
#include <maps/libs/leptidea/lib/simple_leptidea.h>
#include "graph_5_vertices_6_edges_fixture.h"

#include <maps/routing/common/include/types.h>
#include <maps/libs/leptidea/include/multipaths.h>

using namespace leptidea7;
using namespace maps::routing;

const PathFindOptions DEFAULT_OPTIONS = [] {
    PathFindOptions options;
    options.desiredConstraints(Constraints{RoadTrait::AllowedForAuto});
    return options;
}();

const PathFindOptions TOLL_FREE_OPTIONS = [] {
    PathFindOptions options;
    options.desiredConstraints(Constraints{RoadTrait::TollFree|RoadTrait::AllowedForAuto});
    return options;
}();

struct Graph5V6ELeptideaTests : TTestBase, Graph5V6EFixture {
    SparseGraphDijkstra forwardDijkstra{roadGraph()->edgesNumber()};
    SparseGraphDijkstra backwardDijkstra{roadGraph()->edgesNumber()};
    SimpleLeptidea simpleLeptidea{&forwardDijkstra, &backwardDijkstra};

    Source source5{
        PositionOnEdge{EdgeId(5), EdgePosition{2./3}}, {}};
    std::vector<EdgeId> targets35{EdgeId(3), EdgeId(5)};

    UNIT_TEST_SUITE(Graph5V6ELeptideaTests);
    UNIT_TEST(findRegularPaths);
    UNIT_TEST(findTollFreePaths);
    UNIT_TEST(useShortcuts);
    UNIT_TEST(useShortcutsTollFree);
    UNIT_TEST_SUITE_END();

    void findRegularPaths() {
        const auto paths = leptidea()->findPathsToAllTargets(
            {source5}, targets35, *l6aData(), DEFAULT_OPTIONS);
        UNIT_ASSERT(!paths.empty());
        UNIT_ASSERT(paths[0]);
        UNIT_ASSERT_DOUBLES_EQUAL(
                paths[0]->sourcePosition.value(), 2./3, 1e-7);
        const auto pathWithTolls = makeVector<EdgeId>({5, 0, 2, 3});
        UNIT_ASSERT_EQUAL(edgeIds(paths[0]->pathEdges), pathWithTolls);
        UNIT_ASSERT(paths[1]);
        auto cycleWithTools = pathWithTolls;
        cycleWithTools.emplace_back(5);
        UNIT_ASSERT_EQUAL(edgeIds(paths[1]->pathEdges), cycleWithTools);
    }

    void findTollFreePaths() {
        const auto paths = leptidea()->findPathsToAllTargets(
            {source5}, targets35, *l6aData(), TOLL_FREE_OPTIONS);
        UNIT_ASSERT(!paths.empty());
        UNIT_ASSERT(paths[0]);
        UNIT_ASSERT_DOUBLES_EQUAL(
                paths[0]->sourcePosition.value(), 2./3, 1e-7);
        const auto tollFreePath = makeVector<EdgeId>({5, 1, 4, 3});
        UNIT_ASSERT_EQUAL(edgeIds(paths[0]->pathEdges), tollFreePath);
        UNIT_ASSERT(paths[1]);
        auto tollFreeCycle = tollFreePath;
        tollFreeCycle.emplace_back(5);
        UNIT_ASSERT_EQUAL(edgeIds(paths[1]->pathEdges), tollFreeCycle);
    }

    void useShortcuts() {
        const Source source{
            {EdgeId(3), EdgePosition{1./2}},
            Weight::zero()};
        const auto cycle = simpleLeptidea.findOptimalPath(
            {source},
            {Target{EdgeId(3), Weight::zero()}},
            *l6aData(),
            DEFAULT_OPTIONS);
        UNIT_ASSERT(cycle);
        UNIT_ASSERT_DOUBLES_EQUAL(cycle->sourcePosition.value(), 1./2, 1e-7);

        UNIT_ASSERT_EQUAL(
            getEdgeIds(cycle->pathEdges), makeVector<EdgeId>({3, 5, 0, 2, 3}));

        const auto cycleWeight = cycle->arrivalWeight();
        UNIT_ASSERT_DOUBLES_EQUAL(
            cycleWeight.length().value(), 0.5 + std::sqrt(5.) + 1 + 1, 0.05);
        UNIT_ASSERT_DOUBLES_EQUAL(
            cycleWeight.duration().value(), 0.5 + std::sqrt(5.) + 0.5 + 1, 0.05);
    }

    void useShortcutsTollFree() {
        const Source source{
            {EdgeId(3), EdgePosition{2./5}},
            Weight::zero()};
        const auto cycle = simpleLeptidea.findOptimalPath(
            {source},
            {Target{EdgeId(3), Weight::zero()}},
            *l6aData(),
            TOLL_FREE_OPTIONS);
        UNIT_ASSERT(cycle);
        UNIT_ASSERT_DOUBLES_EQUAL(cycle->sourcePosition.value(), 2./5, 1e-7);

        UNIT_ASSERT_EQUAL(
            getEdgeIds(cycle->pathEdges), makeVector<EdgeId>({3, 5, 1, 4, 3}));

        const auto cycleWeight = cycle->pathEdges.back().arrivalWeight;
        UNIT_ASSERT_DOUBLES_EQUAL(
            cycleWeight.length().value(), 0.6 + std::sqrt(5.) + 1 + 1, 0.05);
        UNIT_ASSERT_DOUBLES_EQUAL(
            cycleWeight.duration().value(), 0.6 + std::sqrt(5.) + 1 + 1, 0.05);
    }
};

UNIT_TEST_SUITE_REGISTRATION(Graph5V6ELeptideaTests);

EdgePart ep(EdgeId edgeId, double from, double to) {
    return EdgePart{edgeId, EdgePosition{float(from)}, EdgePosition{float(to)}};
};

namespace leptidea7 {

bool operator==(const EdgePart& lhs, const EdgePart& rhs) {
    return lhs.edgeId == rhs.edgeId &&
           std::fabs(lhs.from.value() - rhs.from.value()) < 1e-6 &&
           std::fabs(lhs.to.value() - rhs.to.value()) < 1e-6;
}

std::ostream& operator<<(std::ostream& o, const EdgePart& edgePart) {
    return o << "e" << edgePart.edgeId.value()
             << "(" << edgePart.from.value()
             << "," << edgePart.to.value() << ")";
}

} // namespace leptidea7

struct Graph5V6EMultipathsTest : TTestBase, Graph5V6EFixture {
    PositionOnEdge pos52{EdgeId(5), EdgePosition{0.2}};
    PositionOnEdge pos54{EdgeId(5), EdgePosition{0.4}};
    PositionOnEdge pos56{EdgeId(5), EdgePosition{0.6}};

    std::vector<PathTopology> findPaths(const std::vector<Layer>& layers) {
        ASSERT(layers.size() >= 2);
        const std::vector<Weight> startWeights(layers.front().size());
        const std::vector<Weight> weightsAfter(layers.back().size());
        return findPathTopologies(
            leptidea().get(), *l6aData(), startWeights, layers, weightsAfter, PathFindOptions{});
    }

    UNIT_TEST_SUITE(Graph5V6EMultipathsTest);
    UNIT_TEST(sortedWaypointsMultipath);
    UNIT_TEST(unorderedWaypointsMultipath);
    UNIT_TEST_SUITE_END();

    void sortedWaypointsMultipath() {
        std::vector<Layer> layers{{pos52}, {pos54}, {pos56}};

        const auto paths = findPaths(layers);

        UNIT_ASSERT_EQUAL(paths.size(), 1);
        const auto& path = paths.front().subpaths;
        UNIT_ASSERT_EQUAL(path.size(), 2);

        const SubpathTopology& subpath0 = path[0];
        UNIT_ASSERT_EQUAL(subpath0.size(), 1);
        UNIT_ASSERT_EQUAL(subpath0.front(), ep(EdgeId(5), 0.2, 0.4));

        const SubpathTopology& subpath1 = path[1];
        UNIT_ASSERT_EQUAL(subpath1.size(), 1);
        UNIT_ASSERT_EQUAL(subpath1[0], ep(EdgeId(5), 0.4, 0.6));
    }

    void unorderedWaypointsMultipath() {
        std::vector<Layer> layers{{pos52}, {pos56}, {pos54}};

        const auto paths = findPaths(layers);

        UNIT_ASSERT_EQUAL(paths.size(), 1);
        const auto& path = paths.front().subpaths;
        UNIT_ASSERT_EQUAL(path.size(), 2);

        const SubpathTopology& subpath0 = path[0];
        UNIT_ASSERT_EQUAL(subpath0.size(), 1);
        UNIT_ASSERT_EQUAL(subpath0.front(), ep(EdgeId(5), 0.2, 0.6));

        const SubpathTopology& subpath1 = path[1];
        UNIT_ASSERT_EQUAL(subpath1.size(), 5);
        UNIT_ASSERT_EQUAL(subpath1[0], ep(EdgeId(5), 0.6, 1.0));
        UNIT_ASSERT_EQUAL(subpath1[1], ep(EdgeId(0), 0.0, 1.0));
        UNIT_ASSERT_EQUAL(subpath1[2], ep(EdgeId(2), 0.0, 1.0));
        UNIT_ASSERT_EQUAL(subpath1[3], ep(EdgeId(3), 0.0, 1.0));
        UNIT_ASSERT_EQUAL(subpath1[4], ep(EdgeId(5), 0.0, 0.4));
    }
};

UNIT_TEST_SUITE_REGISTRATION(Graph5V6EMultipathsTest);
