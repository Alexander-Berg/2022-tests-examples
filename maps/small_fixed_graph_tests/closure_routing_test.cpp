#include <library/cpp/testing/unittest/registar.h>

#include <maps/libs/edge_persistent_index/include/persistent_index.h>
#include <maps/libs/edge_persistent_index/packer/lib/include/persistent_index_builder.h>
#include <maps/libs/leptidea/bin/create_data/lib/graph_data_patch_fb_serializer.h>
#include <maps/libs/leptidea/tests/lib/test_data_generation.h>
#include <maps/libs/geolib/include/point.h>
#include <yandex/maps/jams/router/closures.h>
#include <yandex/maps/jams/router/jams.h>

#include <cstddef>
#include <limits>
#include <memory>
#include <string>
#include <vector>

using namespace maps::road_graph::literals;

using leptidea7::MutableGraphData;
using maps::geolib3::Point2;
using maps::jams::router::Closures;
using maps::jams::router::proto::ClosureType;
using maps::jams::router::Jams;
using maps::road_graph::EdgeId;
using maps::road_graph::LongEdgeId;
using maps::road_graph::PersistentIndex;
using maps::road_graph::PersistentIndexBuilder;

/*                     e4
 *             2--------------->5         7
 *             ^\       T(e7-e9)^\        ^\
 *            /  \             /  \      /  \
 *         e1/    \e5       e7/    \e9  T    \
 *          /      \         /      \  /e10   \e12
 *         /        v       /        v/        v
 * 0--R-->1----R--->3--R-->4----T--->6-------->8
 *    e0     e2,e3     e6       e8       e11
 *
 * Edges e0, e2, e3, e6 are blocked by regular closures.
 * Edges e8, e10 and manoeuvre e7-e9 are blocked by technical closures.
 * Edges e0, e1 have jams, others do not.
 * Length, speed, and jam speed is 1.0 for all edges.
 */

struct ClosedEdge {
    Closures::EdgeId edgeId;
    ClosureType closureType;
};

struct ClosedManoeuvre {
    Closures::Manoeuvre edgeIds;
    ClosureType closureType;
};

const std::string GRAPH_VERSION = "test graph version";
const TopologyDescription TOPOLOGY_DESCRIPTION{0, 1, 2, 3, 4, 5, 6, 7, 8};
const Edges EDGES{
    {0, 1}, {1, 2}, {1, 3}, {1, 3} /* non base edge */, {2, 5}, {2, 3}, {3, 4},
    {4, 5}, {4, 6}, {5, 6}, {6, 7}, {6, 8}, {7, 8}};
const std::vector<ClosedEdge> CLOSED_EDGES = {
    // only base edges
    {0_e, ClosureType::REGULAR},
    {2_e, ClosureType::REGULAR},
    {6_e, ClosureType::REGULAR},
    {8_e, ClosureType::TECHNICAL},
    {10_e, ClosureType::TECHNICAL}
};
const std::vector<ClosedManoeuvre> CLOSED_MANOEUVRES = {
    {{7_e, 9_e}, ClosureType::TECHNICAL}
};

constexpr char JAMS_FILE_PATH[] = "test_jams.pb";
constexpr char CLOSURES_FILE_PATH[] = "test_closures.pb";
constexpr char DUMMY_EXTERNAL_JAMS_FILE_PATH[] = "dummy_external_jams";
constexpr Penalty ENTERING_CLOSURE_PENALTY(86400);
constexpr Penalty MANOEUVRE_CLOSURE_PENALTY(86400);
constexpr Penalty ACCESS_PASS_PENALTY(0);

std::vector<rg::MutableEdgeData> edgesData()
{
    std::vector<rg::MutableEdgeData> data(
        EDGES.size(),
        buildEdgeData(1 /*speed*/, false /*toll*/, rg::AccessId::Automobile));
    std::vector<double> lengths(EDGES.size(), 1.0);
    setLengths(&data, lengths);
    return data;
}

PersistentIndex edgePersistentIndex()
{
    PersistentIndexBuilder builder(GRAPH_VERSION);
    builder.reserve(EDGES.size());
    for (unsigned i = 0; i < EDGES.size(); i++) {
        builder.setEdgePersistentId(EdgeId{i}, LongEdgeId{0});
    }

    return builder.build();
}

Closures makeClosures(
    const PersistentIndex& index)
{
    Closures closures{index, time_t{0}};
    for (const auto& closedEdge : CLOSED_EDGES) {
        closures.addClosure(
            closedEdge.edgeId, 0, time_t{0}, std::numeric_limits<time_t>::max(),
            closedEdge.closureType);
    }

    for (const auto& closedManoeuvre : CLOSED_MANOEUVRES) {
        closures.addManoeuvreClosure(
            closedManoeuvre.edgeIds, 0, time_t{0},
            std::numeric_limits<time_t>::max(), closedManoeuvre.closureType);
    }

    return closures;
}

Jams makeJams(const PersistentIndex& persistentIndex)
{
    Jams jams{persistentIndex};

    // Add jams for part of the edges, to make sure that closures are correctly
    // imported both in presence and in absence of jams.
    jams.addJam(Jams::EdgeId{0}, 1.0);
    jams.addJam(Jams::EdgeId{1}, 1.0);
    return jams;
}

