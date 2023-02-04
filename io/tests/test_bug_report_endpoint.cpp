#include <yandex_io/services/bug_report/bug_report_endpoint.h>

#include <yandex_io/interfaces/user_config/mock/user_config_provider.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/unittest_helper/telemetry_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <future>

using namespace quasar;
using namespace quasar::proto;

namespace {

    class Fixture: public TelemetryTestFixture {
    public:
        using Base = TelemetryTestFixture;

        std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            mockPushd = createIpcServerForTests("pushd");
            mockPushd->listenService();

            mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

    protected:
        YandexIO::Configuration::TestGuard testGuard;

        std::shared_ptr<ipc::IServer> mockPushd;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(BugReportEndpointTests, Fixture) {
    Y_UNIT_TEST(testBugReport) {
        std::promise<std::string> testData;
        std::promise<std::string> labudidabudaiData;
        std::promise<std::string> lolKekData;
        std::promise<std::string> headData;
        std::promise<std::string> test2Data;
        std::promise<std::string> fileData;
        std::promise<std::string> customCommand;
        std::promise<std::string> anotherCustomCommand;

        setKeyValueListener([&](const std::string& /*eventName*/, const std::unordered_map<std::string, std::string>& keyValues, YandexIO::ITelemetry::Flags /*flags*/) {
            for (const auto& pair : keyValues) {
                if (pair.first == "echo_testtest.log") {
                    testData.set_value(pair.second);
                } else if (pair.first == "testfile.txt") {
                    fileData.set_value(pair.second);
                } else if (pair.first == "echo_labudidabudai.log") {
                    labudidabudaiData.set_value(pair.second);
                } else if (pair.first == "echo_lol_kek_cheburek.log") {
                    lolKekData.set_value(pair.second);
                } else if (pair.first == "echo.log") {
                    test2Data.set_value(pair.second);
                } else if (pair.first == "usr_bin_head_-c_10__dev_zero.log") {
                    headData.set_value(pair.second);
                } else if (pair.first == "echo_some_custom_command.log") {
                    customCommand.set_value(pair.second);
                } else if (pair.first == "echo_another_custom_command.log") {
                    anotherCustomCommand.set_value(pair.second);
                } else if (pair.first == "cat_textfile_txt") {
                    UNIT_FAIL("test error: cat is not allowed custom command.log");
                } else if (pair.first == "id") {
                    if (pair.second != "32" && pair.second != "33") {
                        UNIT_FAIL("test error");
                    }
                }
            }
        });

        UNIT_ASSERT(system("echo labudidabudai > testfile.txt") >= 0);

        auto& config = getDeviceForTests() -> configuration() -> getMutableConfig(testGuard);
        config["bug_report"]["fileLogs"] = parseJson(
            "[\n"
            "   {\n"
            "       \"fileName\": \"testfile.txt\",\n"
            "       \"size\": 200000\n"
            "   }\n"
            "]");
        config["bug_report"]["commandLogs"] = parseJson(
            "[\n"
            "   {\n"
            "       \"command\": \"echo\",\n"
            "       \"commandLineArgs\": [\n"
            "           \"testtest\"\n"
            "       ],\n"
            "       \"size\": 5\n"
            "   },\n"
            "   {\n"
            "       \"command\": \"echo\",\n"
            "       \"commandLineArgs\": [\n"
            "           \"labudidabudai\"\n"
            "       ],\n"
            "       \"size\": 1\n"
            "   },\n"
            "   {\n"
            "       \"command\": \"echo\",\n"
            "       \"commandLineArgs\": [\n"
            "           \"lol_kek_cheburek\"\n"
            "       ],\n"
            "       \"size\": 1000000\n"
            "   },\n"
            "   {\n"
            "       \"command\": \"/////usr/bin/head\",\n"
            "       \"commandLineArgs\": [\n"
            "           \"-c\",\n"
            "           \"10\",\n"
            "           \"/dev/zero\"\n"
            "       ],\n"
            "       \"size\": 100\n"
            "   },\n"
            "   {\n"
            "       \"command\": \"echo\",\n"
            "       \"size\": 175000\n"
            "   }\n"
            "]");
        config["bug_report"]["allowedCustomCommands"] = parseJson(
            "[\n"
            "   \"echo\"\n"
            "]");

        BugReportEndpoint bugReportEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockUserConfigProvider);

        auto bugReportConnector = createIpcConnectorForTests("bug_report");
        bugReportConnector->connectToService();
        bugReportConnector->waitUntilConnected();

        QuasarMessage bugReport;
        bugReport.set_bug_report_id("32");
        bugReportConnector->sendMessage(std::move(bugReport));

        UNIT_ASSERT_VALUES_EQUAL(testData.get_future().get(), "test\n");
        UNIT_ASSERT_VALUES_EQUAL(labudidabudaiData.get_future().get(), "\n");
        UNIT_ASSERT_VALUES_EQUAL(lolKekData.get_future().get(), "lol_kek_cheburek\n");
        UNIT_ASSERT_VALUES_EQUAL(headData.get_future().get().size(), 10); // command produced ten zeroes here :). Real test is stripped name of command checked in key matching
        UNIT_ASSERT_VALUES_EQUAL(test2Data.get_future().get(), "\n");

        UNIT_ASSERT_VALUES_EQUAL(fileData.get_future().get(), "labudidabudai\n");

        auto userConfig = quasar::UserConfig{.auth = quasar::UserConfig::Auth::SUCCESS};
        userConfig.system["bug_report"]["customCommandLogs"] = parseJson(
            "[\n"
            "   {\n"
            "       \"command\": \"echo\",\n"
            "       \"commandLineArgs\": [\n"
            "           \"some_custom_command\"\n"
            "       ],\n"
            "       \"size\": 175000\n"
            "   },\n"
            "   {\n"
            "       \"command\": \"echo\",\n"
            "       \"commandLineArgs\": [\n"
            "           \"another_custom_command\"\n"
            "       ],\n"
            "       \"size\": 175000\n"
            "   },\n"
            "   {\n"
            "       \"command\": \"cat\",\n"
            "       \"commandLineArgs\": [\n"
            "           \"testfile.txt\"\n"
            "       ],\n"
            "       \"size\": 175000\n"
            "   }\n"
            "]");
        mockUserConfigProvider->setUserConfig(userConfig);

        testData = std::promise<std::string>();
        labudidabudaiData = std::promise<std::string>();
        lolKekData = std::promise<std::string>();
        headData = std::promise<std::string>();
        test2Data = std::promise<std::string>();
        fileData = std::promise<std::string>();

        QuasarMessage bugReportAfterConfigUpdate;
        bugReportAfterConfigUpdate.set_bug_report_id("33");
        bugReportConnector->sendMessage(std::move(bugReportAfterConfigUpdate));

        UNIT_ASSERT_VALUES_EQUAL(customCommand.get_future().get(), "some_custom_command\n");
        UNIT_ASSERT_VALUES_EQUAL(anotherCustomCommand.get_future().get(), "another_custom_command\n");

        UNIT_ASSERT(system("rm testfile.txt") == 0);
    }

