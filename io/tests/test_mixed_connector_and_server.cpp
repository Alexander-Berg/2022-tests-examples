#include <yandex_io/libs/ipc/mixed/mixed_server.h>
#include <yandex_io/libs/ipc/mixed/mixed_connector.h>

#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/protos/quasar_proto.pb.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

using namespace quasar;
using namespace quasar::ipc;
using namespace quasar::ipc::detail::mixed;
using namespace quasar::TestUtils;

namespace {
    class Fixture: public QuasarUnitTestFixture {
    public:
        using Base = QuasarUnitTestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            portForServer = getPort();
            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["test"]["port"] = portForServer;

            cqServer = std::make_shared<NamedCallbackQueue>("MixedServer");
            cqConnector = std::make_shared<NamedCallbackQueue>("MixedConnector");
        }

        std::shared_ptr<MixedServer> createAndSetupMixedServer()
        {
            auto mixedServer = std::make_shared<MixedServer>("test", nullptr, cqServer);
            mixedServer->setMessageHandler(
                [&](const auto& /*unused*/, auto& client) {
                    ++serverMessageCounter;
                    client.send(proto::QuasarMessage());
                });
            mixedServer->setClientConnectedHandler(
                [&](auto& /*unused*/) {
                    UNIT_ASSERT(!serverConnectCounter.load());
                    UNIT_ASSERT(!serverDisconnectCounter.load());
                    ++serverConnectCounter;
                });
            mixedServer->setClientDisconnectedHandler(
                [&](auto& /*unused*/) {
                    UNIT_ASSERT(serverConnectCounter.load());
                    UNIT_ASSERT(!serverDisconnectCounter.load());
                    ++serverDisconnectCounter;
                });
            return mixedServer;
        }

        std::shared_ptr<MixedConnector> createAndSetupMixedConnector()
        {
            auto mixedConnector = std::make_shared<MixedConnector>(getDeviceForTests()->sharedConfiguration(), "test", nullptr, cqConnector);
            mixedConnector->setMessageHandler(
                [&](const auto& /*unused*/) {
                    ++clientMessageCounter;
                });
            mixedConnector->setConnectHandler(
                [&] {
                    UNIT_ASSERT(!clientConnectCounter.load());
                    UNIT_ASSERT(!clientDisconnectCounter.load());
                    ++clientConnectCounter;
                });
            mixedConnector->setDisconnectHandler(
                [&] {
                    UNIT_ASSERT(clientConnectCounter.load());
                    UNIT_ASSERT(!clientDisconnectCounter.load());
                    ++clientDisconnectCounter;
                });
            return mixedConnector;
        }

        void checkInitialAsserts()
        {
            UNIT_ASSERT(!serverMessageCounter.load());
            UNIT_ASSERT(!serverConnectCounter.load());
            UNIT_ASSERT(!serverDisconnectCounter.load());
            UNIT_ASSERT(!clientMessageCounter.load());
            UNIT_ASSERT(!clientConnectCounter.load());
            UNIT_ASSERT(!clientDisconnectCounter.load());
        }

        YandexIO::Configuration::TestGuard testGuard;
        std::shared_ptr<NamedCallbackQueue> cqConnector;
        std::shared_ptr<NamedCallbackQueue> cqServer;
        int portForServer;

        std::atomic<int> serverMessageCounter{0};
        std::atomic<int> serverConnectCounter{0};
        std::atomic<int> serverDisconnectCounter{0};
        std::atomic<int> clientMessageCounter{0};
        std::atomic<int> clientConnectCounter{0};
        std::atomic<int> clientDisconnectCounter{0};
    };
} // namespace

