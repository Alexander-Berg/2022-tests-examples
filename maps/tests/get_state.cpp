#include "maps/b2bgeo/vm_scheduler/server/libs/server/include/grpc_server.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/task.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;

TEST(GrpcGetTaskState, simple)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, getJobStates).WillOnce(Return(
        Result{JobStates{
            {
                .status = JobStatus::Error,
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
    proto::TaskState taskState;
    grpc::Status status = stub->getTaskState(&context, taskId, &taskState);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::OK);
    EXPECT_EQ(taskState.status(), proto::TaskStatus::TASK_RUNNING);
    EXPECT_EQ(taskState.job_states().size(), 2);
    EXPECT_EQ(taskState.job_states()[0].status(), proto::JobStatus::JOB_ERROR);
    EXPECT_EQ(taskState.job_states()[1].status(), proto::JobStatus::JOB_RUNNING);
}

TEST(GrpcGetTaskState, taskStateNoJobs)
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
    proto::TaskState taskState;
    grpc::Status status = stub->getTaskState(&context, taskId, &taskState);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::NOT_FOUND);
}
