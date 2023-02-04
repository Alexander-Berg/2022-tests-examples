#include <yandex_io/libs/appmetrica/app_metrica_sender.h>
#include <yandex_io/libs/appmetrica/report_configuration.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/protobuf_utils/debug.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>

#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <google/protobuf/text_format.h>

#include <json/json.h>

#include <library/cpp/testing/unittest/registar.h>

#include <memory>
#include <string>
#include <utility>
#include <vector>

using namespace quasar;
using namespace quasar::TestUtils;
namespace {

    proto::DatabaseMetricaEvent makeDbEvent() {
        proto::DatabaseMetricaEvent event;
        auto newEvent = event.mutable_new_event();
        newEvent->set_type(proto::DatabaseMetricaEvent::NewEvent::CLIENT);
        newEvent->set_timestamp(time(nullptr));
        newEvent->set_name("event");
        newEvent->set_value("event_value");
        newEvent->set_serial_number(0);
        newEvent->set_session_id(0);
        newEvent->set_session_start_time(0);
        return event;
    }

    class BaseFixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            YIO_LOG_INFO("SetUp")

            db_ = std::make_shared<EventsDatabase>("tmp.db", 100 /* kB */);
            immediateQueue_ = std::make_shared<blockingQueue>(100);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            std::remove("tmp.db");

            YIO_LOG_INFO("TearDown")

            Base::TearDown(context);
        }

        void initToGetReport() {
            mockMetricaBackend_.onHandlePayload = std::bind(&BaseFixture::onMetricRequest,
                                                            this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);

            metricaHost_ = "http://localhost:" + std::to_string(mockMetricaBackend_.start(getPort()));
        }

        void initForRedirect() {
            mockMetricaBackend_.onHandlePayload = std::bind(&BaseFixture::onRedirectRequest,
                                                            this, std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);

            metricaHost_ = "http://localhost:" + std::to_string(mockMetricaBackend_.start(getPort()));
        }

        void initNoConnection() {
            metricaHost_ = "http://localhost:" + std::to_string(mockMetricaBackend_.start(getPort()));
            mockMetricaBackend_.stop();
        }

        void initFor404() {
            mockMetricaBackend_.onHandlePayload = std::bind(&BaseFixture::on404Request,
                                                            std::placeholders::_1, std::placeholders::_2, std::placeholders::_3);
            metricaHost_ = "http://localhost:" + std::to_string(mockMetricaBackend_.start(getPort()));
        }

    protected:
        void onMetricRequest(const TestHttpServer::Headers& header, const std::string& payload, TestHttpServer::HttpConnection& handler)
        {
            validateHeader(header);
            Json::Value response;
            ReportMessage rep;
            Y_PROTOBUF_SUPPRESS_NODISCARD rep.ParseFromString(TString(payload));
            YIO_LOG_INFO("Metrica Report: " << shortUtf8DebugString(rep));

            {
                std::scoped_lock guard(mutex_);
                reportMessage_ = rep;
                condVar_.notify_one();
            }

            handler.doReplay(200, "application/json", jsonToString(response));
        }

        ReportMessage waitReport() {
            std::unique_lock lock(mutex_);
            condVar_.wait(lock, [this]() {
                return reportMessage_.has_value();
            });
            auto report = reportMessage_.value();
            reportMessage_.reset();
            return report;
        }

        void onRedirectRequest(const TestHttpServer::Headers& header, const std::string& payload, TestHttpServer::HttpConnection& handler)
        {
            const std::string redirectParam = "/redirect";
            if (header.resource == redirectParam) {
                onMetricRequest(header, payload, handler);
            } else {
                const std::string redirectHost = metricaHost_ + redirectParam + "?" + header.query;
                const std::vector<std::pair<std::string, std::string>> extraHeaders = {
                    std::make_pair("Location", redirectHost),
                };
                handler.doReplay(308, "application/json", "{}", extraHeaders);
            }
        }

        static void on404Request(const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/, TestHttpServer::HttpConnection& handler)
        {
            handler.doReplay(404, "application/json", "{}");
        }

        void validateHeader(const TestHttpServer::Headers& header) {
            UNIT_ASSERT_VALUES_EQUAL(header.tryGetHeader("accept"), "application/json");
            UNIT_ASSERT_VALUES_EQUAL(header.tryGetHeader("accept-encoding"), "gzip");
            UNIT_ASSERT_VALUES_EQUAL(header.tryGetHeader("content-encoding"), "gzip");
            UNIT_ASSERT_VALUES_UNEQUAL(header.tryGetHeader("send-timestamp"), "");

            UNIT_ASSERT(header.queryParams.hasValue("deviceid"));
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("deviceid"), getDeviceForTests()->deviceId());
            UNIT_ASSERT(header.queryParams.hasValue("model"));
            UNIT_ASSERT_VALUES_EQUAL(header.queryParams.getValue("model"), getDeviceForTests()->configuration()->getDeviceType());
        }

        std::shared_ptr<blockingQueue> immediateQueue_;
        std::shared_ptr<EventsDatabase> db_;
        std::string metricaHost_;
        TestHttpServer mockMetricaBackend_;

        std::optional<ReportMessage> reportMessage_;
        std::mutex mutex_;
        SteadyConditionVariable condVar_;
    };

} // namespace

