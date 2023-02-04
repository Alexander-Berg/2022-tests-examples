#include "maps/b2bgeo/vm_scheduler/server/libs/server/include/grpc_server.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/server/impl/test_utils.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/task.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/errors.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;

namespace {
    proto::ExecutionJobState getExecutionJobState(
        proto::JobStatus status, const std::optional<std::string>& resultUrl)
    {
        proto::ExecutionJobState executionJobState;
        executionJobState.mutable_vm_id()->set_value(42);
        executionJobState.mutable_job_id()->set_value(55);
        executionJobState.mutable_job_result()->set_status(status);
        if (resultUrl) {
            executionJobState.mutable_job_result()->set_result_url(resultUrl->c_str());
        }
        return executionJobState;
    }
} // anonymous namespace

TEST(GrpcUpdateJobState, simple)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);
    server.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));

    EXPECT_CALL(taskStorageMock, updateJobState).WillOnce(Return(
        Result<void>::Success()));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::AgentApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::ExecutionJobState executionJobState = getExecutionJobState(
        proto::JobStatus::JOB_COMPLETED, std::nullopt);
    google::protobuf::Empty empty;

    grpc::Status status = stub->updateJobState(&context, executionJobState, &empty);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::OK);

    EXPECT_EQ(TUnistat::Instance().GetSignalValueUnsafe("completed_jobs")->GetNumber(), 1);
}

TEST(GrpcUpdateJobState, jobNotFound)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, updateJobState).WillOnce(Return(
        Result<void>::Failure<JobNotFoundException>("job not found")));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::AgentApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::ExecutionJobState executionJobState = getExecutionJobState(
        proto::JobStatus::JOB_RUNNING, std::nullopt);
    google::protobuf::Empty empty;

    grpc::Status status = stub->updateJobState(&context, executionJobState, &empty);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::NOT_FOUND);
}

TEST(GrpcUpdateJobState, jobCancelled)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, updateJobState).WillOnce(Return(
        Result<void>::Failure<JobCancelledException>("job cancelled")));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::AgentApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::ExecutionJobState executionJobState = getExecutionJobState(
        proto::JobStatus::JOB_RUNNING, std::nullopt);
    google::protobuf::Empty empty;

    grpc::Status status = stub->updateJobState(&context, executionJobState, &empty);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::CANCELLED);
}

TEST(GrpcUpdateJobState, jobInvalidStatus)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::AgentApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::ExecutionJobState executionJobState = getExecutionJobState(
        proto::JobStatus::JOB_QUEUED, std::nullopt);
    google::protobuf::Empty empty;

    grpc::Status status = stub->updateJobState(&context, executionJobState, &empty);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::INVALID_ARGUMENT);
}
