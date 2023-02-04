#include <yandex_io/services/updatesd/updater.h>

#include <yandex_io/libs/base/crc32.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <chrono>
#include <optional>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {
    class Fixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard_);

            mockIoHub_ = createIpcServerForTests("iohub_services");
            mockIoHub_->listenService();

            mockBackend_.onHandlePayload = [this](const TestHttpServer::Headers& header, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
                const auto otaResource = "/download/ota.zip";
                if (header.resource == checkUpdatesHandle_) {
                    Json::Value backendResponse;
                    backendResponse["hasUpdate"] = true;
                    backendResponse["critical"] = isCritical_.load();
                    backendResponse["downloadUrl"] = "http://localhost:" + std::to_string(mockBackend_.port()) + otaResource;
                    backendResponse["version"] = "1";
                    backendResponse["crc32"] = getCrc32(updateData_);

                    handler.doReplay(200, "application/json", jsonToString(backendResponse));
                } else if (header.resource == otaResource) {
                    const auto code = failDownload_.load() ? 500 : 200;
                    handler.doReplay(code, "application/zip", updateData_);
                }
            };

            mockBackend_.start(getPort());

            config["common"]["backendUrl"] = "http://localhost:" + std::to_string(mockBackend_.port());
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

    protected:
        void testCase(Updater* updater, bool isCritical, bool failDownload) {
            SteadyConditionVariable condVar;
            std::mutex testMutex;
            std::optional<proto::UpdateState> noneState;
            std::optional<proto::UpdateState> downloadState;
            std::optional<proto::UpdateState> applyingState;

            auto updatesdConnector = createIpcConnectorForTests("updatesd");
            updatesdConnector->setMessageHandler([&](const auto& msg) {
                std::lock_guard<std::mutex> guard(testMutex);
                if (msg->has_update_state()) {
                    switch (msg->update_state().state()) {
                        case proto::UpdateState::NONE: {
                            YIO_LOG_INFO("Got NONE state");
                            noneState = msg->update_state();
                            break;
                        }
                        case proto::UpdateState::DOWNLOADING: {
                            YIO_LOG_INFO("Got DOWNLOADING state");
                            downloadState = msg->update_state();
                            break;
                        }
                        case proto::UpdateState::APPLYING: {
                            YIO_LOG_INFO("Got APPLYING state");
                            applyingState = msg->update_state();
                            break;
                        }
                        default: {
                            UNIT_FAIL("should not happen");
                            break;
                        }
                    }
                    condVar.notify_one();
                }
            });
            updatesdConnector->connectToService();
            updatesdConnector->waitUntilConnected();

            /* Updater should have update to apply, so wait until receive ready_to_apply_update messages */
            std::unique_lock<std::mutex> lock(testMutex);
            condVar.wait(lock, [&]() {
                return noneState.has_value();
            });
            UNIT_ASSERT_EQUAL(noneState.value().state(), proto::UpdateState::NONE);
            noneState.reset();

            updater->start();

            condVar.wait(lock, [&]() {
                return downloadState.has_value();
            });
            UNIT_ASSERT_VALUES_EQUAL(downloadState.value().is_critical(), isCritical);
            UNIT_ASSERT(downloadState.value().has_download_progress());
            UNIT_ASSERT_EQUAL(downloadState.value().state(), proto::UpdateState::DOWNLOADING);

            if (!failDownload) {
                /* If test won't fail download -> device should start apply ota */
                condVar.wait(lock, [&]() {
                    return applyingState.has_value();
                });

                UNIT_ASSERT_VALUES_EQUAL(applyingState.value().is_critical(), isCritical);
                UNIT_ASSERT_EQUAL(applyingState.value().state(), proto::UpdateState::APPLYING);
            } else {
                /* If download fails -> should fallback to NONE state */
                noneState.reset();
                condVar.wait(lock, [&]() {
                    return noneState.has_value();
                });
                UNIT_ASSERT_EQUAL(noneState.value().state(), proto::UpdateState::NONE);
            }
        }

    protected:
        std::atomic_bool failDownload_{false};
        std::atomic_bool isCritical_{true};
        YandexIO::Configuration::TestGuard testGuard_;

    private:
        const std::string updateData_ = "This is an update";
        const std::string checkUpdatesHandle_ = "/check_updates";
        std::shared_ptr<ipc::IServer> mockIoHub_;
        TestHttpServer mockBackend_;
    };

} /* anonymous namespace */

Y_UNIT_TEST_SUITE_F(TestUpdateState, Fixture) {
    Y_UNIT_TEST(testUpdateStateCriticalOta) {
        auto updater = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(1));
        isCritical_ = true;
        failDownload_ = false;
        testCase(updater.get(), isCritical_.load(), failDownload_.load());
    }

    Y_UNIT_TEST(testUpdateStateNotCriticalOta) {
        {
            /* Set up time range so non critical ota will be downloaded anyway */
            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard_);
            config["updatesd"]["minUpdateHour"] = 0;
            config["updatesd"]["maxUpdateHour"] = 23;
        }

        auto updater = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(1));
        isCritical_ = false;
        failDownload_ = false;
        testCase(updater.get(), isCritical_.load(), failDownload_.load());
    }

    Y_UNIT_TEST(testUpdateStateFallbackToNone) {
        auto updater = std::make_unique<Updater>(getDeviceForTests(), ipcFactoryForTests(), std::chrono::seconds(1), std::chrono::seconds(1));
        isCritical_ = true;
        failDownload_ = true;

        testCase(updater.get(), isCritical_.load(), failDownload_.load());
    }
}
