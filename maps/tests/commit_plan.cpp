#include "maps/b2bgeo/vm_scheduler/server/libs/db/include/pg_task_storage.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/db/impl/test_utils.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/db/impl/errors.h"

#include <maps/b2bgeo/vm_scheduler/libs/common/include/stringify.h>

#include <maps/b2bgeo/libs/postgres/helpers.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo;
using namespace maps::b2bgeo::vm_scheduler;
using namespace std::chrono_literals;
namespace t = maps::b2bgeo::vm_scheduler::testing;

namespace {

StateChange getStateChange()
{
    return {
        .jobToVm = {
            {
                1,
                DesiredSlotId(1),
            },
        },
        .desiredSlotMap = {
            {
                DesiredSlotId(1),
                DesiredSlot{
                    .total = SlotCapacity{
                        .cpu = 3_cores,
                        .ram = 4096_MB,
                    },
                    .idle = SlotCapacity{
                        .cpu = 1_cores,
                        .ram = 1024_MB,
                    },
                },
            },
        },
        .updatedIdleCapacities = { },
        .vmsToTerminate = { },
    };
}

} // anonymous namespace

TEST(CommitPlanChange, fulScenario)
{
    Postgres postgres("vms_db_schema.sql");
    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto taskParameters = t::getOneJobTaskParameters();
    const auto jobIdResult = pgTaskStorage.addTask(taskParameters);
    EXPECT_TRUE(jobIdResult.IsSuccess());

    const auto planIdResult = pgTaskStorage.startScheduling("backendId", 2s);
    EXPECT_TRUE(planIdResult.IsSuccess());

    const auto stateChange = getStateChange();

    auto commitResult = pgTaskStorage.commitPlanChange(stateChange, planIdResult.ValueRefOrThrow());
    EXPECT_TRUE(commitResult.IsSuccess());

    const auto currentStateResult = pgTaskStorage.getCurrentState();
    EXPECT_TRUE(currentStateResult.IsSuccess());

    const auto expectedState = State{
        .queuedJobs = { },
        .vms = {
            ActiveVm{
                .id = 1,
                .totalCapacity = SlotCapacity{
                    .cpu = 3_cores,
                    .ram = 4096_MB,
                },
                .idleCapacity = SlotCapacity{
                    .cpu = 1_cores,
                    .ram = 1024_MB,
                },
            },
        },
    };
    EXPECT_EQ(expectedState, currentStateResult.ValueRefOrThrow());

    auto pool = createPool(postgres);
    auto txn = pool.masterWriteableTransaction();
    const auto lock = toString("SELECT status, cpu, ram FROM scheduler.jobs WHERE id = ",
        jobIdResult.ValueRefOrThrow().jobIds[0], ";");
    const auto result = pg::execQuery(lock, *txn);
    EXPECT_EQ(FromString<JobStatus>(result[0].at("status").as<std::string>()), JobStatus::Scheduled);
    EXPECT_EQ(result[0].at("cpu").as<size_t>(), 1u);
    EXPECT_EQ(result[0].at("ram").as<size_t>(), 1024u);
}

TEST(CommitPlanChange, optimisticLockingFailed)
{
    Postgres postgres("vms_db_schema.sql");
    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto taskParameters = t::getOneJobTaskParameters();
    const auto jobIdResult = pgTaskStorage.addTask(taskParameters);
    EXPECT_TRUE(jobIdResult.IsSuccess());

    const auto planIdResult = pgTaskStorage.startScheduling("backendId", 2s);
    EXPECT_TRUE(planIdResult.IsSuccess());

    const auto stateChange = getStateChange();

    const auto cancelResult = pgTaskStorage.cancelTask(jobIdResult.ValueRefOrThrow().taskId);
    EXPECT_TRUE(cancelResult.IsSuccess());

    auto commitResult = pgTaskStorage.commitPlanChange(stateChange, planIdResult.ValueRefOrThrow());
    EXPECT_TRUE(commitResult.IsFailure());
    EXPECT_TRUE(commitResult.holdsErrorType<OptimisticLockingFailure>());
}
