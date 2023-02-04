#include "../test_tools.h"

#include <boost/date_time/posix_time/ptime.hpp>
#include <boost/date_time/posix_time/time_parsers.hpp>
#include <library/cpp/testing/unittest/registar.h>
#include <maps/analyzer/services/jams_analyzer/modules/segmentshandler/lib/state_file.h>
#include <maps/analyzer/libs/manoeuvres/include/types.h>
#include <maps/libs/edge_persistent_index/packer/lib/include/persistent_index_builder.h>

#include <memory>
#include <sstream>
#include <string>
#include <vector>


using maps::analyzer::state::SegmentsState;
using maps::analyzer::VehicleId;
using maps::analyzer::manoeuvres::ManoeuvreId;

using maps::road_graph::EdgeId;
using maps::road_graph::LongEdgeId;
using maps::road_graph::PersistentIndex;

using boost::posix_time::ptime;
using boost::posix_time::time_from_string;

namespace {

typedef std::pair<uint64_t, int32_t> LongShortId;

PersistentIndex buildPersistentIndex(const std::vector<LongShortId>& ids) {
    maps::road_graph::PersistentIndexBuilder builder("some version"); // version should not make any difference
    for (size_t i = 0; i < ids.size(); ++i)
        builder.setEdgePersistentId(EdgeId(ids[i].second), LongEdgeId(ids[i].first));
    return builder.build();
}

PersistentIndex buildDefaultPersistentIndex() {
    return buildPersistentIndex({
        { 12345UL,      1 },
        { 86554654UL,   2 },
        { 3175621111UL, 3 }
    });
}

}   // namespace

