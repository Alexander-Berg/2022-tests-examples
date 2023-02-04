#include "fixture.h"

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <maps/indoor/libs/db/include/task_gateway.h>

namespace maps::mirc::task_activator::tests {
namespace {

const std::string TEST_INDOOR_PLAN_ID = "11111";
const geolib3::Point2 TEST_POINT{10,20};
const int64_t TEST_INTERVAL = 2;

db::ugc::Tasks makeTestTasks(
    unsigned int pendingTasks,
    unsigned int availableTasks,
    unsigned int acquiredTasks,
    unsigned int doneTasks,
    unsigned int cancelledTasks,
    int64_t interval = 0)
{
    db::ugc::Tasks tasks{};
    for(unsigned int i = 0; i < pendingTasks; ++i) {
        db::ugc::Task task;
        task.setStatus(db::ugc::TaskStatus::Pending)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID)
            .setInterval(interval);
        tasks.emplace_back(std::move(task));
    }

    for(unsigned int i = 0; i < availableTasks; ++i) {
        db::ugc::Task task;
        task.setStatus(db::ugc::TaskStatus::Available)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        tasks.emplace_back(std::move(task));
    }

    for(unsigned int i = 0; i < acquiredTasks; ++i) {
        db::ugc::Task task;
        task.setStatus(db::ugc::TaskStatus::Acquired)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        tasks.emplace_back(std::move(task));
    }

    for(unsigned int i = 0; i < doneTasks; ++i) {
        db::ugc::Task task;
        task.setStatus(db::ugc::TaskStatus::Done)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        task.markAsDone();
        tasks.emplace_back(std::move(task));
    }

    for(unsigned int i = 0; i < cancelledTasks; ++i) {
        db::ugc::Task task;
        task.setStatus(db::ugc::TaskStatus::Cancelled)
            .setDistanceInMeters(40000000)
            .setGeodeticPoint(TEST_POINT)
            .setIndoorPlanId(TEST_INDOOR_PLAN_ID);
        tasks.emplace_back(std::move(task));
    }
    return tasks;
}

void checkActivationStatus(
    pgpool3::Pool& pool,
    unsigned int pendingTasksCount,
    unsigned int availableTasksCount,
    unsigned int acquiredTasksCount,
    unsigned int cancelledTasksCount,
    unsigned int doneTasksCount,
    bool activationResult)
{
    unsigned int activationAmendment = activationResult ? 1 : 0;

    auto txn = pool.slaveTransaction();
    auto pendingTasks = db::ugc::TaskGateway{*txn}.load(
        db::ugc::table::Task::indoorPlanId == TEST_INDOOR_PLAN_ID &&
        db::ugc::table::Task::status == db::ugc::TaskStatus::Pending);
    ASSERT_EQ(pendingTasks.size(), pendingTasksCount - activationAmendment);

    auto availableTasks = db::ugc::TaskGateway{*txn}.load(
        db::ugc::table::Task::indoorPlanId == TEST_INDOOR_PLAN_ID &&
        db::ugc::table::Task::status == db::ugc::TaskStatus::Available);
    ASSERT_EQ(availableTasks.size(), availableTasksCount + activationAmendment);

    auto acquiredTasks = db::ugc::TaskGateway{*txn}.load(
        db::ugc::table::Task::indoorPlanId == TEST_INDOOR_PLAN_ID &&
        db::ugc::table::Task::status == db::ugc::TaskStatus::Acquired);
    ASSERT_EQ(acquiredTasks.size(), acquiredTasksCount);

    auto cancelledTasks = db::ugc::TaskGateway{*txn}.load(
        db::ugc::table::Task::indoorPlanId == TEST_INDOOR_PLAN_ID &&
        db::ugc::table::Task::status == db::ugc::TaskStatus::Cancelled);
    ASSERT_EQ(cancelledTasks.size(), cancelledTasksCount);

    auto doneTasks = db::ugc::TaskGateway{*txn}.load(
        db::ugc::table::Task::indoorPlanId == TEST_INDOOR_PLAN_ID &&
        db::ugc::table::Task::status == db::ugc::TaskStatus::Done);
    ASSERT_EQ(doneTasks.size(), doneTasksCount);
}

}

Y_UNIT_TEST_SUITE_F(indoor_task_activator_tests, Fixture) {

Y_UNIT_TEST(indoor_task_activator_test_1)
{
    taskActivator().activate();
    checkActivationStatus(pgPool(), 0, 0, 0, 0, 0, false);
}

Y_UNIT_TEST(indoor_task_activator_test_2)
{
    const unsigned int pendingTasksCount = 2;
    const unsigned int availableTasksCount = 0;
    const unsigned int acquiredTasksCount = 3;
    const unsigned int cancelledTasksCount = 4;
    const unsigned int doneTasksCount = 5;

    {
        auto txn = pgPool().masterWriteableTransaction();
        auto tasks = makeTestTasks(
        pendingTasksCount,
        availableTasksCount,
        acquiredTasksCount,
        doneTasksCount,
        cancelledTasksCount);
        db::ugc::TaskGateway{*txn}.insert(tasks);

        txn->commit();
    }

    taskActivator().activate();

    checkActivationStatus(
        pgPool(),
        pendingTasksCount,
        availableTasksCount,
        acquiredTasksCount,
        cancelledTasksCount,
        doneTasksCount,
        false);
}

Y_UNIT_TEST(indoor_task_activator_test_3)
{
    const unsigned int pendingTasksCount = 10;
    const unsigned int availableTasksCount = 0;
    const unsigned int acquiredTasksCount = 0;
    const unsigned int cancelledTasksCount = 4;
    const unsigned int doneTasksCount = 5;

    {
        auto txn = pgPool().masterWriteableTransaction();
        auto tasks = makeTestTasks(
        pendingTasksCount,
        availableTasksCount,
        acquiredTasksCount,
        doneTasksCount,
        cancelledTasksCount,
        TEST_INTERVAL);
        db::ugc::TaskGateway{*txn}.insert(tasks);

        txn->commit();
    }

    taskActivator().activate();

    checkActivationStatus(
        pgPool(),
        pendingTasksCount,
        availableTasksCount,
        acquiredTasksCount,
        cancelledTasksCount,
        doneTasksCount,
        false);
}

Y_UNIT_TEST(indoor_task_activator_test_4)
{
    const unsigned int pendingTasksCount = 10;
    const unsigned int availableTasksCount = 0;
    const unsigned int acquiredTasksCount = 0;
    const unsigned int cancelledTasksCount = 4;
    const unsigned int doneTasksCount = 5;

    {
        auto txn = pgPool().masterWriteableTransaction();
        auto tasks = makeTestTasks(
            pendingTasksCount,
            availableTasksCount,
            acquiredTasksCount,
            doneTasksCount,
            cancelledTasksCount);
        db::ugc::TaskGateway{*txn}.insert(tasks);

        txn->commit();
    }

    taskActivator().activate();

    checkActivationStatus(
        pgPool(),
        pendingTasksCount,
        availableTasksCount,
        acquiredTasksCount,
        cancelledTasksCount,
        doneTasksCount,
        true);
}

}

} // namespace maps::mirc::task_activator::tests
