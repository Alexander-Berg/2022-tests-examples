#include "maps/b2bgeo/vm_scheduler/server/libs/allocator/include/allocator.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/allocator/include/cloud_client_mock.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/libs/common/include/errors.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;


TEST(Allocate, allocate)
{
    t::TaskStorageMock taskStorageMock;
    auto cloudClientMock = std::make_unique<t::CloudClientMock>();

    const auto allocatedVmInfo = AllocatedVmInfo{
        .id = "cloud vm id",
        .type = "cloud vm type",
    };
    EXPECT_CALL(*cloudClientMock, allocate(_, _, _)).WillOnce(
        Return(Result{allocatedVmInfo}));
    const std::vector<AllocationPendingVmInfo> vmsToAllocate = {
        {
            .id = 5,
            .capacity = {
                .cpu = 1_cores,
                .ram = 1_MB,
            }
        }
    };
    EXPECT_CALL(taskStorageMock, getVmsToAllocate(Eq(1u))).WillOnce(Return(Result{vmsToAllocate}));
    EXPECT_CALL(taskStorageMock, saveVmAvailabilityZone(Eq(5u), _)).WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(taskStorageMock, saveVmAllocationResult(Eq(5u), Eq(allocatedVmInfo))).WillOnce(
        Return(Result<void>::Success()));

    Allocator allocator(&taskStorageMock, std::move(cloudClientMock));
    allocator.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));
    allocator.allocate();

    EXPECT_TRUE(TUnistat::Instance().GetSignalValueUnsafe("allocator_allocate_cycle_time")->HasNumber());
    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("allocator_allocated")->GetNumber(), 1);
}

TEST(Allocate, allocationFailedOnStart)
{
    t::TaskStorageMock taskStorageMock;
    auto cloudClientMock = std::make_unique<t::CloudClientMock>();

    EXPECT_CALL(taskStorageMock, getVmsToAllocate(Eq(1u))).WillOnce(
        Return(Result<std::vector<AllocationPendingVmInfo>>::Failure<RuntimeException>(
        "Unexpected exception while getting pending allocation vms")));
    EXPECT_CALL(taskStorageMock, saveVmAllocationResult(_, _)).Times(0);
    EXPECT_CALL(taskStorageMock, rollbackUnallocatedVmsState(_, _, _)).Times(0);
    EXPECT_CALL(*cloudClientMock, allocate(_, _, _)).Times(0);

    Allocator allocator(&taskStorageMock, std::move(cloudClientMock));
    allocator.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));
    allocator.allocate();

    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("allocator_unallocated")->GetNumber(), 0);
}

TEST(Allocate, allocationFailed)
{
    t::TaskStorageMock taskStorageMock;
    auto cloudClientMock = std::make_unique<t::CloudClientMock>();

    const std::vector<AllocationPendingVmInfo> vmsToAllocate = {
        {
            .id = 5,
            .capacity = {
                .cpu = 1_cores,
                .ram = 1_MB,
            }
        }
    };
    EXPECT_CALL(taskStorageMock, getVmsToAllocate(Eq(1u))).WillOnce(Return(Result{vmsToAllocate}));
    EXPECT_CALL(taskStorageMock, saveVmAvailabilityZone(Eq(5u), _)).WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(*cloudClientMock, allocate(_, _, _)).WillOnce(
        Return(Result<AllocatedVmInfo>::Failure<RuntimeException>(
        "Unexpected exception while requesting a VM")));
    EXPECT_CALL(taskStorageMock, rollbackUnallocatedVmsState(Eq(std::vector<VmId>{5}), _, _)).WillOnce(
        Return(Result<void>::Success()));
    EXPECT_CALL(taskStorageMock, saveVmAllocationResult(_, _)).Times(0);

    Allocator allocator(&taskStorageMock, std::move(cloudClientMock));
    allocator.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));
    allocator.allocate();

    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("allocator_unallocated")->GetNumber(), 1);
}

TEST(Terminate, terminate)
{
    t::TaskStorageMock taskStorageMock;
    auto cloudClientMock = std::make_unique<t::CloudClientMock>();

    const std::vector<TerminationPendingVmInfo> vmsToTerminate = {
        {
            .id = 5,
            .cloudVmId = "cloud vm id",
        }
    };
    EXPECT_CALL(taskStorageMock, getVmsToTerminate(Eq(1u))).WillOnce(Return(Result{vmsToTerminate}));
    EXPECT_CALL(taskStorageMock, saveVmTerminationResult(Eq(5u))).WillOnce(
        Return(Result<void>::Success()));
    EXPECT_CALL(*cloudClientMock, terminate(Eq(vmsToTerminate[0].cloudVmId))).WillOnce(
            Return(Result<void>::Success()));

    Allocator allocator(&taskStorageMock, std::move(cloudClientMock));
    allocator.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));
    allocator.terminate();

    EXPECT_TRUE(TUnistat::Instance().GetSignalValueUnsafe("allocator_terminate_cycle_time")->HasNumber());
    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("allocator_terminated")->GetNumber(), 1);
}

TEST(Terminate, terminationFailedOnStart)
{
    t::TaskStorageMock taskStorageMock;
    auto cloudClientMock = std::make_unique<t::CloudClientMock>();

    EXPECT_CALL(taskStorageMock, getVmsToTerminate(Eq(1u))).WillOnce(
        Return(Result<std::vector<TerminationPendingVmInfo>>::Failure<RuntimeException>(
        "Unexpected exception while getting pending termination vms")));
    EXPECT_CALL(taskStorageMock, rollbackUnterminatedVmsState(_)).Times(0);
    EXPECT_CALL(*cloudClientMock, terminate(_)).Times(0);

    Allocator allocator(&taskStorageMock, std::move(cloudClientMock));
    allocator.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));
    allocator.terminate();

    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("allocator_unterminated")->GetNumber(), 0);
}

TEST(Terminate, terminationFailed)
{
    t::TaskStorageMock taskStorageMock;
    auto cloudClientMock = std::make_unique<t::CloudClientMock>();

    const std::vector<TerminationPendingVmInfo> vmsToTerminate = {
        {
            .id = 5,
            .cloudVmId = "cloud vm id",
        }
    };
    EXPECT_CALL(taskStorageMock, getVmsToTerminate(Eq(1u))).WillOnce(Return(Result{vmsToTerminate}));
    EXPECT_CALL(*cloudClientMock, terminate(Eq(vmsToTerminate[0].cloudVmId))).WillOnce(
        Return(Result<void>::Failure<RuntimeException>(
        "Unexpected exception while VM termination")));

    EXPECT_CALL(taskStorageMock, rollbackUnterminatedVmsState(Eq(std::vector<VmId>{5}))).WillOnce(
        Return(Result<void>::Success()));

    Allocator allocator(&taskStorageMock, std::move(cloudClientMock));
    allocator.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));
    allocator.terminate();

    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("allocator_unterminated")->GetNumber(), 1);
}