Y_UNIT_TEST_SUITE(AppMetricaSenderTests) {
    Y_UNIT_TEST_F(TestSenderSendReport, BaseFixture) {
        initToGetReport();

        time_t time = std::time(nullptr);
        std::vector<std::string> hosts = {metricaHost_};
        auto reportConfig = std::make_shared<ReportConfiguration>(time, hosts, *getDeviceForTests());
        auto sender = AppMetricaSender(reportConfig, db_, immediateQueue_, getDeviceForTests());

        MetricaSessionProvider::Session session{0, 0, 0};
        std::map<std::string, std::string> environmentVariables;
        auto event = ReportMessage_Session_Event();
        event.set_number_in_session(42);
        event.set_type(ReportMessage_Session_Event_EventType::ReportMessage_Session_Event_EventType_EVENT_CLIENT);
        immediateQueue_->push(ReportOneEvent(event, environmentVariables, session));

        const auto report = waitReport();
        UNIT_ASSERT_EQUAL(report.sessions()[0].events().size(), 1);
        UNIT_ASSERT_EQUAL(event.type(), report.sessions()[0].events()[0].type());
        UNIT_ASSERT_EQUAL(event.number_in_session(), report.sessions()[0].events()[0].number_in_session());
    }

    Y_UNIT_TEST_F(TestSenderSendReportWithRedirect, BaseFixture) {
        initForRedirect();

        time_t time = std::time(nullptr);
        std::vector<std::string> hosts = {metricaHost_};
        auto reportConfig = std::make_shared<ReportConfiguration>(time, hosts, *getDeviceForTests());
        auto sender = AppMetricaSender(reportConfig, db_, immediateQueue_, getDeviceForTests());

        MetricaSessionProvider::Session session{0, 0, 0};
        std::map<std::string, std::string> environmentVariables;
        auto event = ReportMessage_Session_Event();
        event.set_number_in_session(42);
        event.set_type(ReportMessage_Session_Event_EventType::ReportMessage_Session_Event_EventType_EVENT_CLIENT);
        immediateQueue_->push(ReportOneEvent(event, environmentVariables, session));

        const auto report = waitReport();
        UNIT_ASSERT_EQUAL(report.sessions()[0].events().size(), 1);
        UNIT_ASSERT_EQUAL(event.type(), report.sessions()[0].events()[0].type());
        UNIT_ASSERT_EQUAL(event.number_in_session(), report.sessions()[0].events()[0].number_in_session());
    }

    Y_UNIT_TEST_F(TestNoConnection, BaseFixture) {
        /* Check that sender don't throw any exceptions if there is no connection when send report */
        initNoConnection();

        time_t time = std::time(nullptr);
        std::vector<std::string> hosts = {metricaHost_};
        auto reportConfig = std::make_shared<ReportConfiguration>(time, hosts, *getDeviceForTests());
        auto sender = AppMetricaSender(reportConfig, db_, immediateQueue_, getDeviceForTests());

        const auto event = makeDbEvent();
        db_->addEvent(event);

        /* Should not throw any exception */
        sender.sendReports();
    }

    Y_UNIT_TEST_F(Test404Response, BaseFixture) {
        /* Check that sender don't throw any exceptions if receive non 200 response */
        initFor404();

        time_t time = std::time(nullptr);
        std::vector<std::string> hosts = {metricaHost_};
        auto reportConfig = std::make_shared<ReportConfiguration>(time, hosts, *getDeviceForTests());
        auto sender = AppMetricaSender(reportConfig, db_, immediateQueue_, getDeviceForTests());

        const auto event = makeDbEvent();
        db_->addEvent(event);

        /* Should not throw any exception */
        sender.sendReports();
    }
}
