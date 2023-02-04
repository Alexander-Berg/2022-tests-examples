#include "metrica_consumer_stub.h"

#include <yandex_io/metrica/metrica2d/metrica_endpoint.h>

#include <yandex_io/metrica/metricad/connector/metrica_connector.h>
#include <yandex_io/metrica/monitor/monitor_endpoint.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/protos/model_objects.pb.h>
#include <yandex_io/services/syncd/sync_endpoint.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::proto;

namespace {

    class MockMetricaEndpoint: public MetricaEndpoint {
    public:
        MockMetricaEndpoint(std::shared_ptr<YandexIO::IDevice> device, std::shared_ptr<ipc::IIpcFactory> ipcFactory)
            : MetricaEndpoint(std::move(device), std::move(ipcFactory))
        {
            registerConsumer<MetricaConsumerStub>();
            start();
        }

        MetricaConsumerStub* getConsumerStub() {
            return dynamic_cast<MetricaConsumerStub*>(consumers_.front().get());
        }

        void waitForQuasarConnector() {
            server_->waitConnectionsAtLeast(1);
        }
    };
}; // namespace

Y_UNIT_TEST_SUITE_F(MetricaEndpointTests, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testMetricaEvent) {
        MockMetricaEndpoint mockMetricaEndpoint(getDeviceForTests(), ipcFactoryForTests());
        MetricaConnector connector(ipcFactoryForTests(), "metrica2d");
        mockMetricaEndpoint.waitForQuasarConnector();
        auto* consumerStub = mockMetricaEndpoint.getConsumerStub();

        connector.reportEvent("testEvent", "testEventValue");

        consumerStub->waitForEvent();
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->lastEvent, "testEvent");
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->lastEventValue, "testEventValue");
    }

    Y_UNIT_TEST(testMetricaError) {
        MockMetricaEndpoint mockMetricaEndpoint(getDeviceForTests(), ipcFactoryForTests());
        MetricaConnector connector(ipcFactoryForTests(), "metrica2d");
        mockMetricaEndpoint.waitForQuasarConnector();
        auto* consumerStub = mockMetricaEndpoint.getConsumerStub();

        connector.reportError("testError", "testErrorValue");

        consumerStub->waitForError();
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->lastError, "testError");
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->lastErrorValue, "testErrorValue");
    }

    Y_UNIT_TEST(testMetricaLatencyPoints) {
        MockMetricaEndpoint mockMetricaEndpoint(getDeviceForTests(), ipcFactoryForTests());
        MetricaConnector connector(ipcFactoryForTests(), "metrica2d");
        mockMetricaEndpoint.waitForQuasarConnector();
        auto* consumerStub = mockMetricaEndpoint.getConsumerStub();

        auto latencyPoint = connector.createLatencyPoint();
        connector.reportLatency(latencyPoint, "testEvent");

        consumerStub->waitForEvent();
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->lastEvent, "testEvent");

        const Json::Value value = parseJson(mockMetricaEndpoint.getConsumerStub()->lastEventValue);
        UNIT_ASSERT(value.isMember("value"));
        UNIT_ASSERT(value["value"].asInt() >= 0);
    }

    Y_UNIT_TEST(testMetricaDeletesEnvironmentVariables) {
        MockMetricaEndpoint mockMetricaEndpoint(getDeviceForTests(), ipcFactoryForTests());
        MetricaConnector connector(ipcFactoryForTests(), "metrica2d");
        mockMetricaEndpoint.waitForQuasarConnector();
        connector.waitUntilConnected();
        auto* consumerStub = mockMetricaEndpoint.getConsumerStub();

        connector.putAppEnvironmentValue("testVariable", "testVariableValue");

        consumerStub->waitForEnvVar();
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->lastEnvironmentVariable, "testVariable");
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->lastEnvironmentVariableValue, "testVariableValue");
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->lastDeletedEnvironmentVariable, "");

        connector.deleteAppEnvironmentValue("testVariable");

        consumerStub->waitForEnvVarDelete();
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->lastDeletedEnvironmentVariable, "testVariable");
    }

    Y_UNIT_TEST(testConnectionType) {
        MockMetricaEndpoint mockMetricaEndpoint(getDeviceForTests(), ipcFactoryForTests());
        MetricaConnector connector(ipcFactoryForTests(), "metrica2d");
        mockMetricaEndpoint.waitForQuasarConnector();
        auto* consumerStub = mockMetricaEndpoint.getConsumerStub();
        UNIT_ASSERT_EQUAL(consumerStub->connectionType, proto::CONNECTION_TYPE_UNKNOWN);

        {
            connector.params()->setNetworkStatus({
                YandexIO::ITelemetry::IParams::NetworkStatus::CONNECTED,
                YandexIO::ITelemetry::IParams::ConnectionType::ETHERNET,
            });

            consumerStub->waitForConnectionStatus();
            UNIT_ASSERT_EQUAL(consumerStub->connectionType, proto::CONNECTION_TYPE_ETHERNET);
        }

        {
            connector.params()->setNetworkStatus({
                YandexIO::ITelemetry::IParams::NetworkStatus::CONNECTED,
                YandexIO::ITelemetry::IParams::ConnectionType::WIFI,
            });

            consumerStub->waitForConnectionStatus();
            UNIT_ASSERT_EQUAL(consumerStub->connectionType, proto::CONNECTION_TYPE_WIFI);
        }
    }

    Y_UNIT_TEST(testConsumerConfigUpdate) {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig({});
        config["metrica2d"]["consumers"]["stub"]["configVar"] = "initialValue";
        MockMetricaEndpoint mockMetricaEndpoint(getDeviceForTests(), ipcFactoryForTests());
        MetricaConnector connector(ipcFactoryForTests(), "metrica2d");
        auto* consumerStub = mockMetricaEndpoint.getConsumerStub();

        UNIT_ASSERT_VALUES_EQUAL(consumerStub->configVar, "initialValue");

        const std::string configVar = "configValue";

        Json::Value userConfig;
        auto& systemConfig = userConfig["system_config"];
        auto& consumerConfig = systemConfig["metricad"]["consumers"][consumerStub->getName()];
        consumerConfig["configVar"] = configVar;

        YIO_LOG_DEBUG("User config: " << jsonToString(userConfig));

        connector.params()->setConfig(jsonToString(userConfig));

        consumerStub->waitForConfig();
        UNIT_ASSERT_VALUES_EQUAL(consumerStub->configVar, configVar);
    }

} // end test suite
