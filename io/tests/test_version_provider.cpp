#include <yandex_io/modules/version_provider/version_provider.h>

#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/system/tempfile.h>

#include <fstream>
#include <future>
#include <memory>

using namespace quasar;
using namespace YandexIO;

namespace {
    class Fixture: public QuasarUnitTestFixture {
    public:
        TestUtils::TestHttpServer mockBackend;
        std::shared_ptr<mock::AuthProvider> mockAuthProvider;
        TTempFile tmpVersionSendGuardFile{MakeTempName()};
        TTempFile tmpCachedVersionFileName{MakeTempName()};
        VersionProvider::Settings settings;
        int port;

        Fixture() {
            NFs::Remove(tmpVersionSendGuardFile.Name());
            NFs::Remove(tmpCachedVersionFileName.Name());

            int port = getPort();
            settings.platform = "platform";
            settings.deviceID = "1234";
            settings.backendUrl = "http://localhost:" + std::to_string(port);
            settings.softwareVersion = "yandexio";
            settings.versionSendGuardFile = tmpVersionSendGuardFile.Name();
            settings.versionSendBaseSleepFor = std::chrono::milliseconds(1000);
            settings.versionSendMaxSleepFor = std::chrono::milliseconds(3000);
            settings.cachedVersionFileName = tmpCachedVersionFileName.Name();

            YIO_LOG_INFO("versionSendGuardFile = " << settings.versionSendGuardFile);
            YIO_LOG_INFO("cachedVersionFileName = " << settings.cachedVersionFileName);

            mockAuthProvider = std::make_shared<mock::AuthProvider>();
            mockAuthProvider->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = "AuthTokenFor_versionProviderTest",
                    .passportUid = "123",
                    .tag = 1,
                });
            mockBackend.start(port);
        }
    };

} // namespace

Y_UNIT_TEST_SUITE_F(VersionProviderTest, Fixture) {
    Y_UNIT_TEST(testSuccessSend) {
        std::promise<void> p;
        auto f = p.get_future();
        mockBackend.onHandlePayload = [&](const TestUtils::TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestUtils::TestHttpServer::HttpConnection& handler) {
            p.set_value();
            handler.doReplay(200, "application/json", R"({"status": "ok"})");
        };
        VersionProvider versionProvider(mockAuthProvider, getDeviceForTests(), "v1.0", settings);
        versionProvider.provideVersion();
        f.get();
        p = std::promise<void>();
        f = p.get_future();
        versionProvider.provideVersion();
        auto status = f.wait_for(std::chrono::seconds(2));
        UNIT_ASSERT_EQUAL(status, std::future_status::timeout);
    }

    Y_UNIT_TEST(testCaching) {
        std::promise<void> p;
        auto f = p.get_future();
        mockBackend.onHandlePayload = [&](const TestUtils::TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestUtils::TestHttpServer::HttpConnection& handler) {
            p.set_value();
            handler.doReplay(200, "application/json", R"({"status": "ok"})");
        };
        VersionProvider versionProvider(mockAuthProvider, getDeviceForTests(), "v2.0", settings);
        versionProvider.provideVersion();
        f.get();
        TestUtils::waitUntil([&]() {
            return fileExists(settings.cachedVersionFileName);
        });
        const auto content = getFileContent(settings.cachedVersionFileName);
        const auto cachedState = parseJson(content);
        UNIT_ASSERT(cachedState.isMember("environmentVersion"));
        UNIT_ASSERT(cachedState.isMember("yandexIOVersion"));
        UNIT_ASSERT_EQUAL(cachedState["environmentVersion"].asString(), "v2.0");
        UNIT_ASSERT_EQUAL(cachedState["yandexIOVersion"].asString(), settings.softwareVersion);
    }

    Y_UNIT_TEST(testBadRequest) {
        constexpr size_t RETRY_COUNT = 3;
        std::mutex mutex;
        std::condition_variable cv;
        size_t counter = 0;
        mockBackend.onHandlePayload = [&](const TestUtils::TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestUtils::TestHttpServer::HttpConnection& handler) {
            std::scoped_lock lock(mutex);
            if (++counter == RETRY_COUNT) {
                handler.doReplay(200, "application/json", R"({"status": "ok"})");
            } else {
                handler.doReplay(500, "application/json", R"({"status": "not ok"})");
            }
            cv.notify_one();
        };
        VersionProvider versionProvider(mockAuthProvider, getDeviceForTests(), "v3.0", settings);
        versionProvider.provideVersion();
        std::unique_lock lock(mutex);
        cv.wait(lock, [&]() {
            return counter == RETRY_COUNT;
        });
        lock.unlock();
        std::this_thread::sleep_for(std::chrono::seconds(5));
        lock.lock();
        UNIT_ASSERT_EQUAL(counter, RETRY_COUNT);
    }
}
