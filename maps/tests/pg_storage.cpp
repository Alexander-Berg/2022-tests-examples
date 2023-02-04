#include "maps/b2bgeo/vm_scheduler/server/libs/db/include/pg_task_storage.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/db/impl/test_utils.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/scheduler/include/scheduler.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/task.h>

#include <maps/b2bgeo/vm_scheduler/libs/common/include/stringify.h>

#include <maps/b2bgeo/libs/postgres_test/postgres.h>
#include <maps/b2bgeo/libs/postgres/helpers.h>

#include <gmock/gmock.h>
#include <util/string/join.h>

#include <memory>


using namespace maps::b2bgeo;
using namespace maps::b2bgeo::vm_scheduler;
using namespace std::chrono_literals;
namespace t = maps::b2bgeo::vm_scheduler::testing;


TEST(StartScheduling, exclusiveLock)
{
    Postgres postgres("vms_db_schema.sql");
    PgTaskStorage pgTaskStorage(createPool(postgres));

    auto pool = createPool(postgres);
    auto txn = pool.masterWriteableTransaction();
    const auto lock = "LOCK TABLE scheduler.plan IN ROW EXCLUSIVE MODE;";
    pg::execQuery(lock, *txn);

    const auto planIdResult = pgTaskStorage.startScheduling("backendId", 2s);
    EXPECT_TRUE(planIdResult.IsFailure());
}

TEST(StartScheduling, consecutiveStartScheduling)
{
    Postgres postgres("vms_db_schema.sql");
    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto firstPlanIdResult = pgTaskStorage.startScheduling("backendId", 2s);
    EXPECT_TRUE(firstPlanIdResult.IsSuccess());

    const auto secondPlanIdResult = pgTaskStorage.startScheduling("backendId", 2s);
    EXPECT_TRUE(secondPlanIdResult.IsFailure());

    const auto thirdPlanIdResult = pgTaskStorage.startScheduling("backendId", 0s);
    EXPECT_TRUE(thirdPlanIdResult.IsSuccess());
}

TEST(GetCurrentState, empty)
{
    Postgres postgres("vms_db_schema.sql");
    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto currentStateResult = pgTaskStorage.getCurrentState();
    EXPECT_TRUE(currentStateResult.IsSuccess());

    const State emptyState;
    EXPECT_EQ(currentStateResult.ValueRefOrThrow(), emptyState);
}

TEST(Allocation, allocate)
{
    Postgres postgres("vms_db_schema.sql");

    auto pool = createPool(postgres);
    const auto vmId = t::insertVm(postgres, VmStatus::PendingAllocation);

    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto vmsToAllocateResult = pgTaskStorage.getVmsToAllocate(5);
    EXPECT_TRUE(vmsToAllocateResult.IsSuccess());
    const auto& vmsToAllocate = vmsToAllocateResult.ValueRefOrThrow();
    const std::vector<AllocationPendingVmInfo> expectedVmInfos = {
        AllocationPendingVmInfo{
            .id = vmId,
            .capacity = {
                .cpu = 2_cores,
                .ram = 2048_MB,
            },
        },
    };
    EXPECT_EQ(vmsToAllocate, expectedVmInfos);

    const auto vmStatusQuery = toString("SELECT status FROM scheduler.vms WHERE id = ", vmId, ";");
    auto readTxn = pool.masterReadOnlyTransaction();
    const auto firstVmStatusResult = pg::execQuery(vmStatusQuery, *readTxn);
    EXPECT_EQ(FromString<VmStatus>(firstVmStatusResult[0].at("status").as<std::string>()), VmStatus::Allocating);

    const auto allocatedVmInfo = AllocatedVmInfo{
        .id = "cloud vm id",
        .type = "cloud vm type",
        .zone = "cloud vm zone",
    };
    const auto saveAllocationResult = pgTaskStorage.saveVmAllocationResult(vmId, allocatedVmInfo);
    EXPECT_TRUE(saveAllocationResult.IsSuccess());

    const auto secondVmStatusResult = pg::execQuery(vmStatusQuery, *readTxn);
    EXPECT_EQ(FromString<VmStatus>(secondVmStatusResult[0].at("status").as<std::string>()), VmStatus::Allocated);
}

TEST(Allocation, empty)
{
    Postgres postgres("vms_db_schema.sql");
    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto vmsToAllocateResult = pgTaskStorage.getVmsToAllocate(5);
    EXPECT_TRUE(vmsToAllocateResult.IsSuccess());
    const auto& vmsToAllocate = vmsToAllocateResult.ValueRefOrThrow();
    EXPECT_EQ(vmsToAllocate.size(), 0u);
}

