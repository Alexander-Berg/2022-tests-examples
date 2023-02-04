#include <maps/b2bgeo/vm_scheduler/agent/libs/job_runner/include/job_runner.h>

#include <maps/b2bgeo/vm_scheduler/agent/libs/docker_client/include/docker_client_mock.h>
#include <maps/b2bgeo/vm_scheduler/agent/libs/result_saver/include/result_saver_mock.h>
#include <maps/b2bgeo/vm_scheduler/agent/libs/vm_scheduler_client/include/vm_scheduler_client_mock.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <exception>
#include <filesystem>
#include <fstream>

using namespace maps::b2bgeo::vm_scheduler;
using namespace maps::b2bgeo::vm_scheduler::agent;
namespace t = maps::b2bgeo::vm_scheduler::agent::testing;
using namespace ::testing;

namespace {

const std::filesystem::path JOB_FOLDER =
    std::filesystem::current_path() / "jobs";
const std::filesystem::path SETTINGS_FILE = "settings.json";
const std::filesystem::path JOB_OPTIONS_FILE = "job_options.json";

RunnerConfig createTestConfig()
{
    return {.jobsPath = JOB_FOLDER};
}

void clearTestData()
{
    std::filesystem::remove_all(JOB_FOLDER);
}

std::string readFile(const std::filesystem::path& path)
{
    auto file = std::ifstream(path);
    if (!file) {
        throw std::runtime_error("File " + path.string() + " not found");
    }
    std::stringstream buffer;
    buffer << file.rdbuf();
    return buffer.str();
}

} // namespace

TEST(JobRunner, simple)
{
    JobStorage storage;
    t::VmSchedulerClientMock schedulerMock;
    t::DockerClientMock dockerMock;
    t::ResultSaverMock saverMock;
    JobRunner runner(
        createTestConfig(), storage, schedulerMock, dockerMock, saverMock);

    std::vector<AgentAssignedJob> jobs{
        {.id = 42, .status = AgentJobStatus::Queued}};
    storage.update(jobs);

    JobToLaunch opt{
        .id = 42,
        .capacityLimits = {1_cores, 1024_MB},
        .taskSettings = "task42",
        .imageVersion = "image42",
        .jobOptions = "job42",
    };
    EXPECT_CALL(schedulerMock, getJobToLaunch(42))
        .WillOnce(Return(Result<JobToLaunch>{opt}));

    RunContainerOptions containerOptions;
    const ContainerId containerId{"ctr-id-42"};
    InSequence seq;

    EXPECT_CALL(dockerMock, run(_))
        .WillOnce(
            DoAll(SaveArg<0>(&containerOptions), Return(Result{containerId})));

    runner.execute();

    EXPECT_EQ(containerOptions.mounts.size(), 1u);
    EXPECT_TRUE(containerOptions.mounts[0].hostPath.rfind(JOB_FOLDER, 0) == 0);
    const auto taskPath = containerOptions.mounts[0].hostPath;

    EXPECT_EQ(readFile(taskPath / SETTINGS_FILE), opt.taskSettings);
    EXPECT_EQ(readFile(taskPath / JOB_OPTIONS_FILE), opt.jobOptions);
    EXPECT_EQ(containerOptions.image, opt.imageVersion);

    auto updatedJobs = storage.getUpdatedJobs();
    EXPECT_EQ(updatedJobs.size(), 1u);
    EXPECT_EQ(updatedJobs[0].first, 42u);
    EXPECT_EQ(updatedJobs[0].second.status, AgentJobStatus::Running);
    EXPECT_FALSE(updatedJobs[0].second.resultUrl.has_value());

    // Check running jobs
    const auto runningState = ContainerState{
        .status = ContainerState::Status::Running, .errorText = {}};
    EXPECT_CALL(dockerMock, getState(containerId))
        .Times(1)
        .WillOnce(Return(Result{runningState}));

    runner.execute();
    updatedJobs = storage.getUpdatedJobs();
    EXPECT_EQ(updatedJobs.size(), 1u);
    EXPECT_EQ(updatedJobs[0].first, 42u);
    EXPECT_EQ(updatedJobs[0].second.status, AgentJobStatus::Running);
    EXPECT_FALSE(updatedJobs[0].second.resultUrl.has_value());

    // Job completed
    const auto completedState = ContainerState{
        .status = ContainerState::Status::Completed, .errorText = {}};
    EXPECT_CALL(dockerMock, getState(containerId))
        .Times(1)
        .WillOnce(Return(Result{completedState}));
    std::filesystem::path resultPath;
    const std::string resultUrl = "http://result-url.com";
    EXPECT_CALL(saverMock, save(_))
        .WillOnce(DoAll(SaveArg<0>(&resultPath), Return(Result{resultUrl})));
    EXPECT_CALL(dockerMock, remove(containerId))
        .WillOnce(Return(Result<void>::Success()));

    runner.execute();
    updatedJobs = storage.getUpdatedJobs();
    EXPECT_EQ(updatedJobs.size(), 1u);
    EXPECT_EQ(updatedJobs[0].first, 42u);
    EXPECT_EQ(updatedJobs[0].second.status, AgentJobStatus::Completed);
    EXPECT_TRUE(updatedJobs[0].second.resultUrl.has_value());
    EXPECT_TRUE(resultPath.string().rfind(JOB_FOLDER, 0) == 0);
    EXPECT_EQ(updatedJobs[0].second.resultUrl.value(), resultUrl);

    // Job deleted after completion
    storage.update({});

    runner.execute();
    EXPECT_TRUE(std::filesystem::is_empty(JOB_FOLDER));
    EXPECT_TRUE(storage.getUpdatedJobs().empty());

    clearTestData();
}

