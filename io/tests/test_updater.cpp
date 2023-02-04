#include <yandex_io/services/updatesd/update_apply_status.h>
#include <yandex_io/services/updatesd/updater.h>

#include <yandex_io/libs/base/crc32.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/cryptography/cryptography.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/modules/geolocation/interfaces/timezone.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/telemetry_test_fixture.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <fstream>
#include <future>
#include <thread>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::proto;

namespace {
    class IOHubWrapper {
    public:
        IOHubWrapper(std::shared_ptr<ipc::IIpcFactory> ipcFactory)
            : server_(ipcFactory->createIpcServer("iohub_services"))
        {
            server_->listenService();
        }

        void setAllowUpdate(bool allowUpdateAll, bool allowCritical) {
            proto::QuasarMessage message;
            message.mutable_io_control()->mutable_allow_update()->set_for_all(allowUpdateAll);
            message.mutable_io_control()->mutable_allow_update()->set_for_critical(allowCritical);
            server_->sendToAll(QuasarMessage{message});
        }

        void setTimezone(const YandexIO::Timezone& timezone) {
            server_->sendToAll(ipc::buildMessage([&timezone](auto& msg) {
                msg.mutable_io_control()->mutable_timezone()->CopyFrom(timezone.toProto());
            }));
        }

        void waitConnection() {
            server_->waitConnectionsAtLeast(1);
        }

    private:
        std::shared_ptr<ipc::IServer> server_;
    };

    struct Fixture: public virtual QuasarUnitTestFixture {
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
            SetUpInternal();
        }

        void SetUpInternal() {
            /* Use empty default config and setup only ACTUALLY important stuff to config */
            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard_);
            config.clear();

            TFsPath("./update-counter.json").ForceDelete();

            config["common"]["deviceType"] = "testDeviceType";
            config["common"]["softwareVersion"] = testVersion_;
            config["common"]["cryptography"]["devicePublicKeyPath"] = std::string(ArcadiaSourceRoot()) + "/yandex_io/misc/cryptography/public.pem";
            config["common"]["cryptography"]["devicePrivateKeyPath"] = std::string(ArcadiaSourceRoot()) + "/yandex_io/misc/cryptography/private.pem";
            config["updatesd"]["minUpdateHour"] = 3;
            config["updatesd"]["maxUpdateHour"] = 4;
            config["updatesd"]["updatesDir"] = ".";
            config["updatesd"]["updatesExt"] = ".zip";
            config["updatesd"]["applyUpdateScript"] = std::string(ArcadiaSourceRoot()) + "/yandex_io/misc/test_device_ota_update.sh",
            config["updatesd"]["otaScriptPublicKey"] = std::string(ArcadiaSourceRoot()) + "/yandex_io/misc/cryptography/ota_script.pub";
            config["updatesd"]["randomWaitLimitSec"] = 3600;
            config["updatesd"]["updateInfoPath"] = updateInfo_;
            config["updatesd"]["attemptsCounterPath"] = "./update-counter.json";
            config["updatesd"]["maxAttempts"] = 3;
            ioHub_ = std::make_unique<IOHubWrapper>(ipcFactoryForTests());

            setUpdateVersion("2.3.1.6.406275671.20190402");

            mockUpdatesEndpoint_.onHandlePayload = [=](const TestHttpServer::Headers& header,
                                                       const std::string& payload, TestHttpServer::HttpConnection& handler) {
                YIO_LOG_INFO("Got http payload: " << payload);
                if (setUpdatesRequestHeaderPromise_) // Prevent future already satisfied error
                {
                    updatesRequestHeaderPromise_.set_value(header);
                    setUpdatesRequestHeaderPromise_ = false;
                }

                handler.doReplay(200, "application/zip", updateData_);
            };

            mockUpdatesEndpoint_.start(getPort());

            mockBackend_.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                               const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
                if (setBackerRequestHeaderPromise_.exchange(false)) // Prevent future already satisfied error
                {
                    backendRequestHeaderPromise_.set_value(header);
                }
                Json::Value backendResponse;
                backendResponse["hasUpdate"] = hasUpdate_.load();
                backendResponse["critical"] = isUpdateCritical_.load();
                backendResponse["downloadUrl"] = "http://localhost:" + std::to_string(mockUpdatesEndpoint_.port()) +
                                                 "/yandexstation/ota/user/f0ed9b20-0220-4b3f-8f17-b8be535d4ded/quasar-2.3.1.6.406275671.20190402.zip";
                backendResponse["version"] = updateVersion_;
                backendResponse["crc32"] = getCrc32(updateData_);

