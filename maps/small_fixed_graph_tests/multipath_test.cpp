#include <library/cpp/testing/unittest/registar.h>

#include "graph_5_vertices_6_edges_fixture.h"

#include <maps/libs/leptidea/include/graph_data.h>
#include <maps/libs/leptidea/include/multipaths.h>
#include <maps/libs/leptidea/tests/lib/test_utils.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

using namespace leptidea7;

using ::testing::_; // NOLINT
using ::testing::Return;

using Layers = std::vector<Layer>;

const EdgePosition START{0.0};
const EdgePosition QUARTER{0.25};
const EdgePosition MIDDLE{0.5};

Source src(EdgeId edgeId) {
    return Source{{edgeId, MIDDLE}, Weight()};
}

Target tgt(EdgeId edgeId) {
    return Target{edgeId, Weight()};
}

class MockLeptidea : public Leptidea {
public:
    MOCK_METHOD(std::vector<std::optional<MultilevelPath>>, findPathsToAllTargets, (
                const Sources&,
                const std::vector<EdgeId>&,
                const GraphData& graphData,
                const PathFindOptions&), (override));

    MOCK_METHOD(std::vector<MultilevelPath>, findAlternatives, (
            const Sources&,
            const Targets&,
            const GraphData&,
            const PathFindOptions&,
            const AlternativesConfig&), (override));

    MOCK_METHOD(std::vector<std::vector<Weight>>, findOptimalWeightsMatrix, (
            const std::vector<Sources>& sources,
            const std::vector<Layer>& targetsList,
            const GraphData& graphData,
            const PathFindOptions& options), (override));
};

/*
 * "--->" -- toll-free edge
 * "===>" -- toll edge
 *
 *         ^ Y
 *         |
 *         |         4
 *         |        /^
 *         |     e5/ |
 *         |      /  |e3
 *         | e2  /   |
 *         1========>2
 *         ^   /     ^
 *       e0|  /      |e4
 *         | /       |
 *         |L        |
 *         0-------->3-------------> X
 *              e1
 */

// To match sources inside google mock
namespace leptidea7 {
bool operator==(const Source& lhs, const Source& rhs) {
    return equalWithTolerance(lhs, rhs);
}
} // namespace leptidea7

using OptPath = std::optional<MultilevelPath>;

EdgePart lastEdgePart(const std::vector<PathTopology>& paths) {
    UNIT_ASSERT(!paths.empty());
    const auto& path = paths.front().subpaths;
    UNIT_ASSERT(!path.empty());
    const auto& subpath = path.front();
    UNIT_ASSERT(!subpath.empty());
    return subpath.back();
}

struct MultipathTests : TTestBase, Graph5V6EFixture {
    void expectFindPathsToAllTargets(
            const Sources& sources,
            const std::vector<std::optional<MultilevelPath>>& paths) {
        UNIT_ASSERT(mockLeptidea);
        EXPECT_CALL(*mockLeptidea, findPathsToAllTargets(sources, _, _, _))
            .WillOnce(Return(paths));
    }

    void expectFindAlternatives(
            const Sources& sources,
            const std::vector<MultilevelPath>& alternatives) {
        UNIT_ASSERT(mockLeptidea);
        EXPECT_CALL(*mockLeptidea, findAlternatives(sources, _, _, _, _))
            .WillOnce(Return(alternatives));
    }

    std::vector<PathTopology> findPathTopologies(
            const std::vector<Layer>& layers,
            const AlternativesConfig& alternativesConfig =
                AlternativesConfig::singlePath(),
            std::vector<Weight> weightsAfter = {}) {
        ASSERT(!layers.empty());
        ASSERT(!layers.back().empty());
        if (weightsAfter.empty()) {
            weightsAfter.resize(layers.back().size(), Weight::zero());
        }
        return leptidea7::findPathTopologies(
            mockLeptidea.get(),
            *l6aData(),
            {Weight()},
            layers,
            weightsAfter,
            PathFindOptions{},
            alternativesConfig);
    }

    std::vector<Weight> findPathWeights(
            const Sources& sources, const Layer& targets) {
        return leptidea7::findPathWeights(
            mockLeptidea.get(),
            *l6aData(),
            sources,
            targets,
            std::vector<Weight>(targets.size()),
            PathFindOptions{});
    }

