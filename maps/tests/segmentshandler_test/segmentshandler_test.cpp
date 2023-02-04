#include "../test_tools.h"

#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/libs/common/include/exception.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/config.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/segmentshandler.h>

#include <cassert>
#include <cstdio>
#include <filesystem>
#include <string>
#include <vector>
#include <map>
#include <sstream>


namespace pt = boost::posix_time;
namespace ma = maps::analyzer;

using maps::road_graph::EdgeId;
using maps::road_graph::SegmentId;
using maps::road_graph::SegmentIndex;

using ma::data::SegmentTravelTime;
using ma::data::SegmentTravelTimeCollection;

const double EPS = 1e-5;


void waitWhileAllRequestProcessed(const SegmentsHandler& handler)
{
    pt::ptime start = maps::nowUtc();
    while (
        !handler.statistic().allProcessed() &&
        handler.statistic().oldestTaskAge.total_seconds() > -3
    ) {
        UNIT_ASSERT(maps::nowUtc() - start < pt::seconds(2));
        sleep(1);
    }
}

struct SegmentsHandlerTestFixture : NUnitTest::TBaseFixture
{
    SegmentsHandlerTestFixture()
        : config_(makeSegmentshandlerConfig("segmentshandler.conf"))
        , stateFile_(Config(config_).stateFile())
    {
        clearState();
    }

    ~SegmentsHandlerTestFixture()
    {
        clearState();
    }

    void clearState()
    {
        std::filesystem::remove_all(stateFile_);
    }

    std::string config_;
    const std::string stateFile_;
};

namespace {

bool isSignificantEdge(const maps::road_graph::Graph& graph, EdgeId edgeId) {
    return graph.edgeData(edgeId).category() < 8;
}

EdgeId findSignificantEdgeId(const maps::road_graph::Graph& graph, EdgeId initialId) {
    for (auto id = initialId; id < graph.edgesNumber(); ++id)
        if (isSignificantEdge(graph, id) &&
            graph.isBase(id))
            return id;

    UNIT_FAIL("Insufficient edge quantity in graph data file");
    return EdgeId(0);
}

SegmentId firstSignificantSegment(const maps::road_graph::Graph& graph) {
    return seg(findSignificantEdgeId(graph, EdgeId(0)), 0);
}

size_t countSignificantEdges(const maps::road_graph::Graph& graph, size_t numEdges) {
    size_t res = 0;
    for (EdgeId i(0); i < EdgeId(numEdges); ++i)
        if (isSignificantEdge(graph, i))
            if (graph.isBase(i))   // base only
                ++res;

    return res;
}

std::map<EdgeId, SegmentTravelTime>
prepareSegmentTravelTimes(const SegmentsHandler& handler, size_t numEdges,
                          bool significantOnly = false) {

    const auto& graph = handler.graphData();

    std::map<EdgeId, SegmentTravelTime> segments;
    pt::ptime now = maps::nowUtc();

    for (EdgeId edgeId(0); edgeId < EdgeId(numEdges); ++edgeId) {
        if (graph.edgeData(edgeId).length() == 0.0)
            continue;   //empty edges will be ignored in interpolation

        if (!graph.isBase(edgeId))
            continue;

        if (significantOnly && !isSignificantEdge(graph, edgeId))
            continue;

        size_t segNumber = graph.edgeData(edgeId).geometry().segmentsNumber();
        auto id = seg(edgeId, edgeId.value() % segNumber);
        double travelTime = maps::road_graph::segmentGeoLength(graph, id) / 20.0;

        SegmentTravelTime segment = createTravelTime(
            id, now, ma::VehicleId("a", "b"), travelTime, 1.0
        );

        // some edges are non-base, they do not have corresponding long id.
        auto longEdgeId = handler.persistentIndex().findLongId(edgeId);
        if (longEdgeId) {
            segment.setPersistentEdgeId(*longEdgeId);
        }

        segments[edgeId] = segment;
    }
    return segments;
}

void checkAccepted(SegmentsHandler& handler, size_t expectedSize,
                   const std::map<EdgeId, SegmentTravelTime>& srcSegments) {
    waitWhileAllRequestProcessed(handler);
    const auto interpolated = handler.getSegmentInfos(JamsType::DEFAULT);

    UNIT_ASSERT_EQUAL(interpolated.size(), expectedSize);

    std::set<EdgeId> edgeExists;
    for (size_t i = 0; i < interpolated.size(); ++i) {
        const mad::SegmentInfo& info = interpolated[i];
        auto id = info.segmentId();
        auto segmentIt = srcSegments.find(id.edgeId);
        UNIT_ASSERT(segmentIt != srcSegments.end());
        UNIT_ASSERT_EQUAL(id, segmentIt->second.segmentId());
        UNIT_ASSERT_DOUBLES_EQUAL(info.averageSpeed(), 20.0, EPS);
        UNIT_ASSERT_EQUAL(info.usersNumber(), 1);
        UNIT_ASSERT(handler.graphData().edgeData(EdgeId(id.edgeId)).category() <= 8);
        UNIT_ASSERT(edgeExists.insert(id.edgeId).second);

        // make sure persistent edge ids are set correctly
        auto longId = info.persistentEdgeId();
        UNIT_ASSERT(longId);
        UNIT_ASSERT_EQUAL(
            handler
                .persistentIndex()
                .findShortId(maps::road_graph::LongEdgeId(longId->value())),
            EdgeId(id.edgeId)
        );
    }
}

}   // namespace

