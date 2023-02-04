#include <yandex_io/libs/ipc/asio/asio_ipc_factory.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>

#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <mutex>
#include <random>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::proto;

namespace {

    class AsioConnectorTestFixture: public QuasarUnitTestFixtureWithoutIpc {
    public:
        using Base = QuasarUnitTestFixtureWithoutIpc;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            ipcFactory1_ = std::make_shared<ipc::AsioIpcFactory>(device_->sharedConfiguration());
            ipcFactory2_ = std::make_shared<ipc::AsioIpcFactory>(device_->sharedConfiguration());
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            ipcFactory1_.reset();
            ipcFactory2_.reset();

            Base::TearDown(context);
        }

    public:
        std::shared_ptr<ipc::AsioIpcFactory> ipcFactory1_;
        std::shared_ptr<ipc::AsioIpcFactory> ipcFactory2_;
    };

} // Anonymous namespace

Y_UNIT_TEST_SUITE_F(AsioServer, AsioConnectorTestFixture) {
    Y_UNIT_TEST(testOnConnect)
    {
        std::atomic<int> fOnConnected{0};
        auto server = ipcFactory1_->createIpcServer("test");
        server->setClientConnectedHandler([&](auto& /*unused*/) {
            ++fOnConnected;
        });
        const int port = server->listenTcpLocal(getPort());

        auto connector1 = ipcFactory1_->createIpcConnector("test");
        connector1->connectToTcpHost("localhost", port);
        connector1->waitUntilConnected();
        doUntil([&] { return fOnConnected > 0; }, 10000);
        UNIT_ASSERT_VALUES_EQUAL(fOnConnected.load(), 1);

        auto connector2 = ipcFactory2_->createIpcConnector("test");
        connector2->connectToTcpHost("localhost", port);
        connector2->waitUntilConnected();
        doUntil([&] { return fOnConnected > 1; }, 10000);
        UNIT_ASSERT_VALUES_EQUAL(fOnConnected.load(), 2);
    }

    Y_UNIT_TEST(testOnDisconnectTypical)
    {
        std::atomic<int> fOnDisconnected{0};
        auto server = ipcFactory1_->createIpcServer("test");
        server->setClientConnectedHandler([&](auto& client) {
            client.send(ipc::buildMessage([&](auto& msg) {
                msg.mutable_wifi_list_request();
            }));
        });
        server->setClientDisconnectedHandler([&](auto& /*unused*/) {
            ++fOnDisconnected;
        });
        const int port = server->listenTcpLocal(getPort());

        // Connector 1
        std::atomic<bool> fMessage1{false};
        auto connector1 = ipcFactory1_->createIpcConnector("test");
        connector1->setMessageHandler([&](auto& /*unused*/) {
            fMessage1 = true;
        });
        connector1->connectToTcpHost("localhost", port);
        doUntil([&] { return fMessage1.load(); }, 5000);
        UNIT_ASSERT(fMessage1);
        connector1.reset();
        doUntil([&] { return fOnDisconnected > 0; }, 5000);
        UNIT_ASSERT_VALUES_EQUAL(fOnDisconnected.load(), 1);

        // Connector 2
        std::atomic<bool> fMessage2{false};
        auto connector2 = ipcFactory2_->createIpcConnector("test");
        connector2->setMessageHandler([&](auto& /*unused*/) {
            fMessage2 = true;
        });
        connector2->connectToTcpHost("localhost", port);
        doUntil([&] { return fMessage2.load(); }, 5000);
        UNIT_ASSERT(fMessage2);
        connector2.reset();
        doUntil([&] { return fOnDisconnected > 1; }, 5000);
        UNIT_ASSERT_VALUES_EQUAL(fOnDisconnected.load(), 2);
    }

    Y_UNIT_TEST(testOnDisconnectImmediately)
    {
        std::atomic<int> fOnConnected{0};
        std::atomic<int> fOnDisconnected{0};
        auto server = ipcFactory1_->createIpcServer("test");
        server->setClientConnectedHandler([&](auto& /*unused*/) {
            ++fOnConnected;
        });
        server->setClientDisconnectedHandler([&](auto& /*unused*/) {
            ++fOnDisconnected;
        });
        const int port = server->listenTcpLocal(getPort());

        auto connector1 = ipcFactory1_->createIpcConnector("test");
        connector1->connectToTcpHost("localhost", port);
        connector1->waitUntilConnected();
        connector1.reset();
        doUntil([&] { return fOnConnected > 0 && fOnDisconnected > 0; }, 5000);
        UNIT_ASSERT_VALUES_EQUAL(fOnConnected.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(fOnDisconnected.load(), 1);

        auto connector2 = ipcFactory2_->createIpcConnector("test");
        connector2->connectToTcpHost("localhost", port);
        connector2->waitUntilConnected();
        connector2.reset();
        doUntil([&] { return fOnConnected > 1 && fOnDisconnected > 1; }, 5000);
        UNIT_ASSERT_VALUES_EQUAL(fOnConnected.load(), 2);
        UNIT_ASSERT_VALUES_EQUAL(fOnDisconnected.load(), 2);
    }

    Y_UNIT_TEST(testOnDisconnectServerFirst)
    {
        std::atomic<int> fOnConnected{0};
        std::atomic<int> fOnDisconnected{0};
        auto server = ipcFactory1_->createIpcServer("test");
        server->setClientConnectedHandler([&](auto& /*unused*/) {
            ++fOnConnected;
        });
        server->setClientDisconnectedHandler([&](auto& /*unused*/) {
            ++fOnDisconnected;
        });
        const int port = server->listenTcpLocal(getPort());

        auto connector1 = ipcFactory1_->createIpcConnector("test");
        connector1->connectToTcpHost("localhost", port);
        connector1->waitUntilConnected();
        doUntil([&] { return fOnConnected > 0; }, 5000);
        UNIT_ASSERT_VALUES_EQUAL(fOnConnected.load(), 1);

        auto connector2 = ipcFactory2_->createIpcConnector("test");
        connector2->connectToTcpHost("localhost", port);
        connector2->waitUntilConnected();
        doUntil([&] { return fOnConnected > 1; }, 5000);
        UNIT_ASSERT_VALUES_EQUAL(fOnConnected.load(), 2);

        server.reset();
        doUntil([&] { return fOnDisconnected > 1; }, 5000);
        UNIT_ASSERT_VALUES_EQUAL(fOnDisconnected.load(), 2);
    }
}
