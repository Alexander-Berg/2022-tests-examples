#include <yandex_io/metrica/metricad/connector/metrica_connector.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <future>
#include <memory>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE_F(MetricaConnectorTests, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testMetricaConnector) {
        auto mockMetricaService = createIpcServerForTests("metricad");
        std::unique_ptr<std::promise<proto::QuasarMessage>> receivedPromise;
        mockMetricaService->setMessageHandler([&](const auto& message, auto& connection) {
            if (message->has_metrica_message() && message->metrica_message().has_stats()) {
                YIO_LOG_INFO("Skip stats message");
            } else {
                if (receivedPromise) {
                    receivedPromise->set_value(*message);
                } else {
                    std::cout << "NOT" << std::endl;
                }
            }
            if (message->has_request_id()) {
                proto::QuasarMessage acknowledge;
                acknowledge.set_request_id(message->request_id());
                connection.send(std::move(acknowledge));
            }
        });
        mockMetricaService->listenService();

        MetricaConnector metricaConnector(ipcFactoryForTests(), "metricad");

        receivedPromise = std::make_unique<std::promise<proto::QuasarMessage>>();
        metricaConnector.reportEvent("testEvent");
        auto receivedMessage = receivedPromise->get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(receivedMessage.metrica_message().report_event(), "testEvent");

        receivedPromise = std::make_unique<std::promise<proto::QuasarMessage>>();
        metricaConnector.reportError("error happened");
        receivedMessage = receivedPromise->get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(receivedMessage.metrica_message().report_error(), "error happened");

        receivedPromise = std::make_unique<std::promise<proto::QuasarMessage>>();
        metricaConnector.reportLatency(metricaConnector.createLatencyPoint(), "Latency", "{\"json\": 1}");
        receivedMessage = receivedPromise->get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(receivedMessage.metrica_message().report_event(), "Latency");
        const Json::Value latencyJson = tryParseJsonOrEmpty(receivedMessage.metrica_message().report_event_json_value());
        UNIT_ASSERT_VALUES_EQUAL(latencyJson["json"].asInt(), 1);
        UNIT_ASSERT(latencyJson.isMember("value"));

        receivedPromise = std::make_unique<std::promise<proto::QuasarMessage>>();
        metricaConnector.reportLogError("error msg", "source.cc", 42, "{\"module_name\": \"foo\", \"event_name\": \"foo.error\"}");
        receivedMessage = receivedPromise->get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(receivedMessage.metrica_message().report_error(), "log_error");
        UNIT_ASSERT_VALUES_EQUAL(receivedMessage.metrica_message().report_error_value(), "{\"event_name\":\"foo.error\",\"file_line\":42,\"file_name\":\"source.cc\",\"log\":\"error msg\",\"module_name\":\"foo\"}\n");

        receivedPromise = std::make_unique<std::promise<proto::QuasarMessage>>();
        metricaConnector.reportLogError("error msg", "source.cc", 42, "yadda yadda");
        receivedMessage = receivedPromise->get_future().get();
        UNIT_ASSERT_VALUES_EQUAL(receivedMessage.metrica_message().report_error(), "log_error");
        UNIT_ASSERT_VALUES_EQUAL(receivedMessage.metrica_message().report_error_value(), "{\"bad_event\":\"yadda yadda\",\"file_line\":42,\"file_name\":\"source.cc\",\"log\":\"error msg\"}\n");
    }

    Y_UNIT_TEST(testReportEnvironmentVariablesOnReconnect) {
        std::mutex mutex;
        std::queue<proto::QuasarMessage> messageQueue;
        SteadyConditionVariable condVar;

        const auto createMockMetricaAndStart = [&]() {
            auto mockMetricaService = createIpcServerForTests("metricad");
            mockMetricaService->setMessageHandler([&](const auto& message, auto& connection) {
                {
                    std::scoped_lock lock(mutex);
                    messageQueue.push(*message);
                    condVar.notify_one();
                }
                if (message->has_request_id()) {
                    proto::QuasarMessage acknowledge;
                    acknowledge.set_request_id(message->request_id());
                    connection.send(std::move(acknowledge));
                }
            });
            mockMetricaService->listenService();
            return mockMetricaService;
        };
        auto mockMetricaService = createMockMetricaAndStart();

        MetricaConnector metricaConnector(ipcFactoryForTests(), "metricad");
        metricaConnector.waitUntilConnected();
        TestUtils::waitUntil([&]() {
            // make sure onConnected callback is fired before setting up env variables. otherwise
            // there can be a race
            return metricaConnector.isOnConnectedCalled();
        });
        mockMetricaService->waitConnectionsAtLeast(1);

        auto waitAndCheckNextKeyValue = [&](const std::string& key, const std::string& value) {
            auto fetchMessage = [&]() {
                std::unique_lock<std::mutex> lock(mutex);
                proto::QuasarMessage message;
                do {
                    condVar.wait(lock, [&]() {
                        return messageQueue.size() > 0;
                    });
                    message = std::move(messageQueue.front());
                    messageQueue.pop();
                    UNIT_ASSERT(message.has_metrica_message());
                } while (message.metrica_message().has_stats());
                return message;
            };
            const auto message = fetchMessage();
            UNIT_ASSERT(message.metrica_message().has_app_environment_value());
            const auto& messageKey = message.metrica_message().app_environment_value().key();
            const auto& messageValue = message.metrica_message().app_environment_value().value();
            UNIT_ASSERT_VALUES_EQUAL(key, messageKey);
            UNIT_ASSERT_VALUES_EQUAL(value, messageValue);
        };
        metricaConnector.putAppEnvironmentValue("key1", "value1");
        waitAndCheckNextKeyValue("key1", "value1");

        metricaConnector.putAppEnvironmentValue("key2", "value2");
        waitAndCheckNextKeyValue("key2", "value2");

        metricaConnector.putAppEnvironmentValue("key3", "value3");
        waitAndCheckNextKeyValue("key3", "value3");

        auto waitDeleteVariable = [&](const std::string& key) {
            std::unique_lock<std::mutex> lock(mutex);
            condVar.wait(lock, [&]() {
                return messageQueue.size() > 0;
            });
            const auto message = std::move(messageQueue.front());
            messageQueue.pop();
            UNIT_ASSERT(message.has_metrica_message());
            UNIT_ASSERT(message.metrica_message().has_delete_environment_key());
            const auto& messageKey = message.metrica_message().delete_environment_key();
            UNIT_ASSERT_VALUES_EQUAL(key, messageKey);
        };

        metricaConnector.deleteAppEnvironmentValue("key2");
        waitDeleteVariable("key2");

        auto waitVariables = [&](auto mapToCheck) {
            while (!mapToCheck.empty()) {
                std::unique_lock<std::mutex> lock(mutex);
                condVar.wait(lock, [&]() {
                    return messageQueue.size() > 0;
                });
                const auto message = std::move(messageQueue.front());
                messageQueue.pop();
                UNIT_ASSERT(message.has_metrica_message());
                UNIT_ASSERT(message.metrica_message().has_app_environment_value());
                const auto& messageKey = message.metrica_message().app_environment_value().key();
                const auto& messageValue = message.metrica_message().app_environment_value().value();
                UNIT_ASSERT(mapToCheck.count(messageKey) > 0);
                UNIT_ASSERT_VALUES_EQUAL(mapToCheck[messageKey], messageValue);
                mapToCheck.erase(messageKey);
            }
        };
        YIO_LOG_INFO("Handle shutdown");
        /* Emulate service crash. Shutdown server. After reconnect metricaConnector should send remaining env vars */
        mockMetricaService.reset();
        metricaConnector.waitUntilDisconnected();
        /* restart on server */
        mockMetricaService = createMockMetricaAndStart();

        std::map<std::string, std::string> reconnectMap;
        reconnectMap["key1"] = "value1";
        reconnectMap["key3"] = "value3";
        waitVariables(reconnectMap);
    }

} /* MetricaConnectorTests suite */