    std::unique_ptr<MockLeptidea> mockLeptidea{new MockLeptidea{}};

    PositionOnEdge middle5{EdgeId(5), MIDDLE};
    PositionOnEdge middle0{EdgeId(0), MIDDLE};
    PositionOnEdge  start0{EdgeId(0),  START};
    PositionOnEdge middle1{EdgeId(1), MIDDLE};
    PositionOnEdge middle2{EdgeId(2), MIDDLE};
    PositionOnEdge quaRter4{EdgeId(4), QUARTER};
    PositionOnEdge threeQuaRters4{EdgeId(4), QUARTER * 3};

    Source src5{middle5, Weight()};
    Source src0{ start0, Weight()};
    Source wayPoint0{start0, createWeight(std::sqrt(5.f) / 2)};
    Source src1{middle1, Weight()};

    MultilevelPath path14{
        MIDDLE,
        {
            {EdgeId(1), Weight()},
            {EdgeId(4), createWeight(0.5)}
        }
    };
    MultilevelPath path50{
        MIDDLE,
        {
            {EdgeId(5), Weight()},
            {EdgeId(0), createWeight(std::sqrt(5.f) / 2)}
        }
    };
    MultilevelPath path51{
        MIDDLE,
        {
            {EdgeId(5), Weight()},
            {
                EdgeId(1), createWeight(std::sqrt(5.f) / 2 + 0.1)
            }
        }
    };
    MultilevelPath path02{
        START,
        {
            {EdgeId(0), createWeight(std::sqrt(5.f) / 2)},
            {EdgeId(2), createWeight(std::sqrt(5.f) / 2 + 1)}
        }
    };

    UNIT_TEST_SUITE(MultipathTests);
    UNIT_TEST(alternativesFound);
    UNIT_TEST(alternativesNotFound);
    UNIT_TEST(alternativesOneEdgePathFound);
    UNIT_TEST(alternativesToTwoTargetsOnSameEdge);
    UNIT_TEST(noRouteFound);
    UNIT_TEST(routeFoundSimple);
    UNIT_TEST(partialRouteFound);
    UNIT_TEST(twoLayersRoute);
    UNIT_TEST(oneEdgePathSourceBeforeTarget);
    UNIT_TEST(oneEdgePathTargetBeforeSource);
    UNIT_TEST(oneEdgePathCompeteWithRegularPathTopology);
    UNIT_TEST(correctAddingPieceOfTargetEdge);
    UNIT_TEST(twoTargetsOnEdge);
    UNIT_TEST(weightOnUnreachableEdge);
    UNIT_TEST(pathWeight);
    UNIT_TEST(duplicateTargetWeight);
    UNIT_TEST(oneEdgePathCorrectWeightComputation);
    UNIT_TEST(oneEdgePathCompeteWithRegularPathWeights);
    UNIT_TEST_SUITE_END();

    void alternativesFound() {
        expectFindAlternatives({src5}, {path50, path50, path50});

        const AlternativesConfig cfg {
            .alternativesNumber = 100,
            .maxSharing = 100,
            .maxSlowdown = 100
        };
        const auto paths = findPathTopologies({{middle5}, {middle0}}, cfg);

        UNIT_ASSERT_EQUAL(paths.size(), 3);
        for (size_t i = 0; i != 3; ++i) {
            UNIT_ASSERT_EQUAL(paths[i].subpaths.size(), 1);
        }
    }

    void alternativesNotFound() {
        expectFindAlternatives({src5}, {});

        const AlternativesConfig cfg {
            .alternativesNumber = 2,
            .maxSharing = 1,
            .maxSlowdown = 100
        };
        const auto paths = findPathTopologies({{middle5}, {middle0}}, cfg);

        UNIT_ASSERT(paths.empty());
    }

    void alternativesOneEdgePathFound() {
        expectFindPathsToAllTargets({src0}, {{}});

        const AlternativesConfig cfg {
            .alternativesNumber = 2,
            .maxSharing = 1,
            .maxSlowdown = 100
        };
        const auto paths = findPathTopologies({{start0}, {middle0}}, cfg);

        UNIT_ASSERT_EQUAL(paths.size(), 1);
        UNIT_ASSERT_EQUAL(paths.front().subpaths.size(), 1);
    }