                handler.doReplay(200, "application/json", jsonToString(backendResponse));
            };
            mockBackend_.start(getPort());
            config["common"]["backendUrl"] = "http://localhost:" + std::to_string(mockBackend_.port());

            UpdateApplyStatus applyStatus(getDeviceForTests());
            applyStatus.deleteFromDisk();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            TearDownInternal();
            Base::TearDown(context);
        }

        void TearDownInternal() {
            updater_.reset();
            mockBackend_.stop();
            mockUpdatesEndpoint_.stop();
            std::remove(updateFileName_.c_str());
            std::remove(appliedUpdateFileName_.c_str());
            std::remove(updateInfo_.c_str());
            std::remove("./update-counter.json");
        }

        void setUpdateVersion(const std::string& version) {
            updateVersion_ = version;
            updateFileName_ = "update_" + Updater::escapePath(updateVersion_) + ".zip";
            appliedUpdateFileName_ = updateFileName_ + "_applied";
            std::remove(updateFileName_.c_str());
            std::remove(appliedUpdateFileName_.c_str());
        }

        std::promise<TestHttpServer::Headers> updatesRequestHeaderPromise_;
        std::atomic<bool> setUpdatesRequestHeaderPromise_{true};
        std::unique_ptr<IOHubWrapper> ioHub_;
        TestHttpServer mockUpdatesEndpoint_;

        const std::string updateInfo_ = "./update-info.json";
        const std::string testVersion_ = "__test_version__";
        const char* updateData_ = "This is an update";
        std::string updateVersion_;
        std::string updateFileName_;
        std::string appliedUpdateFileName_; // test_device_ota_update.sh script just copies update to filename with "_applied" added

        std::promise<TestHttpServer::Headers> backendRequestHeaderPromise_;
        std::atomic<bool> setBackerRequestHeaderPromise_{true};

        void resetBackendPromise()
        {
            YIO_LOG_INFO("Resetting backend promise");
            backendRequestHeaderPromise_ = std::promise<TestHttpServer::Headers>();
            setBackerRequestHeaderPromise_ = true;
        }

        void resetUpdatesPromise()
        {
            YIO_LOG_INFO("Resetting updates promise");
            updatesRequestHeaderPromise_ = std::promise<TestHttpServer::Headers>();
            setUpdatesRequestHeaderPromise_ = true;
        }

        std::atomic_bool hasUpdate_{true};
        std::atomic_bool isUpdateCritical_{true};
        TestHttpServer mockBackend_;

        YandexIO::Configuration::TestGuard testGuard_;

        const YandexIO::Timezone timezone_ = {
            .timezoneName = "Europe/Saratov",
            .timezoneOffsetSec = 2 * 60 * 60};

        std::unique_ptr<Updater> updater_;
    };

    struct FixtureMetrics: public TelemetryTestFixture, public Fixture {
        FixtureMetrics()
            : TelemetryTestFixture()
            ,         /* Init Yandex::Device with Telemetry for metrics tests */
            Fixture() /* Init base test environment */
        {
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            TelemetryTestFixture::SetUp(context);
            Fixture::SetUpInternal();

            Json::Value& updatesdConfig = getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"];
            const std::string updatesDir = updatesdConfig["updatesDir"].asString();
            const std::string updatesExt = updatesdConfig["updatesExt"].asString();
            const std::string updateStorePath = updatesDir + "/update_" + updateVersion_ + updatesExt;
            ::unlink(updateStorePath.c_str());
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Fixture::TearDownInternal();
            TelemetryTestFixture::TearDown(context);
        }
    };

    class TestableUpdater: public Updater {
    public:
        using Updater::Updater;

        std::chrono::system_clock::time_point getCurrentTimestamp() const override {
            return currentTimestamp.load();
        }

        std::atomic<std::chrono::system_clock::time_point> currentTimestamp{std::chrono::system_clock::time_point()};
    };
} /* anonymous namespace */