void checkDiffUuid(SegmentsHandler& handler, const pt::ptime now)
{
    SegmentId id = firstSignificantSegment(handler.graphData());
    double travelTime = \
        maps::road_graph::segmentGeoLength(handler.graphData(), id) / 5.0;

    const int CNT_USERS = 10;

    for (int i = 0; i < CNT_USERS; ++i) {
        pt::ptime start(now - pt::seconds(i));
        ma::VehicleId vehicleId("auto", boost::lexical_cast<std::string>(i));
        handler.addTravelTime(createTravelTime(id, start, vehicleId, travelTime, 1.0));
    }

    waitWhileAllRequestProcessed(handler);
    const auto interpolated = handler.getSegmentInfos(JamsType::DEFAULT);

    UNIT_ASSERT_EQUAL(interpolated.size(), 1);
    const mad::SegmentInfo& info = interpolated[0];
    UNIT_ASSERT_EQUAL(info.segmentId(), id);
    UNIT_ASSERT_DOUBLES_EQUAL(info.averageSpeed(), 5.0, EPS);
    UNIT_ASSERT_EQUAL(info.usersNumber(), CNT_USERS);
}

void checkCollection(const SegmentTravelTimeCollection& collection,
                    const std::map<EdgeId, SegmentTravelTime>& srcSegments,
                    size_t expectedAccepted,
                    SegmentsHandlerTestFixture& testCtx) {

    testCtx.clearState();
    SegmentsHandler handler(testCtx.config_);

    std::string proto;
    collection.serializeToString(&proto);

    const size_t accepted = handler.addTravelTimes(proto);  // may throw
    UNIT_ASSERT_EQUAL(accepted, expectedAccepted);
    checkAccepted(handler, accepted, srcSegments);
}