Y_UNIT_TEST_SUITE(StateFileTest)
{
    Y_UNIT_TEST(EmptyState)
    {
        SegmentsState state(std::make_unique<Dump>(), "graphVersion", boost::posix_time::ptime());
        std::stringstream stream;

        PersistentIndex edgeIndex = buildDefaultPersistentIndex();

        state.save(stream, edgeIndex);
        auto loadedState = SegmentsState::load(stream, maps::nowUtc(), edgeIndex);

        UNIT_ASSERT_EQUAL("graphVersion", loadedState.graphVersion);
        UNIT_ASSERT_EQUAL(ptime(), loadedState.lastSignalTime);
        UNIT_ASSERT_EQUAL(0u, loadedState.dump->size());
    }


    Y_UNIT_TEST(BadStreamThrows)
    {
        SegmentsState state(std::make_unique<Dump>(), "graphVersion", boost::posix_time::ptime());

        PersistentIndex edgeIndex = buildDefaultPersistentIndex();

        std::stringstream stream;
        stream.setstate(std::ios::failbit | std::ios::badbit);

        UNIT_ASSERT_EXCEPTION(state.save(stream, edgeIndex), maps::Exception);
        UNIT_ASSERT_EXCEPTION(SegmentsState::load(stream, maps::nowUtc(), edgeIndex), maps::Exception);
    }


    namespace {

    void saveSimpleState(std::stringstream& stream,
                        const PersistentIndex& edgeIndex) {
        SegmentsState state(std::make_unique<Dump>(), "12.11.10", time_from_string("2016-01-01 12:00:00"));

        for (size_t edgeId = 1; edgeId <= 3; ++edgeId) {
            for (size_t copyIndex = 0; copyIndex <= (edgeId == 3); ++copyIndex) {
                SegmentsQueue unprocessedRequests;
                auto unprocessedTT = createTravelTime(
                                seg(edgeId, 0),
                                time_from_string("2016-01-01 12:00:00") /* enterTime */,
                                VehicleId("clid1", "uuid1"),
                                2.2 /* travelTime */,
                                3.3 /* travelTimeBase */);
                if (copyIndex == 1) {
                    unprocessedTT.setManoeuvreId(ManoeuvreId(123));
                }
                unprocessedRequests.push_back(unprocessedTT);

                SegmentsQueue taskSegments;
                auto taskSegmentsTT = createTravelTime(
                                seg(edgeId, 0),
                                time_from_string("2016-01-01 11:00:00") /* enterTime */,
                                VehicleId("clid2", "uuid2"),
                                4.4 /* travelTime */,
                                5.5 /* travelTimeBase */);
                if (copyIndex == 1) {
                    taskSegmentsTT.setManoeuvreId(ManoeuvreId(123));
                }
                taskSegments.push_back(taskSegmentsTT);

                ManoeuvrableEdgeId id = {.edgeId = EdgeId(edgeId)};
                if (copyIndex == 1) {
                    id.manoeuvreId = ManoeuvreId(123);
                }
                InterpolateTask interpolateTask(
                        id /* taskId: ManoeuvrableEdgeId */,
                        time_from_string("2015-01-01 10:00:00") /* time */,
                        &taskSegments,
                        time_from_string("2015-01-01 9:00:00") /* interpolateTime */);

                auto edgeHistory = EdgeHistory::Builder(1)
                        .setRegionId(911)
                        .setLastTime(time_from_string("2016-01-01 8:00:00"))
                        .addItem(SegmentHistory::Builder(1)
                                .setLastSignalTime(
                                        time_from_string("2016-01-01 7:00:00"))
                                .putItem(
                                        "uuid3",
                                        AggregationInfo(
                                                6.6 /* travelTime */,
                                                7.7 /* travelTimeBase */,
                                                time_from_string("2016-01-01 6:00:00")))
                                .build())
                        .build();

                state.dump->push(
                        unprocessedRequests,
                        std::make_unique<EdgeHistory>(std::move(edgeHistory)),
                        interpolateTask);
            }
        }

        state.save(stream, edgeIndex);
    }

    void checkSimpleStateItem(const Dump::ConstDumpObject& dumpObject, ManoeuvrableEdgeId id) {

        const double EPS = 1e-5;
        const auto edgeId = id.edgeId;

        // Verify dumpObject.unprocessedRequests

        UNIT_ASSERT_EQUAL(1u, dumpObject.unprocessedRequests->size());
        auto unprocessedRequest = dumpObject.unprocessedRequests->at(0);
        UNIT_ASSERT_EQUAL(seg(edgeId, 0), unprocessedRequest.segmentId());
        UNIT_ASSERT_EQUAL(time_from_string("2016-01-01 12:00:00"),
                        *unprocessedRequest.enterTime());
        UNIT_ASSERT_EQUAL("uuid1", unprocessedRequest.uuid());
        UNIT_ASSERT_DOUBLES_EQUAL(2.2, *unprocessedRequest.travelTime(), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(3.3, *unprocessedRequest.travelTimeBase(), EPS);

        // Verify dumpObject.unprocessedTask

        auto taskSegment = dumpObject.unprocessedTask->segments().at(0);
        UNIT_ASSERT_EQUAL(seg(edgeId, 0), taskSegment.segmentId());
        UNIT_ASSERT_EQUAL(time_from_string("2016-01-01 11:00:00"),
                        *taskSegment.enterTime());
        UNIT_ASSERT_EQUAL("uuid2", taskSegment.uuid());
        UNIT_ASSERT_DOUBLES_EQUAL(4.4, *taskSegment.travelTime(), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(5.5, *taskSegment.travelTimeBase(), EPS);

        UNIT_ASSERT_EQUAL(id, dumpObject.unprocessedTask->id());
        UNIT_ASSERT_EQUAL(time_from_string("2016-01-02 0:00:00"),
                        dumpObject.unprocessedTask->time());
        UNIT_ASSERT_EQUAL(time_from_string("2016-01-02 0:00:00"),
                        dumpObject.unprocessedTask->interpolateTime());
        UNIT_ASSERT_EQUAL(false, dumpObject.unprocessedTask->isMarker());
        UNIT_ASSERT_EQUAL(1u, dumpObject.unprocessedTask->segments().size());

        // Verify dumpObject.processedInfo

        UNIT_ASSERT_EQUAL(911, dumpObject.processedInfo->regionId());
        UNIT_ASSERT_EQUAL(time_from_string("2016-01-01 8:00:00"),
                        dumpObject.processedInfo->lastTime());

        UNIT_ASSERT_EQUAL(1u, dumpObject.processedInfo->segments().size());
        auto& segmentHistory = dumpObject.processedInfo->segments().at(0);
        UNIT_ASSERT_EQUAL(time_from_string("2016-01-01 7:00:00"),
                        segmentHistory.lastSignalTime());

        UNIT_ASSERT_EQUAL(1u, segmentHistory.items().size());
        auto [uuid, info] = *segmentHistory.items().begin();
        UNIT_ASSERT_EQUAL("uuid3", uuid);
        UNIT_ASSERT_DOUBLES_EQUAL(6.6, info.travelTime(), EPS);
        UNIT_ASSERT_DOUBLES_EQUAL(7.7, info.travelTimeBase(), EPS);
        UNIT_ASSERT_EQUAL(time_from_string("2016-01-01 6:00:00"), info.enterTime());
    }

    }   // namespace


    Y_UNIT_TEST(SuccessfulSerialization)
    {
        PersistentIndex edgeIndex = buildDefaultPersistentIndex();

        std::stringstream stream;
        saveSimpleState(stream, edgeIndex);

        auto loadedState = SegmentsState::load(stream, time_from_string("2016-01-02 0:00:00"), edgeIndex);

        UNIT_ASSERT_EQUAL("12.11.10", loadedState.graphVersion);
        UNIT_ASSERT_EQUAL(time_from_string("2016-01-01 12:00:00"),
                        loadedState.lastSignalTime);
        UNIT_ASSERT(loadedState.dump->size() == 4);

        for (int i = 0; i < 3; ++i) {
            size_t edgeId = i + 1;
            checkSimpleStateItem(loadedState.dump->at(i), {.edgeId = EdgeId(edgeId)});
        }

        checkSimpleStateItem(loadedState.dump->at(3), {.edgeId = EdgeId(3), .manoeuvreId = ManoeuvreId(123)});
    }

    namespace {
        SegmentsState saveAndLoadSimpleState(const PersistentIndex& saveEdgeIndex,
                const PersistentIndex& loadEdgeIndex)
        {
            std::stringstream stream;
            saveSimpleState(stream, saveEdgeIndex);
            return SegmentsState::load(stream, time_from_string("2016-01-02 0:00:00"), loadEdgeIndex);
        }
    }

    Y_UNIT_TEST(PersistentState)
    {
        PersistentIndex saveEdgeIndex = buildPersistentIndex({
            { 1000, 1 },
            { 2000, 2 },
            { 3000, 3 }
        });

        {
            PersistentIndex loadEdgeIndex = buildPersistentIndex({
                { 1000, 1 },
                // { 2000, 2 }    // no persistent id for Id=2
                { 3000, 30 }   // new short Id (3 -> 30)
            });

            auto state = saveAndLoadSimpleState(saveEdgeIndex, loadEdgeIndex);

            UNIT_ASSERT(state.dump->size() == 3);
            UNIT_ASSERT(state.unknownEdgeItems == 1);       // one state is skipped
            checkSimpleStateItem(state.dump->at(0), {.edgeId=EdgeId(1)});     // restored with old short id
            checkSimpleStateItem(state.dump->at(1), {.edgeId=EdgeId(30)});    // restored with new short id
            checkSimpleStateItem(state.dump->at(2), {.edgeId=EdgeId(30), .manoeuvreId = ManoeuvreId(123)});    // restored with new short id
        }

        {
            PersistentIndex loadEdgeIndex = buildPersistentIndex({
                //{ 1000, 1 }
                { 2000, 20 }     // new short Id (2 -> 20)
                //{ 3000, 3 }
            });

            auto state = saveAndLoadSimpleState(saveEdgeIndex, loadEdgeIndex);

            UNIT_ASSERT(state.dump->size() == 1);
            UNIT_ASSERT(state.unknownEdgeItems == 3);
            checkSimpleStateItem(state.dump->at(0), {.edgeId=EdgeId(20)});
        }

        {
            PersistentIndex loadEdgeIndex = buildPersistentIndex({
                { 1000,  1 },   // same long id
                { 2000,  20 },  // same long id, new short id
                { 3002,  3 }    // new long id
            });

            auto state = saveAndLoadSimpleState(saveEdgeIndex, loadEdgeIndex);

            UNIT_ASSERT(state.dump->size() == 2);
            UNIT_ASSERT(state.unknownEdgeItems == 2);
            checkSimpleStateItem(state.dump->at(0), {.edgeId=EdgeId(1)});
            checkSimpleStateItem(state.dump->at(1), {.edgeId=EdgeId(20)});
        }

        {
            PersistentIndex loadEdgeIndex = buildPersistentIndex({
                { 1001,  1 },   // all new long ids
                { 2001,  2 },
                { 3002,  3 }
            });

            auto state = saveAndLoadSimpleState(saveEdgeIndex, loadEdgeIndex);

            UNIT_ASSERT(state.dump->size() == 0);
            UNIT_ASSERT(state.unknownEdgeItems == 4);
        }
    }
}

