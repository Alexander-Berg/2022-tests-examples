#include "maps/b2bgeo/vm_scheduler/agent/libs/docker_client/impl/docker_client_impl.h"

#include <maps/b2bgeo/vm_scheduler/libs/common/include/env.h>

#include <maps/libs/log8/include/log8.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <chrono>
#include <filesystem>
#include <thread>

using namespace maps::b2bgeo::vm_scheduler::agent;

namespace {

using namespace maps::b2bgeo::vm_scheduler;

const SlotCapacity CAPACITY{.cpu = 1_cores, .ram = 100_MB};

DockerClientConfig createTestDockerClientConfig()
{
    return {
        .socketPath = createDockerClientConfig().socketPath,
    };
}

} // namespace

class DockerClientCase : public ::testing::Test {
protected:
    void SetUp() override
    {
        // You can set any value to this env, 1 for example
        if (!std::getenv("VMA_ENABLE_DOCKER_TESTS")) {
            GTEST_SKIP() << "To run docker tests with maps/b2bgeo/vm_scheduler/agent/libs/docker_client,"
                         << " please, specify VMA_ENABLE_DOCKER_TESTS.";
        }
    }
};

TEST_F(DockerClientCase, nonameUbuntu)
{
    using namespace std;
    DockerClientConfig config = createTestDockerClientConfig();
    DockerClientImpl client(config);
    const auto request = RunContainerOptions{
        .image = {"ubuntu"},
        .cmd = {"echo", "test"},
        .limits = CAPACITY,
    };
    const auto containerId = client.run(request).ValueOrThrow();
    this_thread::sleep_for(2s);
    const auto state = client.getState(containerId).ValueOrThrow();
    EXPECT_EQ(state.status, ContainerState::Status::Completed);
    EXPECT_TRUE(state.errorText.empty());

    const auto deleteResult = client.remove(containerId);
    EXPECT_FALSE(deleteResult.IsFailure());
    EXPECT_TRUE(client.getState(containerId).IsFailure());
}

TEST_F(DockerClientCase, ubuntu2210)
{
    using namespace std;
    DockerClientConfig config = createTestDockerClientConfig();
    DockerClientImpl client(config);
    const auto request = RunContainerOptions{
        .name = "vm_scheduler-agent-docker_client-tests-simpleUbuntuCase",
        .image = {"ubuntu:22.10"},
        .cmd = {"echo", "test"},
        .limits = CAPACITY,
    };
    const auto containerId = client.run(request).ValueOrThrow();
    this_thread::sleep_for(2s);
    const auto state = client.getState(containerId).ValueOrThrow();
    EXPECT_EQ(state.status, ContainerState::Status::Completed);
    EXPECT_TRUE(state.errorText.empty());

    const auto deleteResult = client.remove(containerId);
    EXPECT_FALSE(deleteResult.IsFailure());
    EXPECT_TRUE(client.getState(containerId).IsFailure());
}

TEST_F(DockerClientCase, simpleUbuntuCase)
{
    using namespace std;
    DockerClientConfig config = createTestDockerClientConfig();
    DockerClientImpl client(config);
    const auto request = RunContainerOptions{
        .name = {"vm_scheduler-agent-docker_client-tests-simpleUbuntuCase"},
        .image = {"ubuntu"},
        .cmd = {"echo", "test"},
        .limits = CAPACITY,
    };
    const auto containerId = client.run(request).ValueOrThrow();
    this_thread::sleep_for(2s);
    const auto state = client.getState(containerId).ValueOrThrow();
    EXPECT_EQ(state.status, ContainerState::Status::Completed);
    EXPECT_TRUE(state.errorText.empty());

    const auto deleteResult = client.remove(containerId);
    EXPECT_FALSE(deleteResult.IsFailure());
    EXPECT_TRUE(client.getState(containerId).IsFailure());
}

TEST_F(DockerClientCase, errorUbuntuCase)
{
    using namespace std;
    DockerClientConfig config = createTestDockerClientConfig();
    DockerClientImpl client(config);
    const auto request = RunContainerOptions{
        .name = {"vm_scheduler-agent-docker_client-tests-errorUbuntuCase"},
        .image = {"ubuntu"},
        .cmd = {"bash", "-c", "\"exit 1\""},
        .limits = CAPACITY,
    };
    const auto containerId = client.run(request).ValueOrThrow();
    this_thread::sleep_for(2s);
    const auto state = client.getState(containerId).ValueOrThrow();
    EXPECT_EQ(state.status, ContainerState::Status::Failed);
    EXPECT_EQ(state.errorText, "");
    EXPECT_TRUE(client.remove(containerId).IsSuccess());
}

TEST_F(DockerClientCase, runningUbuntuCase)
{
    using namespace std;
    DockerClientConfig config = createTestDockerClientConfig();
    DockerClientImpl client(config);
    const auto request = RunContainerOptions{
        .name = {"vm_scheduler-agent-docker_client-tests-runningUbuntuCase"},
        .image = {"ubuntu"},
        .cmd = {"sleep", "5"},
        .limits = CAPACITY,
    };
    const auto containerId = client.run(request).ValueOrThrow();
    this_thread::sleep_for(2s);
    const auto runningState = client.getState(containerId).ValueOrThrow();
    EXPECT_EQ(runningState.status, ContainerState::Status::Running);
    EXPECT_EQ(runningState.errorText, "");
    this_thread::sleep_for(5s);
    const auto completeState = client.getState(containerId).ValueOrThrow();
    EXPECT_EQ(completeState.status, ContainerState::Status::Completed);
    EXPECT_EQ(runningState.errorText, "");
    EXPECT_TRUE(client.remove(containerId).IsSuccess());
}

