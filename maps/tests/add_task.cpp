#include "maps/b2bgeo/vm_scheduler/server/libs/server/include/grpc_server.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/server/impl/test_utils.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/task.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;

TEST(GrpcAddTask, simple)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, addTask).WillOnce(Return(Result{CreatedJobs{12345, {}}}));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::PublicApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::TaskAdditionResult protoResult;
    const auto task = t::generateProtoTask();

    grpc::Status status = stub->addTask(&context, task, &protoResult);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::OK);
    EXPECT_EQ(protoResult.task_id().value(), 12345u);
}

TEST(GrpcAddTask, addTaskInvalidJobCount)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, addTask).Times(0);

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::PublicApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::TaskAdditionResult protoResult;
    auto task = t::generateProtoTask();
    task.set_job_count(5);

    grpc::Status status = stub->addTask(&context, task, &protoResult);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::INVALID_ARGUMENT);
    EXPECT_EQ(status.error_message(), "`job_options` size (2) must be equal to `job_count`(5).");
}

TEST(GrpcAddTask, addTaskInvalidJson)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, addTask).Times(0);

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::PublicApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::TaskAdditionResult protoResult;
    auto task = t::generateProtoTask();
    task.set_settings("invalid json");

    grpc::Status status = stub->addTask(&context, task, &protoResult);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::INVALID_ARGUMENT);
    EXPECT_EQ(status.error_message(), "Field `settings` validation failed. Invalid json at position: 0");
}