    Y_UNIT_TEST(testBugReportOnPush) {
        std::promise<const std::string> sendBugReportPromise;

        setKeyValueListener([&](const std::string& /*eventName*/, const std::unordered_map<std::string, std::string>& keyValues, YandexIO::ITelemetry::Flags /*flags*/) {
            for (const auto& pair : keyValues) {
                if (pair.first == "id") {
                    sendBugReportPromise.set_value(pair.second);
                    break;
                }
            }
        });

        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["bug_report"]["commandLogs"] = parseJson(
            "[\n"
            "   {\n"
            "       \"command\": \"echo\",\n"
            "       \"commandLineArgs\": [\n"
            "           \"hello\"\n"
            "       ],\n"
            "       \"size\": 175000\n"
            "   }\n"
            "]");

        BugReportEndpoint bugReportEndpoint(getDeviceForTests(), ipcFactoryForTests(), mockUserConfigProvider);

        auto bugReportConnector = createIpcConnectorForTests("bug_report");
        bugReportConnector->connectToService();
        bugReportConnector->waitUntilConnected();

        QuasarMessage pushMessage;
        pushMessage.mutable_push_notification()->set_operation("send_bugreport");
        mockPushd->waitConnectionsAtLeast(1); // make sure that bug_report service is connected to pushd
        mockPushd->sendToAll(std::move(pushMessage));

        std::future<const std::string> sendBugReportFuture = sendBugReportPromise.get_future();
        const std::string bugReportId = sendBugReportFuture.get();
        UNIT_ASSERT_VALUES_EQUAL(bugReportId.substr(0, 5), "PUSH-");
        UNIT_ASSERT(isUUID(bugReportId.substr(5)));
    }
}