TEST(Termination, terminate)
{
    Postgres postgres("vms_db_schema.sql");

    auto pool = createPool(postgres);
    const auto vmId = t::insertVm(postgres, VmStatus::PendingTermination);

    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto vmsToTerminateResult = pgTaskStorage.getVmsToTerminate(5);
    EXPECT_TRUE(vmsToTerminateResult.IsSuccess());
    const auto& vmsToTerminate = vmsToTerminateResult.ValueRefOrThrow();
    const std::vector<TerminationPendingVmInfo> expectedVmInfos = {
        TerminationPendingVmInfo{
            .id = vmId,
            .cloudVmId = "cloud vm id",
        },
    };
    EXPECT_EQ(vmsToTerminate, expectedVmInfos);

    const auto vmStatusQuery = toString("SELECT status FROM scheduler.vms WHERE id = ", vmId, ";");
    auto readTxn = pool.masterReadOnlyTransaction();
    const auto firstVmStatusResult = pg::execQuery(vmStatusQuery, *readTxn);
    EXPECT_EQ(FromString<VmStatus>(firstVmStatusResult[0].at("status").as<std::string>()), VmStatus::Terminating);

    const auto saveTerminationResult = pgTaskStorage.saveVmTerminationResult(vmId);
    EXPECT_TRUE(saveTerminationResult.IsSuccess());

    const auto secondVmStatusResult = pg::execQuery(vmStatusQuery, *readTxn);
    EXPECT_EQ(FromString<VmStatus>(secondVmStatusResult[0].at("status").as<std::string>()), VmStatus::Terminated);
}

TEST(Termination, empty)
{
    Postgres postgres("vms_db_schema.sql");
    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto vmsToTerminateResult = pgTaskStorage.getVmsToTerminate(5);
    EXPECT_TRUE(vmsToTerminateResult.IsSuccess());
    const auto& vmsToTerminate = vmsToTerminateResult.ValueRefOrThrow();
    EXPECT_EQ(vmsToTerminate.size(), 0u);
}

TEST(Cancellation, cancelQueued)
{
    Postgres postgres("vms_db_schema.sql");
    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto taskParameters = t::getThreeJobTaskParameters();
    const auto jobsResult = pgTaskStorage.addTask(taskParameters);
    EXPECT_TRUE(jobsResult.IsSuccess());

    const auto createdJobs = CreatedJobs{
        .taskId = 1,
        .jobIds = {1, 2, 3},
    };
    EXPECT_EQ(jobsResult.ValueRefOrThrow(), createdJobs);

    const auto cancelResult = pgTaskStorage.cancelTask(createdJobs.taskId);
    EXPECT_TRUE(cancelResult.IsSuccess());

    const auto jobStates = pgTaskStorage.getJobStates(createdJobs.taskId);
    EXPECT_TRUE(jobStates.IsSuccess());
    EXPECT_TRUE(std::all_of(
        jobStates.ValueRefOrThrow().cbegin(), jobStates.ValueRefOrThrow().cend(),
        [](const JobState& jobState){ return jobState.status == JobStatus::Cancelled; }));
}

TEST(Cancellation, cancelScheduled)
{
    Postgres postgres("vms_db_schema.sql");
    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto taskParameters = t::getThreeJobTaskParameters();
    const auto jobsResult = pgTaskStorage.addTask(taskParameters);
    EXPECT_TRUE(jobsResult.IsSuccess());

    auto pool = createPool(postgres);
    auto writeTxn = pool.masterWriteableTransaction();
    const auto updateJobsQuery = toString(
        "UPDATE scheduler.jobs ",
        "SET status = '", ToString(JobStatus::Scheduled), "' ",
        "WHERE id IN (", JoinSeq(", ", jobsResult.ValueRefOrThrow().jobIds), ");");
    pg::execQuery(updateJobsQuery, *writeTxn);
    writeTxn->commit();

    const auto cancelResult = pgTaskStorage.cancelTask(jobsResult.ValueRefOrThrow().taskId);
    EXPECT_TRUE(cancelResult.IsSuccess());

    const auto jobStates = pgTaskStorage.getJobStates(jobsResult.ValueRefOrThrow().taskId);
    EXPECT_TRUE(jobStates.IsSuccess());
    EXPECT_TRUE(std::all_of(
        jobStates.ValueRefOrThrow().cbegin(), jobStates.ValueRefOrThrow().cend(),
        [](const JobState& jobState){ return jobState.status == JobStatus::Cancelled; }));
}
