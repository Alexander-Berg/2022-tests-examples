#include "common.h"

#include <mapreduce/yt/tests/yt_unittest_lib/yt_unittest_lib.h>
#include <mapreduce/yt/util/ypath_join.h>

#include <library/cpp/testing/unittest/registar.h>
#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/json/json_writer.h>

#include <util/system/env.h>
#include <util/stream/file.h>
#include <util/stream/input.h>
#include <util/stream/str.h>

#include <chrono>
#include <thread>

namespace maps::garden::yt_task_handler::tests {

namespace {

constexpr char MODULE_EXECUTOR_PATH[] = "//home/garden/test/bin/module_executor";

class GlobalFixture
{
public:
    GlobalFixture();

    NYT::IClientPtr getClient() const { return client_; }
    TString getWorkingDir() const { return workingDir_; }
    TString getYtProxy() const { return ytProxy_; }

    const CommonYtSettings& commonYtSettings() const { return commonSpec_; }
    TaskSpec taskSpec() const { return taskSpec_; }
    BuildSpec buildSpec() const { return buildSpec_; }

private:
    void uploadFile(IInputStream* in, const TString& ytPath);
    void uploadFileFromArcadia(const TString& arcadiaPath, const TString& ytPath);
    void uploadJson(const NJson::TJsonValue& json, const TString& ytPath);

    NYT::IClientPtr client_;
    TString workingDir_;
    TString ytProxy_;
    CommonYtSettings commonSpec_;
    TaskSpec taskSpec_;
    BuildSpec buildSpec_;
};

GlobalFixture::GlobalFixture() {
    ytProxy_ = GetEnv("YT_PROXY");
    client_ = NYT::NTesting::CreateTestClient();
    workingDir_ = NYT::NTesting::CreateTestDirectory(client_);

    NJson::TJsonValue environmentSettings(NJson::EJsonValueType::JSON_MAP);
    environmentSettings.SetValueByPath("garden.server_hostname", "localhost");
    environmentSettings.SetValueByPath("logs_storage.type", "cypress");
    environmentSettings.SetValueByPath("logs_storage.dir", NYT::JoinYPaths(workingDir_, "log_file"));
    environmentSettings.SetValueByPath("logs_storage.ttl_hours", 2);
    environmentSettings.SetValueByPath("yt_servers.hahn.yt_config.proxy.url", ytProxy_);
    auto environmentSettingsPath = NYT::JoinYPaths(workingDir_, "environment_settings.json");
    uploadJson(environmentSettings, environmentSettingsPath);

    commonSpec_.cluster = ytProxy_;
    commonSpec_.token = "";

    commonSpec_.instance = "instance";

    commonSpec_.specOverrides = R"({"acl": [], "max_failed_job_count": 1, "fail_on_job_restart": true})";

    buildSpec_.moduleName = "test_module";
    buildSpec_.contourName = "contourName";
    buildSpec_.environmentName = "unittest";
    buildSpec_.modulePath = NYT::JoinYPaths(workingDir_, "test_module");
    buildSpec_.environmentSettingsPath = environmentSettingsPath;
    buildSpec_.tmpDir = workingDir_;

    taskSpec_.taskId = 1234;
    taskSpec_.taskName = "taskName";
    taskSpec_.taskKey = "123456";
    taskSpec_.logFile = "log_file";
    taskSpec_.cpuLimit = 1;
    taskSpec_.memoryLimit = 1;
    taskSpec_.needTmpfs = false;
    taskSpec_.ssdSize = 0;
    // TODO: Network_project does not work in tests. Uncomment after YT-13268
    // taskSpec_.networkProject = "network_project";

    // Create empty module binary. It is not used actually.
    client_->Create(TString(buildSpec_.modulePath), NYT::NT_FILE, NYT::TCreateOptions().Recursive(true).Force(true));

    uploadFileFromArcadia(
        "maps/garden/libs_server/yt_task_handler/tests/test_executor_simple/test_executor_simple",
        TString(MODULE_EXECUTOR_PATH));
}

void GlobalFixture::uploadFileFromArcadia(const TString& arcadiaPath, const TString& ytPath) {
    auto absPath = BinaryPath(arcadiaPath);
    TFileInput localFileReader(absPath);
    uploadFile(&localFileReader, ytPath);
}

void GlobalFixture::uploadFile(IInputStream* in, const TString& ytPath) {
    client_->Create(ytPath, NYT::NT_FILE, NYT::TCreateOptions().Recursive(true).Force(true));
    auto writer = client_->CreateFileWriter(ytPath);
    TransferData(in, writer.Get());
    writer->Finish();
}

void GlobalFixture::uploadJson(const NJson::TJsonValue& json, const TString& ytPath) {
    client_->Create(ytPath, NYT::NT_FILE, NYT::TCreateOptions().Recursive(true).Force(true));
    auto writer = client_->CreateFileWriter(ytPath);

    NJson::WriteJson(writer.Get(), &json);

    writer->Finish();
}

GlobalFixture& getGlobalFixture() {
    static GlobalFixture fixture;
    return fixture;
}

} // namespace

NYT::IClientPtr getClient() {
    return getGlobalFixture().getClient();
}

TString getWorkingDir() {
    return getGlobalFixture().getWorkingDir();
}

TString getYtProxy() {
    return getGlobalFixture().getYtProxy();
}

Task runTask(const TString& taskKey) {
    auto taskSpec = getGlobalFixture().taskSpec();
    taskSpec.taskKey = taskKey;
    return runTask(taskSpec);
}

Task runTask(const TaskSpec& taskSpec) {
    TaskRunner taskRunner(getGlobalFixture().commonYtSettings(), 1, 1, "search_marker", MODULE_EXECUTOR_PATH);

    TaskFullSpec taskFullSpec;
    taskFullSpec.taskSpec = taskSpec;
    taskFullSpec.buildSpec = getGlobalFixture().buildSpec();
    taskRunner.enqueueTaskStart(taskFullSpec);

    std::vector<TaskStatus> ops;
    size_t attempts = 0;
    while (ops.size() == 0 && attempts++ < 2000) {
        ops = taskRunner.popStartedTasks();
        std::this_thread::sleep_for(std::chrono::milliseconds(10));
    }

    UNIT_ASSERT_VALUES_EQUAL(ops.size(), 1);
    UNIT_ASSERT_VALUES_EQUAL(ops[0].error, "");
    return ops[0].task;
}

} // namespace maps::garden::yt_task_handler::tests
