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
    proto::LaunchRequest getLaunchRequest()
    {
        proto::LaunchRequest launchRequest;
        launchRequest.mutable_vm_id()->set_value(42);
        launchRequest.mutable_job_id()->set_value(55);
        return launchRequest;
    }
} // anonymous namespace

TEST(GrpcGetJobToLaunch, simple)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, getJobToLaunch).WillOnce(Return(
        Result<JobToLaunch>{
            JobToLaunch{
                .id = 13,
                .capacityLimits = {
                    .cpu = 1_cores,
                    .ram = 2_MB,
                },
                .taskSettings = "{}",
                .imageVersion = "last version",
                .jobOptions = "[]",
            }
        }));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::AgentApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::LaunchRequest launchRequest = getLaunchRequest();
    proto::JobToLaunch jobToLaunch;

    grpc::Status status = stub->getJobToLaunch(&context, launchRequest, &jobToLaunch);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::OK);
    EXPECT_EQ(jobToLaunch.id().value(), 13u);
    EXPECT_EQ(jobToLaunch.job_limits().cpu_cores(), 1u);
    EXPECT_EQ(jobToLaunch.job_limits().memory_mb(), 2u);
    EXPECT_EQ(jobToLaunch.task_settings(), "{}");
    EXPECT_EQ(jobToLaunch.image_version(), "last version");
    EXPECT_EQ(jobToLaunch.job_options(), "[]");
}

TEST(GrpcGetJobToLaunch, jobNotFound)
{
    const auto config = createServerConfig();
    t::TaskStorageMock taskStorageMock;
    GrpcServer server(config, &taskStorageMock);

    EXPECT_CALL(taskStorageMock, getJobToLaunch).WillOnce(Return(
        Result<JobToLaunch>::Failure<JobNotFoundException>("job not found")));

    auto channel = grpc::CreateChannel(config.address.c_str(), grpc::InsecureChannelCredentials());
    auto stub = proto::AgentApiScheduler::NewStub(channel);

    grpc::ClientContext context;
    proto::LaunchRequest launchRequest = getLaunchRequest();
    proto::JobToLaunch jobToLaunch;

    grpc::Status status = stub->getJobToLaunch(&context, launchRequest, &jobToLaunch);
    EXPECT_EQ(status.error_code(), grpc::StatusCode::NOT_FOUND);
}