    void alternativesToTwoTargetsOnSameEdge() {
        expectFindAlternatives({src1}, {path14});

        const std::vector<Layer> layers{{middle1}, {quaRter4, threeQuaRters4}};
        const AlternativesConfig cfg {
            .alternativesNumber = 100,
            .maxSharing = 100,
            .maxSlowdown = 100
        };
        const std::vector<Weight> weightsAfter{createWeight(1000), createWeight(0)};
        const auto paths = findPathTopologies(layers, cfg, weightsAfter);

        UNIT_ASSERT_DOUBLES_EQUAL(lastEdgePart(paths).to.value(), 0.75, 1e-4);
    }

    void noRouteFound() {
        expectFindPathsToAllTargets({src5}, {{}});

        const auto paths = findPathTopologies({{middle5}, {middle0}});

        UNIT_ASSERT(paths.empty());
    }

    void routeFoundSimple() {
        expectFindPathsToAllTargets({src5}, {path50});
        const auto paths = findPathTopologies({{middle5}, {middle0}});

        UNIT_ASSERT_EQUAL(paths.size(), 1);
        UNIT_ASSERT_EQUAL(paths.front().subpaths.size(), 1);
        UNIT_ASSERT_EQUAL(paths.front().subpaths.front().size(), 2);
    }

    void partialRouteFound() {
        expectFindPathsToAllTargets({src5}, {path50});
        expectFindPathsToAllTargets({wayPoint0}, {{}});

        const auto paths = findPathTopologies({{middle5}, {middle0}, {middle1}});

        UNIT_ASSERT_EQUAL(paths.size(), 1);
        UNIT_ASSERT_EQUAL(paths.front().subpaths.size(), 1);
        UNIT_ASSERT_EQUAL(paths.front().subpaths.front().size(), 2);
    }

    void twoLayersRoute() {
        expectFindPathsToAllTargets({src5}, {path50});
        expectFindPathsToAllTargets({wayPoint0}, {path02});

        const auto paths = findPathTopologies({{middle5}, {middle0}, {middle2}});

        UNIT_ASSERT_EQUAL(paths.size(), 1);
        UNIT_ASSERT_EQUAL(paths.front().subpaths.size(), 2);
        for (const auto& subpath: paths.front().subpaths) {
            UNIT_ASSERT_EQUAL(subpath.size(), 2);
        }
    }

    void oneEdgePathSourceBeforeTarget() {
        expectFindPathsToAllTargets({src0}, {{}});

        const auto paths = findPathTopologies({{start0}, {middle0}});

        UNIT_ASSERT_EQUAL(paths.size(), 1);
        UNIT_ASSERT_EQUAL(paths.front().subpaths.size(), 1);
    }

    void oneEdgePathTargetBeforeSource() {
        const Source src{middle0, Weight()};
        expectFindPathsToAllTargets({src}, {{}});

        const auto paths = findPathTopologies({{middle0}, {start0}});

        UNIT_ASSERT(paths.empty());
    }

    void oneEdgePathCompeteWithRegularPathTopology() {
        // Find path from 1st and 4th edge to 4th edge
        // with startWeight4 start weight at 4th edge
        auto getSubpath = [&](const Weight& startWeight4) {
            const Source src4{quaRter4, startWeight4};
            expectFindPathsToAllTargets({src1, src4}, {OptPath{path14}});

            const auto paths = leptidea7::findPathTopologies(
                mockLeptidea.get(),
                *l6aData(),
                {src1.startWeight, src4.startWeight},
                { {src1.positionOnEdge, src4.positionOnEdge}, {threeQuaRters4} },
                {Weight()},
                PathFindOptions{},
                AlternativesConfig::singlePath());

            UNIT_ASSERT_EQUAL(paths.size(), 1);
            UNIT_ASSERT_EQUAL(paths.front().subpaths.size(), 1);
            return paths.front().subpaths.front();
        };
        // One-edge-path wins
        const auto oneEdgeSubpath = getSubpath(Weight());
        UNIT_ASSERT_EQUAL(oneEdgeSubpath.size(), 1);

        // One-edge-path loses
        const auto subpath = getSubpath(createWeight(1.f));
        UNIT_ASSERT_EQUAL(subpath.size(), 2);
    }

