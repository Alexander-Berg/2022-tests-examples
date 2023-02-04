#include "maps/b2bgeo/vm_scheduler/server/libs/failure_detector/include/failure_detector.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/allocator/include/allocator.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/allocator/include/cloud_client_mock.h>
#include <maps/b2bgeo/vm_scheduler/libs/common/include/errors.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;


TEST(FailureDetector, simple)
{
    t::TaskStorageMock taskStorageMock;
    auto cloudClientMock = std::make_unique<t::CloudClientMock>();

    EXPECT_CALL(*cloudClientMock, getAllAllocatedVms())
         .WillOnce(Return(Result{AllocatedVmInfos{}}));

    Allocator allocator(&taskStorageMock, std::move(cloudClientMock));

    FailureDetector failureDetector(&taskStorageMock, &allocator);
    failureDetector.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));

    EXPECT_CALL(taskStorageMock, restartStaleAllocatingVms(_, _))
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(taskStorageMock, terminateStaleAllocatingVms(_, _, _))
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(taskStorageMock, terminateVmsWithInactiveAgents(_, _))
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(taskStorageMock, terminateVmsWithoutAgents(_, _))
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(taskStorageMock, cancelTimedOutJobs())
        .WillOnce(Return(Result<void>::Success()));
    EXPECT_CALL(taskStorageMock, getAllocatedVms())
        .WillOnce(Return(Result{AllocatedVmInfos{}}));

    failureDetector.monitor();
    EXPECT_TRUE(TUnistat::Instance().GetSignalValueUnsafe("failure_detector_cycle_time")->HasNumber());
}
