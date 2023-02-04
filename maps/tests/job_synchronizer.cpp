#include <maps/b2bgeo/vm_scheduler/agent/libs/job_synchronizer/include/job_synchronizer.h>
#include <maps/b2bgeo/vm_scheduler/agent/libs/vm_scheduler_client/include/vm_scheduler_client_mock.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace maps::b2bgeo::vm_scheduler;
using namespace maps::b2bgeo::vm_scheduler::agent;
namespace t = maps::b2bgeo::vm_scheduler::agent::testing;
using namespace ::testing;

TEST(JobSynchronizer, receiveAssignedJobs)
{
    JobStorage storage;
    t::VmSchedulerClientMock clientMock;
    JobSynchronizer sync(storage, clientMock);

    EXPECT_CALL(clientMock, getAssignedJobs)
        .WillOnce(Return(Result<std::vector<AgentAssignedJob>>{
            {{.id = 1, .status = AgentJobStatus::Queued},
             {.id = 2, .status = AgentJobStatus::Queued}}}));

    sync.execute();
    const auto receivedJobs = storage.getNewJobs();
    std::unordered_map<JobId, AgentJobStatus> jobsMap;
    for (const auto& [jobId, jobStatus]: receivedJobs) {
        jobsMap[jobId] = jobStatus;
    }
    EXPECT_EQ(jobsMap.size(), 2u);
    std::unordered_set<JobId> expected{1, 2};
    for (const auto& [jobId, jobStatus]: receivedJobs) {
        EXPECT_TRUE(expected.contains(jobId));
        EXPECT_EQ(jobStatus, AgentJobStatus::Queued);
    }
}

TEST(JobSynchronizer, deleteJobs)
{
    JobStorage storage;
    t::VmSchedulerClientMock clientMock;
    JobSynchronizer sync(storage, clientMock);

    EXPECT_CALL(clientMock, getAssignedJobs)
        .WillOnce(Return(Result<std::vector<AgentAssignedJob>>{{
            {.id = 1, .status = AgentJobStatus::Queued},
            {.id = 3, .status = AgentJobStatus::Queued},
        }}));

    storage.update({
        {.id = 1, .status = AgentJobStatus::Queued},
        {.id = 2, .status = AgentJobStatus::Queued},
        {.id = 3, .status = AgentJobStatus::Queued},
        {.id = 4, .status = AgentJobStatus::Queued},
    });
    sync.execute();

    const auto receivedJobs = storage.getNewJobs();
    EXPECT_EQ(receivedJobs.size(), 2u);
    std::unordered_set<JobId> expected{1, 3};
    for (const auto& job: receivedJobs) {
        expected.erase(job.id);
    }
    EXPECT_TRUE(expected.empty());

    const auto deletedJobs = storage.getRemovedJobs();
    EXPECT_EQ(deletedJobs.size(), 2u);
    expected = {2, 4};
    for (const auto& job: deletedJobs) {
        expected.erase(job);
    }
    EXPECT_TRUE(expected.empty());
}

TEST(JobSynchronizer, noJobs)
{
    JobStorage storage;
    t::VmSchedulerClientMock clientMock;
    JobSynchronizer sync(storage, clientMock);
    std::vector<AgentAssignedJob> jobs;

    EXPECT_CALL(clientMock, getAssignedJobs)
        .WillOnce(Return(Result<std::vector<AgentAssignedJob>>{jobs}));

    sync.execute();

    const auto receivedJobs = storage.getNewJobs();
    EXPECT_TRUE(receivedJobs.empty());
}

TEST(JobSynchronizer, sendJobState)
{
    JobStorage storage;
    t::VmSchedulerClientMock clientMock;
    JobSynchronizer sync(storage, clientMock);

    std::vector<AgentAssignedJob> assignedJobs;
    std::vector<AgentAssignedJob> jobs{
        {.id = 1, .status = AgentJobStatus::Running}};
    storage.update(jobs);
    storage.setLocalState(
        1,
        AgentJobState{
            .status = AgentJobStatus::Completed, .resultUrl = "testurl"});
    AgentJobState calledWithState;

    Result<std::vector<AgentAssignedJob>> assignedJobsResult{jobs};
    EXPECT_CALL(clientMock, getAssignedJobs)
        .WillRepeatedly(ReturnPointee(&assignedJobsResult));
    EXPECT_CALL(clientMock, updateJobState(1, _))
        .WillOnce(DoAll(
            SaveArg<1>(&calledWithState), Return(Result<void>::Success())));

    sync.execute();
    EXPECT_EQ(calledWithState.status, AgentJobStatus::Completed);
    EXPECT_EQ(calledWithState.resultUrl, "testurl");
    jobs[0].status = AgentJobStatus::Completed;
    assignedJobsResult = Result{jobs};

    // We do it twice, so test, that second call does not send status (see WillOnce exptectation on updateJobState call)
    sync.execute();
}