TEST_F(DockerClientCase, mountingUbuntuCase)
{
    using namespace std;
    DockerClientConfig config = createTestDockerClientConfig();
    DockerClientImpl client(config);
    const auto local = filesystem::current_path() / "mount";
    {
        filesystem::create_directory(local);
        auto file = ofstream(local / "test.txt");
        file << "For test purposees";
    }
    const auto request = RunContainerOptions{
        .name = {"vm_scheduler-agent-docker_client-tests-mountingUbuntuCase"},
        .image = {"ubuntu"},
        .cmd = {"cat", "/test/test.txt"},
        .mounts = {{local, "/test"}},
        .limits = CAPACITY,
    };
    const auto containerId = client.run(request).ValueOrThrow();
    this_thread::sleep_for(2s);
    const auto runningState = client.getState(containerId).ValueOrThrow();
    // linux cat returns non zero code, when file not exists
    EXPECT_EQ(runningState.status, ContainerState::Status::Completed);
    EXPECT_EQ(runningState.errorText, "");
    EXPECT_TRUE(client.remove(containerId).IsSuccess());
    // check, that nothing hanging on this directory
    EXPECT_TRUE(filesystem::remove_all(local));
}

TEST_F(DockerClientCase, readMountedOutputFileUbuntuCase)
{
    using namespace std;
    DockerClientConfig config = createTestDockerClientConfig();
    DockerClientImpl client(config);
    const auto local = filesystem::current_path() / "mount2";
    filesystem::create_directory(local);
    const auto request = RunContainerOptions{
        .name = {"vm_scheduler-agent-docker_client-tests-readMountedOutputFileUbuntuCase"},
        .image = {"ubuntu"},
        .cmd = {"/bin/bash", "-c", "echo test > /test/output.txt"},
        .mounts = {{local, "/test"}},
        .limits = CAPACITY,
    };
    const auto containerId = client.run(request).ValueOrThrow();
    this_thread::sleep_for(2s);
    EXPECT_EQ(
        client.getState(containerId).ValueOrThrow().status,
        ContainerState::Status::Completed);
    EXPECT_TRUE(client.remove(containerId).IsSuccess());
    EXPECT_TRUE(filesystem::exists(local));
    EXPECT_TRUE(filesystem::exists(local / "output.txt"));
    auto file = ifstream(local / "output.txt");
    EXPECT_TRUE(file);
    std::stringstream buffer;
    buffer << file.rdbuf();
    EXPECT_EQ(buffer.str(), "test\n");
    filesystem::remove_all(local);
}

/*
To run this test DOCKER_USERNAME and DOCKER_PASSWORD env variables should be specified.
Run this:

aws configure set aws_access_key_id $(ya vault get version sec-01fcxga20786whdheyw65gba4f -o access-key-id)
aws configure set aws_secret_access_key $(ya vault get version sec-01fcxga20786whdheyw65gba4f -o secret-access-key)
aws configure set default.region us-east-2
aws configure set default.output json
DOCKER_PASSWORD=`aws ecr get-login-password --region us-east-2`

docker rmi 582601430203.dkr.ecr.us-east-2.amazonaws.com/ubuntu:22.10
DOCKER_USERNAME=AWS DOCKER_PASSWORD=$DOCKER_PASSWORD VMA_ENABLE_DOCKER_TESTS=1 ya make -t
*/
TEST_F(DockerClientCase, awsEcr)
{
    using namespace std;
    if (!getenv("DOCKER_PASSWORD")) {
        INFO() << "Test docker_client::awsEcr was not launched. "
               << "Please, specify DOCKER_PASSWORD. ()";
        return;
    }
    const auto config = DockerClientConfig{
        .socketPath = "/var/run/docker.sock",
        .getDockerCreds =
            []() {
                auto creds = DockerCreds{
                    .username = getFromEnvOrThrow("DOCKER_USERNAME"),
                    .password = getFromEnvOrThrow("DOCKER_PASSWORD"),
                };
                return Result{creds};
            },
    };
    DockerClientImpl client(config);
    const auto request = RunContainerOptions{
        .image = {"582601430203.dkr.ecr.us-east-2.amazonaws.com/ubuntu:22.10"},
        .cmd = {"echo", "test"},
        .limits = CAPACITY,
    };
    const auto containerId = client.run(request).ValueOrThrow();
    this_thread::sleep_for(2s);
    const auto state = client.getState(containerId).ValueOrThrow();
    EXPECT_EQ(state.status, ContainerState::Status::Completed);
    EXPECT_TRUE(state.errorText.empty());

    const auto deleteResult = client.remove(containerId);
    EXPECT_FALSE(deleteResult.IsFailure());
    EXPECT_TRUE(client.getState(containerId).IsFailure());
}
