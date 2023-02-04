#include "maps/b2bgeo/vm_scheduler/server/libs/task_registry/include/task_registry.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/allocator/include/cloud_client_mock.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <memory>
#include <thread>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;
using namespace std::chrono_literals;


TEST(TaskRegistry, simple)
{
    const auto config = createConfig();

    auto taskStorageMock = std::make_unique<t::TaskStorageMock>();
    auto cloudClientMock = std::make_unique<t::CloudClientMock>();

    EXPECT_CALL(*taskStorageMock, startScheduling(_, _))
        .WillOnce(::testing::Return(Result{PlanId{456}}));
    EXPECT_CALL(*taskStorageMock, getCurrentState())
        .WillOnce(::testing::Return(Result{State{}}));
    EXPECT_CALL(*taskStorageMock, commitPlanChange(_, _))
        .WillOnce(::testing::Return(Result<void>::Success()));
    EXPECT_CALL(*taskStorageMock, getVmsToAllocate(_))
        .WillOnce(Return(Result{std::vector<AllocationPendingVmInfo>{}}));
    EXPECT_CALL(*taskStorageMock, getVmsToTerminate(_))
        .WillOnce(Return(Result{std::vector<TerminationPendingVmInfo>{}}));
    EXPECT_CALL(*taskStorageMock, restartStaleAllocatingVms(_, _))
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(*taskStorageMock, terminateStaleAllocatingVms(_, _, _))
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(*taskStorageMock, terminateVmsWithInactiveAgents(_, _))
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(*taskStorageMock, terminateVmsWithoutAgents(_, _))
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(*taskStorageMock, cancelTimedOutJobs())
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(*taskStorageMock, getAllocatedVms())
        .WillOnce(Return(Result{AllocatedVmInfos{}}));
    EXPECT_CALL(*cloudClientMock, getAllAllocatedVms())
        .WillOnce(Return(Result{AllocatedVmInfos{}}));
    EXPECT_CALL(*cloudClientMock, getPossibleSlots())
        .WillOnce(Return(std::vector<SlotCapacity>{
            SlotCapacity{
                .cpu = 1_cores,
                .ram = 1024_MB,
            },
            SlotCapacity{
                .cpu = 2_cores,
                .ram = 2048_MB,
            }
    }));

    TaskRegistry taskRegistry(config, std::move(taskStorageMock), std::move(cloudClientMock));
    taskRegistry.start();
    std::this_thread::sleep_for(1s);
}


TEST(TaskRegistry, unistat)
{
    const auto config = createConfig();

    auto taskStorageMock = std::make_unique<t::TaskStorageMock>();
    auto cloudClientMock = std::make_unique<t::CloudClientMock>();
    EXPECT_CALL(*cloudClientMock, getPossibleSlots())
        .WillOnce(Return(std::vector<SlotCapacity>{
            SlotCapacity{
                .cpu = 1_cores,
                .ram = 1024_MB,
            },
            SlotCapacity{
                .cpu = 2_cores,
                .ram = 2048_MB,
            }
    }));

    EXPECT_CALL(*taskStorageMock, getJobsForStats())
        .WillOnce(Return(Result{std::vector<JobForStats>{
            JobForStats{
                .taskId = 1,
                .status = JobStatus::Queued,
                .vmId = 0,
            },
            JobForStats{
                .taskId = 1,
                .status = JobStatus::Running,
                .vmId = 1,
            },
    }}));
    EXPECT_CALL(*taskStorageMock, getVmsForStats())
        .WillOnce(Return(Result{std::vector<VmForStats>{
            VmForStats{
                .status = VmStatus::Allocating,
                .lifetimeSeconds = 100,
                .totalCapacity = SlotCapacity{
                    .cpu = 1_cores,
                    .ram = 1024_MB,
                },
                .idleCapacity = SlotCapacity{
                    .cpu = 1_cores,
                    .ram = 1024_MB,
                },
            },
            VmForStats{
                .status = VmStatus::Allocated,
                .lifetimeSeconds = 300,
                .totalCapacity = SlotCapacity{
                    .cpu = 1_cores,
                    .ram = 1024_MB,
                },
                .idleCapacity = SlotCapacity{
                    .cpu = 0_cores,
                    .ram = 0_MB,
                },
            },
    }}));
    TaskRegistry taskRegistry(config, std::move(taskStorageMock), std::move(cloudClientMock));
    taskRegistry.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));

    taskRegistry.pushUnistatSignals();
    const std::unordered_map<std::string, int> expected = {
        {"queued_jobs", 1},
        {"running_jobs", 1},
        {"allocating_vms", 1},
        {"allocated_vms", 1},
        {"allocated_cpu", 2},
        {"allocated_ram", 2048},
        {"idle_cpu", 1},
        {"idle_ram", 1024},
        {"tasks_in_progress", 1},
        {"vms_lifetime_perc_50", 100},
        {"vms_lifetime_perc_75", 100},
        {"vms_lifetime_perc_90", 100},
        {"vms_lifetime_perc_99", 100},
        {"vms_lifetime_perc_100", 300},
        {"jobs_per_vm_perc_50", 1},
        {"jobs_per_vm_perc_75", 1},
        {"jobs_per_vm_perc_90", 1},
        {"jobs_per_vm_perc_95", 1},
        {"jobs_per_vm_perc_100", 1},
    };
    for (const auto& [key, expectedValue] : expected) {
        const auto value = TUnistat::Instance().GetSignalValueUnsafe(key.c_str())->GetNumber();
        EXPECT_EQ(value, expectedValue) << "Key '" << key << "', expected " << expectedValue << ", got " << value;
    }
}
