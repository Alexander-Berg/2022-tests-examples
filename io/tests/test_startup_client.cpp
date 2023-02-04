#include <yandex_io/libs/appmetrica/startup_client.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>

#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <json/json.h>

#include <library/cpp/testing/unittest/registar.h>

#include <array>
#include <atomic>
#include <string>
#include <thread>
#include <utility>
#include <vector>

using namespace quasar;
using namespace quasar::TestUtils;

namespace {

    class BaseFixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

    protected:
        void validateHeader(const TestHttpServer::Headers& header) {
            UNIT_ASSERT_VALUES_EQUAL(header.tryGetHeader("accept"), "application/json");

            UNIT_ASSERT(header.queryParams.hasValue("deviceid"));
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("deviceid"), getDeviceForTests()->deviceId());
            UNIT_ASSERT(header.queryParams.hasValue("model"));
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("model"), getDeviceForTests()->configuration()->getDeviceType());
        }

        static Json::Value prepareUrls(const std::vector<std::string>& urls) {
            Json::Value array = Json::arrayValue;
            for (const auto& url : urls) {
                array.append(url);
            }
            return array;
        }

        StartupConfiguration getStartupConfiguration() {
            return StartupConfiguration{
                .startTime = std::time(nullptr),
                .startupHost = appMetricaStartupHost_,
                .apiKey = "api_key",
                .deviceID = getDeviceForTests()->deviceId(),
                .UUID = "uuid",
                .model = getDeviceForTests()->configuration()->getDeviceType(),
                .appVersion = "app_version",
                .appVersionName = "app_version_name",
                .metricaAppID = "metricaAppId",
                .osVersion = "TestVersion",
            };
        }

        /* Data */
    protected:
        const std::string uuid_ = makeUUID();
        std::string appMetricaStartupHost_;
        TestHttpServer mockMetricaHostsBackend_;
    };

    class SimpleStartupRequestFixture: public BaseFixture {
    public:
        using Base = BaseFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            /* Create Mock Backend for Metrica Startup requests */
            mockMetricaHostsBackend_.onHandlePayload = std::bind(&SimpleStartupRequestFixture::onStartupRequest,
                                                                 this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);

            appMetricaStartupHost_ = "http://localhost:" + std::to_string(mockMetricaHostsBackend_.start(getPort()));
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

    protected:
        void onStartupRequest(const TestHttpServer::Headers& header, const std::string& /*payload*/,
                              TestHttpServer::HttpConnection& handler) {
            validateHeader(header);

            Json::Value response;
            response["uuid"]["value"] = uuid_;
            response["query_hosts"]["list"]["startup"]["urls"] = prepareUrls(startupUrls_);
            response["query_hosts"]["list"]["report"]["urls"] = prepareUrls(reportUrls_);
            handler.doReplay(200, "application/json", jsonToString(response));
        }

        /* Data */
    protected:
        const std::vector<std::string> startupUrls_{
            "https://yandex.ru/startupUrl1",
            "https://yandex.ru/startupUrl2",
            "https://yandex.ru/startupUrl3",
        };

        const std::vector<std::string> reportUrls_{
            "https://yandex.ru/reportUrl1",
            "https://yandex.ru/reportUrl2",
            "https://yandex.ru/reportUrl3",
        };
    };

    class MultipleStartupHostsFixture: public BaseFixture {
    public:
        using Base = BaseFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            /* Create Mock Backend for Metrica Startup requests */
            mockMetricaHostsBackend_.onHandlePayload = std::bind(&MultipleStartupHostsFixture::onHostStartupRequest,
                                                                 this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);

            appMetricaStartupHost_ = "http://localhost:" + std::to_string(mockMetricaHostsBackend_.start(getPort()));
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

    private:
        /* Handler that response with urls for more StartUp hosts */
        void onHostStartupRequest(const TestHttpServer::Headers& header, const std::string& /*payload*/,
                                  TestHttpServer::HttpConnection& handler) {
            validateHeader(header);

            /* Save Duplicate hosts in sendReportHosts_ vector, so after StartupClient::getReportConfig is complete
             * test will compare this vector with ReportConfiguration::reportRequestURLS result. Duplicate host should be
             * met just once in hosts vector (because StartupClient should remove duplicates)
             */
            sentReportHosts_.push_back(duplicateHost_);

            Json::Value response;
            response["uuid"]["value"] = uuid_;
            std::vector<std::string> startupUrls;
            for (auto& host : startupHosts_) {
                /* Init hosts that will be requested by StartupClient to receive more Report Hosts */
                host.onHandlePayload = std::bind(&MultipleStartupHostsFixture::onReportHostsRequest,
                                                 this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);
                startupUrls.push_back("http://localhost:" + std::to_string(host.start(getPort())));
            }
            /* Set up startup urls, so StartupClient will make requests for more report hosts */
            response["query_hosts"]["list"]["startup"]["urls"] = prepareUrls(startupUrls);

            /* Return some report hosts */
            const auto reportHosts = generateAndSaveReportHosts();
            response["query_hosts"]["list"]["report"]["urls"] = prepareUrls(reportHosts);

            handler.doReplay(200, "application/json", jsonToString(response));
        }

        void onReportHostsRequest(const TestHttpServer::Headers& header, const std::string& /*payload*/,
                                  TestHttpServer::HttpConnection& handler) {
            validateHeader(header);

            Json::Value response;
            response["uuid"]["value"] = uuid_;

            const auto reportHosts = generateAndSaveReportHosts();

            response["query_hosts"]["list"]["report"]["urls"] = prepareUrls(reportHosts);
            handler.doReplay(200, "application/json", jsonToString(response));
        }

        /* Return vector with Report Hosts:
         *    One duplicate (test that StartupClient will remove duplicates) and random hosts
         * NOTE: This function save random hosts to sentReportHosts_
         */
        std::vector<std::string> generateAndSaveReportHosts() {
            std::vector<std::string> reportHosts;
            /* Each StartUp host return this duplicate host in order to make sure that StartupClient will skip duplicates */
            reportHosts.push_back(duplicateHost_);

            const std::string someHost = "https://yandex.ru/" + makeUUID(); // some random host
            reportHosts.push_back(someHost);
            sentReportHosts_.push_back(someHost); // save host, so test will compare this vector with received from ReportConfiguration

            return reportHosts;
        }

        /* Data */
    protected:
        /* Vector that store all report hosts urls that was sent by all StartUpHosts (without duplicates) */
        std::vector<std::string> sentReportHosts_;

        const std::string duplicateHost_ = "https://yandex.ru/duplicate";

        std::array<TestHttpServer, 3> startupHosts_;
    };

    class RedirectHostFixture: public SimpleStartupRequestFixture {
    public:
        using Base = SimpleStartupRequestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            /* Redirect request to another host */
            mockMetricaHostsBackend_.onHandlePayload = std::bind(&RedirectHostFixture::onRedirectHost,
                                                                 this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);

            /* mockMetricaHostsBackend_ will redirect request to redirectTargetHost_. redirectTargetHost_ should response
             * with startup and report hosts to make sure that StartupClient will receive values from redirected host
             */
            redirectTargetHost_.onHandlePayload = std::bind(&RedirectHostFixture::onStartupRequest,
                                                            this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);
            redirectTargetHost_.start(getPort());
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

    private:
        /* Handler that wil redirect request to another host */
        void onRedirectHost(const TestHttpServer::Headers& header, const std::string& /*payload*/,
                            TestHttpServer::HttpConnection& handler) {
            validateHeader(header);
            const std::string redirectHost = "http://localhost:" + std::to_string(redirectTargetHost_.port()) + header.resource + "?" + header.query;
            YIO_LOG_DEBUG("onRedirectHost redirectHost=" << redirectHost);
            const std::vector<std::pair<std::string, std::string>> extraHeaders = {
                std::make_pair("Location", redirectHost),
            };
            handler.doReplay(301, "application/json", "{}", extraHeaders);
        }

        /* Data */
    protected:
        TestHttpServer redirectTargetHost_;
    };

} // namespace

