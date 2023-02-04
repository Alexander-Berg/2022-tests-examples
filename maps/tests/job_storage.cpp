#include <maps/b2bgeo/vm_scheduler/agent/libs/job_storage/include/job_storage.h>

#include <library/cpp/testing/gtest/gtest.h>

using namespace maps::b2bgeo::vm_scheduler;
using namespace maps::b2bgeo::vm_scheduler::agent;

namespace {

std::vector<AgentAssignedJob> getTestAssignedJobs()
{
    return {
        {.id = 1, .status = AgentJobStatus::Queued},
        {.id = 2, .status = AgentJobStatus::Running},
        {.id = 3, .status = AgentJobStatus::Running},
    };
}

} // namespace

TEST(JobStorage, getNewJobs)
{
    JobStorage storage;
    const auto jobs = getTestAssignedJobs();
    storage.update(jobs);
    const auto newJobs = storage.getNewJobs();
    std::unordered_map<JobId, AgentJobStatus> newJobsById;
    for (const auto& job: newJobs) {
        newJobsById[job.id] = job.status;
    }
    EXPECT_EQ(newJobsById.size(), jobs.size());
    for (const auto& job: jobs) {
        EXPECT_TRUE(newJobsById.contains(job.id));
        EXPECT_EQ(job.status, newJobsById[job.id]);
    }
}

TEST(JobStorage, getRemovedJobs)
{
    JobStorage storage;
    auto jobs = getTestAssignedJobs();
    storage.update(jobs);
    jobs.pop_back();
    storage.update(jobs);
    const auto deletedJobs = storage.getRemovedJobs();
    EXPECT_EQ(deletedJobs.size(), 1u);
    EXPECT_EQ(*deletedJobs.begin(), 3u);
}

TEST(JobStorage, deleteJobs)
{
    JobStorage storage;
    auto jobs = getTestAssignedJobs();
    storage.update(jobs);
    jobs.pop_back();
    storage.update(jobs);
    auto deletedJobs = storage.getRemovedJobs();
    jobs.pop_back();
    storage.update(jobs);
    storage.deleteJobs(deletedJobs);
    deletedJobs = storage.getRemovedJobs();
    EXPECT_EQ(deletedJobs.size(), 1u);
    EXPECT_EQ(*deletedJobs.begin(), 2u);
}

TEST(JobStorage, getNewJobsAfterSetLocalState)
{
    JobStorage storage;
    const auto jobs = getTestAssignedJobs();
    storage.update(jobs);
    AgentJobState state{.status = AgentJobStatus::Completed};
    storage.setLocalState(1, state);
    const auto newJobs = storage.getNewJobs();
    std::unordered_set<JobId> newIds;
    for (const auto& [jobId, _]: newJobs) {
        newIds.insert(jobId);
    }
    EXPECT_EQ(newIds.size(), 2u);
    std::unordered_set<JobId> expected{2, 3};
    for (const auto jobId: newIds) {
        EXPECT_TRUE(expected.contains(jobId));
    }
}

TEST(JobStorage, getUpdatedJobs)
{
    JobStorage storage;
    const auto jobs = getTestAssignedJobs();
    storage.update(jobs);
    AgentJobState state{
        .status = AgentJobStatus::Completed, .resultUrl = std::nullopt};
    storage.setLocalState(1, state);
    storage.setLocalState(2, state);
    const auto updatedJobs = storage.getUpdatedJobs();
    std::unordered_map<JobId, AgentJobState> updatedJobsById;
    for (const auto& [jobId, jobState]: updatedJobs) {
        updatedJobsById[jobId] = jobState;
    }
    EXPECT_EQ(updatedJobsById.size(), 2u);
    std::unordered_set<JobId> expected{1, 2};
    for (const auto& [jobId, jobState]: updatedJobsById) {
        EXPECT_TRUE(expected.contains(jobId));
        EXPECT_FALSE(jobState.resultUrl.has_value());
        EXPECT_EQ(jobState.status, AgentJobStatus::Completed);
    }
}

TEST(JobStorage, sentJobs)
{
    JobStorage storage;
    const auto jobs = getTestAssignedJobs();
    storage.update(jobs);
    AgentJobState state{
        .status = AgentJobStatus::Running, .resultUrl = std::nullopt};
    storage.setLocalState(1, state);
    storage.setLocalState(2, state);
    const auto updatedJobs = storage.getUpdatedJobs();
    EXPECT_EQ(updatedJobs.size(), 1u);
    EXPECT_EQ(updatedJobs[0].first, 1u);
    EXPECT_EQ(updatedJobs[0].second.status, AgentJobStatus::Running);
    EXPECT_FALSE(updatedJobs[0].second.resultUrl.has_value());
}

TEST(JobStorage, clearByUpdate)
{
    JobStorage storage;
    auto jobs = getTestAssignedJobs();
    storage.update(jobs);
    jobs.pop_back();
    storage.update(jobs);
    const auto newJobs = storage.getNewJobs();
    std::unordered_set<JobId> newIds;
    for (const auto& [jobId, _]: newJobs) {
        newIds.insert(jobId);
    }
    EXPECT_EQ(newIds.size(), 2u);
    std::unordered_set<JobId> expected{1, 2};
    for (const auto jobId: newIds) {
        EXPECT_TRUE(expected.contains(jobId));
    }
}