void finishedTest(const std::string& config, size_t numUsers)
{
    const size_t NUM_ITERS = 20;
    pt::ptime now = maps::nowUtc() - pt::seconds(NUM_ITERS * numUsers);
    std::cerr << "finish test:"
        << " users=" << numUsers
        << " iterations=" << NUM_ITERS
        << " threads=" << Config(config).threadsNumber()
        << " initial time=" << now
        << std::endl;
    SegmentsHandler handler(config);

    for (size_t iter = 0; iter < NUM_ITERS; ++iter) {
        auto edgeId = findSignificantEdgeId(handler.graphData(), EdgeId(0));

        for (size_t i = 0; i < numUsers; ++i) {
            pt::ptime start(now + pt::seconds((i + 1) * (iter + 1)));
            size_t segmentsNumber =
                handler.graphData().edgeData(edgeId).geometry().segmentsNumber();
            SegmentId id = seg(edgeId, i % segmentsNumber);

            SegmentTravelTime segment = createTravelTime(
                id,
                start,
                ma::VehicleId("auto", boost::lexical_cast<std::string>(i)),
                1.0,
                1.0
            );
            handler.addTravelTime(segment);

            edgeId = findSignificantEdgeId(handler.graphData(), edgeId + 1);
        }
        checkUnfinish(handler.finishResult());
        sleep(numUsers <= 100 ? 2 : 5);
        checkFinished(handler.finishResult(),
            now + pt::seconds(numUsers * (iter + 1) + 1)); // + 1 for travel_time
    }
}

