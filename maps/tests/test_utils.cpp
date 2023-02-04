#include "maps/b2bgeo/vm_scheduler/server/libs/scheduler/tests/test_utils.h"

#include <library/cpp/testing/gtest/gtest.h>


namespace maps::b2bgeo::vm_scheduler::testing {

std::vector<SlotCapacity> getPossibleSlots()
{
    return {
        {
            .cpu = 1_cores,
            .ram = 512_MB,
        },
        {
            .cpu = 1_cores,
            .ram = 1024_MB,
        },
        {
            .cpu = 1_cores,
            .ram = 2048_MB,
        },
        {
            .cpu = 2_cores,
            .ram = 1024_MB,
        },
        {
            .cpu = 4_cores,
            .ram = 2048_MB,
        },
        {
            .cpu = 8_cores,
            .ram = 4096_MB,
        },
        {
            .cpu = 16_cores,
            .ram = 8192_MB,
        },
    };
}

void checkStateConstrains(const State& state, const StateChange& stateChange)
{
    SlotCapacity queuedJobsCapacity = {
        .cpu = 0_cores,
        .ram = 0_MB,
    };

    SlotCapacity addedBusyCapacity = {
        .cpu = 0_cores,
        .ram = 0_MB,
    };

    for (const auto& job : state.queuedJobs) {
        queuedJobsCapacity += job.requiredCapacity;
    }
    for (const auto& [_, desiredSlot] : stateChange.desiredSlotMap) {
        addedBusyCapacity += (desiredSlot.total - desiredSlot.idle);
    }
    for (const auto& vm : state.vms) {
        if (stateChange.updatedIdleCapacities.contains(vm.id)) {
            addedBusyCapacity += vm.idleCapacity;
        }
    }
    for (const auto& [_, slot] : stateChange.updatedIdleCapacities) {
        addedBusyCapacity -= slot;
    }

    EXPECT_EQ(queuedJobsCapacity, addedBusyCapacity);
}

} // namespace maps::b2bgeo::vm_scheduler::testing