    void correctAddingPieceOfTargetEdge() {
        auto getPathTarget = [&](EdgePosition part1) {
            const PositionOnEdge pos1{EdgeId(1), part1};

            expectFindPathsToAllTargets(
                {src5}, { OptPath{path50}, OptPath{path51} });
            const auto paths = findPathTopologies(
                { {src5.positionOnEdge}, {middle0, pos1} });

            UNIT_ASSERT_EQUAL(paths.size(), 1);
            const auto path = paths.front().subpaths;
            UNIT_ASSERT_EQUAL(path.size(), 1);
            const auto& subpath = path.front();
            UNIT_ASSERT_EQUAL(subpath.size(), 2);
            return subpath.back().edgeId;
        };
        UNIT_ASSERT_EQUAL(getPathTarget(QUARTER), EdgeId(1));
        UNIT_ASSERT_EQUAL(getPathTarget(QUARTER * 2), EdgeId(0));
        UNIT_ASSERT_EQUAL(getPathTarget(QUARTER * 3), EdgeId(0));
    }

    void twoTargetsOnEdge() {
        expectFindPathsToAllTargets({src1}, { OptPath{path14}, OptPath{path14} });

        const std::vector<Weight> weightsAfter{createWeight(1000), createWeight(0)};
        const auto cfg = AlternativesConfig::singlePath();
        const auto paths = findPathTopologies(
            { {src1.positionOnEdge}, {quaRter4, threeQuaRters4} }, cfg, weightsAfter);
        UNIT_ASSERT_DOUBLES_EQUAL(lastEdgePart(paths).to.value(), 0.75, 1e-4);
    }

    void weightOnUnreachableEdge() {
        expectFindPathsToAllTargets({src5}, {{}});

        const auto weights = findPathWeights({src5}, {start0});

        UNIT_ASSERT(weights.size() == 1);
        UNIT_ASSERT(weights[0] == Weight::infinity());
    }

    void pathWeight() {
        expectFindPathsToAllTargets({src5}, {path50});

        const auto weights = findPathWeights({src5}, {middle0});

        UNIT_ASSERT(weights.size() == 1);
        const auto correctWeight = createWeight(std::sqrt(5.f) / 2 + 0.5f);
        UNIT_ASSERT(weights[0].equalWithTolerance(correctWeight, 0.01));
    }

    void duplicateTargetWeight() {
        expectFindPathsToAllTargets({src5}, {path50, path50});

        const auto weights = findPathWeights({src5}, {middle0, middle0});
        UNIT_ASSERT(weights.size() == 2);
        UNIT_ASSERT(weights[0] == weights[1]);
        UNIT_ASSERT(weights[0] != Weight::infinity());
    }

    void oneEdgePathCorrectWeightComputation() {
        expectFindPathsToAllTargets({src5}, {{}});

        const PositionOnEdge targetPosition{
            EdgeId(5), EdgePosition{0.6}};
        const auto weights = findPathWeights({src5}, {targetPosition});

        UNIT_ASSERT(weights.size() == 1);
        const auto correctWeight = createWeight(std::sqrt(5.0f) * 0.1f);
        UNIT_ASSERT(weights[0].equalWithTolerance(correctWeight, 0.01));
    }

    void oneEdgePathCompeteWithRegularPathWeights() {
        // Find path from 1st and 4th edge to 4th edge
        // with startWeight4 start weight at 4th edge
        auto getMinWeight = [&](const Weight& startWeight4) {
            const Source src4{quaRter4, startWeight4};
            expectFindPathsToAllTargets({src1, src4}, {OptPath{path14}});

            const auto weights = findPathWeights({src1, src4}, {threeQuaRters4});

            UNIT_ASSERT_EQUAL(weights.size(), 1);
            return weights.front();
        };
        // One-edge-path wins
        const auto trivialPathWeight = getMinWeight(Weight());
        UNIT_ASSERT(
            trivialPathWeight.equalWithTolerance(createWeight(0.5f), 0.01));

        // One-edge-path loses
        const auto regularPathWeight = getMinWeight(createWeight(1.f));
        UNIT_ASSERT(
            regularPathWeight.equalWithTolerance(createWeight(1.25f), 0.01));
    }
};

UNIT_TEST_SUITE_REGISTRATION(MultipathTests);
