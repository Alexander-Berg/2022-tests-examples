#include "maps/b2bgeo/vm_scheduler/server/libs/server/include/grpc_server.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/task.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;


TEST(GrpcCancelTask, simple)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, cancelTask).WillOnce(Return(Result<void>::Success()));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::PublicApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::TaskId taskId;
    taskId.set_value(123);
    google::protobuf::Empty empty;

    grpc::Status status = stub->cancelRunningJobs(&context, taskId, &empty);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::OK);
}
