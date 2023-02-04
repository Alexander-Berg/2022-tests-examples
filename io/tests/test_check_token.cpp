#include <yandex_io/services/firstrund/check_token.h>

#include <yandex_io/interfaces/auth/mock/auth_provider.h>
#include <yandex_io/interfaces/device_state/mock/device_state_provider.h>
#include <yandex_io/libs/base/persistent_file.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/jwt/jwt.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <json/value.h>

#include <library/cpp/testing/unittest/registar.h>

#include <util/folder/path.h>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {
    struct BackendResponse {
        bool valid_;
        std::string status_;
        int httpStatusCode_;

        BackendResponse(bool valid, std::string status, int httpStatusCode)
            : valid_(valid)
            , status_(status)
            , httpStatusCode_(httpStatusCode)
        {
        }

        std::string toJson() const {
            Json::Value result;
            result["status"] = status_;
            result["valid"] = valid_;

            return jsonToString(result);
        }
    };

    struct CheckTokenFixture: public QuasarUnitTestFixture {
        TestHttpServer mockBackend;
        YandexIO::Configuration::TestGuard configGuard;
        std::shared_ptr<mock::AuthProvider> mockAuthProvider;
        std::shared_ptr<mock::DeviceStateProvider> mockDeviceStateProvider;

        TFsPath deviceIdFileName;

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            deviceIdFileName = JoinFsPaths(tryGetRamDrivePath(), "temp-" + makeUUID(), "device.id");

            const int backendPort = mockBackend.start(getPort());
            YIO_LOG_DEBUG("Backend port: " + std::to_string(backendPort));

            mockAuthProvider = std::make_shared<mock::AuthProvider>();
            mockAuthProvider->setOwner(
                AuthInfo2{
                    .source = AuthInfo2::Source::AUTHD,
                    .authToken = "$authToken$",
                    .passportUid = "123",
                    .tag = 1600000000,
                });
            mockDeviceStateProvider = std::make_shared<mock::DeviceStateProvider>();
            mockDeviceStateProvider->setDeviceState(mock::defaultDeviceState());

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(configGuard);
            config["common"]["backendUrl"] = "http://localhost:" + std::to_string(backendPort);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            const auto dir = getDirectoryName(deviceIdFileName.GetPath());
            TFsPath(dir).ForceDelete();

            Base::TearDown(context);
        }

        void setBackendResponse() {
            mockBackend.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                              const std::string& /*payload*/,
                                              TestHttpServer::HttpConnection& handler) {
                BackendResponse okResponse(true, "ok", 200);
                handler.doReplay(200, "application/json", okResponse.toJson());
            };
        }

        void setDeviceId(const std::string& deviceId) {
            auto& config = getDeviceForTests()->configuration()->getMutableConfig(configGuard);
            config["common"]["deviceIdFileName"] = deviceIdFileName.GetPath();

            deviceIdFileName.Parent().MkDirs();

            PersistentFile deviceIdFile(deviceIdFileName.GetPath(), PersistentFile::Mode::TRUNCATE);
            deviceIdFile.write(deviceId);
        }
    };

} // namespace

Y_UNIT_TEST_SUITE_F(CheckTokenTest, CheckTokenFixture) {
    Y_UNIT_TEST(shouldReturnFalse_whenTokenIsExpired) {
        // Arrange
        CheckToken checkToken_(getDeviceForTests(), mockAuthProvider, mockDeviceStateProvider, std::chrono::seconds(60));

        setDeviceId("DEVICE_ID");

        const long future = time(nullptr) - 10 * 1000;
        const std::unordered_map<std::string, JwtValue> values{
            {"aud", "glagol"},
            {"exp", future},
            {"iss", "quasar-backend"},
            {"plt", "platform"},
            {"sub", "DEVICE_ID"}};

        const std::string token = encodeJWT(values);

        // Act
        const bool result = checkToken_.check(token);

        // Assert
        UNIT_ASSERT_VALUES_EQUAL(result, false);
    }

    Y_UNIT_TEST(shouldReturnFalse_whenDeviceIdDoesntMatch) {
        // Arrange
        CheckToken checkToken_(getDeviceForTests(), mockAuthProvider, mockDeviceStateProvider, std::chrono::seconds(60));

        setDeviceId("DEVICE_ID");

        const long future = time(nullptr) + 10 * 1000;
        const std::unordered_map<std::string, JwtValue> values{
            {"aud", "glagol"},
            {"exp", future},
            {"iss", "quasar-backend"},
            {"plt", "platform"},
            {"sub", "DEFINITELY_NOT_DEVICE_ID"}};

        const std::string token = encodeJWT(values);

        YIO_LOG_DEBUG("enter check token");
        // Act
        const bool result = checkToken_.check(token);

        YIO_LOG_DEBUG("result returned:" + std::to_string(result));

        // Assert
        UNIT_ASSERT_VALUES_EQUAL(result, false);
        YIO_LOG_DEBUG("Assertion ok");
    }

    Y_UNIT_TEST(shouldReturnFalse_whenTokenCannotBeParsed) {
        // Arrange
        setBackendResponse();
        CheckToken checkToken_(getDeviceForTests(), mockAuthProvider, mockDeviceStateProvider, std::chrono::seconds(60));

        // Act
        bool result = checkToken_.check("abcdef");

        // Assert
        UNIT_ASSERT_VALUES_EQUAL(result, false);
    }

    Y_UNIT_TEST(shouldReturnTrue_whenEverythingIsOk) {
        // Arrange
        setBackendResponse();
        CheckToken checkToken_(getDeviceForTests(), mockAuthProvider, mockDeviceStateProvider, std::chrono::seconds(60));
        setDeviceId("DEVICE_ID");

        const auto future = time(nullptr) + 100;

        const std::unordered_map<std::string, JwtValue> values{
            {"aud", "glagol"},
            {"exp", future},
            {"iss", "quasar-backend"},
            {"plt", "platform"},
            {"sub", "DEVICE_ID"}};
        const std::string token = encodeJWT(values);

        // Act
        bool result = checkToken_.check(token);

        // Assert
        UNIT_ASSERT_VALUES_EQUAL(result, true);
    }
}
