#include <yandex_io/services/updatesd/updater.h>

#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <memory>
#include <string>

#include <fcntl.h>

using namespace quasar;
using namespace TestUtils;

namespace {

    constexpr const size_t SCRIPT_CMD_TIMESTAMP = 1539936676;

    constexpr const char* UPDATE_SCRIPT_TEST_FILE = "quasar_ota_script_test";
    constexpr const char* UPDATE_CMD_TEST_FILE = "quasar_ota_cmd_test";

    constexpr const char* UPDATE_SCRIPT = R"(#!/bin/sh

echo "Hello world"
touch quasar_ota_script_test)";
    constexpr const char* UPDATE_SCRIPT_SIGN = "T5a9UDM4UuKAZdwJ2R/vNvNk5ccKDYtF4L4B1VeK8acZh9XjVdooqXQsVKjt+WmYrkgHnI28FjkXQMRcnklA3L9DrzZeSxycNKnF143GUhs/VQZkThR/8vcNVEEeXVm0lTkh4TfPFqYIo1LZ4E8bBTAdRJx9+cLyyilBbUdaPhkMXw8zXyzAfmmfasOWeljMZnI/L9Aev2Sskv2/vw9pBRQCXr2r0d4JhJN+jYYLstaqVkrPq25jQJZWU2uS5bTEKTyve4cH3cg45K66U+KSw1xxTmUF3RYYBEwY1752AEugcZZLoM542n9OSjZSGhglc105EyEdN9wYivky3V5QpQ==";
    constexpr const char* UPDATE_SCRIPT_FAKE = R"(#!/bin/sh

echo "FAKE"
touch quasar_ota_script_test)";

    constexpr const char* UPDATE_CMD = "touch quasar_ota_cmd_test";
    constexpr const char* UPDATE_CMD_SIGN = "jC+32OOg4yqBLbAsHehC2UBo6Mz9vxe6GuWeEvvm14UOXV8l6DfBn5aCAO3VJZYVpjn5OBQZR5xW93Jf0Lu+39B1ZCnabUxNssmMmyJUpv1AyccIJkRSvFvRJDle1Vdep7+hm9msp10bNwGAH84QaU2jfYC6f05fa6dCuMBOxELUAMGOvcbYAuxTQ3RHdW1x8m42cHoOU7BjhVIohaqvDyrA7pvKj0ohf1Rq+BcXYEfF8lC0AffGql7zE62kP7kAkro7dNIv0tRGvc4WleRO83sZvnwVfmKcwUbbv5l34CM23ug0mWKPmSO5tyOG7bEEZTFhQP6rD4n9HJ2WCqyhSQ==";
    constexpr const char* UPDATE_CMD_FAKE = "touch FAKE_quasar_ota_cmd_test";

    struct Fixture: public QuasarUnitTestFixture {
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            unlink(UPDATE_SCRIPT_TEST_FILE);
            unlink(UPDATE_CMD_TEST_FILE);

            const int backendPort = mockBackend.start(getPort());

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["common"]["backendUrl"] = "http://localhost:" + std::to_string(backendPort);

            config["updatesd"]["applyUpdateScript"] = "stub";
            config["updatesd"]["minUpdateHour"] = 0;
            config["updatesd"]["maxUpdateHour"] = 0;
            config["updatesd"]["updatesDir"] = "stub";
            config["updatesd"]["updatesExt"] = "stub";

            mockServer = createIpcServerForTests("iohub_services");
            mockServer->listenService();

            updater = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(0), std::chrono::seconds(0));
            /* create activation state receiver */
            mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
                Json::Value response;

                response["hasUpdate"] = true;

                response["updateScriptSign"] = updateScriptSign;
                response["updateScriptTimestamp"] = SCRIPT_CMD_TIMESTAMP;

                if (isUpdateCmd) {
                    response["updateCommand"] = updateScript;
                } else {
                    response["updateScript"] = updateScript;
                }

                handler.doReplay(200, "application/json", jsonToString(response));
            };
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            unlink(UPDATE_SCRIPT_TEST_FILE);
            unlink(UPDATE_CMD_TEST_FILE);

            Base::TearDown(context);
        }

        void overrideQuasarTimestamp(size_t timestamp) const {
            updater->setBuildTimestamp(timestamp);
        }

        YandexIO::Configuration::TestGuard testGuard;

        TestHttpServer mockBackend;
        std::unique_ptr<Updater> updater;

        std::string updateScript;
        std::string updateScriptSign;

        std::atomic_bool isUpdateCmd{false}; // whether updateScript is script or just a command
        std::shared_ptr<ipc::IServer> mockServer;
    };

} /* anonymous namespace */