Y_UNIT_TEST_SUITE(TestUpdater) {

    Y_UNIT_TEST_F(testUpdaterCriticalUpdate, Fixture)
    {
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));

        updater_->start();

        auto backendHeader = backendRequestHeaderPromise_.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.verb, "GET");
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.resource, "/check_updates");
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.queryParams.getValue("version"), getDeviceForTests()->softwareVersion());
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.queryParams.getValue("platform"), getDeviceForTests()->configuration()->getDeviceType());

        auto updatesHeader = updatesRequestHeaderPromise_.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.verb, "GET");
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.resource, "/yandexstation/ota/user/f0ed9b20-0220-4b3f-8f17-b8be535d4ded/quasar-2.3.1.6.406275671.20190402.zip");

        /* Wait until updatesd download content */
        waitUntil([&]() {
            return (fileExists(appliedUpdateFileName_) &&
                    getFileContent(appliedUpdateFileName_) == updateData_);
        });

        UNIT_ASSERT_VALUES_EQUAL(getFileContent(appliedUpdateFileName_), updateData_);
    }

    Y_UNIT_TEST_F(testUpdaterNonCriticalUpdate, Fixture)
    {
        getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"]["checkPeriodMs"] = 100;
        getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"]["criticalCheckPeriodMs"] = 100;

        TestableUpdater* const updater_Ptr = new TestableUpdater(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        updater_.reset(updater_Ptr);

        /* Set up not critical update in response */
        isUpdateCritical_ = false;
        updater_->start();

        ioHub_->waitConnection();
        ioHub_->setTimezone(timezone_);

        updater_->waitUntilTimezoneReceived(timezone_.timezoneOffsetSec / 3600);

        auto backendHeader = backendRequestHeaderPromise_.get_future().get();
        auto verifyBackendHeader = [device{getDeviceForTests()}](const auto& header) {
            UNIT_ASSERT_VALUES_EQUAL(header.verb, "GET");
            UNIT_ASSERT_VALUES_EQUAL(header.resource, "/check_updates");
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("device_id"), device->deviceId());
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("version"), device->softwareVersion());
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("platform"), device->configuration()->getDeviceType());
        };

        verifyBackendHeader(backendHeader);

        auto updatesFuture = updatesRequestHeaderPromise_.get_future();
        UNIT_ASSERT_EQUAL(updatesFuture.wait_for(std::chrono::milliseconds(200)), std::future_status::timeout);

        YIO_LOG_DEBUG("Random wait seconds: " << updater_->getRandomWaitSeconds());
        /* move timestamp by 1 hour ahead, so timestamp still will be not in [minUpdateHour..maxUpdateHour] range, so device won't update yet */
        auto nowMidnight = getStartOfDayUTC(std::chrono::system_clock::now());
        updater_Ptr->currentTimestamp =
            nowMidnight + std::chrono::hours(1) + std::chrono::seconds(updater_->getRandomWaitSeconds() - 1);
        backendRequestHeaderPromise_ = std::promise<TestHttpServer::Headers>();
        setBackerRequestHeaderPromise_ = true;
        backendHeader = backendRequestHeaderPromise_.get_future().get();
        verifyBackendHeader(backendHeader);

        UNIT_ASSERT_EQUAL(updatesFuture.wait_for(std::chrono::milliseconds(200)), std::future_status::timeout);

        /* move timestamp by 1 more hour ahead, So timestamp still will be in [minUpdateHour..maxUpdateHour] range, so deice will update */
        updater_Ptr->currentTimestamp =
            nowMidnight + std::chrono::hours(1) + std::chrono::seconds(updater_->getRandomWaitSeconds() + 1);

        auto updatesHeader = updatesFuture.get();
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.verb, "GET");
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.resource, "/yandexstation/ota/user/f0ed9b20-0220-4b3f-8f17-b8be535d4ded/quasar-2.3.1.6.406275671.20190402.zip");

        waitUntil([&]() {
            return (fileExists(appliedUpdateFileName_) &&
                    getFileContent(appliedUpdateFileName_) == updateData_);
        });

        UNIT_ASSERT_VALUES_EQUAL(getFileContent(appliedUpdateFileName_), updateData_);
    }

    Y_UNIT_TEST_F(testUpdaterUpdateTimezone, Fixture)
    {
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        updater_->start();

        ioHub_->waitConnection();
        ioHub_->setTimezone(timezone_);

        updater_->waitUntilTimezoneReceived(timezone_.timezoneOffsetSec / 3600);

        std::this_thread::sleep_for(std::chrono::milliseconds(100)); // Wait until message received and nothing fails;

        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(testUpdaterSendsMetricForDownloadStart, FixtureMetrics) {
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));

        std::vector<std::promise<std::pair<std::string, std::string>>> msgReceived(1);
        uint32_t msgNumber = 0;

        setEventListener([&](const std::string& event, const std::string& eventJson, YandexIO::ITelemetry::Flags /*flags*/) {
            if (event == "http_request") {
                return; // skip http client metrics
            }
            if (msgNumber < msgReceived.size()) {
                msgReceived[msgNumber++].set_value({event, eventJson});
            }
        });

        updater_->start();

        waitUntil([&]() {
            return (fileExists(appliedUpdateFileName_) &&
                    getFileContent(appliedUpdateFileName_) == updateData_);
        });

        std::vector<std::pair<std::string, std::string>> metricaMessages;
        for (auto& msgPromise : msgReceived) {
            metricaMessages.push_back(msgPromise.get_future().get());
        }

        UNIT_ASSERT_VALUES_EQUAL(metricaMessages.at(0).first, "updateDownloadStart");

        const Json::Value startMessageBody = parseJson(metricaMessages.at(0).second);
        UNIT_ASSERT_VALUES_EQUAL(startMessageBody["toVersion"].asString(), updateVersion_);
        UNIT_ASSERT_VALUES_EQUAL(startMessageBody["fromVersion"].asString(), testVersion_);
    }

    Y_UNIT_TEST_F(testUpdaterSendsMetricForDownloadComplete, FixtureMetrics) {
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));

        std::vector<std::promise<std::pair<std::string, std::string>>> msgReceived(2);
        uint32_t msgNumber = 0;

        setEventListener([&](const std::string& event, const std::string& eventJson, YandexIO::ITelemetry::Flags /*flags*/) {
            if (event == "http_request") {
                return; // skip http client metrics
            }
            if (msgNumber < msgReceived.size()) {
                msgReceived[msgNumber++].set_value({event, eventJson});
            }
        });

        updater_->start();

        waitUntil([&]() {
            return (fileExists(appliedUpdateFileName_) &&
                    getFileContent(appliedUpdateFileName_) == updateData_);
        });

        std::vector<std::pair<std::string, std::string>> metricaMessages;
        for (auto& msgPromise : msgReceived) {
            metricaMessages.push_back(msgPromise.get_future().get());
        }

        UNIT_ASSERT_VALUES_EQUAL(metricaMessages.at(1).first, "updateDownloadComplete");

        const Json::Value resumeMessageBody = parseJson(metricaMessages.at(1).second);
        UNIT_ASSERT_VALUES_EQUAL(resumeMessageBody["toVersion"].asString(), updateVersion_);
        UNIT_ASSERT_VALUES_EQUAL(resumeMessageBody["fromVersion"].asString(), testVersion_);
        UNIT_ASSERT(resumeMessageBody.isMember("durationMs"));
        UNIT_ASSERT(resumeMessageBody["durationMs"].asInt64() >= 0);
    }

    Y_UNIT_TEST_F(testUpdaterSendsMetricForDownloadResume, FixtureMetrics) {
        std::vector<std::promise<std::pair<std::string, std::string>>> msgReceived(1);
        uint32_t msgNumber = 0;
        setEventListener([&](const std::string& event, const std::string& eventJson, YandexIO::ITelemetry::Flags /*flags*/) {
            if (event == "http_request") {
                return; // skip http client metrics
            }
            if (msgNumber < msgReceived.size()) {
                msgReceived[msgNumber++].set_value({event, eventJson});
            }
        });

        Json::Value& updatesdConfig = getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"];
        const std::string updatesDir = updatesdConfig["updatesDir"].asString();
        const std::string updatesExt = updatesdConfig["updatesExt"].asString();
        const std::string updateStorePath = updatesDir + "/update_" + updateVersion_ + updatesExt;

        // simulate update file left after previous attempt
        std::ofstream storedUpdate(updateStorePath);
        storedUpdate << "Some junk";
        storedUpdate.close();

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));

        updater_->start();

        waitUntil([&]() {
            return (fileExists(appliedUpdateFileName_) &&
                    getFileContent(appliedUpdateFileName_) == updateData_);
        });

        std::vector<std::pair<std::string, std::string>> metricaMessages;
        for (auto& msgPromise : msgReceived) {
            metricaMessages.push_back(msgPromise.get_future().get());
        }

        UNIT_ASSERT_VALUES_EQUAL(metricaMessages.at(0).first, "updateDownloadResume");

        const Json::Value metricaMessageBody = parseJson(metricaMessages.at(0).second);
        UNIT_ASSERT_VALUES_EQUAL(metricaMessageBody["toVersion"].asString(), updateVersion_);
        UNIT_ASSERT_VALUES_EQUAL(metricaMessageBody["fromVersion"].asString(), testVersion_);
    }

    Y_UNIT_TEST_F(testUpdaterSendsMetricForDownloadError, FixtureMetrics) {
        std::vector<std::promise<std::pair<std::string, std::string>>> msgReceived(2);
        uint32_t msgNumber = 0;
        setEventListener([&](const std::string& event, const std::string& eventJson, YandexIO::ITelemetry::Flags /*flags*/) {
            if (event == "http_request") {
                return; // skipp http client metrics
            }
            if (msgNumber < msgReceived.size()) {
                msgReceived[msgNumber++].set_value({event, eventJson});
            }
        });

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));

        mockUpdatesEndpoint_.onHandlePayload = [=](const TestHttpServer::Headers& /*headers*/,
                                                   const std::string& /*payload*/,
                                                   TestHttpServer::HttpConnection& handler) {
            handler.doError("something something");
        };

        updater_->start();

        std::vector<std::pair<std::string, std::string>> messages;
        for (auto& msgPromise : msgReceived) {
            messages.push_back(msgPromise.get_future().get());
        }
        auto& metricaMessage = messages.at(1);

        UNIT_ASSERT_VALUES_EQUAL(metricaMessage.first, "updateDownloadError");

        const Json::Value metricaMessageBody = parseJson(metricaMessage.second);
        UNIT_ASSERT_VALUES_EQUAL(metricaMessageBody["toVersion"].asString(), updateVersion_);
        UNIT_ASSERT_VALUES_EQUAL(metricaMessageBody["fromVersion"].asString(), testVersion_);
    }

    Y_UNIT_TEST_F(testUpdaterSendsMetricForSuccessfulUpdateApply, FixtureMetrics) {
        std::vector<std::promise<std::pair<std::string, std::string>>> msgReceivedBeforeReboot(2);
        std::promise<std::pair<std::string, std::string>> msgReceivedAfterReboot;
        int msgNumber = 0;
        setEventListener([&](const std::string& event, const std::string& eventJson, YandexIO::ITelemetry::Flags /*flags*/) {
            if (event == "http_request") {
                return; // skipp http client metrics
            }
            if (msgNumber < 2) {
                msgReceivedBeforeReboot[msgNumber].set_value({event, eventJson});
            } else if (msgNumber == 2) {
                msgReceivedAfterReboot.set_value({event, eventJson});
            }
            msgNumber++;
        });

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));

        updater_->start();

        for (auto& msgPromise : msgReceivedBeforeReboot) {
            msgPromise.get_future().get();
        }

        updater_.reset();

        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard_);
        config["common"]["softwareVersion"] = updateVersion_;

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        updater_->start();

        auto afterRebootMessage = msgReceivedAfterReboot.get_future().get();

        UNIT_ASSERT_VALUES_EQUAL(afterRebootMessage.first, "updateApplySuccess");

        const Json::Value metricaMessageBody = parseJson(afterRebootMessage.second);
        UNIT_ASSERT_VALUES_EQUAL(metricaMessageBody["toVersion"].asString(), updateVersion_);
        UNIT_ASSERT_VALUES_EQUAL(metricaMessageBody["fromVersion"].asString(), testVersion_);

        setEventListener(nullptr);
    }

    Y_UNIT_TEST_F(testUpdaterSendsMetricForNotSuccessfulUpdateApply, FixtureMetrics) {
        std::vector<std::promise<std::pair<std::string, std::string>>> msgReceivedBeforeReboot(2);
        std::promise<std::pair<std::string, std::string>> msgReceivedAfterReboot;
        int msgNumber = 0;
        setEventListener([&](const std::string& event, const std::string& eventJson, YandexIO::ITelemetry::Flags /*flags*/) {
            if (event == "http_request") {
                return; // skipp http client metrics
            }
            if (msgNumber < 2) {
                msgReceivedBeforeReboot[msgNumber].set_value({event, eventJson});
            } else if (msgNumber == 2) {
                msgReceivedAfterReboot.set_value({event, eventJson});
            }
            msgNumber++;
        });

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));

        updater_->start();

        for (auto& msgPromise : msgReceivedBeforeReboot) {
            msgPromise.get_future().get();
        }

        updater_.reset();

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        updater_->start();

        auto afterRebootMessage = msgReceivedAfterReboot.get_future().get();

        UNIT_ASSERT_VALUES_EQUAL(afterRebootMessage.first, "updateApplyFailure");

        const Json::Value metricaMessageBody = parseJson(afterRebootMessage.second);
        UNIT_ASSERT_VALUES_EQUAL(metricaMessageBody["toVersion"].asString(), updateVersion_);
        UNIT_ASSERT_VALUES_EQUAL(metricaMessageBody["fromVersion"].asString(), testVersion_);

        setEventListener(nullptr);
    }

    Y_UNIT_TEST_F(testUpdaterUpdateState, Fixture)
    {
        getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"]["checkPeriodMs"] = 100;
        getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"]["criticalCheckPeriodMs"] = 100;

        TestableUpdater* const updater_Ptr = new TestableUpdater(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        updater_.reset(updater_Ptr);

        std::atomic_bool gotNoneState{false};
        std::atomic_bool gotDownloadingState{false};
        std::atomic_bool gotApplyingState{false};
        std::vector<int> downloadProgress;

        auto updatesdConnector = createIpcConnectorForTests("updatesd");
        updatesdConnector->setMessageHandler([&](const auto& msg) {
            if (msg->has_update_state()) {
                if (msg->update_state().state() == UpdateState_State::UpdateState_State_NONE) {
                    gotNoneState = true;
                } else if (msg->update_state().state() == UpdateState_State::UpdateState_State_DOWNLOADING) {
                    gotDownloadingState = true;
                    downloadProgress.push_back(msg->update_state().download_progress());
                } else if (msg->update_state().state() == UpdateState_State::UpdateState_State_APPLYING) {
                    gotApplyingState = true;
                }
            }
        });
        updatesdConnector->connectToService();
        /* set up not critical update in response */
        isUpdateCritical_ = false;

        updater_->start();

        ioHub_->waitConnection();
        ioHub_->setTimezone(timezone_);

        updater_->waitUntilTimezoneReceived(timezone_.timezoneOffsetSec / 3600);

        waitUntil([&gotNoneState]() {
            return gotNoneState.load();
        });

        auto nowMidnight = getStartOfDayUTC(std::chrono::system_clock::now());
        /* Set current timestamp for updater_, so it will start to download update */
        updater_Ptr->currentTimestamp =
            nowMidnight + std::chrono::hours(1) + std::chrono::seconds(updater_->getRandomWaitSeconds() + 1);

        /* Make sure that updatesd start to download OTA */
        waitUntil([&gotDownloadingState]() {
            return gotDownloadingState.load();
        });

        /* Make sure that updatesd start to apply OTA */
        waitUntil([&gotApplyingState]() {
            return gotApplyingState.load();
        });

        /* Make Sure that updatesd send progress [0 .. 100] */
        UNIT_ASSERT(!downloadProgress.empty());
        for (size_t i = 0; i < downloadProgress.size() - 1; ++i) {
            UNIT_ASSERT_GE(downloadProgress[i + 1], downloadProgress[i]);
        }
        UNIT_ASSERT_VALUES_EQUAL(downloadProgress.back(), 100);
    }

    Y_UNIT_TEST_F(testUpdaterProhibition, Fixture)
    {
        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard_);
        config["updatesd"]["updateAllowedAtStartForAll"] = false;
        config["updatesd"]["updateAllowedAtStartForCritical"] = true;
        config["updatesd"]["updateAllowedAtStart"] = false;

        TestableUpdater* const updater_Ptr = new TestableUpdater(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        // Pass ownership of updater_Ptr to updater_
        updater_.reset(updater_Ptr);
        /* Set current timestamp for updater_, so it will start to download update */
        auto nowMidnight = getStartOfDayUTC(std::chrono::system_clock::now());
        updater_Ptr->currentTimestamp =
            nowMidnight + std::chrono::hours(1) + std::chrono::seconds(updater_->getRandomWaitSeconds() + 1);

        // Update is not critical, it mustn't be allowed at start
        isUpdateCritical_ = false;
        updater_->start();

        ioHub_->waitConnection();
        ioHub_->setTimezone(timezone_);

        TestHttpServer::Headers backendHeader;

        // Update prohibited, we get header, see it's not crit and not download it
        backendHeader = backendRequestHeaderPromise_.get_future().get();
        TestHttpServer::Headers updatesHeader;
        const auto status = updatesRequestHeaderPromise_.get_future().wait_for(std::chrono::milliseconds(3000));
        UNIT_ASSERT(status == std::future_status::timeout);

        // Reset promises so future will not be retrieved twice
        resetUpdatesPromise();
        resetBackendPromise();

        /* Test the fact that YandexIO SDK break in when Updatesd already exist and do something */
        ioHub_->setAllowUpdate(true, true);
        // Now non-critical updates are allowed

        backendHeader = backendRequestHeaderPromise_.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.verb, "GET");
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.resource, "/check_updates");
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.queryParams.getValue("version"), getDeviceForTests()->softwareVersion());
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.queryParams.getValue("platform"), getDeviceForTests()->configuration()->getDeviceType());

        updatesHeader = updatesRequestHeaderPromise_.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.verb, "GET");
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.resource, "/yandexstation/ota/user/f0ed9b20-0220-4b3f-8f17-b8be535d4ded/quasar-2.3.1.6.406275671.20190402.zip");

        waitUntil([&]() {
            return (fileExists(appliedUpdateFileName_) &&
                    getFileContent(appliedUpdateFileName_) == updateData_);
        });

        UNIT_ASSERT_VALUES_EQUAL(getFileContent(appliedUpdateFileName_), updateData_);
    }

    Y_UNIT_TEST_F(testUpdaterProhibitionAfterDownload, Fixture)
    {
        mockUpdatesEndpoint_.onHandlePayload = [&](const TestHttpServer::Headers& header,
                                                   const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            if (setUpdatesRequestHeaderPromise_) // Prevent future already satisfied error
            {
                updatesRequestHeaderPromise_.set_value(header);
                setUpdatesRequestHeaderPromise_ = false;
                // Prohibit all updates, even critical
                ioHub_->setAllowUpdate(false, false);

                // Wait for state update. If we won't wait there can be case when zip will be downloaded before prohibition
                updater_->waitForAllowState(false, false);
            }

            handler.doReplay(200, "application/zip", updateData_);
        };

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        // Crit update is not allowed, but all updates are allowed, so crit update is allowed implicitly
        ioHub_->waitConnection();
        ioHub_->setAllowUpdate(true, false);

        updater_->start();

        TestHttpServer::Headers backendHeader;

        backendHeader = backendRequestHeaderPromise_.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.verb, "GET");
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.resource, "/check_updates");
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.queryParams.getValue("device_id"), getDeviceForTests()->deviceId());
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.queryParams.getValue("version"), getDeviceForTests()->softwareVersion());
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.queryParams.getValue("platform"), getDeviceForTests()->configuration()->getDeviceType());

        auto updatesHeader = updatesRequestHeaderPromise_.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.verb, "GET");
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.resource, "/yandexstation/ota/user/f0ed9b20-0220-4b3f-8f17-b8be535d4ded/quasar-2.3.1.6.406275671.20190402.zip");

        /* Make sure that ota won't applied */
        UNIT_ASSERT(!doUntil([&]() {
            return (fileExists(appliedUpdateFileName_) &&
                    getFileContent(appliedUpdateFileName_) == updateData_);
        }, 1500));

        // Crit updates are allowed
        ioHub_->setAllowUpdate(false, true);

        waitUntil([&]() {
            return (fileExists(appliedUpdateFileName_) &&
                    getFileContent(appliedUpdateFileName_) == updateData_);
        });

        UNIT_ASSERT_VALUES_EQUAL(getFileContent(appliedUpdateFileName_), updateData_);
    }

    Y_UNIT_TEST_F(testCheckUpdatesNoAuthorizationHeader, Fixture)
    {
        hasUpdate_ = false;
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        updater_->start();

        auto backendHeader = backendRequestHeaderPromise_.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.verb, "GET");
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.resource, "/check_updates");

        /* Should not contain authorization header because check_updates doesn't require OAuth */
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.tryGetHeader("authorization"), "");
        /* Only 3 default & 3 signature headers should be set up */
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.headers.size(), 3 + 3);

        UNIT_ASSERT(backendHeader.getHeader("accept") != "");
        UNIT_ASSERT(backendHeader.getHeader("host") != "");
        UNIT_ASSERT(backendHeader.getHeader("user-agent") != "");

        UNIT_ASSERT(backendHeader.getHeader("x-quasar-signature") != "");
        UNIT_ASSERT(backendHeader.getHeader("x-quasar-signature-version") != "");
        UNIT_ASSERT(backendHeader.getHeader("x-quasar-signature-cryptography") != "");
    }

    Y_UNIT_TEST_F(testCheckUpdatesSignature, Fixture)
    {
        hasUpdate_ = false;
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        updater_->start();

        auto backendHeader = backendRequestHeaderPromise_.get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.verb, "GET");
        UNIT_ASSERT_VALUES_EQUAL(backendHeader.resource, "/check_updates");

        UNIT_ASSERT_VALUES_EQUAL(backendHeader.getHeader("x-quasar-signature-version"), "2");

        Cryptography cryptography;
        cryptography.loadPublicKey(std::string(ArcadiaSourceRoot()) + "/yandex_io/misc/cryptography/public.pem");

        UNIT_ASSERT(cryptography.checkSignature(backendHeader.query, base64Decode(urlDecode(backendHeader.getHeader("x-quasar-signature")))));
    }

    Y_UNIT_TEST_F(testDownloadNoAuthorization, Fixture)
    {
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        updater_->start();

        auto updatesHeader = updatesRequestHeaderPromise_.get_future().get();

        /* Should not contain authorization header because check_updates doesn't require OAuth */
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.tryGetHeader("authorization"), "");
        /* Only 3 default headers should be set up */
        UNIT_ASSERT_VALUES_EQUAL(updatesHeader.headers.size(), 3);

        /* Check that updater do not set up any unexpected headers */
        UNIT_ASSERT(updatesHeader.getHeader("accept") != "");
        UNIT_ASSERT(updatesHeader.getHeader("host") != "");
        UNIT_ASSERT(updatesHeader.getHeader("user-agent") != "");
    }

    Y_UNIT_TEST_F(testCheckUpdatesMessageHandle, Fixture)
    {
        {
            /* Set up very large delay btw updates, so Updatesd will wake up by toggles only */
            Json::Value& updatesdConfig = getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"];
            updatesdConfig["updatesd"]["checkPeriodMs"] = 60 * 60 * 1000;
            updatesdConfig["updatesd"]["criticalCheckPeriodMs"] = 60 * 60 * 1000;
        }
        /* set up not critical update in response */
        isUpdateCritical_ = false;

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        SteadyConditionVariable condVar;
        std::mutex testMutex;
        bool hasNoCritical{false};
        bool hasCritical{false};

        auto updatesdConnector = createIpcConnectorForTests("updatesd");
        updatesdConnector->setMessageHandler([&](const auto& msg) {
            std::lock_guard<std::mutex> guard(testMutex);
            if (msg->has_no_critical_update_found()) {
                hasNoCritical = true;
            } else if (msg->has_start_critical_update()) {
                hasCritical = true;
            }
            condVar.notify_one();
        });
        updatesdConnector->connectToService();
        updatesdConnector->waitUntilConnected();

        updater_->start();

        /* Wait until Updatesd will check backend and will see that there is no critical update */
        std::unique_lock<std::mutex> lock(testMutex);
        condVar.wait(lock, [&]() {
            return hasNoCritical;
        });

        /* Set up critical update so updatesd will receive Critical update in next check */
        isUpdateCritical_ = true;
        /* Send CheckUpdates message, so Updater will visit check_updates backend handler immediately */
        QuasarMessage message;
        message.mutable_check_updates();
        updatesdConnector->sendMessage(QuasarMessage{message});

        condVar.wait(lock, [&]() {
            return hasCritical;
        });

        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(testCheckUpdaterSendReadyToApplyUpdateMessage, Fixture)
    {
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));
        SteadyConditionVariable condVar;
        std::mutex testMutex;
        bool hasReadyToApplyUpdate{false};

        auto updatesdConnector = createIpcConnectorForTests("updatesd");
        updatesdConnector->setMessageHandler([&](const auto& msg) {
            std::lock_guard<std::mutex> guard(testMutex);
            if (msg->has_ready_to_apply_update()) {
                hasReadyToApplyUpdate = true;
                condVar.notify_one();
            }
        });
        updatesdConnector->connectToService();
        updatesdConnector->waitUntilConnected();

        updater_->start();

        /* Updater should have update to apply, so wait until receive ready_to_apply_update messages */
        std::unique_lock<std::mutex> lock(testMutex);
        condVar.wait(lock, [&]() {
            return hasReadyToApplyUpdate;
        });

        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(testUpdaterConfirmOta, Fixture)
    {
        auto downloadPromise = std::make_shared<std::promise<void>>();
        mockUpdatesEndpoint_.onHandlePayload = [downloadPromise, this](const TestHttpServer::Headers& /*headers*/,
                                                                       const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            downloadPromise->set_value();
            handler.doReplay(200, "application/zip", updateData_);
        };

        /* Set up very large Soft and Hard timeouts for ota confirmation, so Updater won't visit mockUpdatesEndpoint_
         * without confirm
         */
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::hours(1), std::chrono::hours(1));
        SteadyConditionVariable condVar;
        std::mutex testMutex;
        bool hasReadyToApplyUpdate{false};

        auto updatesdConnector = createIpcConnectorForTests("updatesd");
        updatesdConnector->setMessageHandler([&](const auto& msg) {
            std::lock_guard<std::mutex> guard(testMutex);
            if (msg->has_ready_to_apply_update()) {
                hasReadyToApplyUpdate = true;
                condVar.notify_one();
            }
        });
        updatesdConnector->connectToService();
        updatesdConnector->waitUntilConnected();

        updater_->start();

        /* Updater should have update to apply, so wait until receive ready_to_apply_update messages */
        std::unique_lock<std::mutex> lock(testMutex);
        condVar.wait(lock, [&]() {
            return hasReadyToApplyUpdate;
        });

        auto downloadFuture = downloadPromise->get_future();
        /* Make sure that Updater do not visit backend to download ota without confirmation */
        auto status = downloadFuture.wait_for(std::chrono::seconds(5));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);

        /* Confirm OTA */
        QuasarMessage message;
        message.mutable_confirm_update_apply();
        updatesdConnector->sendMessage(QuasarMessage{message});

        /* Make sure that updatesd will download ota after confirmation */
        downloadFuture.get();
    }

    Y_UNIT_TEST_F(testUpdaterHardDeadline, Fixture)
    {
        auto downloadPromise = std::make_shared<std::promise<void>>();
        mockUpdatesEndpoint_.onHandlePayload = [downloadPromise, this](const TestHttpServer::Headers& /*headers*/,
                                                                       const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            downloadPromise->set_value();
            handler.doReplay(200, "application/zip", updateData_);
        };

        /* In this test set up  quite big Soft Timeout and run postponesWorker that will send postpone messages
         * in busyloop. Updater should download OTA anyway because HardDeadline should exceed
         */
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(10), std::chrono::seconds(35));

        SteadyConditionVariable condVar;
        std::mutex testMutex;
        bool hasReadyToApplyUpdate{false};

        auto updatesdConnector = createIpcConnectorForTests("updatesd");
        updatesdConnector->setMessageHandler([&](const auto& msg) {
            std::lock_guard<std::mutex> guard(testMutex);
            if (msg->has_ready_to_apply_update()) {
                hasReadyToApplyUpdate = true;
                condVar.notify_one();
            }
        });
        updatesdConnector->connectToService();
        updatesdConnector->waitUntilConnected();

        std::atomic_bool stopped{false};
        auto postponesWorker = std::thread([&]() {
            /* Simple busy loop which always Ping Updater with postpone messages */
            while (!stopped) {
                QuasarMessage message;
                message.mutable_postpone_update_apply();
                updatesdConnector->sendMessage(QuasarMessage{message});
            }
        });

        updater_->start();

        /* Updater should have update to apply, so wait until receive ready_to_apply_update message */
        std::unique_lock<std::mutex> lock(testMutex);
        condVar.wait(lock, [&]() {
            return hasReadyToApplyUpdate;
        });

        auto downloadFuture = downloadPromise->get_future();
        /* Make sure that Updater do not visit backend to download ota without confirmation */
        auto status = downloadFuture.wait_for(std::chrono::seconds(20));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);

        /* Make sure that updatesd will download ota because of Hard timeout */
        downloadFuture.get();
        stopped = true;
        postponesWorker.join();
    }

    Y_UNIT_TEST_F(testUpdaterSoftDeadline, Fixture)
    {
        auto downloadPromise = std::make_shared<std::promise<void>>();
        mockUpdatesEndpoint_.onHandlePayload = [downloadPromise, this](const TestHttpServer::Headers& /*headers*/,
                                                                       const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            downloadPromise->set_value();
            handler.doReplay(200, "application/zip", updateData_);
        };

        /* Set up unreachable hard deadline, and small enough soft deadline. Ota should be downloaded after Soft Deadline */
        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(3), std::chrono::hours(1));

        SteadyConditionVariable condVar;
        std::mutex testMutex;
        bool hasReadyToApplyUpdate{false};

        auto updatesdConnector = createIpcConnectorForTests("updatesd");
        updatesdConnector->setMessageHandler([&](const auto& msg) {
            std::lock_guard<std::mutex> guard(testMutex);
            if (msg->has_ready_to_apply_update()) {
                hasReadyToApplyUpdate = true;
                condVar.notify_one();
            }
        });
        updatesdConnector->connectToService();
        updatesdConnector->waitUntilConnected();

        updater_->start();

        /* Updater should have update to apply, so wait until receive ready_to_apply_update messages */
        std::unique_lock<std::mutex> lock(testMutex);
        condVar.wait(lock, [&]() {
            return hasReadyToApplyUpdate;
        });

        auto downloadFuture = downloadPromise->get_future();

        /* Updater should download OTA without any confirmations because Soft Deadline should exceed */
        downloadFuture.get();
    }

    Y_UNIT_TEST_F(testUpdaterVersionWithSlash, Fixture)
    {
        setUpdateVersion("0.3.1.21.852829949.20201222.yandexmini_2/MP.ENG");

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));

        updater_->start();

        waitUntil([&]() {
            return (fileExists(appliedUpdateFileName_) &&
                    getFileContent(appliedUpdateFileName_) == updateData_);
        });

        UNIT_ASSERT_VALUES_EQUAL(getFileContent(appliedUpdateFileName_), updateData_);
    }

    Y_UNIT_TEST_F(testFailedCrc32Check, Fixture)
    {
        setUpdateVersion("version_1");

        getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"]["checkPeriodMs"] = 100;
        getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"]["criticalCheckPeriodMs"] = 100;
        getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"]["maxAttempts"] = 3;
        getDeviceForTests()->configuration()->getMutableConfig(testGuard_)["updatesd"]["checkCrc32AfterWrite"] = true;

        std::atomic<uint32_t> downloadRequestsCount = 0;
        std::atomic<uint32_t> updateAttempts = 0;

        mockBackend_.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                           const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            Json::Value backendResponse;
            backendResponse["hasUpdate"] = hasUpdate_.load();
            backendResponse["critical"] = isUpdateCritical_.load();
            backendResponse["downloadUrl"] = "http://localhost:" + std::to_string(mockUpdatesEndpoint_.port()) +
                                             "/yandexstation/ota/user/f0ed9b20-0220-4b3f-8f17-b8be535d4ded/quasar-2.3.1.6.406275671.20190402.zip";
            backendResponse["version"] = updateVersion_;
            backendResponse["crc32"] = 42; // Totally wrong value to ensure crc32 check failure

            handler.doReplay(200, "application/json", jsonToString(backendResponse));
        };

        mockUpdatesEndpoint_.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                                   const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            downloadRequestsCount++;
            handler.doReplay(200, "application/zip", updateData_);
        };

        auto updatesdConnector = createIpcConnectorForTests("updatesd");
        updatesdConnector->setMessageHandler([&](const auto& msg) {
            if (msg->has_update_state()) {
                if (msg->update_state().state() == UpdateState_State_NONE) {
                    updateAttempts++;
                }
            }
        });
        updatesdConnector->connectToService();

        updater_ = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(2));

        updater_->start();

        waitUntil([&]() {
            return updateAttempts >= 5;
        });

        UNIT_ASSERT_EQUAL(downloadRequestsCount, 3);

        downloadRequestsCount = 0;
        setUpdateVersion("version_2");

        /* Check that attempts counter is reset when new version is available, and updater tries to download updates again */
        waitUntil([&]() {
            return downloadRequestsCount > 0;
        });
    }
}