TEST(JobRunner, cancelledJob)
{
    JobStorage storage;
    t::VmSchedulerClientMock schedulerMock;
    t::DockerClientMock dockerMock;
    t::ResultSaverMock saverMock;
    JobRunner runner(
        createTestConfig(), storage, schedulerMock, dockerMock, saverMock);

    std::vector<AgentAssignedJob> jobs{
        {.id = 42, .status = AgentJobStatus::Queued}};
    storage.update(jobs);

    JobToLaunch opt{
        .id = 42,
        .capacityLimits = {1_cores, 1024_MB},
        .taskSettings = "task42",
        .imageVersion = "image42",
        .jobOptions = "job42",
    };
    EXPECT_CALL(schedulerMock, getJobToLaunch(42))
        .WillOnce(Return(Result<JobToLaunch>{opt}));

    RunContainerOptions containerOptions;
    const ContainerId containerId{"ctr-id-42"};

    EXPECT_CALL(dockerMock, run(_))
        .WillOnce(
            DoAll(SaveArg<0>(&containerOptions), Return(Result{containerId})));

    runner.execute();
    storage.update({});

    EXPECT_CALL(dockerMock, remove(containerId))
        .WillOnce(Return(Result<void>::Success()));
    runner.execute();

    const auto updatedJobs = storage.getUpdatedJobs();
    EXPECT_TRUE(updatedJobs.empty());
    clearTestData();
}

TEST(JobRunner, failedJob)
{
    JobStorage storage;
    t::VmSchedulerClientMock schedulerMock;
    t::DockerClientMock dockerMock;
    t::ResultSaverMock saverMock;
    JobRunner runner(
        createTestConfig(), storage, schedulerMock, dockerMock, saverMock);

    std::vector<AgentAssignedJob> jobs{
        {.id = 42, .status = AgentJobStatus::Queued}};
    storage.update(jobs);

    JobToLaunch opt{
        .id = 42,
        .capacityLimits = {1_cores, 1024_MB},
        .taskSettings = "task42",
        .imageVersion = "image42",
        .jobOptions = "job42",
    };
    EXPECT_CALL(schedulerMock, getJobToLaunch(42))
        .WillOnce(Return(Result<JobToLaunch>{opt}));

    RunContainerOptions containerOptions;
    const ContainerId containerId{"ctr-id-42"};

    EXPECT_CALL(dockerMock, run(_))
        .WillOnce(
            DoAll(SaveArg<0>(&containerOptions), Return(Result{containerId})));

    runner.execute();

    const auto failedState = ContainerState{
        .status = ContainerState::Status::Failed, .errorText = "error desc"};
    EXPECT_CALL(dockerMock, getState(containerId))
        .WillRepeatedly(Return(Result{failedState}));
    EXPECT_CALL(dockerMock, remove(containerId))
        .WillOnce(Return(Result<void>::Success()));

    runner.execute();

    const auto updatedJobs = storage.getUpdatedJobs();
    EXPECT_EQ(updatedJobs.size(), 1u);
    EXPECT_EQ(updatedJobs[0].first, 42u);
    EXPECT_EQ(updatedJobs[0].second.status, AgentJobStatus::Error);
    EXPECT_FALSE(updatedJobs[0].second.resultUrl.has_value());

    // Check that container is not deleted twice
    runner.execute();
    clearTestData();
}