Y_UNIT_TEST_SUITE_F(UpdateScriptTest, Fixture) {
    Y_UNIT_TEST(testUpdateOtaScriptCorrectSignature)
    {
        isUpdateCmd = false;

        updateScript = UPDATE_SCRIPT;
        updateScriptSign = UPDATE_SCRIPT_SIGN;

        /* Quasar timestamp is lower than script */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP - 100);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_SCRIPT_TEST_FILE, F_OK), 0);
        unlink(UPDATE_SCRIPT_TEST_FILE);
        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_SCRIPT_TEST_FILE, F_OK), -1);

        /* Quasar timestamp equals script timestamp */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_SCRIPT_TEST_FILE, F_OK), 0);
        unlink(UPDATE_SCRIPT_TEST_FILE);
        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_SCRIPT_TEST_FILE, F_OK), -1);

        /* Quasar timestamp higher than script timestamp */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP + 100);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_SCRIPT_TEST_FILE, F_OK), -1);
    }

    Y_UNIT_TEST(testUpdateOtaScriptWrongSignature)
    {
        isUpdateCmd = false;

        updateScript = UPDATE_SCRIPT_FAKE;
        updateScriptSign = UPDATE_SCRIPT_SIGN;

        /* Quasar timestamp is lower than script */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP - 100);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_SCRIPT_TEST_FILE, F_OK), -1);

        /* Quasar timestamp equals script timestamp */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_SCRIPT_TEST_FILE, F_OK), -1);

        /* Quasar timestamp higher than script timestamp */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP + 100);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_SCRIPT_TEST_FILE, F_OK), -1);
        unlink(UPDATE_SCRIPT_TEST_FILE);
    }

    Y_UNIT_TEST(testUpdateOtaCommand)
    {
        isUpdateCmd = true;

        updateScript = UPDATE_CMD;
        updateScriptSign = UPDATE_CMD_SIGN;

        /* Quasar timestamp is lower than script */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP - 100);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_CMD_TEST_FILE, F_OK), 0);
        unlink(UPDATE_CMD_TEST_FILE);
        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_CMD_TEST_FILE, F_OK), -1);

        /* Quasar timestamp equals script timestamp */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_CMD_TEST_FILE, F_OK), 0);
        unlink(UPDATE_CMD_TEST_FILE);
        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_CMD_TEST_FILE, F_OK), -1);

        /* Quasar timestamp higher than script timestamp */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP + 100);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_CMD_TEST_FILE, F_OK), -1);
    }

    Y_UNIT_TEST(testUpdateOtaWrongSignature)
    {
        isUpdateCmd = true;

        updateScript = UPDATE_CMD_FAKE;
        updateScriptSign = UPDATE_CMD_SIGN;

        /* Quasar timestamp is lower than script */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP - 100);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_CMD_TEST_FILE, F_OK), -1);

        /* Quasar timestamp equals script timestamp */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_CMD_TEST_FILE, F_OK), -1);

        /* Quasar timestamp higher than script timestamp */

        overrideQuasarTimestamp(SCRIPT_CMD_TIMESTAMP + 100);
        updater->checkUpdate();

        UNIT_ASSERT_VALUES_EQUAL(access(UPDATE_CMD_TEST_FILE, F_OK), -1);
    }
}
