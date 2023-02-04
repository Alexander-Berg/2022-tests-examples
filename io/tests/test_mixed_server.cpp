#include "mock_local_connection.h"

#include <yandex_io/libs/ipc/mixed/mixed_server.h>

#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/ipc/datacratic/public.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::ipc;
using namespace quasar::ipc::detail::mixed;
using namespace quasar::TestUtils;
using namespace testing;

namespace {
    class Fixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            portForServer = getPort();
            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["test"]["port"] = portForServer;

            callbackQueue = std::make_shared<NamedCallbackQueue>("MixedServer");
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        YandexIO::Configuration::TestGuard testGuard;
        std::shared_ptr<NamedCallbackQueue> callbackQueue;
        int portForServer;
    };
} // namespace

Y_UNIT_TEST_SUITE_F(MixedServer, Fixture) {
    Y_UNIT_TEST(Ctor)
    {
        UNIT_ASSERT_NO_EXCEPTION(MixedServer("test", nullptr, callbackQueue));
    }

    Y_UNIT_TEST(CtorWithEmptyName)
    {
        UNIT_ASSERT_EXCEPTION(MixedServer("", nullptr, callbackQueue), std::exception);
    }

    Y_UNIT_TEST(serviceName)
    {
        MixedServer ms("test", nullptr, callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(ms.serviceName(), "test");
    }

    Y_UNIT_TEST(init)
    {
        const auto device = getDeviceForTests();
        int factoryCallCounter = 0;
        auto factoryLife = std::make_shared<int>(0);
        std::weak_ptr<int> factoryLifeChecker = factoryLife;

        auto factory =
            [&, factoryLife{std::move(factoryLife)}](std::string serviceName, const quasar::Lifetime& /*lifetime*/, const std::shared_ptr<ICallbackQueue>& /*connection*/) {
                ++factoryCallCounter;
                return datacratic::createIpcServer(std::move(serviceName), device->sharedConfiguration());
            };
        MixedServer mixedServer("test", std::move(factory), callbackQueue);

        UNIT_ASSERT(!mixedServer.isStarted());

        int p = mixedServer.listenTcpLocal(portForServer);
        UNIT_ASSERT_VALUES_EQUAL(p, portForServer);
        UNIT_ASSERT(mixedServer.isStarted());
    }

    Y_UNIT_TEST(initServiceByName)
    {
        int factoryCallCounter = 0;
        auto factoryLife = std::make_shared<int>(0);
        std::weak_ptr<int> factoryLifeChecker = factoryLife;

        auto factory =
            [&, factoryLife{std::move(factoryLife)}](std::string /*serviceName*/, const quasar::Lifetime& /*lifetime*/, const std::shared_ptr<ICallbackQueue>& /*cbQueue*/) {
                ++factoryCallCounter;
                return nullptr;
            };
        MixedServer mixedServer("test", std::move(factory), callbackQueue);

        // init by correct name
        UNIT_ASSERT(!mixedServer.isStarted());
        mixedServer.listenService();
        UNIT_ASSERT_VALUES_EQUAL(mixedServer.port(), 0); // Factory return nullptr for network server
        UNIT_ASSERT(mixedServer.isStarted());

        // Factory must be called
        UNIT_ASSERT_VALUES_EQUAL(factoryCallCounter, 1);

        // Factory must be reset
        UNIT_ASSERT_VALUES_EQUAL(factoryLifeChecker.expired(), true);

        // No double init
        UNIT_ASSERT_EXCEPTION(mixedServer.listenService(), std::exception);
    }

    Y_UNIT_TEST(initServiceByHostAndPort)
    {
        const auto device = getDeviceForTests();
        int factoryCallCounter = 0;
        auto factoryLife = std::make_shared<int>(0);
        std::weak_ptr<int> factoryLifeChecker = factoryLife;

        auto factory =
            [&, factoryLife{std::move(factoryLife)}](std::string serviceName, const quasar::Lifetime& /*lifetime*/, const std::shared_ptr<ICallbackQueue>& /*cbQueue*/) {
                ++factoryCallCounter;
                return datacratic::createIpcServer(std::move(serviceName), device->sharedConfiguration());
            };
        MixedServer mixedServer("test", std::move(factory), callbackQueue);

        UNIT_ASSERT(!mixedServer.isStarted());
        UNIT_ASSERT_NO_EXCEPTION(mixedServer.listenTcpHost("127.0.0.1", portForServer));
        UNIT_ASSERT(mixedServer.isStarted());

        UNIT_ASSERT_VALUES_EQUAL(mixedServer.port(), portForServer);
    }

    Y_UNIT_TEST(addLocalConnectionBeforeInit)
    {
        MixedServer mixedServer("test", nullptr, callbackQueue);

        std::atomic<int> clientConnectedCounter = 0;
        std::atomic<bool> readyToConnect{false};
        mixedServer.setClientConnectedHandler(
            [&](IServer::IClientConnection& /*connection*/) {
                UNIT_ASSERT(callbackQueue->isWorkingThread());
                UNIT_ASSERT(readyToConnect.load());
                ++clientConnectedCounter;
            });
        auto mockLocalConnection = std::make_shared<quasar::mock::LocalConnection>();
        EXPECT_CALL(*mockLocalConnection, onConnect());
        mixedServer.addLocalConnection(mockLocalConnection);

        readyToConnect = true;
        mixedServer.listenService();
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(clientConnectedCounter.load(), 1);
    }

    Y_UNIT_TEST(addLocalConnectionAfterInit)
    {
        MixedServer mixedServer("test", nullptr, callbackQueue);

        std::atomic<int> clientConnectedCounter = 0;
        std::atomic<bool> readyToConnect{false};
        mixedServer.setClientConnectedHandler(
            [&](IServer::IClientConnection& /*connection*/) {
                UNIT_ASSERT(callbackQueue->isWorkingThread());
                UNIT_ASSERT(readyToConnect.load());
                ++clientConnectedCounter;
            });
        mixedServer.listenService();
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(clientConnectedCounter.load(), 0);

        readyToConnect = true;
        auto mockLocalConnection = std::make_shared<quasar::mock::LocalConnection>();
        EXPECT_CALL(*mockLocalConnection, onConnect());
        mixedServer.addLocalConnection(mockLocalConnection);
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(clientConnectedCounter.load(), 1);
    }

    Y_UNIT_TEST(sendToAll)
    {
        MixedServer mixedServer("test", nullptr, callbackQueue);

        mixedServer.listenService();
        flushCallbackQueue(callbackQueue);

        auto mockLocalConnection = std::make_shared<quasar::mock::LocalConnection>();
        EXPECT_CALL(*mockLocalConnection, send(Matcher<const SharedMessage&>(_))).WillOnce(Invoke([](const SharedMessage& m) {
            UNIT_ASSERT(m->has_wifi_list_request());
        }));
        mixedServer.addLocalConnection(mockLocalConnection);

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_list_request();
        });
        mixedServer.sendToAll(message);
        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(messageFromLocalConnection)
    {
        MixedServer mixedServer("test", nullptr, callbackQueue);
        auto mockLocalConnection = std::make_shared<quasar::mock::LocalConnection>();

        std::atomic<bool> messageReceived{false};
        mixedServer.setMessageHandler(
            [&](const SharedMessage& message, ipc::IServer::IClientConnection& connection) {
                UNIT_ASSERT(callbackQueue->isWorkingThread());
                UNIT_ASSERT_VALUES_EQUAL(message->has_wifi_list_request(), true);
                UNIT_ASSERT_VALUES_EQUAL(connection.share().get(), mockLocalConnection.get());
                messageReceived = true;

                auto answer = ipc::buildMessage([](auto& msg) {
                    msg.mutable_wifi_networks_enable();
                });
                connection.send(answer);
            });

        mixedServer.listenService();
        flushCallbackQueue(callbackQueue);

        EXPECT_CALL(*mockLocalConnection, send(Matcher<const ipc::SharedMessage&>(_))).WillOnce(Invoke([](const ipc::SharedMessage& m) {
            UNIT_ASSERT(m->has_wifi_networks_enable());
        }));
        mixedServer.addLocalConnection(mockLocalConnection);

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_list_request();
        });
        mixedServer.messageFromLocalConnection(message, *mockLocalConnection);
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(messageReceived.load(), true);
    }

    Y_UNIT_TEST(messageFromLocalConnectionBeforeListenService)
    {
        MixedServer mixedServer("test", nullptr, callbackQueue);
        auto mockLocalConnection = std::make_shared<quasar::mock::LocalConnection>();

        mixedServer.setMessageHandler(
            [&](const SharedMessage& /*msg*/, ipc::IServer::IClientConnection& /*connection*/) {
                UNIT_ASSERT(false);
            });

        ON_CALL(*mockLocalConnection, onConnect()).WillByDefault(Invoke([]() {
            UNIT_ASSERT(false);
        }));
        mixedServer.addLocalConnection(mockLocalConnection);
        flushCallbackQueue(callbackQueue);

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_list_request();
        });
        mixedServer.messageFromLocalConnection(message, *mockLocalConnection);
        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(withQuasarServer)
    {
        const auto device = getDeviceForTests();
        std::weak_ptr<IServer> quasarServer;
        auto factory =
            [&](std::string serviceName, const quasar::Lifetime& /*lifetime*/, const std::shared_ptr<ICallbackQueue>& /*cbQueue*/) {
                auto qs = datacratic::createIpcServer(std::move(serviceName), device->sharedConfiguration());
                quasarServer = qs;
                return qs;
            };

        MixedServer mixedServer("test", factory, callbackQueue);
        std::atomic<int> connectionCounter{0};
        mixedServer.setClientConnectedHandler(
            [&](IServer::IClientConnection& /*connection*/) {
                ++connectionCounter;
            });

        mixedServer.listenService();
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT(!quasarServer.expired());

        /* local connection */
        std::atomic<bool> localReceived{false};
        auto mockLocalConnection = std::make_shared<quasar::mock::LocalConnection>();

        EXPECT_CALL(*mockLocalConnection, onConnect());
        EXPECT_CALL(*mockLocalConnection, send(Matcher<const SharedMessage&>(_))).WillOnce(Invoke([&](const SharedMessage& m) {
            UNIT_ASSERT(m->has_wifi_list_request());
            localReceived = true;
        }));

        mixedServer.addLocalConnection(mockLocalConnection);
        flushCallbackQueue(callbackQueue);

        /* network connection */
        std::atomic<bool> netReceived{false};
        auto netConnector = quasar::ipc::datacratic::createIpcConnector("test", device->sharedConfiguration());
        netConnector->setMessageHandler(
            [&](const SharedMessage& m) {
                UNIT_ASSERT(m->has_wifi_list_request());
                netReceived = true;
            });
        netConnector->connectToService();

        waitUntil([&] { return connectionCounter.load() == 2; });

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_list_request();
        });
        mixedServer.sendToAll(message);

        waitUntil([&] { return localReceived.load() && netReceived.load(); });
    }

    Y_UNIT_TEST(sendToAllAfterShutdown)
    {
        MixedServer mixedServer("test", nullptr, callbackQueue);

        UNIT_ASSERT(!mixedServer.isStarted());
        mixedServer.listenService();
        UNIT_ASSERT(mixedServer.isStarted());
        flushCallbackQueue(callbackQueue);

        auto mockLocalConnection = std::make_shared<quasar::mock::LocalConnection>();
        ON_CALL(*mockLocalConnection, send(Matcher<const SharedMessage&>(_))).WillByDefault(Invoke([](const auto& /*msg*/) {
            UNIT_ASSERT(false);
        }));
        mixedServer.addLocalConnection(mockLocalConnection);
        flushCallbackQueue(callbackQueue);

        mixedServer.shutdown();
        UNIT_ASSERT(!mixedServer.isStarted());
        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_list_request();
        });
        mixedServer.sendToAll(message);
        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(ResetAllQueueAfterShutdown)
    {
        MixedServer mixedServer("test", nullptr, callbackQueue);
        auto mockLocalConnection = std::make_shared<quasar::mock::LocalConnection>();

        std::atomic<int> stage{0};
        std::atomic<bool> stage1Ready{false};
        mixedServer.setMessageHandler(
            [&](auto /*msg*/, auto& /*connection*/) {
                if (stage == 1) {
                    UNIT_ASSERT(true);
                    stage1Ready = true;
                    waitUntil([&] { return stage == 2; });
                } else {
                    UNIT_ASSERT(false);
                }
            });

        mixedServer.listenService();
        flushCallbackQueue(callbackQueue);

        mixedServer.addLocalConnection(mockLocalConnection);

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_list_request();
        });

        stage = 1;
        mixedServer.messageFromLocalConnection(message, *mockLocalConnection);
        waitUntil([&] { return stage1Ready == true; });

        mixedServer.messageFromLocalConnection(message, *mockLocalConnection); // schedule second message in interlal queue
        stage = 2;                                                             // shoud exit from callback

        UNIT_ASSERT(mixedServer.isStarted());
        mixedServer.shutdown(); // shoud cancel second message
        UNIT_ASSERT(!mixedServer.isStarted());

        flushCallbackQueue(callbackQueue);
    }

} // Y_UNIT_TEST_SUITE_F(MixedServer)
