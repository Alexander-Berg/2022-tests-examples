#include <library/cpp/testing/unittest/registar.h>

#include <yandex_io/metrica/monitor/metrics_collector/metrics_collector.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <json/json.h>

#include <sstream>

using namespace quasar;

namespace {
    struct Fixture: public QuasarUnitTestFixture {
        class MetricsCollectorTest: public quasar::MetricsCollectorBase {
        public:
            struct NetworkStatisticsTest: public NetworkStatistics {
                void collect(const quasar::MetricsCollectorBase& /*collector*/) override {
                }
            };
        };
    };

    Y_UNIT_TEST_SUITE_F(MetricsCollectorTests, Fixture) {
        Y_UNIT_TEST(testIsInterfaceInWhiteList) {
            Fixture::MetricsCollectorTest::NetworkStatisticsTest stats;
            UNIT_ASSERT_VALUES_EQUAL(stats.isInterfaceInWhiteList("wlan0"), true);
            UNIT_ASSERT_VALUES_EQUAL(stats.isInterfaceInWhiteList("eth0"), true);
            UNIT_ASSERT_VALUES_EQUAL(stats.isInterfaceInWhiteList("lo"), true);

            UNIT_ASSERT_VALUES_EQUAL(stats.isInterfaceInWhiteList("awlan0"), false);

            UNIT_ASSERT_VALUES_EQUAL(stats.isInterfaceInWhiteList("wlan123"), true);
            UNIT_ASSERT_VALUES_EQUAL(stats.isInterfaceInWhiteList("ethabc"), true);

            UNIT_ASSERT_VALUES_EQUAL(stats.isInterfaceInWhiteList(""), false);
        }

        Y_UNIT_TEST(testGetOperstate) {
            std::istringstream operstateStream;
            UNIT_ASSERT_VALUES_EQUAL(Fixture::MetricsCollectorTest::NetworkStatisticsTest::getOperstate(operstateStream), "");
            operstateStream = std::istringstream("");
            UNIT_ASSERT_VALUES_EQUAL(Fixture::MetricsCollectorTest::NetworkStatisticsTest::getOperstate(operstateStream), "");
            operstateStream = std::istringstream("up");
            UNIT_ASSERT_VALUES_EQUAL(Fixture::MetricsCollectorTest::NetworkStatisticsTest::getOperstate(operstateStream),
                                     "up");
            operstateStream = std::istringstream("down 123");
            UNIT_ASSERT_VALUES_EQUAL(Fixture::MetricsCollectorTest::NetworkStatisticsTest::getOperstate(operstateStream),
                                     "down");
            operstateStream = std::istringstream("   down    ");
            UNIT_ASSERT_VALUES_EQUAL(Fixture::MetricsCollectorTest::NetworkStatisticsTest::getOperstate(operstateStream),
                                     "down");
        }

        Y_UNIT_TEST(testGetDefaultInterfaceName) {
            std::istringstream routingTableStream;
            UNIT_ASSERT_VALUES_EQUAL(
                Fixture::MetricsCollectorTest::NetworkStatisticsTest::getDefaultInterfaceName(routingTableStream),
                "");

            routingTableStream = std::istringstream("");
            UNIT_ASSERT_VALUES_EQUAL(
                Fixture::MetricsCollectorTest::NetworkStatisticsTest::getDefaultInterfaceName(routingTableStream),
                "");

            routingTableStream = std::istringstream(
                "Iface\tDestination\tGateway \tFlags\tRefCnt\tUse\tMetric\tMask\t\tMTU\tWindow\tIRTT\n"
                "iface1\t00000000\tFEEF2D05\t0003\t0\t0\t600\t00000000\t0\t0\t0\n"
                "iface2\t00EC2D05\t00000000\t0001\t0\t0\t600\t00FCFFFF\t0\t0\t0");
            UNIT_ASSERT_VALUES_EQUAL(
                Fixture::MetricsCollectorTest::NetworkStatisticsTest::getDefaultInterfaceName(routingTableStream),
                "iface2");

            routingTableStream = std::istringstream(
                "Iface\tDestination\tGateway \tFlags\tRefCnt\tUse\tMetric\tMask\t\tMTU\tWindow\tIRTT");
            UNIT_ASSERT_VALUES_EQUAL(
                Fixture::MetricsCollectorTest::NetworkStatisticsTest::getDefaultInterfaceName(routingTableStream),
                "");

            routingTableStream = std::istringstream(
                "Iface\tDestination\tGateway \tFlags\tRefCnt\tUse\tMetric\tMask\t\tMTU\tWindow\tIRTT\n"
                "iface1\t00000000\tFEEF2D05\t0003\t0\t0\t600\t00000000\t0\t0\t0");
            UNIT_ASSERT_VALUES_EQUAL(
                Fixture::MetricsCollectorTest::NetworkStatisticsTest::getDefaultInterfaceName(routingTableStream),
                "");

            routingTableStream = std::istringstream("Iface	Destination\n"
                                                    "wlp59s0	00000000");
            UNIT_ASSERT_VALUES_EQUAL(
                Fixture::MetricsCollectorTest::NetworkStatisticsTest::getDefaultInterfaceName(routingTableStream),
                "");
        }

        Y_UNIT_TEST(testGetInterfacesInfoEmptyInput) {
            std::istringstream operstateStream;
            std::vector<Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo> result;
            result = Fixture::MetricsCollectorTest::NetworkStatisticsTest::getInterfacesInfo(operstateStream);
            UNIT_ASSERT_VALUES_EQUAL(result.size(), 0);

            operstateStream = std::istringstream("");
            result = Fixture::MetricsCollectorTest::NetworkStatisticsTest::getInterfacesInfo(operstateStream);
            UNIT_ASSERT_VALUES_EQUAL(result.size(), 0);
        }

        Y_UNIT_TEST(testGetInterfacesInfoNoneInterfaces) {
            std::istringstream operstateStream;
            std::vector<Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo> result;

            operstateStream = std::istringstream(
                std::string("Inter-|   Receive                                                |  Transmit\n") +
                std::string(" face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed"));
            result = Fixture::MetricsCollectorTest::NetworkStatisticsTest::getInterfacesInfo(operstateStream);
            UNIT_ASSERT_VALUES_EQUAL(result.size(), 0);
        }

        Y_UNIT_TEST(testGetInterfacesInfoStandartInput) {
            std::istringstream operstateStream;
            std::vector<Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo> result;

            operstateStream = std::istringstream(
                std::string("Inter-|   Receive                                                |  Transmit\n") +
                std::string(" face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed\n") +
                std::string("    lo: 1385184136  499313    123    0    0     0          0         0 1385184137  499313    0    0    0     0       0          0\n") +
                std::string("wlan0: 13539086925 12522849    0    0    0     0          0         0 657248995 3186188    0    0    0     0       0          0"));
            result = Fixture::MetricsCollectorTest::NetworkStatisticsTest::getInterfacesInfo(operstateStream);
            UNIT_ASSERT_VALUES_EQUAL(result.size(), 2);
            UNIT_ASSERT_VALUES_EQUAL(result[0].name, "lo");
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics.size(), 8);
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics["bytes"], 1385184136);
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics["errs"], 123);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics.size(), 8);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics["bytes"], 1385184137);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics["errs"], 0);
            UNIT_ASSERT_VALUES_EQUAL(result[1].name, "wlan0");
            UNIT_ASSERT_VALUES_EQUAL(result[1].receiveStatistics.size(), 8);
            UNIT_ASSERT_VALUES_EQUAL(result[1].receiveStatistics["bytes"], 13539086925);
            UNIT_ASSERT_VALUES_EQUAL(result[1].receiveStatistics["errs"], 0);
            UNIT_ASSERT_VALUES_EQUAL(result[1].transmitStatistics.size(), 8);
            UNIT_ASSERT_VALUES_EQUAL(result[1].transmitStatistics["bytes"], 657248995);
            UNIT_ASSERT_VALUES_EQUAL(result[1].transmitStatistics["errs"], 0);
        }

        Y_UNIT_TEST(testGetInterfacesInfoDataRearrange) {
            std::istringstream operstateStream;
            std::vector<Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo> result;

            operstateStream = std::istringstream(std::string("Inter-| Receive | Transmit\n") +
                                                 std::string("face |bytes errs packets |bytes    packets errs\n") +
                                                 std::string("lo: 1385184136    123 499313 1385184137  499313    0"));
            result = Fixture::MetricsCollectorTest::NetworkStatisticsTest::getInterfacesInfo(operstateStream);
            UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(result[0].name, "lo");
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics.size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics["bytes"], 1385184136);
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics["errs"], 123);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics.size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics["bytes"], 1385184137);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics["errs"], 0);
        }

        Y_UNIT_TEST(testGetInterfacesInfoMissedTransmit) {
            std::istringstream operstateStream;
            std::vector<Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo> result;

            operstateStream = std::istringstream(std::string("Inter-|   Receive |  Transmit\n") +
                                                 std::string(" face |bytes    packets errs|\n") +
                                                 std::string(" lo: 1385184136    0 499313"));
            result = Fixture::MetricsCollectorTest::NetworkStatisticsTest::getInterfacesInfo(operstateStream);
            UNIT_ASSERT_VALUES_EQUAL(result.size(), 0);
        }

        Y_UNIT_TEST(testGetInterfacesInfoMissedData) {
            std::istringstream operstateStream;
            std::vector<Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo> result;

            operstateStream = std::istringstream(std::string("Inter-|   Receive |  Transmit\n") +
                                                 std::string(" face |drop fifo frame|drop fifo colls\n") +
                                                 std::string(" lo: 1 2 3 4 5 6"));
            result = Fixture::MetricsCollectorTest::NetworkStatisticsTest::getInterfacesInfo(operstateStream);
            UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(result[0].name, "lo");
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics.size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics.count("bytes"), 0);
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics.count("errs"), 0);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics.size(), 3);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics.count("bytes"), 0);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics.count("errs"), 0);
        }

        Y_UNIT_TEST(testGetInterfacesInfoInvalidDataCount) {
            std::istringstream operstateStream;
            std::vector<Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo> result;

            operstateStream = std::istringstream(
                std::string("Inter-|   Receive                                                |  Transmit\n") +
                std::string(" face |bytes    packets errs drop fifo frame compressed multicast|bytes    packets errs drop fifo colls carrier compressed\n") +
                std::string("lo: 1385184136  499313    123    0    0     0          0         0 1385184137  499313    1    2    3     4\n"));
            result = Fixture::MetricsCollectorTest::NetworkStatisticsTest::getInterfacesInfo(operstateStream);
            UNIT_ASSERT_VALUES_EQUAL(result.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(result[0].name, "lo");
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics.size(), 8);
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics["bytes"], 1385184136);
            UNIT_ASSERT_VALUES_EQUAL(result[0].receiveStatistics["errs"], 123);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics.size(), 8);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics["bytes"], 1385184137);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics["errs"], 1);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics["colls"], 4);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics.count("compressed"), 1);
            UNIT_ASSERT_VALUES_EQUAL(result[0].transmitStatistics["compressed"], 0);
        }

        Y_UNIT_TEST(testToJson) {
            Fixture::MetricsCollectorTest::NetworkStatisticsTest stats;
            Json::Value expected = Json::objectValue;
            Json::Value interfaces = Json::arrayValue;
            expected["defaultInterface"] = "";
            expected["interfaces"] = interfaces;
            UNIT_ASSERT_EQUAL(stats.toJson(), expected);

            Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo info;
            info.name = "abcd";
            info.operstate = "up";
            stats.interfaces_.push_back(info);

            info = Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo();
            info.name = "wlan0";
            info.operstate = "up";
            info.receiveStatistics["bytes"] = 123;
            info.receiveStatistics["errs"] = 1;
            info.receiveStatistics["packets"] = 4;
            info.transmitStatistics["bytes"] = 321;
            info.transmitStatistics["errs"] = 2;
            info.transmitStatistics["packets"] = 5;
            stats.interfaces_.push_back(info);

            info = Fixture::MetricsCollectorTest::NetworkStatisticsTest::InterfaceInfo();
            info.name = "lo";
            info.operstate = "unknown";
            info.receiveStatistics["errs"] = 1;
            info.transmitStatistics["bytes"] = 321;
            stats.interfaces_.push_back(info);

            stats.defaultIfaceName_ = "wlan0";

            interfaces = Json::arrayValue;
            Json::Value data;
            data["name"] = "wlan0";
            data["operstate"] = "up";
            data["rx_bytes"] = 123;
            data["rx_errs"] = 1;
            data["tx_bytes"] = 321;
            data["tx_errs"] = 2;
            interfaces.append(data);

            data = Json::Value();
            data["name"] = "lo";
            data["operstate"] = "unknown";
            data["rx_bytes"] = 0;
            data["rx_errs"] = 1;
            data["tx_bytes"] = 321;
            data["tx_errs"] = 0;
            interfaces.append(data);

            expected = Json::objectValue;
            expected["defaultInterface"] = "wlan0";
            expected["interfaces"] = interfaces;

            UNIT_ASSERT_EQUAL(stats.toJson(), expected);
        }

        Y_UNIT_TEST(testServiceMatching) {
            using namespace std::string_literals;

            std::string cmdline = "/system/vendor/quasar/updater_switcher";
            const std::unordered_set<std::string> services{"updater", "updater_switcher", "updater_gateway"};
            UNIT_ASSERT_EQUAL(MetricsCollectorTest::matchProcWithService(cmdline, services), std::string("updater_switcher"));
            cmdline = "/system/vendor/quasar/maind\0--updater_gateway"s;
            UNIT_ASSERT_EQUAL(MetricsCollectorTest::matchProcWithService(cmdline, services), std::string("updater_gateway"));
            cmdline = "/system/vendor/quasar/maind\0--updater_switcher\0--updater_gateway"s;
            UNIT_ASSERT_EQUAL(MetricsCollectorTest::matchProcWithService(cmdline, services), std::string("updater_switcher"));
            cmdline = "/system/vendor/quasar/updater_switcher\0--updater\0--updater_gateway"s;
            UNIT_ASSERT_EQUAL(MetricsCollectorTest::matchProcWithService(cmdline, services), std::string("updater_switcher"));
            cmdline = "/system/vendor/quasar/not_updater_switcher";
            UNIT_ASSERT_EQUAL(MetricsCollectorTest::matchProcWithService(cmdline, services), nullptr);
        }
    } /* MetricsCollectorTests end */
} // namespace
