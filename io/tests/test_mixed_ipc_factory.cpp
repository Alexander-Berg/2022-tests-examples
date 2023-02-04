#include "mock_local_connection.h"

#include <yandex_io/libs/ipc/mixed/mixed_ipc_factory.h>

#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>

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

        struct ServiceDesc {
            std::string name;
            MixedIpcFactory::Transport transport;
            std::shared_ptr<ICallbackQueue> callback;
            std::shared_ptr<std::atomic<bool>> fCreated;
            std::shared_ptr<std::atomic<bool>> fDeleted;
            std::shared_ptr<std::atomic<bool>> fListen;
            std::shared_ptr<std::atomic<bool>> fShutdown;
        };

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["test"]["port"] = getPort();
            config["testMixed"]["port"] = getPort();
            config["testNet"]["port"] = getPort();
            config["testLocal"] = Json::objectValue;
            config["common"]["ipc"]["mixed"].append("testMixed");
            config["common"]["ipc"]["net"].append("testNet");
            config["common"]["ipc"]["local"].append("testLocal");
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        YandexIO::Configuration::TestGuard testGuard;
        std::shared_ptr<NamedCallbackQueue> callbackQueue;
    };
} // namespace

Y_UNIT_TEST_SUITE_F(MixedIpcFactory, Fixture) {
    Y_UNIT_TEST(Ctor)
    {
        auto context = std::make_shared<MixedIpcFactory::Context>(
            MixedIpcFactory::Context{
                .configuration = getDeviceForTests()->sharedConfiguration(),
            });
        MixedIpcFactory mixedIpcFactory(context, Name_);
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST(Test1)
    {
        std::shared_ptr<ICallbackQueue> serverCallbackQueue;
        std::shared_ptr<ICallbackQueue> connectorCallbackQueue;
        auto callbackQueueCreator =
            [&](const std::string& serviceName, MixedIpcFactory::Purpose type, bool /*unused*/) -> std::shared_ptr<ICallbackQueue> {
            switch (type) {
                case MixedIpcFactory::Purpose::FACTORY:
                    return std::make_shared<NamedCallbackQueue>("FACTORY");
                case MixedIpcFactory::Purpose::MIXED_SERVER:
                    serverCallbackQueue = std::make_shared<NamedCallbackQueue>("SERVER:" + serviceName);
                    return serverCallbackQueue;
                case MixedIpcFactory::Purpose::MIXED_CONNECTOR:
                    connectorCallbackQueue = std::make_shared<NamedCallbackQueue>("CONNECTOR:" + serviceName);
                    return connectorCallbackQueue;
                default:
                    throw std::runtime_error("error");
            }
        };
        auto context = std::make_shared<MixedIpcFactory::Context>(
            MixedIpcFactory::Context{
                .configuration = getDeviceForTests()->sharedConfiguration(),
                .transport = MixedIpcFactory::Transport::LOCAL,
                .callbackQueueCreator = callbackQueueCreator,
            });

        MixedIpcFactory mixedIpcFactory(context, Name_);
        auto server = mixedIpcFactory.createIpcServer("test");
        UNIT_ASSERT(server);
        UNIT_ASSERT(serverCallbackQueue);

        std::atomic<int> serverOnConnected{0};
        server->setClientConnectedHandler(
            [&](auto& connection) {
                ++serverOnConnected;
                auto* localConnection = dynamic_cast<IMixedServer::LocalConnection*>(&connection);
                UNIT_ASSERT(localConnection);
            });

        std::atomic<int> connectorOnConnected{0};
        auto connector = mixedIpcFactory.createIpcConnector("test");
        connector->setConnectHandler([&] { ++connectorOnConnected; });
        UNIT_ASSERT(connector);
        UNIT_ASSERT(connectorCallbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(serverOnConnected.load(), 0);
        UNIT_ASSERT_VALUES_EQUAL(connectorOnConnected.load(), 0);

        server->listenService();
        flushCallbackQueue(serverCallbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(serverOnConnected.load(), 0);
        UNIT_ASSERT_VALUES_EQUAL(connectorOnConnected.load(), 0);

        connector->connectToService();

        flushCallbackQueue(serverCallbackQueue);
        flushCallbackQueue(connectorCallbackQueue);

        UNIT_ASSERT_VALUES_EQUAL(serverOnConnected.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(connectorOnConnected.load(), 1);
    }

} // Y_UNIT_TEST_SUITE_F(MixedIpcFactory)