Y_UNIT_TEST_SUITE(MetricaStartupClientTest) {
    Y_UNIT_TEST_F(TestStartupConfigRequest, SimpleStartupRequestFixture) {
        StartupClient client(getStartupConfiguration(), getDeviceForTests());
        std::atomic_bool threadStopped{false};
        const auto result = client.getStartupConfig(threadStopped);
        UNIT_ASSERT_VALUES_EQUAL(result.UUID, uuid_);
        UNIT_ASSERT_VALUES_EQUAL(result.startupHosts, startupUrls_);
        UNIT_ASSERT_VALUES_EQUAL(result.reportHosts, reportUrls_);
    }

    Y_UNIT_TEST_F(TestReportConfigRequest, MultipleStartupHostsFixture) {
        StartupClient client(getStartupConfiguration(), getDeviceForTests());
        std::atomic_bool threadStopped{false};
        const auto result = client.getReportConfig(threadStopped);
        UNIT_ASSERT_VALUES_EQUAL(result->getUuid(), uuid_);
        const auto reportRequestUrls = result->reportRequestURLS();
        UNIT_ASSERT_VALUES_EQUAL(reportRequestUrls.size(), sentReportHosts_.size());
        /* Check that received ReportConfiguration contains all unique report hosts */
        for (size_t i = 0; i < reportRequestUrls.size(); ++i) {
            /* Check that reportRequestUrls[i] starts with sentReportHosts_[i] */
            UNIT_ASSERT_VALUES_EQUAL(reportRequestUrls[i].find(sentReportHosts_[i]), 0);
        }
    }

    Y_UNIT_TEST_F(TestStartupClientDoNotHang, BaseFixture) {
        /* Check that StartupClient will not hang up if backend never response with 200 */
        std::mutex mutex;
        SteadyConditionVariable condVar;
        int requestsCount = 0;

        mockMetricaHostsBackend_.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/,
                                                       const std::string& /*payload*/, TestHttpServer::HttpConnection& handler) {
            std::lock_guard<std::mutex> guard(mutex);
            ++requestsCount;
            condVar.notify_one();
            /* Always send bad response, so getStartupConfig will retry */
            handler.doReplay(404, "application/json", "{}");
        };

        appMetricaStartupHost_ = "http://localhost:" + std::to_string(mockMetricaHostsBackend_.start(getPort()));

        StartupClient client(getStartupConfiguration(), getDeviceForTests());
        std::atomic_bool threadStopped{false};

        /* Run separate thread with getReportConfig, so main thread will be able to stop it via threadStopped flag */
        std::thread getConfigThread = std::thread([&]() {
            client.getReportConfig(threadStopped);
        });

        /* Make sure that it will make at least a few requests to backend */
        std::unique_lock<std::mutex> lock(mutex);
        condVar.wait(lock, [&]() {
            return requestsCount == 2;
        });
        lock.unlock();
        /* getReportConfig should return after setting up atomic flag to true */
        threadStopped = true;

        getConfigThread.join();

        mockMetricaHostsBackend_.stop();
        mockMetricaHostsBackend_.onHandlePayload = nullptr;
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST_F(TestStartupClientFollowRedirect, RedirectHostFixture) {
        /* Check that StartupClient follow Url Redirect. appMetricaStartupHost_ will redirect request to another host */
        StartupClient client(getStartupConfiguration(), getDeviceForTests());
        std::atomic_bool threadStopped{false};
        const auto result = client.getStartupConfig(threadStopped);
        UNIT_ASSERT_VALUES_EQUAL(result.UUID, uuid_);
        UNIT_ASSERT_VALUES_EQUAL(result.startupHosts, startupUrls_);
        UNIT_ASSERT_VALUES_EQUAL(result.reportHosts, reportUrls_);
    }
}
