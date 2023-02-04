#include "maps/b2bgeo/vm_scheduler/server/libs/db/include/pg_task_storage.h"
#include "maps/b2bgeo/vm_scheduler/server/libs/db/impl/test_utils.h"
#include "maps/libs/pgpool/include/pgpool3.h"

#include <maps/b2bgeo/vm_scheduler/server/libs/task_storage/include/task_storage.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/scheduler/include/scheduler.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/errors.h>
#include <maps/b2bgeo/vm_scheduler/server/libs/state/include/task.h>

#include <maps/b2bgeo/vm_scheduler/libs/common/include/errors.h>
#include <maps/b2bgeo/vm_scheduler/libs/common/include/stringify.h>

#include <maps/b2bgeo/libs/postgres_test/postgres.h>
#include <maps/b2bgeo/libs/postgres/helpers.h>

#include <library/cpp/testing/gtest/gtest.h>
#include <util/string/join.h>

#include <memory>


using namespace maps::b2bgeo;
using namespace maps::b2bgeo::vm_scheduler;
using namespace std::chrono_literals;
namespace t = maps::b2bgeo::vm_scheduler::testing;

namespace {

JobState getJobState(const JobStatus status) {
    return {
        .status = status,
        .resultUrl
            = (status == JobStatus::Completed)
            ? std::optional<std::string>("result url")
            : std::nullopt,
    };
}

} // anonymous namespace


TEST(AgentLogic, fullScenario)
{
    Postgres postgres("vms_db_schema.sql");
    auto pool = createPool(postgres);
    const auto vmId = t::insertVm(postgres, VmStatus::Allocated);

    PgTaskStorage pgTaskStorage(createPool(postgres));
    pgTaskStorage.registerUnistat(TUnistat::Instance(), NUnistat::TPriority(10));

    const auto taskParameters = t::getThreeJobTaskParameters();
    const auto jobsResult = pgTaskStorage.addTask(taskParameters);
    EXPECT_TRUE(jobsResult.IsSuccess());

    auto writeTxn = pool.masterWriteableTransaction();
    const auto updateJobsQuery = toString(
        "UPDATE scheduler.jobs ",
        "SET vm_id = ", vmId, ", status = '", ToString(JobStatus::Scheduled), "' ",
        "WHERE id IN (", JoinSeq(", ", jobsResult.ValueRefOrThrow().jobIds), ");");
    pg::execQuery(updateJobsQuery, *writeTxn);
    writeTxn->commit();

    const auto assignedJobs = pgTaskStorage.getAssignedJobs(vmId);
    EXPECT_TRUE(assignedJobs.IsSuccess());
    EXPECT_EQ(assignedJobs.ValueRefOrThrow().size(), 3u);
    EXPECT_TRUE(TUnistat::Instance().GetSignalValueUnsafe("agent_start_time")->HasNumber());

    const auto jobToLaunch = pgTaskStorage.getJobToLaunch(vmId, assignedJobs.ValueRefOrThrow()[0].id);
    EXPECT_TRUE(jobToLaunch.IsSuccess());
    EXPECT_EQ(jobToLaunch.ValueRefOrThrow().id, assignedJobs.ValueRefOrThrow()[0].id);
    EXPECT_EQ(jobToLaunch.ValueRefOrThrow().capacityLimits, taskParameters.requiredCapacity);
    EXPECT_EQ(jobToLaunch.ValueRefOrThrow().taskSettings, taskParameters.settings);
    EXPECT_EQ(jobToLaunch.ValueRefOrThrow().imageVersion, taskParameters.imageVersion);
    EXPECT_EQ(jobToLaunch.ValueRefOrThrow().jobOptions, taskParameters.jobOptions[0]);

    const auto updateResult = pgTaskStorage.updateJobState(
        vmId, assignedJobs.ValueRefOrThrow()[0].id, getJobState(JobStatus::Running));
    EXPECT_TRUE(updateResult.IsSuccess());
    EXPECT_TRUE(TUnistat::Instance().GetSignalValueUnsafe("job_start_time")->HasNumber());

    const auto jobInfoQuery = toString(
        "SELECT started, finished, status, result_url FROM scheduler.jobs WHERE id = ",
        assignedJobs.ValueRefOrThrow()[0].id, ";");

    const auto vmInfoQuery = toString(
        "SELECT cpu, cpu_idle, ram, ram_idle FROM scheduler.vms WHERE id = ", vmId, ";");
    auto readTxn = pool.masterReadOnlyTransaction();
    {
        const auto jobInfo = pg::execQuery(jobInfoQuery, *readTxn);
        EXPECT_EQ(FromString<JobStatus>(jobInfo[0].at("status").as<std::string>()), JobStatus::Running);
        EXPECT_TRUE(jobInfo[0].at("started").as<std::optional<std::string>>());
        EXPECT_TRUE(!jobInfo[0].at("finished").as<std::optional<std::string>>());
        EXPECT_TRUE(!jobInfo[0].at("result_url").as<std::optional<std::string>>());

        const auto vmInfo = pg::execQuery(vmInfoQuery, *readTxn);
        EXPECT_EQ(vmInfo[0].at("cpu").as<std::optional<size_t>>(), 2u);
        EXPECT_EQ(vmInfo[0].at("cpu_idle").as<std::optional<size_t>>(), 1u);
        EXPECT_EQ(vmInfo[0].at("ram").as<std::optional<size_t>>(), 2048u);
        EXPECT_EQ(vmInfo[0].at("ram_idle").as<std::optional<size_t>>(), 1024u);
    }

    const auto finishJobResult = pgTaskStorage.updateJobState(
        vmId, assignedJobs.ValueRefOrThrow()[0].id, getJobState(JobStatus::Completed));
    EXPECT_TRUE(finishJobResult.IsSuccess());
    {
        const auto jobInfo = pg::execQuery(jobInfoQuery, *readTxn);
        EXPECT_EQ(FromString<JobStatus>(jobInfo[0].at("status").as<std::string>()), JobStatus::Completed);
        EXPECT_TRUE(jobInfo[0].at("started").as<std::optional<std::string>>());
        EXPECT_TRUE(jobInfo[0].at("finished").as<std::optional<std::string>>());
        EXPECT_TRUE(jobInfo[0].at("result_url").as<std::optional<std::string>>());

        const auto vmInfo = pg::execQuery(vmInfoQuery, *readTxn);
        EXPECT_EQ(vmInfo[0].at("cpu").as<std::optional<size_t>>(), 2u);
        EXPECT_EQ(vmInfo[0].at("cpu_idle").as<std::optional<size_t>>(), 2u);
        EXPECT_EQ(vmInfo[0].at("ram").as<std::optional<size_t>>(), 2048u);
        EXPECT_EQ(vmInfo[0].at("ram_idle").as<std::optional<size_t>>(), 2048u);
    }
}

