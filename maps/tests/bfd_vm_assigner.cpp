#include "maps/b2bgeo/vm_scheduler/server/libs/scheduler/impl/bfd_vm_assigner.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/scheduler/tests/test_utils.h"

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t =  maps::b2bgeo::vm_scheduler::testing;

TEST(BFDVmAssigner, assign)
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
    const auto possibleSlots = t::getPossibleSlots();
    BFDVmAssigner vmAssigner(initialState, possibleSlots);
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
                    5u,
                },
            },
        .desiredSlotMap =
            {
                {
                    DesiredSlotId(0),
                    DesiredSlot{
                        .total =
                            SlotCapacity{
                                .cpu = 8_cores,
                                .ram = 4096_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 6_cores,
                                .ram = 0_MB,
                            },
                    },
                },
            },
        .updatedIdleCapacities = {
            {
                5,
                {
                    .cpu = 3_cores,
                    .ram = 1024_MB,
                }
            }
        },
        .vmsToTerminate = {},
    };

    EXPECT_EQ(stateChange, expectedStateChange);
    t::checkStateConstrains(initialState, stateChange);
}

TEST(BFDVmAssigner, emptyJobs)
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

    const auto possibleSlots = t::getPossibleSlots();
    BFDVmAssigner vmAssigner(initialState, possibleSlots);
    const auto stateChange = vmAssigner.assign();

        const auto expectedStateChange = StateChange{
            .jobToVm = {},
            .desiredSlotMap = {},
            .updatedIdleCapacities = {},
            .vmsToTerminate = {3, 5},
        };

    EXPECT_EQ(stateChange, expectedStateChange);
    t::checkStateConstrains(initialState, stateChange);
}

TEST(BFDVmAssigner, emptyVms)
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
    const auto possibleSlots = t::getPossibleSlots();
    BFDVmAssigner vmAssigner(initialState, possibleSlots);
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
                                .cpu = 8_cores,
                                .ram = 4096_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 5_cores,
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

TEST(BFDVmAssigner, assignTest2)
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
                    .id = 2,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 1024_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 3,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 6_cores,
                            .ram = 1024_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 4,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 2_cores,
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
                            .cpu = 3_cores,
                            .ram = 1024_MB,
                        },
                },
                ActiveVm{
                    .id = 6,
                    .totalCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 2048_MB,
                        },
                    .idleCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 1024_MB,
                        },
                },
            },
    };
    const auto possibleSlots = t::getPossibleSlots();
    BFDVmAssigner vmAssigner(initialState, possibleSlots);
    const auto stateChange = vmAssigner.assign();

    const auto expectedStateChange = StateChange{
        .jobToVm =
            {
                {
                    4,
                    5u,
                },
                {
                    2,
                    6u,
                },
                {
                    0,
                    DesiredSlotId(0),
                },
                {
                    3,
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
                                .cpu = 16_cores,
                                .ram = 8192_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 8_cores,
                                .ram = 3072_MB,
                            },
                    },
                },
            },
        .updatedIdleCapacities = {
            {
                5,
                {
                    .cpu = 1_cores,
                    .ram = 0_MB,
                }
            },
            {
                6,
                {
                    .cpu = 0_cores,
                    .ram = 0_MB,
                }
            }
        },
        .vmsToTerminate = {},
    };

    EXPECT_EQ(stateChange, expectedStateChange);
    t::checkStateConstrains(initialState, stateChange);
}


