#include "mock_mixed_server.h"

#include <yandex_io/libs/ipc/mixed/mixed_connector.h>
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
    const std::string SERVICE_NAME_TEST = "test";
    class Fixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["test"]["port"] = getPort();

            callbackQueue = std::make_shared<NamedCallbackQueue>("MixedConnector");
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        YandexIO::Configuration::TestGuard testGuard;
        std::shared_ptr<NamedCallbackQueue> callbackQueue;
    };
} // namespace

Y_UNIT_TEST_SUITE_F(MixedConnector, Fixture) {
    Y_UNIT_TEST(Ctor)
    {
        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST(serviceName)
    {
        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(mixedConnector.serviceName(), "test");
    }

    Y_UNIT_TEST(bindBeforeConnect1)
    {
        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));
        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(true));
        UNIT_ASSERT_VALUES_EQUAL(mockMixedServer.use_count(), 1);

        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);

        ON_CALL(*mockMixedServer, addLocalConnection(_)).WillByDefault(Invoke([&](const std::shared_ptr<IMixedServer::LocalConnection>& /*connection*/) {
            UNIT_ASSERT(false);
        }));
        mixedConnector.bind(mockMixedServer);
        UNIT_ASSERT_VALUES_EQUAL(mockMixedServer.use_count(), 1);

        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(bindBeforeConnect2)
    {
        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));
        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(true));

        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);
        mixedConnector.bind(mockMixedServer);
        EXPECT_CALL(*mockMixedServer, addLocalConnection(_)).WillOnce(Invoke([&](const std::shared_ptr<IMixedServer::LocalConnection>& localConnection) {
            UNIT_ASSERT(localConnection);
        }));

        mixedConnector.connectToService();
        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(bindAfterConnect)
    {
        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));
        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(true));

        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);
        EXPECT_CALL(*mockMixedServer, addLocalConnection(_)).WillOnce(Invoke([&](const std::shared_ptr<IMixedServer::LocalConnection>& localConnection) {
            UNIT_ASSERT(localConnection);
        }));

        mixedConnector.connectToService();
        mixedConnector.bind(mockMixedServer);

        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(isConnected)
    {
        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));

        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);
        EXPECT_CALL(*mockMixedServer, addLocalConnection(_)).WillOnce(Invoke([&](const std::shared_ptr<IMixedServer::LocalConnection>& localConnection) {
            UNIT_ASSERT(localConnection);
        }));

        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(false));
        UNIT_ASSERT(!mixedConnector.isConnected());
        mixedConnector.connectToService();
        UNIT_ASSERT(!mixedConnector.isConnected());
        mixedConnector.bind(mockMixedServer);
        UNIT_ASSERT(!mixedConnector.isConnected());

        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(true));
        UNIT_ASSERT(mixedConnector.isConnected());

        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(sendMessage)
    {
        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);

        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));
        EXPECT_CALL(*mockMixedServer, addLocalConnection(_));
        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(true));

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_networks_disable();
        });

        UNIT_ASSERT_VALUES_EQUAL(mixedConnector.sendMessage(message), false);

        mixedConnector.connectToService();
        mixedConnector.bind(mockMixedServer);

        EXPECT_CALL(*mockMixedServer, messageFromLocalConnection(_, _));
        UNIT_ASSERT_VALUES_EQUAL(mixedConnector.sendMessage(message), true);

        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(sendMessageBeforeRealyConnect)
    {
        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);

        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));
        EXPECT_CALL(*mockMixedServer, addLocalConnection(_));
        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(false));
        ON_CALL(*mockMixedServer, messageFromLocalConnection(_, _)).WillByDefault(Invoke([&](const SharedMessage& /*msg*/, IMixedServer::LocalConnection& /*connection*/) {
            UNIT_ASSERT(false);
        }));

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_networks_disable();
        });

        mixedConnector.connectToService();
        mixedConnector.bind(mockMixedServer);

        UNIT_ASSERT(!mixedConnector.isConnected());
        bool success = mixedConnector.sendMessage(message);
        UNIT_ASSERT_VALUES_EQUAL(success, false);

        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(sendMessageAlwaysAsyncAndCanBeSendedFromInsideHandler)
    {
        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);

        std::shared_ptr<quasar::mock::MixedServer::LocalConnection> localConnection;

        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));
        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(true));
        EXPECT_CALL(*mockMixedServer, addLocalConnection(_)).WillRepeatedly(Invoke([&](const std::shared_ptr<quasar::mock::MixedServer::LocalConnection>& lc) {
            localConnection = lc;
        }));

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_wifi_networks_disable();
        });

        std::atomic<bool> serverProcessMessage{false};
        mixedConnector.setMessageHandler(
            [&](const auto& message) {
                UNIT_ASSERT(callbackQueue->isWorkingThread());
                UNIT_ASSERT(!serverProcessMessage.load());
                mixedConnector.sendMessage(message);
                waitUntil([&] { return serverProcessMessage.load(); });
            });

        mixedConnector.connectToService();
        mixedConnector.bind(mockMixedServer);
        UNIT_ASSERT(localConnection);

        EXPECT_CALL(*mockMixedServer, messageFromLocalConnection(_, _)).WillRepeatedly(Invoke([&](const SharedMessage& /*msg*/, IMixedServer::LocalConnection& /*connection*/) {
            serverProcessMessage = true;
        }));
        localConnection->send(message);

        flushCallbackQueue(callbackQueue);
    }

    Y_UNIT_TEST(sendRequest)
    {
        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);

        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));
        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(true));
        EXPECT_CALL(*mockMixedServer, addLocalConnection(_));

        mixedConnector.connectToService();
        mixedConnector.bind(mockMixedServer);

        std::string requestId;
        std::weak_ptr<IServer::IClientConnection> weakClientConnection;
        EXPECT_CALL(*mockMixedServer, messageFromLocalConnection(_, _)).WillOnce(Invoke([&](const SharedMessage& message, IMixedServer::LocalConnection& localConnection) {
            UNIT_ASSERT_VALUES_UNEQUAL(message->request_id(), "");
            weakClientConnection = localConnection.share();
            requestId = message->request_id();
        }));

        auto message = ipc::buildUniqueMessage([](auto& msg) {
            msg.mutable_wifi_networks_disable();
        });

        std::atomic<bool> answerReceived{false};
        mixedConnector.sendRequest(
            std::move(message),
            [&](const SharedMessage& msg) {
                UNIT_ASSERT_VALUES_EQUAL(msg->request_id(), requestId);
                UNIT_ASSERT_VALUES_EQUAL(msg->has_wifi_networks_enable(), true);
                answerReceived = true;
            },
            [&](const std::string& text) {
                UNIT_ASSERT_VALUES_EQUAL(text, ""); // always false
            }, std::chrono::seconds{10000});
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_UNEQUAL(requestId, "");

        auto answer = ipc::buildMessage([&](auto& msg) {
            msg.mutable_wifi_networks_enable();
            msg.set_request_id(TString(requestId));
        });

        auto clientConnection = weakClientConnection.lock();
        UNIT_ASSERT(clientConnection);
        clientConnection->send(answer);
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(answerReceived.load(), true);
    }

    Y_UNIT_TEST(sendRequestError)
    {
        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);
        mixedConnector.connectToService();

        auto message = ipc::buildUniqueMessage([](auto& msg) {
            msg.mutable_wifi_networks_disable();
        });

        std::atomic<bool> errorReceived{false};
        mixedConnector.sendRequest(
            std::move(message),
            [&](const SharedMessage& /*msg*/) {
                UNIT_ASSERT(false);
            },
            [&](const std::string& text) {
                UNIT_ASSERT_VALUES_UNEQUAL(text, "");
                errorReceived = true;
            }, std::chrono::seconds{10000});
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(errorReceived.load(), true);
    }

    Y_UNIT_TEST(sendRequestTimeout)
    {
        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        MixedConnector mixedConnector(getDeviceForTests()->sharedConfiguration(), "test", nullptr, callbackQueue);

        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));
        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(true));
        EXPECT_CALL(*mockMixedServer, addLocalConnection(_));

        mixedConnector.connectToService();
        mixedConnector.bind(mockMixedServer);

        EXPECT_CALL(*mockMixedServer, messageFromLocalConnection(_, _));

        auto message = ipc::buildUniqueMessage([](auto& msg) {
            msg.mutable_wifi_networks_disable();
        });

        std::atomic<bool> errorReceived{false};
        mixedConnector.sendRequest(
            std::move(message),
            [&](const SharedMessage& /*msg*/) {
                UNIT_ASSERT(false);
            },
            [&](const std::string& text) {
                UNIT_ASSERT_VALUES_UNEQUAL(text, "");
                UNIT_ASSERT(text.find("The waiting time for a response") != std::string::npos);
                errorReceived = true;
            }, std::chrono::seconds{0});
        flushCallbackQueue(callbackQueue);

        std::this_thread::sleep_for(std::chrono::seconds{1});
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(errorReceived.load(), true);
    }

    Y_UNIT_TEST(destoryQuasarConnectorAfteLocalBinding)
    {
        const auto device = getDeviceForTests();
        std::weak_ptr<ipc::IConnector> wQuasarConnector;
        auto factory =
            [&](auto serviceName, const auto& /*lifetime*/, const auto& /*cbQueue*/) {
                auto quasarConnector = datacratic::createIpcConnector(serviceName, device->sharedConfiguration());
                wQuasarConnector = quasarConnector;
                return quasarConnector;
            };
        MixedConnector mixedConnector(device->sharedConfiguration(), "test", factory, callbackQueue);

        auto mockMixedServer = std::make_shared<quasar::mock::MixedServer>();
        EXPECT_CALL(*mockMixedServer, serviceName()).WillRepeatedly(ReturnRef(SERVICE_NAME_TEST));
        ON_CALL(*mockMixedServer, isStarted()).WillByDefault(Return(true));
        mixedConnector.connectToService();
        flushCallbackQueue(callbackQueue);
        UNIT_ASSERT(!wQuasarConnector.expired());

        EXPECT_CALL(*mockMixedServer, addLocalConnection(_));
        mixedConnector.bind(mockMixedServer);
        flushCallbackQueue(callbackQueue);

        UNIT_ASSERT(wQuasarConnector.expired());
    }
}
