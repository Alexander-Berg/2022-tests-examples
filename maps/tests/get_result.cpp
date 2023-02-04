#include "maps/b2bgeo/vm_scheduler/server/libs/server/include/grpc_server.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/task.h>

#include <gmock/gmock.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;

TEST(GrpcGetResult, simple)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, getJobStates).WillOnce(Return(
        Result{JobStates{
            {
                .status = JobStatus::Scheduled,
                .resultUrl = std::nullopt,
            },
            {
                .status = JobStatus::Running,
                .resultUrl = std::nullopt,
            }
        }}
    ));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::PublicApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::TaskId taskId;
    taskId.set_value(123);
    proto::TaskExecutionResult taskResult;
    grpc::Status status = stub->getTaskResult(&context, taskId, &taskResult);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::OK);
    EXPECT_EQ(taskResult.status(), proto::TaskStatus::TASK_RUNNING);
    EXPECT_EQ(taskResult.job_results().size(), 2);
    EXPECT_FALSE(taskResult.job_results()[0].has_result_url());
    EXPECT_EQ(taskResult.job_results()[0].status(), proto::JobStatus::JOB_QUEUED);
    EXPECT_FALSE(taskResult.job_results()[1].has_result_url());
    EXPECT_EQ(taskResult.job_results()[1].status(), proto::JobStatus::JOB_RUNNING);
}

TEST(GrpcGetResult, taskResultNoJobs)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, getJobStates).WillOnce(Return(
        Result{JobStates{}}));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::PublicApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::TaskId taskId;
    taskId.set_value(123);
    proto::TaskExecutionResult taskResult;
    grpc::Status status = stub->getTaskResult(&context, taskId, &taskResult);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::NOT_FOUND);
}
