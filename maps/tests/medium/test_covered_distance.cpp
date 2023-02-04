#include <maps/indoor/libs/db/include/assignment_gateway.h>
#include <maps/indoor/libs/db/include/assignment_task_path.h>
#include <maps/indoor/libs/db/include/task_gateway.h>

#include <maps/indoor/libs/unittest/fixture.h>
#include <maps/indoor/libs/unittest/include/yandex/maps/mirc/unittest/unittest_config.h>

#include <maps/indoor/long-tasks/src/startrek-sync/lib/distance.h>

#include <maps/libs/chrono/include/time_point.h>
#include <maps/libs/http/include/test_utils.h>

namespace maps::mirc::startrek::tests {

namespace {

const std::string TEST_INDOOR_LEVEL_ID = "1";
const std::string TEST_USER_ID = "TestUserId";
const std::string TEST_INDOOR_PLAN_ID = "12345";
const geolib3::Polyline2 TEST_PATH = geolib3::Polyline2({{geolib3::Point2(0, 0), geolib3::Point2(1, 1)}});
const geolib3::Point2 TEST_POINT{40.0000, 60.0000};

using db::ugc::AssignmentTaskPathStatus;

void prepareDataBase(pqxx::transaction_base& txn)
{
    db::ugc::Task task;

    // create task & task_paths
    {
        task.setStatus(db::ugc::TaskStatus::Available)
            .setDistanceInMeters(1111.)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);

        for (size_t i = 0; i < 10; i++) {
            task.addPath(
                TEST_PATH,
                TEST_INDOOR_LEVEL_ID,
                std::chrono::seconds(100),
                111.1,
                std::nullopt);
        }

        db::ugc::TaskGateway{txn}.insert(task);
    }

    db::TId assignmentId = 0;
    // create assignment
    {
        auto assignment = task.assignTo(TEST_USER_ID);
        assignment.markAsCompleted();

        db::ugc::AssignmentGateway{txn}.insert(assignment);
        task.markAsDone();
        db::ugc::TaskGateway{txn}.update(task);
        assignmentId = assignment.id();
    }

    // create createAssignmentPaths
    {
        auto assignment = db::ugc::AssignmentGateway{txn}.loadById(assignmentId);
        assignment.createAssignmentPaths(task.paths(), AssignmentTaskPathStatus::Accepted);
        db::ugc::AssignmentTaskPathGateway{txn}.insert(assignment.assignmentTaskPaths());
    }
}

void markOneAssignmentTaskPathAsRejected(pqxx::transaction_base& txn)
{
    auto assignmentTaskPath = db::ugc::AssignmentTaskPathGateway{txn}.load()[0];
    assignmentTaskPath.markAsRejected();
    db::ugc::AssignmentTaskPathGateway{txn}.update(assignmentTaskPath);
}

} // namespace

Y_UNIT_TEST_SUITE_F(test_covered_distance, unittest::Fixture)
{

Y_UNIT_TEST(test_distance_with_accepted_status)
{
    auto& pool = pgPool();
    auto txn = pool.masterWriteableTransaction();
    prepareDataBase(*txn);
    txn->commit();
    txn = pool.slaveTransaction();
    UNIT_ASSERT(std::abs(distance::getFullDistance(*txn, 1) - 1111.) < 0.1);
    UNIT_ASSERT(std::abs(distance::getPassedDistance(*txn, 1) - 1111.) < 0.1);
}

Y_UNIT_TEST(test_distance_with_invalid_status)
{
    auto& pool = pgPool();
    auto txn = pool.masterWriteableTransaction();
    prepareDataBase(*txn);
    markOneAssignmentTaskPathAsRejected(*txn);
    txn->commit();
    txn = pool.slaveTransaction();
    UNIT_ASSERT(std::abs(distance::getFullDistance(*txn, 1) - 1111.) < 0.1);
    UNIT_ASSERT(std::abs(distance::getPassedDistance(*txn, 1) - 999.9) < 0.1);
}

} // test_covered_distance suite

} // namespace maps::mirc::startrek::tests
