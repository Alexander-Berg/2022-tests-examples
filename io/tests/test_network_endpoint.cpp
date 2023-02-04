#include <yandex_io/services/networkd/network_endpoint.h>

#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <future>
#include <memory>

using quasar::proto::NetworkStatus;
using quasar::proto::QuasarMessage;
using quasar::proto::WifiStatus;

using namespace quasar;

namespace {
    class NetworkEndpointTestFixture: public QuasarUnitTestFixture {
    public:
        YandexIO::Configuration::TestGuard testGuard_;

        std::unique_ptr<NetworkEndpoint> testNetworkEndpointPtr_;

        std::shared_ptr<ipc::IServer> mockWifidServer_;
        std::shared_ptr<ipc::IConnector> networkdConnector_;

        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            testNetworkEndpointPtr_ = std::make_unique<NetworkEndpoint>(ipcFactoryForTests());

            mockWifidServer_ = createIpcServerForTests("wifid");
            networkdConnector_ = createIpcConnectorForTests(NetworkEndpoint::SERVICE_NAME);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            mockWifidServer_->shutdown();
            networkdConnector_->shutdown();
            networkdConnector_->waitUntilDisconnected();
            testNetworkEndpointPtr_.reset();

            Base::TearDown(context);
        }

        void start() const {
            mockWifidServer_->listenService();
            networkdConnector_->connectToService();

            networkdConnector_->waitUntilConnected();
            mockWifidServer_->waitConnectionsAtLeast(1);   // make sure that wifid received connection from networkd
            testNetworkEndpointPtr_->waitUntilConnected(); // make sure that networkd is connected to wifid
        }
    };
} // namespace

Y_UNIT_TEST_SUITE_F(NetworkEndpointTests, NetworkEndpointTestFixture) {
    Y_UNIT_TEST(testWifiStatusTransformsToNetworkStatus)
    {
        std::promise<QuasarMessage> networkdStatusSent;
        networkdConnector_->setMessageHandler([&](const auto& msg) {
            networkdStatusSent.set_value(*msg);
        });

        start();

        QuasarMessage message;
        ::quasar::proto::WifiStatus wifiStatus;
        wifiStatus.set_status(WifiStatus::CONNECTED);
        wifiStatus.set_internet_reachable(true);
        wifiStatus.set_signal(WifiStatus::EXCELLENT);
        *message.mutable_wifi_status() = wifiStatus;
        mockWifidServer_->sendToAll(std::move(message));

        auto sentMessage = networkdStatusSent.get_future().get();
        UNIT_ASSERT(sentMessage.has_network_status());
        const auto& networkStatus = sentMessage.network_status();

        UNIT_ASSERT_EQUAL(NetworkStatus::CONNECTED, networkStatus.status());
        UNIT_ASSERT_EQUAL(WifiStatus::CONNECTED, networkStatus.wifi_status().status());
        UNIT_ASSERT_EQUAL(WifiStatus::EXCELLENT, networkStatus.wifi_status().signal());
        UNIT_ASSERT_EQUAL(true, networkStatus.wifi_status().internet_reachable());
    }

    Y_UNIT_TEST(testWifiMessagesAreProxiedToWifid)
    {
        std::promise<QuasarMessage> wifiMessageReceived;
        mockWifidServer_->setMessageHandler([&](const auto& msg, auto& /*connection*/) {
            wifiMessageReceived.set_value(*msg);
        });

        start();

        QuasarMessage message;
        message.mutable_reset_network();
        networkdConnector_->sendMessage(std::move(message));

        auto sentMessage = wifiMessageReceived.get_future().get();
        UNIT_ASSERT(sentMessage.has_reset_wifi());
    }
}