Y_UNIT_TEST_SUITE_F(MixedConnectorAndServer, Fixture) {
    Y_UNIT_TEST(Bind_Listen_Connect)
    {
        auto mixedServer = createAndSetupMixedServer();
        auto mixedConnector = createAndSetupMixedConnector();

        checkInitialAsserts();
        mixedConnector->bind(mixedServer);
        mixedServer->listenService();
        checkInitialAsserts();
        mixedConnector->connectToService();

        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverConnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientConnectCounter.load(), 1);

        mixedConnector->sendMessage(proto::QuasarMessage{});
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverMessageCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientMessageCounter.load(), 1);

        mixedConnector->unbind();
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverDisconnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientDisconnectCounter.load(), 1);
    }

    Y_UNIT_TEST(Bind_Connect_Listen)
    {
        auto mixedServer = createAndSetupMixedServer();
        auto mixedConnector = createAndSetupMixedConnector();

        checkInitialAsserts();
        mixedConnector->bind(mixedServer);
        mixedConnector->connectToService();
        checkInitialAsserts();
        mixedServer->listenService();

        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverConnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientConnectCounter.load(), 1);

        mixedConnector->sendMessage(proto::QuasarMessage{});
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverMessageCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientMessageCounter.load(), 1);

        mixedConnector->unbind();
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverDisconnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientDisconnectCounter.load(), 1);
    }

    Y_UNIT_TEST(Listen_Bind_Connect)
    {
        auto mixedServer = createAndSetupMixedServer();
        auto mixedConnector = createAndSetupMixedConnector();

        checkInitialAsserts();
        mixedServer->listenService();
        mixedConnector->bind(mixedServer);
        checkInitialAsserts();
        mixedConnector->connectToService();

        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverConnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientConnectCounter.load(), 1);

        mixedConnector->sendMessage(proto::QuasarMessage{});
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverMessageCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientMessageCounter.load(), 1);

        mixedConnector->unbind();
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverDisconnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientDisconnectCounter.load(), 1);
    }

    Y_UNIT_TEST(Listen_Connect_Bind)
    {
        auto mixedServer = createAndSetupMixedServer();
        auto mixedConnector = createAndSetupMixedConnector();

        checkInitialAsserts();
        mixedServer->listenService();
        mixedConnector->connectToService();
        checkInitialAsserts();
        mixedConnector->bind(mixedServer);

        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverConnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientConnectCounter.load(), 1);

        mixedConnector->sendMessage(proto::QuasarMessage{});
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverMessageCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientMessageCounter.load(), 1);

        mixedConnector->unbind();
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverDisconnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientDisconnectCounter.load(), 1);
    }

    Y_UNIT_TEST(Connect_Listen_Bind)
    {
        auto mixedServer = createAndSetupMixedServer();
        auto mixedConnector = createAndSetupMixedConnector();

        checkInitialAsserts();
        mixedConnector->connectToService();
        mixedServer->listenService();
        checkInitialAsserts();
        mixedConnector->bind(mixedServer);

        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverConnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientConnectCounter.load(), 1);

        mixedConnector->sendMessage(proto::QuasarMessage{});
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverMessageCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientMessageCounter.load(), 1);

        mixedConnector->unbind();
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverDisconnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientDisconnectCounter.load(), 1);
    }

    Y_UNIT_TEST(Connect_Bind_Listen)
    {
        auto mixedServer = createAndSetupMixedServer();
        auto mixedConnector = createAndSetupMixedConnector();

        checkInitialAsserts();
        mixedConnector->connectToService();
        mixedConnector->bind(mixedServer);
        checkInitialAsserts();
        mixedServer->listenService();

        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverConnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientConnectCounter.load(), 1);

        mixedConnector->sendMessage(proto::QuasarMessage{});
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverMessageCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientMessageCounter.load(), 1);

        mixedConnector->unbind();
        flushCallbackQueue(cqServer, cqConnector);
        UNIT_ASSERT_VALUES_EQUAL(serverDisconnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientDisconnectCounter.load(), 1);
    }

    Y_UNIT_TEST(DeleteServer)
    {
        auto mixedServer = createAndSetupMixedServer();
        auto mixedConnector = createAndSetupMixedConnector();

        mixedServer->listenService();
        mixedConnector->connectToService();
        mixedConnector->bind(mixedServer);

        flushCallbackQueue(cqServer, cqConnector);

        mixedServer.reset();
        flushCallbackQueue(cqServer, cqConnector);

        UNIT_ASSERT_VALUES_EQUAL(serverDisconnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientDisconnectCounter.load(), 1);
    }

    Y_UNIT_TEST(DeleteConnector)
    {
        auto mixedServer = createAndSetupMixedServer();
        auto mixedConnector = createAndSetupMixedConnector();

        mixedServer->listenService();
        mixedConnector->connectToService();
        mixedConnector->bind(mixedServer);

        flushCallbackQueue(cqServer, cqConnector);

        mixedConnector.reset();
        flushCallbackQueue(cqServer, cqConnector);

        UNIT_ASSERT_VALUES_EQUAL(serverDisconnectCounter.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(clientDisconnectCounter.load(), 1);
    }

    Y_UNIT_TEST(sendRequestSyncOK)
    {
        std::string uuid;

        auto mixedServer = std::make_shared<MixedServer>("test", nullptr, cqServer);
        auto mixedConnector = std::make_shared<MixedConnector>(getDeviceForTests()->sharedConfiguration(), "test", nullptr, cqConnector);

        mixedServer->setMessageHandler(
            [&](const auto& msg, auto& client) {
                UNIT_ASSERT(!msg->request_id().empty());
                UNIT_ASSERT(uuid.empty());

                uuid = msg->request_id();
                client.send(ipc::buildMessage([&](auto& msg) {
                    msg.set_request_id(TString(uuid));
                }));
            });

        mixedConnector->bind(mixedServer);
        mixedServer->listenService();
        mixedConnector->connectToService();
        flushCallbackQueue(cqServer, cqConnector);

        auto replay = mixedConnector->sendRequestSync(proto::QuasarMessage{}, std::chrono::seconds{1});
        UNIT_ASSERT(replay);
        UNIT_ASSERT(!replay->request_id().empty());
    }

    Y_UNIT_TEST(sendRequestSyncTimeout)
    {
        std::string uuid;

        auto mixedServer = std::make_shared<MixedServer>("test", nullptr, cqServer);
        auto mixedConnector = std::make_shared<MixedConnector>(getDeviceForTests()->sharedConfiguration(), "test", nullptr, cqConnector);

        mixedConnector->bind(mixedServer);
        mixedServer->listenService();
        mixedConnector->connectToService();
        flushCallbackQueue(cqServer, cqConnector);

        UNIT_ASSERT_EXCEPTION(
            mixedConnector->sendRequestSync(proto::QuasarMessage{}, std::chrono::seconds{1}),
            std::runtime_error);
    }

    Y_UNIT_TEST(sendRequestSyncDeadlock)
    {
        std::string uuid;

        auto mixedServer = std::make_shared<MixedServer>("test", nullptr, cqServer);
        auto mixedConnector = std::make_shared<MixedConnector>(getDeviceForTests()->sharedConfiguration(), "test", nullptr, cqConnector);

        mixedConnector->bind(mixedServer);
        mixedServer->listenService();
        mixedConnector->connectToService();
        flushCallbackQueue(cqServer, cqConnector);

        std::atomic<bool> fContinue{false};
        cqConnector->add([&] { doUntil([&] { return fContinue.load(); }, 10000); });

        UNIT_ASSERT_EXCEPTION(
            mixedConnector->sendRequestSync(proto::QuasarMessage{}, std::chrono::seconds{1}),
            std::logic_error);

        fContinue = true;
        flushCallbackQueue(cqServer, cqConnector);
    }

} // Y_UNIT_TEST_SUITE_F(MixedConnectorAndServer)
