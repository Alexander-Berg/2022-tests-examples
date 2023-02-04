#include "maps/b2bgeo/vm_scheduler/server/libs/server/include/grpc_server.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/server/impl/test_utils.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage_mock.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/task.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/errors.h>

#include <library/cpp/testing/gtest/gtest.h>


using namespace maps::b2bgeo::vm_scheduler;
namespace t = maps::b2bgeo::vm_scheduler::testing;
using namespace ::testing;

TEST(GrpcGetAssignedJob, simple)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, getAssignedJobs).WillOnce(Return(
        Result<std::vector<AssignedJob>>{{
            {
                .id = 123,
                .status = JobStatus::Scheduled
            },
            {
                .id = 456,
                .status = JobStatus::Cancelled
            },
        }}));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::AgentApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::VmId vmId;
    vmId.set_value(42);
    proto::AssignedJobs assignedJobs;

    grpc::Status status = stub->getAssignedJobs(&context, vmId, &assignedJobs);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::OK);
    EXPECT_EQ(assignedJobs.jobs().size(), 2);
    EXPECT_EQ(assignedJobs.jobs()[0].id().value(), 123u);
    EXPECT_EQ(assignedJobs.jobs()[0].status(), proto::JobStatus::JOB_QUEUED);
    EXPECT_EQ(assignedJobs.jobs()[1].id().value(), 456u);
    EXPECT_EQ(assignedJobs.jobs()[1].status(), proto::JobStatus::JOB_CANCELLED);
}