std::unique_ptr<GraphDataPatchFb> makeGraphDataPatch(
    const SmallGraph& smallGraph,
    const PersistentIndex& persistentIndex)
{
    auto jams = makeJams(persistentIndex);
    jams.save(JAMS_FILE_PATH);

    auto closures = makeClosures(persistentIndex);
    closures.save(CLOSURES_FILE_PATH);

    auto patchedGraphData = buildPatchedGraphData(
        smallGraph.l6aData(),
        &persistentIndex,
        {JAMS_FILE_PATH},
        DUMMY_EXTERNAL_JAMS_FILE_PATH,
        CLOSURES_FILE_PATH,
        false,  // preserve turn penalties
        ENTERING_CLOSURE_PENALTY,
        MANOEUVRE_CLOSURE_PENALTY,
        ACCESS_PASS_PENALTY,
        1, // threadsNumber
        false, // ignoreEdgesSlowdown
        0, // closuresTimeBias
        smallGraph.roadGraph()->builtForAccessIdMask(),
        nullptr // tzData
        );

    return std::make_unique<GraphDataPatchFb>(
        GraphDataPatchFbSerializer(std::move(patchedGraphData)).serializeToBuffer());
}

void assertPathEdges(
    const MultilevelPath& path, const std::vector<EdgeId::ValueType>& edgeIds)
{
    UNIT_ASSERT_VALUES_EQUAL(path.pathEdges.size(), edgeIds.size());
    for (size_t i = 0; i < path.pathEdges.size(); i++) {
        const auto& pathEdgeId = path.pathEdges.at(i).edgeId;
        UNIT_ASSERT_VALUES_EQUAL(pathEdgeId.value(), edgeIds.at(i));
    }
}

void assertPathDuration(const MultilevelPath& path, float duration)
{
    UNIT_ASSERT_VALUES_EQUAL(path.weight().duration().value(), duration);
}

void assertPathPenalty(const MultilevelPath& path, float penalty)
{
    UNIT_ASSERT_VALUES_EQUAL(path.weight().penalty().value(), penalty);
}

void assertPath(
    const MultilevelPath& path,
    const std::vector<EdgeId::ValueType>& edgeIds,
    float extraPenalty = 0)
{
    assertPathEdges(path, edgeIds);

    auto pathLength = path.pathEdges.size() - 1;
    assertPathDuration(path, pathLength);
    assertPathPenalty(path, extraPenalty + pathLength);
}

class ClosureRoutingTests : public TTestBase {
public:
    ClosureRoutingTests()
        : smallGraph_(TOPOLOGY_DESCRIPTION, EDGES, edgesData(), {}, {}, {})
        , persistentIndex_(edgePersistentIndex())
        , patchedGraphData_(
            smallGraph_.serializedGraphData(),
            smallGraph_.topology(),
            makeGraphDataPatch(smallGraph_, persistentIndex_))
        , leptidea_(makeLeptidea(smallGraph_.topology()))
    { }

    UNIT_TEST_SUITE(ClosureRoutingTests);
    UNIT_TEST(avoidRegularClosuresTest);
    UNIT_TEST(noPathThroughTechnicalClosuresTest);
    UNIT_TEST_SUITE_END();

    void avoidRegularClosuresTest()
    {
        // e0 -> e6 route detours via (2). Its weight contains 1 penalty for entering
        // a blocked edge, but duration is not affected.
        auto paths = findTestPaths({{EdgeId{0}, EdgePosition{0}}}, {EdgeId{6}});
        UNIT_ASSERT_VALUES_EQUAL(paths.size(), 1);
        assertPath(paths.front(), {0, 1, 5, 6}, ENTERING_CLOSURE_PENALTY.value());
    }

    void noPathThroughTechnicalClosuresTest()
    {
        auto paths = findTestPaths({{EdgeId{6}, EdgePosition{0}}}, {EdgeId{11}});
        UNIT_ASSERT_VALUES_EQUAL(paths.size(), 0);

        paths = findTestPaths({{EdgeId{6}, EdgePosition{0}}}, {EdgeId{8}});
        UNIT_ASSERT_VALUES_EQUAL(paths.size(), 0);

        // It's possible to pass through e9 despite of blocked manoeuvre e7-e9
        paths = findTestPaths({{EdgeId{4}, EdgePosition{0}}}, {EdgeId{11}});
        UNIT_ASSERT_VALUES_EQUAL(paths.size(), 1);
        assertPath(paths.front(), {4, 9, 11});

        // It's allowed to enter edge e10 with technical closure
        // because source is on edge e8 with technical closure
        paths = findTestPaths({{EdgeId{8}, EdgePosition{0}}}, {EdgeId{12}});
        UNIT_ASSERT_VALUES_EQUAL(paths.size(), 1);
        assertPath(paths.front(), {8, 10, 12}, ENTERING_CLOSURE_PENALTY.value());
    }

private:
    std::vector<MultilevelPath> findTestPaths(
        const std::vector<PositionOnEdge>& sourcePositions,
        const std::vector<EdgeId>& targetEdges)
    {
        std::vector<Source> sources;
        for (const auto& positionOnEdge : sourcePositions) {
            sources.emplace_back(positionOnEdge, Weight{});
        }
        std::vector<Target> targets;
        for (const auto& targetEdge : targetEdges) {
            targets.emplace_back(targetEdge, Weight{});
        }

        return leptidea_->findAlternatives(sources, targets, patchedGraphData_, PathFindOptions{});
    }

    SmallGraph smallGraph_;
    PersistentIndex persistentIndex_;
    GraphData patchedGraphData_;
    std::unique_ptr<Leptidea> leptidea_;
};

UNIT_TEST_SUITE_REGISTRATION(ClosureRoutingTests);
