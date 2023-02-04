#include "test_tcp_endpoints.h"

#include <yandex_io/libs/ipc/datacratic/quasar_server.h>

#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/threading/callback_queue.h>
#include <yandex_io/libs/threading/lifetime.h>

#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::ipc;
using namespace quasar::ipc::detail::datacratic;

Y_UNIT_TEST_SUITE_F(TestQuasarServerCQ, QuasarUnitTestFixtureWithoutIpc) {
    Y_UNIT_TEST(testServiceName)
    {
        const auto device = getDeviceForTests();
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();
        QuasarServer quasarServer("test", device->sharedConfiguration(), lifetime, callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(quasarServer.serviceName(), "test");
    }

    Y_UNIT_TEST(testTCPEndpointSendAll)
    {
        const auto device = getDeviceForTests();
        Lifetime lifetime;
        auto callbackQueue = std::make_shared<CallbackQueue>();
        QuasarServer quasarServer("test", device->sharedConfiguration(), lifetime, callbackQueue);

        std::atomic<int> connectedCount{0};
        std::atomic<bool> received{false};
        std::atomic<int> disconnectedCount{0};
        quasarServer.setMessageHandler([&](const SharedMessage& /*msg*/, auto& /* connection */) {
            UNIT_ASSERT(callbackQueue->isWorkingThread());
            received = true;
        });
        quasarServer.setClientConnectedHandler([&](auto& /* connection */) {
            UNIT_ASSERT(callbackQueue->isWorkingThread());
            ++connectedCount;
        });
        quasarServer.setClientDisconnectedHandler([&](auto& /* connection */) {
            UNIT_ASSERT(callbackQueue->isWorkingThread());
            --disconnectedCount;
        });

        int port = quasarServer.listenTcpLocal(getPort());

        // connector ---------------------------
        {
            TestTCPConnector connector1;
            std::atomic<bool> connector1Received{false};
            connector1.onMessage = [&](const std::string& /*msg*/) {
                connector1Received = true;
            };
            connector1.init("localhost", port);

            doUntil([&]() { return connectedCount != 0; }, 5 * 1000);

            quasar::proto::QuasarMessage message;
            quasarServer.sendToAll(std::move(message));

            doUntil([&]() { return received != 0; }, 5 * 1000);
        }

        doUntil([&]() { return disconnectedCount != 0; }, 5 * 1000);
    }
}
