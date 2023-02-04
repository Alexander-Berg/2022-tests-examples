#include "maps/b2bgeo/vm_scheduler/server/libs/scheduler/include/scheduler.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/scheduler/tests/test_utils.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/libs/common/include/errors.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;

TEST(Scheduler, schedule)
{
    t::TaskStorageMock taskStorageMock;
    const auto possibleSlots = t::getPossibleSlots();
    Scheduler scheduler("backendId", &taskStorageMock, possibleSlots);
    scheduler.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));

    EXPECT_CALL(taskStorageMock, addTask).Times(0);
    EXPECT_CALL(taskStorageMock, startScheduling(_, _)).WillOnce(Return(Result{PlanId{456}}));
    EXPECT_CALL(taskStorageMock, getCurrentState()).WillOnce(Return(Result{State{}}));
    EXPECT_CALL(taskStorageMock, commitPlanChange(_, _)).WillOnce(Return(Result<void>::Success()));

    scheduler.schedule();

    EXPECT_TRUE(TUnistat::Instance().GetSignalValueUnsafe("scheduler_cycle_time")->HasNumber());
    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("scheduler_vms_created")->GetNumber(), 0);
    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("scheduler_vms_terminated")->GetNumber(), 0);
    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("scheduler_jobs_assigned")->GetNumber(), 0);
}

TEST(Scheduler, schedulingCancelled)
{
    t::TaskStorageMock taskStorageMock;
    const auto possibleSlots = t::getPossibleSlots();
    Scheduler scheduler("backendId", &taskStorageMock, possibleSlots);
    scheduler.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));


    EXPECT_CALL(taskStorageMock, startScheduling(_, _)).WillOnce(
        Return(Result<PlanId>::Failure<SchedulingCancelled>(
        "The plan was recently updated, new scheduling is not needed yet")));
    EXPECT_CALL(taskStorageMock, getCurrentState).Times(0);
    EXPECT_CALL(taskStorageMock, commitPlanChange).Times(0);

    scheduler.schedule();
    EXPECT_TRUE(TUnistat::Instance().GetSignalValueUnsafe("scheduler_cycle_time")->HasNumber());
    EXPECT_TRUE(TUnistat::Instance().GetSignalValueUnsafe("scheduler_failures").Empty());
}
