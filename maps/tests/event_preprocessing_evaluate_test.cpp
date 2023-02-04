#include <maps/indoor/libs/db/include/assignment_task_path.h>
#include <maps/indoor/libs/db/include/assignment_gateway.h>
#include <maps/indoor/libs/db/include/event_preprocessing_record_gateway.h>
#include <maps/indoor/libs/db/include/radiomap_transmitter_gateway.h>
#include <maps/indoor/libs/db/include/task_gateway.h>
#include <maps/indoor/libs/db/include/task_path_track_gateway.h>
#include <maps/indoor/libs/db/include/transmitter_last_seen_time_gateway.h>
#include <maps/indoor/long-tasks/src/event-preprocessing/lib/impl/timestamp_last_seen.cpp>
#include <maps/indoor/long-tasks/src/evotor/include/persistent_id_cache.h>
#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/include/radiomap_evaluator.h>
#include <maps/indoor/long-tasks/src/radiomap-evaluation-cron-job/lib/impl/utils.h>

#include <maps/libs/common/include/make_batches.h>
#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <util/generic/guid.h>

#include <random>

#include "maps/indoor/libs/unittest/fixture.h"

namespace maps::mirc::data_init::tests {
using namespace ::testing;

namespace pindoor = yandex::maps::sproto::offline::mrc::indoor;

namespace {

const std::string TEST_USER_ID = "TestUserId";
const std::string TEST_INDOOR_PLAN_ID = "12345";
const std::string TEST_INDOOR_LEVEL_ID = "1";
const geolib3::Point2 TEST_POINT{40.0000, 60.0000};
const geolib3::Polyline2 TEST_PATH = geolib3::Polyline2({{
    radiomap_evaluator::localPlanarToGeodetic(geolib3::Point2(0, 0), TEST_POINT),
    radiomap_evaluator::localPlanarToGeodetic(geolib3::Point2(1, 1), TEST_POINT)
}});

std::string generateTrackId()
{
    const auto tsGuid = CreateGuidAsString();
    return std::string(tsGuid.data(), tsGuid.length());
}

void prepareDataBase(chrono::TimePoint currentTime, pgpool3::Pool& pool_)
{
    db::ugc::Task task;
    auto txn = pool_.masterWriteableTransaction();

    // create task & task_paths
    {
        task.setStatus(db::ugc::TaskStatus::Available)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        task.addPath(
            TEST_PATH,
            TEST_INDOOR_LEVEL_ID,
            std::chrono::seconds(100),
            40000,
            std::nullopt);

        db::ugc::TaskGateway{*txn}.insert(task);
    }

    db::TId assignmentId = 0;
    // create assignment
    {
        auto assignment = task.assignTo(TEST_USER_ID);
        assignment.markAsCompleted();
        assignment.setSubmittedTime(currentTime - std::chrono::hours(1));
        assignment.setAcquiredTime(currentTime - std::chrono::hours(3));

        db::ugc::AssignmentGateway{*txn}.insert(assignment);
        task.markAsDone();
        db::ugc::TaskGateway{*txn}.update(task);
        assignmentId = assignment.id();
    }

    // create createAssignmentPaths
    {
        auto assignment = db::ugc::AssignmentGateway{*txn}.loadById(assignmentId);
        assignment.createAssignmentPaths(task.paths());
        db::ugc::AssignmentTaskPathGateway{*txn}.insert(assignment.assignmentTaskPaths());
    }

    // create TaskPathTracks
    {
        auto assignmentTaskPaths = db::ugc::AssignmentTaskPathGateway{*txn}.load(
            db::ugc::table::AssignmentTaskPath::assignmentId == assignmentId);
        for (auto& assignmentTaskPath : assignmentTaskPaths) {
            auto taskPaths = db::ugc::TaskPathGateway{*txn}.load(
                db::ugc::table::TaskPath::id == assignmentTaskPath.taskPathId());
            auto trackId = generateTrackId();
            assignmentTaskPath.markAsAccepted();
            auto taskPathTrack = assignmentTaskPath.createTaskPathTracks(trackId);
            db::ugc::TaskPathTrackGateway{*txn}.insert(taskPathTrack);
        }

    }
    txn->commit();
}

void testLoadTrackTimes(chrono::TimePoint currentTime, pgpool3::Pool& pool_) {
    auto txn = pool_.masterWriteableTransaction();

    auto tasks = db::ugc::TaskGateway{*txn}.load();
    ASSERT_TRUE(tasks.size() == 1);

    auto assignment = db::ugc::AssignmentGateway{*txn}.load();
    ASSERT_TRUE(assignment.size() == 1);

    auto assignment_task_path = db::ugc::AssignmentTaskPathGateway{*txn}.load();
    ASSERT_TRUE(assignment_task_path.size() == 1);

    auto assignment_task_path_track = db::ugc::TaskPathTrackGateway{*txn}.load();
    ASSERT_TRUE(assignment_task_path_track.size() == 1);

    const std::vector<db::preprocessing::EventRecord> events = {
        db::preprocessing::EventRecord{
            db::preprocessing::EventRecordId{0},
            std::chrono::milliseconds{0},
            "1",
            db::ugc::TransmitterType::Beacon,
            db::preprocessing::TrackId(assignment_task_path_track[0].id())
        }
    };

    const auto batch = maps::common::makeBatches(events, 1)[0];
    event_preprocessing::last_seen::EventProcessorImpl eventProcessorImpl(pool_);

    auto trackTimes = eventProcessorImpl.loadTrackTimes(
        batch,
        *txn
    );

    ASSERT_TRUE(trackTimes[events[0].trackId] == currentTime - std::chrono::hours(2));
}

void testUpdateLastSeenTimeInDB(chrono::TimePoint currentTime, pgpool3::Pool& pool_) {
    auto txn = pool_.masterWriteableTransaction();

    auto assignment_task_path_track = db::ugc::TaskPathTrackGateway{*txn}.load();
    ASSERT_TRUE(assignment_task_path_track.size() == 1);

    const std::vector<db::preprocessing::EventRecord> events = {
        db::preprocessing::EventRecord{
            db::preprocessing::EventRecordId{0},
            std::chrono::milliseconds{0},
            "1",
            db::ugc::TransmitterType::Beacon,
            db::preprocessing::TrackId(assignment_task_path_track[0].id())
        }
    };

    const auto batch = maps::common::makeBatches(events, 1)[0];
    event_preprocessing::last_seen::EventProcessorImpl eventProcessorImpl(pool_);

    eventProcessorImpl.updateLastSeenTimeInDB(batch, *txn);

    auto temp = db::ugc::TransmitterLastSeenTimeGateway{*txn}.load();
    ASSERT_TRUE(temp.size() == 1);
    ASSERT_TRUE(temp[0].lastSeenAt() == currentTime - std::chrono::hours(2));
}

}

Y_UNIT_TEST_SUITE_F(data_init_evaluate_tests, unittest::Fixture) {

Y_UNIT_TEST(load_track_times_test)
{
    auto currentTime = chrono::TimePoint::clock::now();
    prepareDataBase(currentTime, pgPool());
    testLoadTrackTimes(currentTime, pgPool());
}

Y_UNIT_TEST(update_last_seen_time_in_db_test)
{
    auto currentTime = chrono::TimePoint::clock::now();
    prepareDataBase(currentTime, pgPool());
    testUpdateLastSeenTimeInDB(currentTime, pgPool());
}

} // Y_UNIT_TEST_SUITE_F

} // namespace maps::mirc::data_init::tests