TEST(BFDVmAssigner, assignTest3)
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
                    .id = 2,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 1024_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 3,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 6_cores,
                            .ram = 1024_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 4,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 2_cores,
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
                            .cpu = 3_cores,
                            .ram = 1024_MB,
                        },
                },
                ActiveVm{
                    .id = 6,
                    .totalCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 2048_MB,
                        },
                    .idleCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 1024_MB,
                        },
                },
                ActiveVm{
                    .id = 7,
                    .totalCapacity =
                        SlotCapacity{
                            .cpu = 8_cores,
                            .ram = 4096_MB,
                        },
                    .idleCapacity =
                        SlotCapacity{
                            .cpu = 7_cores,
                            .ram = 3584_MB,
                        },
                },
            },
    };
    const auto possibleSlots = t::getPossibleSlots();
    BFDVmAssigner vmAssigner(initialState, possibleSlots);
    const auto stateChange = vmAssigner.assign();

    const auto expectedStateChange = StateChange{
        .jobToVm =
            {
                {
                    4,
                    5u,
                },
                {
                    2,
                    6u,
                },
                {
                    0,
                    DesiredSlotId(0),
                },
                {
                    3,
                    7u,
                },
            },
        .desiredSlotMap =
            {
                {
                    DesiredSlotId(0),
                    DesiredSlot{
                        .total =
                            SlotCapacity{
                                .cpu = 8_cores,
                                .ram = 4096_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 6_cores,
                                .ram = 0_MB,
                            },
                    },
                },
            },
        .updatedIdleCapacities = {
            {
                7,
                {
                    .cpu = 1_cores,
                    .ram = 2560_MB,
                }
            },
            {
                5,
                {
                    .cpu = 1_cores,
                    .ram = 0_MB,
                }
            },
            {
                6,
                {
                    .cpu = 0_cores,
                    .ram = 0_MB,
                }
            },
        },
        .vmsToTerminate = {},
    };

    EXPECT_EQ(stateChange, expectedStateChange);
    t::checkStateConstrains(initialState, stateChange);
}


TEST(BFDVmAssigner, assignTest4)
{
    const auto initialState = State{
        .queuedJobs =
            {
                QueuedJobInfo{
                    .id = 0,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 50_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 1,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 1000_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 2,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 2_cores,
                            .ram = 2000_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 3,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 9_cores,
                            .ram = 1024_MB,
                        },
                },
            },
        .vms = {},
    };
    const auto possibleSlots = t::getPossibleSlots();
    BFDVmAssigner vmAssigner(initialState, possibleSlots);
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
                    DesiredSlotId(0),
                },
                {
                    2,
                    DesiredSlotId(0),
                },
                {
                    3,
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
                                .cpu = 16_cores,
                                .ram = 8192_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 3_cores,
                                .ram = 4118_MB,
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


TEST(BFDVmAssigner, assignTest5)
{
    const auto initialState = State{
        .queuedJobs =
            {
                QueuedJobInfo{
                    .id = 0,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 50_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 1,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 1_cores,
                            .ram = 100_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 2,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 2_cores,
                            .ram = 1000_MB,
                        },
                },
                QueuedJobInfo{
                    .id = 3,
                    .requiredCapacity =
                        SlotCapacity{
                            .cpu = 17_cores,
                            .ram = 1024_MB,
                        },
                },
            },
        .vms = {},
    };
    const auto possibleSlots = t::getPossibleSlots();
    BFDVmAssigner vmAssigner(initialState, possibleSlots);
    const auto stateChange = vmAssigner.assign();

    const auto expectedStateChange = StateChange{
        .jobToVm =
            {
                {
                    0,
                    DesiredSlotId(1),
                },
                {
                    1,
                    DesiredSlotId(1),
                },
                {
                    2,
                    DesiredSlotId(1),
                },
                {
                    3,
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
                                .cpu = 16_cores,
                                .ram = 8192_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 0_cores,
                                .ram = 7168_MB,
                            },
                    },
                },
                {
                    DesiredSlotId(1),
                    DesiredSlot{
                        .total =
                            SlotCapacity{
                                .cpu = 4_cores,
                                .ram = 2048_MB,
                            },
                        .idle =
                            SlotCapacity{
                                .cpu = 0_cores,
                                .ram = 898_MB,
                            },
                    },
                },
            },
        .updatedIdleCapacities = {},
        .vmsToTerminate = {},
    };

    EXPECT_EQ(stateChange, expectedStateChange);
}
