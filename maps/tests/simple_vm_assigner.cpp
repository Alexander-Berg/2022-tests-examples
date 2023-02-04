#include "maps/b2bgeo/vm_scheduler/server/libs/scheduler/impl/simple_vm_assigner.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/scheduler/tests/test_utils.h"

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t =  maps::b2bgeo::vm_scheduler::testing;

TEST(SimpleVmAssigner, assign)
{
    const auto initialState = State{
        .queuedJobs =
            {
                QueuedJobInfo{
                    .id = 0,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 2_cores,
                            .ram = 4096_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 1,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 1024_MB,
                        },
                },
            },
        .vms =
            {
                ActiveVm{
                    .id = 5,
                    .totalCapacity =
                        SlotCapacity{
                            .cpu = 4_cores,
                            .ram = 2048_MB,
                        },
                    .idleCapacity =
                        SlotCapacity{
                            .cpu = 4_cores,
                            .ram = 2048_MB,
                        },
                },
            },
    };
    SimpleVmAssigner vmAssigner(initialState);
    const auto stateChange = vmAssigner.assign();

    const auto expectedStateChange = StateChange{
        .jobToVm =
            {
                {
                    0,
                    DesiredSlotId(0),
                },
                {
                    1,
                    DesiredSlotId(1),
                },
            },
        .desiredSlotMap =
            {
                {
                    DesiredSlotId(0),
                    DesiredSlot{
                        .total =
                            SlotCapacity{
                                .cpu = 2_cores,
                                .ram = 4096_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 0_cores,
                                .ram = 0_MB,
                            },
                    },
                },
                {
                    DesiredSlotId(1),
                    DesiredSlot{
                        .total =
                            SlotCapacity{
                                .cpu = 1_cores,
                                .ram = 1024_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 0_cores,
                                .ram = 0_MB,
                            },
                    },
                },
            },
        .updatedIdleCapacities = {},
        .vmsToTerminate = {5},
    };

    EXPECT_EQ(stateChange, expectedStateChange);
    t::checkStateConstrains(initialState, stateChange);
}

TEST(SimpleVmAssigner, emptyJobs)
{
    const auto initialState = State{
        .queuedJobs = {},
        .vms =
            {
                ActiveVm{
                    .id = 5,
                    .totalCapacity =
                        SlotCapacity{
                            .cpu = 4_cores,
                            .ram = 2048_MB,
                        },
                    .idleCapacity =
                        SlotCapacity{
                            .cpu = 4_cores,
                            .ram = 2048_MB,
                        },
                },
                ActiveVm{
                    .id = 1,
                    .totalCapacity =
                        SlotCapacity{
                            .cpu = 2_cores,
                            .ram = 2048_MB,
                        },
                    .idleCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 1024_MB,
                        },
                },
                ActiveVm{
                    .id = 3,
                    .totalCapacity =
                        SlotCapacity{
                            .cpu = 2_cores,
                            .ram = 1024_MB,
                        },
                    .idleCapacity =
                        SlotCapacity{
                            .cpu = 2_cores,
                            .ram = 1024_MB,
                        },
                },
            },
    };
    SimpleVmAssigner vmAssigner(initialState);
    const auto stateChange = vmAssigner.assign();

    const auto expectedStateChange = StateChange{
        .jobToVm = {},
        .desiredSlotMap = {},
        .updatedIdleCapacities = {},
        .vmsToTerminate = {5, 3},
    };

    EXPECT_EQ(stateChange, expectedStateChange);
    t::checkStateConstrains(initialState, stateChange);
}

TEST(SimpleVmAssigner, emptyVms)
{
    const auto initialState = State{
        .queuedJobs =
            {
                QueuedJobInfo{
                    .id = 9,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 3_cores,
                            .ram = 4096_MB,
                        },
                },
            },
        .vms = {},
    };
    SimpleVmAssigner vmAssigner(initialState);
    const auto stateChange = vmAssigner.assign();

    const auto expectedStateChange = StateChange{
        .jobToVm =
            {
                {
                    9,
                    DesiredSlotId(0),
                },
            },
        .desiredSlotMap =
            {
                {
                    DesiredSlotId(0),
                    DesiredSlot{
                        .total =
                            SlotCapacity{
                                .cpu = 3_cores,
                                .ram = 4096_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 0_cores,
                                .ram = 0_MB,
                            },
                    },
                },
            },
        .updatedIdleCapacities = {},
        .vmsToTerminate = {},
    };

    EXPECT_EQ(stateChange, expectedStateChange);
    t::checkStateConstrains(initialState, stateChange);
}