Y_UNIT_TEST_SUITE_F(SegmentsHandlerTest, SegmentsHandlerTestFixture)
{
    Y_UNIT_TEST(CheckDataWithoutMerge)
    {
        SegmentsHandler handler(config_);

        // empty state without signals
        auto emptyInfos = handler.getSegmentInfos(JamsType::DEFAULT);
        UNIT_ASSERT(emptyInfos.empty());

        const size_t NUM_EDGES = 1000;
        const size_t NUM_SIGNIFICANT_EDGES =
            countSignificantEdges(handler.graphData(), NUM_EDGES); // category < 8

        auto segments = prepareSegmentTravelTimes(handler, NUM_EDGES);
        for (const auto& it : segments)
            handler.addTravelTime(it.second);

        checkAccepted(handler, NUM_SIGNIFICANT_EDGES, segments);
    }


    Y_UNIT_TEST(CheckDataWithMerge)
    {
        SegmentsHandler handler(config_);
        pt::ptime now = maps::nowUtc();

        SegmentId id = firstSignificantSegment(handler.graphData());
        double travelTime = maps::road_graph::segmentGeoLength(handler.graphData(), id) / 20.0;

        handler.addTravelTime(createTravelTime(id, now, ma::VehicleId("a", "b"), travelTime * 3, 0.1));
        handler.addTravelTime(createTravelTime(id, now, ma::VehicleId("a", "b"), travelTime, 0.3));
        handler.addTravelTime(createTravelTime(id, now, ma::VehicleId("a", "b"), travelTime * 2, 0.2));

        waitWhileAllRequestProcessed(handler);
        const auto interpolated = handler.getSegmentInfos(JamsType::DEFAULT);

        UNIT_ASSERT_EQUAL(interpolated.size(), 1);
        const mad::SegmentInfo& info = interpolated[0];
        UNIT_ASSERT_EQUAL(info.segmentId(), id);
        UNIT_ASSERT_DOUBLES_EQUAL(info.averageSpeed(), 20.0, EPS);
        UNIT_ASSERT_EQUAL(info.usersNumber(), 1);
    }


    Y_UNIT_TEST(CheckInterpolation)
    {
        SegmentsHandler handler(config_);
        pt::ptime now = maps::nowUtc();

        SegmentId id = firstSignificantSegment(handler.graphData());
        const double baseSpeed = 5.0;
        const double travelTime1 = \
            maps::road_graph::segmentGeoLength(handler.graphData(), id) / baseSpeed;
        const double k = 1.0 / 1.5;
        const double travelTime2 = k * travelTime1;
        const pt::ptime enterTime1 = now - pt::seconds(2);
        const pt::ptime enterTime2 = now;
        const pt::ptime exitTime1 = enterTime1 + pt::seconds(static_cast<boost::int64_t>(travelTime1));
        const pt::ptime exitTime2 = enterTime2 + pt::seconds(static_cast<boost::int64_t>(travelTime2));
        handler.addTravelTime(createTravelTime(id, enterTime1, ma::VehicleId("a", "b"), travelTime1, 1.0));
        handler.addTravelTime(createTravelTime(id, enterTime2, ma::VehicleId("b", "c"), travelTime2, 1.0));
        waitWhileAllRequestProcessed(handler);
        const auto interpolated = handler.getSegmentInfos(JamsType::DEFAULT);

        UNIT_ASSERT_EQUAL(interpolated.size(), 1);
        const mad::SegmentInfo& info = interpolated[0];
        UNIT_ASSERT_EQUAL(info.segmentId(), id);

        double r1 = handler.engine().commonRatio();
        double r2 = 1.0;
        if (exitTime1 > exitTime2)
            std::swap(r1, r2);

        double expectedAvgSpeed = (r1 + r2) / (r1 * 1.0 + r2 * k) * baseSpeed;
        UNIT_ASSERT_DOUBLES_EQUAL(info.averageSpeed(), expectedAvgSpeed, EPS);
        UNIT_ASSERT_EQUAL(info.usersNumber(), 2);
    }

    Y_UNIT_TEST(CheckDataWithDiffUuid)
    {
        SegmentsHandler handler(config_);
        checkDiffUuid(handler, maps::nowUtc());
    }


    Y_UNIT_TEST(CheckOffline)
    {
        updateConfig(config_, "mode", "offline");

        SegmentsHandler handler(config_);
        pt::ptime t0 = pt::from_time_t(60 * 60 * 24 * 1200);
        checkDiffUuid(handler, t0);
    }

    Y_UNIT_TEST(CheckRecover)
    {
        const size_t NUM_EDGES = 1000;

        std::map<EdgeId, SegmentTravelTime> segments;
        {
            // -> saveDump
            SegmentsHandler handler(config_);
            pt::ptime now = maps::nowUtc();
            for (size_t i = 0; i < NUM_EDGES; ++i) {
                EdgeId edgeId(i);

                // skip non-base edges, they have no persistent ids
                // and cannot be restored from the dump
                if (!handler.graphData().isBase(edgeId))
                    continue;

                size_t segmentsNumber = handler.graphData().edgeData(edgeId).geometry().segmentsNumber();
                auto id = SegmentId{edgeId, SegmentIndex{i % segmentsNumber}};
                double travelTime = maps::road_graph::segmentGeoLength(handler.graphData(), id) / 20.0;
                SegmentTravelTime segment = createTravelTime(
                    id, now, ma::VehicleId("a", "b"), travelTime, 1.0
                );
                if (handler.graphData().edgeData(edgeId).length() != 0.0) {
                    //empty edges will be ignored in interpolation
                    handler.addTravelTime(segment);
                    segments[edgeId] = segment;
                }
            }
        }
        {
            // loadDump -> saveDump
            SegmentsHandler handler(config_);
        }
        {
            // loadDump -> check
            SegmentsHandler handler(config_);

            // count without non-base edges (they cannot be restored anyway)
            size_t signifEdges = countSignificantEdges(handler.graphData(), NUM_EDGES);

            waitWhileAllRequestProcessed(handler);
            const auto interpolated = handler.getSegmentInfos(JamsType::DEFAULT);

            UNIT_ASSERT_EQUAL(interpolated.size(), signifEdges);
            std::set<EdgeId> edgeExists;
            for (size_t i = 0; i < interpolated.size(); ++i) {
                const mad::SegmentInfo& info = interpolated[i];
                auto id = info.segmentId();
                UNIT_ASSERT(segments.count(id.edgeId));
                UNIT_ASSERT_EQUAL(id, segments[id.edgeId].segmentId());
                UNIT_ASSERT_DOUBLES_EQUAL(info.averageSpeed(), 20.0, EPS);
                UNIT_ASSERT_EQUAL(info.usersNumber(), 1);
                //TODO:check graph version and regionId

                UNIT_ASSERT(edgeExists.insert(id.edgeId).second);
            }
        }
    }

    Y_UNIT_TEST(CheckAddTravelTimesWithPersistentEgdeIds)
    {
        SegmentsHandler handler(config_);

        bool significantOnly = false;

        const size_t allEdges = 1000;
        const size_t signifEdges =
            countSignificantEdges(handler.graphData(), allEdges);

        auto segments = prepareSegmentTravelTimes(handler, allEdges, significantOnly);
        const std::string graphVersion(handler.graphData().version());
        const std::string anotherGraphVersion = "yet another graph version";

        SegmentTravelTimeCollection orig(graphVersion);
        for (const auto& it : segments)
            orig.push(it.second);


        {
            // all signigicant segments have persistent edge id,
            // all should be restored and accepted
            SegmentTravelTimeCollection copy(orig);
            checkCollection(copy, segments, signifEdges, *this);
        }

        // now leave only siginficant segments
        significantOnly = true;
        segments = prepareSegmentTravelTimes(handler, allEdges, significantOnly);
        orig = SegmentTravelTimeCollection(graphVersion);
        for (const auto& it : segments)
            orig.push(it.second);

        {
            // remove persistent edge id from ALL segments.
            // they still should be accepted as before
            // as long as graph version is the same
            SegmentTravelTimeCollection copy(orig);
            for (auto& tt : *copy.data().mutable_segment_travel_time())
                tt.clear_persistent_edge_id();

            // except one with fake short id
            copy.data().mutable_segment_travel_time(0)->set_edge_id(11111111);
            // and one without short id
            copy.data().mutable_segment_travel_time(0)->clear_edge_id();

            checkCollection(copy, segments, signifEdges - 1, *this);
        }
        {
            // remove persistent edge id from ALL segments, nothing is accepted,
            // should trigger an exception ("all rejected")
            SegmentTravelTimeCollection copy(orig);
            copy.setGraphVersion(anotherGraphVersion);
            for (auto& tt : *copy.data().mutable_segment_travel_time())
                tt.clear_persistent_edge_id();

            UNIT_ASSERT_EXCEPTION(
                checkCollection(copy, segments, signifEdges, *this),
                maps::Exception);
        }
        {
            // remove persistent edge id from SOME segments.
            // these should not be accepted with alien graph version
            SegmentTravelTimeCollection copy(orig);
            copy.setGraphVersion(anotherGraphVersion);
            size_t noLongId = 0;
            for (auto& tt : *copy.data().mutable_segment_travel_time())
                if (noLongId < 10) {
                    tt.clear_persistent_edge_id();
                    ++noLongId;
                }

            checkCollection(copy, segments, signifEdges - noLongId, *this);
        }
        {
            // remove SHORT edge id from SOME segments, all should be accepted
            // even with alien graph version (long ids are used to restore short ones)
            SegmentTravelTimeCollection copy(orig);
            copy.setGraphVersion(anotherGraphVersion);
            size_t noShortId = 0;
            for (auto& tt : *copy.data().mutable_segment_travel_time())
                if (noShortId < 30) {
                    tt.clear_edge_id();
                    ++noShortId;
                }

            checkCollection(copy, segments, signifEdges, *this);
        }
        {
            // set some fake long ids, these should not be accepted
            maps::road_graph::LongEdgeId fakeId(123456789UL);
            UNIT_ASSERT(!handler.persistentIndex().findShortId(fakeId));

            SegmentTravelTimeCollection copy(orig);
            size_t fakeLongIds = 0;
            for (auto& tt : *copy.data().mutable_segment_travel_time())
                if (fakeLongIds < 50) {
                    tt.set_persistent_edge_id(fakeId.value());
                    ++fakeLongIds;
                }

            checkCollection(copy, segments, signifEdges - fakeLongIds, *this);
        }
    }

    Y_UNIT_TEST(CheckExpired)
    {
        SegmentsHandler handler(config_);

        pt::ptime now = pt::from_time_t(60 * 60 * 24 * 1200);
        const int CNT_USERS = 400;
        for (int i = 0; i < CNT_USERS; ++i) {
            SegmentId id{EdgeId(i), SegmentIndex{0}};
            ma::VehicleId vehicleId("auto", boost::lexical_cast<std::string>(i));
            pt::ptime start(now - pt::seconds(i));
            SegmentTravelTime segment = createTravelTime(
                id, start, vehicleId, 10.0, 1.0
            );
            handler.addTravelTime(segment);
        }
        waitWhileAllRequestProcessed(handler);
        const auto interpolated = handler.getSegmentInfos(JamsType::DEFAULT);

        UNIT_ASSERT(interpolated.empty());
    }

    Y_UNIT_TEST(CheckVisualizationMinSignals)
    {
        updateConfig(config_, "reinterpolate_waiting", 5);
        updateConfig(config_, "visualization_min_signals", 2);
        updateConfig(
            config_, "interpolation_horizon", Config(config_).interpolationWindow().total_seconds()
        );

        SegmentsHandler handler(config_);

        SegmentId id = firstSignificantSegment(handler.graphData());
        pt::ptime anHourAgo = maps::nowUtc() - Config(config_).interpolationWindow();
        handler.addTravelTime(createTravelTime(id, anHourAgo, ma::VehicleId("a", "1"), 1.0, 1.0));
        handler.addTravelTime(createTravelTime(id, anHourAgo, ma::VehicleId("b", "2"), 9.0, 9.0));

        waitWhileAllRequestProcessed(handler);
        auto interpolated = handler.getSegmentInfos(JamsType::DEFAULT);
        // we have 2 signals in window, segment must be in output
        UNIT_ASSERT_EQUAL(interpolated.size(), 1);
        UNIT_ASSERT_EQUAL(interpolated[0].usersNumber(), 2);

        sleep(6);
        interpolated = handler.getSegmentInfos(JamsType::DEFAULT);
        // only 1 signal left in window, but segment still must be in output,
        // because there were 2 signals on this segment some time ago
        UNIT_ASSERT_EQUAL(interpolated.size(), 1);
        UNIT_ASSERT_EQUAL(interpolated[0].usersNumber(), 1);

        sleep(6);
        interpolated = handler.getSegmentInfos(JamsType::DEFAULT);
        // after interpolation horizon has passed, segment is deleted
        UNIT_ASSERT(interpolated.empty());

        handler.addTravelTime(createTravelTime(id, maps::nowUtc(), ma::VehicleId("auto", "1"), 1.0, 1.0));
        waitWhileAllRequestProcessed(handler);
        interpolated = handler.getSegmentInfos(JamsType::DEFAULT);
        // now it is absolutely new segment, and 1 signal is not enough for visualization
        UNIT_ASSERT(interpolated.empty());
    }

    Y_UNIT_TEST(CheckFinished1)
    {
        updateConfig(config_, "mode", "offline");
        updateConfig(config_, "reinterpolate_waiting", 1);
        updateConfig(config_, "threads_number", 1);

        finishedTest(config_, 1);
    }

    Y_UNIT_TEST(CheckFinished2)
    {
        updateConfig(config_, "mode", "offline");
        updateConfig(config_, "reinterpolate_waiting", 1);
        updateConfig(config_, "threads_number", 1);

        finishedTest(config_, 1000);
    }

    Y_UNIT_TEST(RouterJams)
    {
        std::map<EdgeId, SegmentTravelTime> segments;
        SegmentsHandler handler(config_);

        // empty state without signals
        auto emptyJams = handler.getRouterJams(JamsType::DEFAULT, {});
        UNIT_ASSERT(emptyJams && emptyJams->jamsCount() == 0);


        pt::ptime now = maps::nowUtc();
        const size_t NUM_EDGES = 1000;
        for (EdgeId edgeId{0}; edgeId.value() < NUM_EDGES; ++edgeId) {
            size_t segmentsNumber = handler.graphData().edgeData(EdgeId(edgeId)).geometry().segmentsNumber();
            SegmentId id{edgeId, SegmentIndex{edgeId.value() % segmentsNumber}};
            double segmentLength =
                    maps::road_graph::segmentGeoLength(handler.graphData(), id);

            if (segmentLength > EPS) {
                // Avoiding zero-length segments
                double travelTime = segmentLength / 10.0;
                SegmentTravelTime segment = createTravelTime(
                        id, now, ma::VehicleId("a", "b"), travelTime, 1.0);
                handler.addTravelTime(segment);
                segments[edgeId] = segment;
            }
        }

        waitWhileAllRequestProcessed(handler);

        auto noJams = handler.getRouterJams(JamsType::DEFAULT, {.jams = false});
        UNIT_ASSERT(noJams && noJams->jamsCount() == 0);

        auto builder = handler.getRouterJams(JamsType::DEFAULT, {});
        UNIT_ASSERT(builder);

        std::stringstream tmpBuf;
        builder->save(&tmpBuf);
        maps::jams::router::Jams routerJams(handler.persistentIndex());
        routerJams.read(&tmpBuf);

        // Compare with number of edges with category < 8
        const size_t signifEdges = countSignificantEdges(handler.graphData(), NUM_EDGES);
        UNIT_ASSERT_EQUAL(routerJams.jamsCount(), signifEdges);

        for (const auto& jamElement : routerJams.jams()) {
            const EdgeId edgeId{jamElement.first};
            const auto& edgeJam = jamElement.second;

            UNIT_ASSERT(
                handler
                    .persistentIndex()
                    .findLongId(maps::road_graph::EdgeId(edgeId))
            );

            const auto& segmentTravelTime = segments[edgeId];

            double jamLength = segmentGeoLength(
                    handler.graphData(), segmentTravelTime.segmentId());

            auto edgeData = handler.graphData().edgeData(EdgeId(edgeId));
            UNIT_ASSERT(edgeData.category() <= 8);

            double time = (edgeData.length() - jamLength) / edgeData.speed()
                        + *segmentTravelTime.travelTime();

            double expectedSpeed = edgeData.length() / time;

            UNIT_ASSERT_DOUBLES_EQUAL(expectedSpeed, edgeJam.speed(), EPS);
            UNIT_ASSERT_UNEQUAL(edgeJam.region(), 0);
        }
    }

    Y_UNIT_TEST(NoGraphData)
    {
        updateConfig(config_, "path_to_graph_data", NONEXISTENT_PATH);
        UNIT_ASSERT_EXCEPTION(
            SegmentsHandler(config_),
            ma::NoExternalResourceError
        );
    }

    Y_UNIT_TEST(NoEdgesPersIndex)
    {
        updateConfig(config_, "path_to_edges_persistent_index", NONEXISTENT_PATH);
        UNIT_ASSERT_EXCEPTION(
            SegmentsHandler(config_),
            ma::NoExternalResourceError
        );
    }

    Y_UNIT_TEST(NoGeodata)
    {
        updateConfig(config_, "path_to_geobase", NONEXISTENT_PATH);
        UNIT_ASSERT_EXCEPTION(
            SegmentsHandler(config_),
            ma::NoExternalResourceError
        );
    }

    Y_UNIT_TEST(NoTzdata)
    {
        updateConfig(config_, "path_to_tzdata", NONEXISTENT_PATH);
        UNIT_ASSERT_EXCEPTION(
            SegmentsHandler(config_),
            ma::NoExternalResourceError
        );
    }
}