TEST(AgentLogic, jobNotFound)
{
    Postgres postgres("vms_db_schema.sql");
    auto pool = createPool(postgres);
    const auto vmId = t::insertVm(postgres, VmStatus::AgentStarted);

    PgTaskStorage pgTaskStorage(createPool(postgres));

    const auto assignedJobs = pgTaskStorage.getAssignedJobs(vmId);
    EXPECT_TRUE(assignedJobs.IsSuccess());
    EXPECT_EQ(assignedJobs.ValueRefOrThrow().size(), 0u);

    const JobId nonExistingJobId = 13;
    const auto finishJobResult = pgTaskStorage.updateJobState(
        vmId, nonExistingJobId, getJobState(JobStatus::Completed));
    EXPECT_TRUE(finishJobResult.IsFailure());
    EXPECT_TRUE(finishJobResult.holdsErrorType<JobNotFoundException>());
}

TEST(AgentLogic, vmNotFound)
{
    Postgres postgres("vms_db_schema.sql");
    auto pool = createPool(postgres);

    PgTaskStorage pgTaskStorage(createPool(postgres));

    const VmId nonExistingVmId = 42;
    const auto assignedJobs = pgTaskStorage.getAssignedJobs(nonExistingVmId);
    EXPECT_TRUE(assignedJobs.IsSuccess());
    EXPECT_EQ(assignedJobs.ValueRefOrThrow().size(), 0u);

    const JobId nonExistingJobId = 13;
    const auto finishJobResult = pgTaskStorage.updateJobState(
        nonExistingVmId, nonExistingJobId, getJobState(JobStatus::Completed));
    EXPECT_TRUE(finishJobResult.IsFailure());
    EXPECT_TRUE(finishJobResult.holdsErrorType<JobNotFoundException>());
}
