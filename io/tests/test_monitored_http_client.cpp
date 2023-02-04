#include <yandex_io/libs/http_client/http_client.h>

#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>

#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using testing::_;
using TestUtils::TestHttpServer;

namespace {
    std::shared_ptr<YandexIO::IDevice> makeTestDevice(std::shared_ptr<YandexIO::ITelemetry> telemetry) {
        return std::make_shared<YandexIO::Device>(
            QuasarUnitTestFixture::makeTestDeviceId(),
            QuasarUnitTestFixture::makeTestConfiguration(),
            std::move(telemetry),
            QuasarUnitTestFixture::makeTestHAL());
    }

    class TelemetryMock: public NullMetrica {
    public:
        MOCK_METHOD(void, reportEvent, (const std::string&, const std::string&, YandexIO::ITelemetry::Flags), (override));
    };

    void safeCall(std::function<void()> func) {
        try {
            func();
        } catch (...) {
        }
    }

    bool verifyJson(const std::string eventJson, const std::string& method, const std::string& tag, const std::string& clientName, bool codeIsTimeout) {
        const auto json = quasar::parseJson(eventJson);
        if (json["method"].asString() != method) {
            return false;
        }
        if (json["tag"].asString() != tag) {
            return false;
        }
        if (json["client_name"].asString() != clientName) {
            return false;
        }
        if (!json.isMember("code")) {
            return false;
        }
        const auto code = json["code"].asInt();
        if (codeIsTimeout && !(code == 0 || code >= 6000)) { // 6000 is curl error codes
            return false;
        }
        if (!codeIsTimeout) {
            // check it's valid http code
            if (code < 100 || code > 599) {
                return false;
            }
        }
        if (!json.isMember("timing_ms")) {
            return false;
        }
        return true;
    }

    MATCHER_P3(VerifyTimeoutMetric, method, tag, clientName, "description") {
        if (!verifyJson(arg, method, tag, clientName, true)) {
            *result_listener << "Invalid json: " << arg;
            return false;
        }
        return true;
    }

    MATCHER_P3(VerifySuccessMetric, method, tag, clientName, "description") {
        if (!verifyJson(arg, method, tag, clientName, false)) {
            *result_listener << "Invalid json: " << arg;
            return false;
        }
        return true;
    }

} // namespace

Y_UNIT_TEST_SUITE(MonitoredHttpClientTest) {
    Y_UNIT_TEST_F(TestRetries, QuasarUnitTestFixture) {
        const auto url = "http://localhost:" + std::to_string(getPort());
        auto telemetry = std::make_shared<TelemetryMock>();
        auto device = makeTestDevice(telemetry);
        {
            testing::InSequence seq;
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifyTimeoutMetric("GET", "get-tag", "testName"), _)).Times(3);
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifyTimeoutMetric("POST", "post-tag", "testName"), _)).Times(3);
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifyTimeoutMetric("HEAD", "head-tag", "testName"), _)).Times(3);
        }

        HttpClient client{"testName", device};
        client.setRetriesCount(2);
        // ignore exceptions. Actual http request will fail
        safeCall([&]() {
            client.get("get-tag", url);
        });
        safeCall([&]() {
            client.post("post-tag", url, "data");
        });
        safeCall([&]() {
            client.head("head-tag", url);
        });
    }

    Y_UNIT_TEST_F(TestSuccess, QuasarUnitTestFixture) {
        TestHttpServer server;
        server.onHandlePayload = [](const TestHttpServer::Headers& /*unused*/, const std::string& /*unused*/, TestHttpServer::HttpConnection& handler) {
            handler.doReplay(200, "application/json", {});
        };
        const auto url = "http://localhost:" + std::to_string(server.start(getPort()));

        auto telemetry = std::make_shared<TelemetryMock>();
        auto device = makeTestDevice(telemetry);

        {
            testing::InSequence seq;
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifySuccessMetric("GET", "get-tag", "testName"), _)).Times(1);
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifySuccessMetric("POST", "post-tag", "testName"), _)).Times(1);
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifySuccessMetric("HEAD", "head-tag", "testName"), _)).Times(1);
        }

        HttpClient client{"testName", device};
        client.setRetriesCount(2);
        client.get("get-tag", url);
        client.post("post-tag", url, "data");
        client.head("head-tag", url);
    }

    Y_UNIT_TEST_F(TestSuccessAfterRetry, QuasarUnitTestFixture) {
        TestHttpServer server;
        server.onHandlePayload = [post{false}, get{false}, head{false}, val{200}](const TestHttpServer::Headers& header, const std::string& /*unused*/, TestHttpServer::HttpConnection& handler) mutable {
            YIO_LOG_INFO("Request with verb: " << header.verb);
            bool shouldFail = false;
            // Fail 1 request for each verb
            if (header.verb == "POST") {
                if (!std::exchange(post, true)) {
                    shouldFail = true;
                }
            } else if (header.verb == "HEAD") {
                if (!std::exchange(head, true)) {
                    shouldFail = true;
                }
            } else if (header.verb == "GET") {
                if (!std::exchange(get, true)) {
                    shouldFail = true;
                }
            }
            if (shouldFail) {
                YIO_LOG_INFO("Fail verb: " << header.verb);
                handler.close();
            } else {
                YIO_LOG_INFO("Response verb: " << header.verb);
                handler.doReplay(val++, "application/json", {});
            }
        };
        const auto port = getPort();
        // disable keep alive for the sake of this test: test HttpClient retries
        constexpr bool keepAliveEnabled = false;
        server.start(port, keepAliveEnabled);
        const auto url = "http://localhost:" + std::to_string(port);

        auto telemetry = std::make_shared<TelemetryMock>();
        auto device = makeTestDevice(telemetry);

        {
            testing::InSequence seq;
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifyTimeoutMetric("GET", "get-tag", "testName"), _)).Times(1);
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifySuccessMetric("GET", "get-tag", "testName"), _)).Times(1);

            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifyTimeoutMetric("POST", "post-tag", "testName"), _)).Times(1);
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifySuccessMetric("POST", "post-tag", "testName"), _)).Times(1);

            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifyTimeoutMetric("HEAD", "head-tag", "testName"), _)).Times(1);
            EXPECT_CALL(*telemetry, reportEvent("http_request", VerifySuccessMetric("HEAD", "head-tag", "testName"), _)).Times(1);
        }

        HttpClient client{"testName", device};
        client.setRetriesCount(1);
        client.get("get-tag", url);
        client.post("post-tag", url, "data");
        client.head("head-tag", url);
    }
}
