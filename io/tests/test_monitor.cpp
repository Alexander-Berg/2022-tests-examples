#include <yandex_io/metrica/monitor/monitor_endpoint.h>
#include <yandex_io/metrica/monitor/metrics_collector/metrics_collector.h>
#include <yandex_io/metrica/monitor/metrics_collector/ping_manager/ping_manager.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/unittest_helper/telemetry_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <future>
#include <mutex>
#include <string>
#include <vector>

namespace {
    using namespace quasar;
    using namespace quasar::proto;

    class ServicesInitFixture: public TelemetryTestFixture {
    public:
        YandexIO::Configuration::TestGuard testGuard;
        std::shared_ptr<ipc::IServer> mockSyncd;
        std::shared_ptr<ipc::IServer> mockWifid;
        std::shared_ptr<ipc::IServer> mockNetworkd;

        using Base = TelemetryTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            mockSyncd = createIpcServerForTests("syncd");
            mockWifid = createIpcServerForTests("wifid");
            mockNetworkd = createIpcServerForTests("networkd");

            mockSyncd->listenService();
            mockWifid->listenService();
            mockNetworkd->listenService();
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }
    };

    class TestMetricsCollectorImpl final: public MetricsCollectorBase {
    public:
        TestMetricsCollectorImpl(std::shared_ptr<YandexIO::IDevice> device, std::chrono::milliseconds period, std::shared_ptr<ipc::IIpcFactory> ipcFactory)
            : MetricsCollectorBase(device, ipcFactory, std::make_shared<PingManager>())
        {
            executor_ = std::make_unique<quasar::PeriodicExecutor>(
                std::bind(&TestMetricsCollectorImpl::collectSystemMetrics, this),
                period);
        }
    };

    Y_UNIT_TEST_SUITE_F(MonitorEndpointTests, ServicesInitFixture) {
        Y_UNIT_TEST(testMonitordCollectsDefaultMetrics) {
            std::promise<std::string> eventPromise;
            std::promise<std::string> eventJsonPromise;
            setEventListener([&, is_first_receive = true](const std::string& event, const std::string& eventJson, YandexIO::ITelemetry::Flags /*flags*/) mutable {
                if (is_first_receive) {
                    is_first_receive = false;
                } else {
                    /* filter not expected metric */
                    if (event == "systemMetrics") {
                        eventPromise.set_value(event);
                        eventJsonPromise.set_value(eventJson);
                    }
                }
            });

            auto mockYandexioServer = createIpcServerForTests("iohub_services");
            mockYandexioServer->listenService();

            MonitorEndpoint monitorEndpoint(getDeviceForTests(), ipcFactoryForTests(), std::make_unique<TestMetricsCollectorImpl>(getDeviceForTests(), std::chrono::seconds(1), ipcFactoryForTests()));
            UNIT_ASSERT_VALUES_EQUAL("systemMetrics", eventPromise.get_future().get());
            Json::Value json = parseJson(eventJsonPromise.get_future().get());
            const std::vector<std::string> defaultKeys{
                "cpuSystemPercent",
                "loadAverage1min",
                "uptimeSeconds",
                "cpuIoWaitPercent",
                "majorPageFaults",
                "slabBytes",
                "cpuIdlePercent",
                "vmallocUsedBytes",
                "freeSwapBytes",
                "cpuNicePercent",
                "cpuUserPercent",
                "freeRAMBytes",
                "mappedBytes",
                "totalRAMBytes",
                "cachedBytes",
                "totalSwapBytes",
                "partitions",
            };
            for (auto& defaultKey : defaultKeys) {
                UNIT_ASSERT(json.isMember(defaultKey));
            }
        }

        Y_UNIT_TEST(testMonitordCollectsCustomMetrics) {
            std::promise<double> numericPromise;
            std::promise<std::string> categoricalPromise;
            setEventListener([&](const std::string& event, const std::string& eventJson, YandexIO::ITelemetry::Flags /*flags*/) {
                if (event == "systemMetrics") {
                    Json::Value json = parseJson(eventJson);
                    if (json.isMember("platformValues")) {
                        Json::Value platformValues = json["platformValues"];
                        if (platformValues.isMember("numeric") && platformValues["numeric"].isMember("numberKey")) {
                            numericPromise.set_value(platformValues["numeric"]["numberKey"]["mean"].asDouble());
                        }

                        if (platformValues.isMember("categorical") && platformValues["categorical"].isMember("categoricalKey")) {
                            categoricalPromise.set_value(platformValues["categorical"]["categoricalKey"][0].asString());
                        }
                    }
                }
            });

            auto ioHub = createIpcServerForTests("iohub_services");
            ioHub->listenService();

            MonitorEndpoint monitorEndpoint(getDeviceForTests(), ipcFactoryForTests(), std::make_unique<TestMetricsCollectorImpl>(getDeviceForTests(), std::chrono::seconds(1), ipcFactoryForTests()));
            ioHub->waitConnectionsAtLeast(1);
            {
                proto::QuasarMessage msg;
                msg.mutable_io_control()->mutable_numeric_metrics()->set_key("numberKey");
                msg.mutable_io_control()->mutable_numeric_metrics()->set_value(42);
                ioHub->sendToAll(std::move(msg));
            }
            {
                proto::QuasarMessage msg;
                msg.mutable_io_control()->mutable_categorical_metrics()->set_key("categoricalKey");
                msg.mutable_io_control()->mutable_categorical_metrics()->set_value("catValue");
                ioHub->sendToAll(std::move(msg));
            }
            auto numericFuture = numericPromise.get_future();
            EXPECT_NEAR(42.0, numericFuture.get(), 0.01);
            auto categoricalFuture = categoricalPromise.get_future();
            UNIT_ASSERT_VALUES_EQUAL("catValue", categoricalFuture.get());
        }

    } /* MonitorEndpointTests */
} // namespace
