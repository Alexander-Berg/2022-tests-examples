#include "test_tcp_endpoints.h"

#include <yandex_io/libs/ipc/datacratic/tcp_endpoint.h>

#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <mutex>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::ipc::detail::datacratic;

namespace {
    std::mutex gTestMutex;
    SteadyConditionVariable gTestCV;
} // Anonymous namespace

Y_UNIT_TEST_SUITE_F(TestTCPEndpoint, QuasarUnitTestFixtureWithoutIpc) {
    Y_UNIT_TEST(testTCPEndpointSendAll)
    {
        MockTCPEndpoint testEndpoint("testTCPEndpointSendAll");
        int connectedCount = 0;
        testEndpoint.setClientConnectedHandler([&](const std::shared_ptr<TCPConnectionHandler>& /* handler */) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            ++connectedCount;
            gTestCV.notify_all();
        });
        testEndpoint.setClientDisconnectedHandler([&](const std::shared_ptr<TCPConnectionHandler>& /* handler */) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            --connectedCount;
            gTestCV.notify_all();
        });

        int port = testEndpoint.init(getPort());

        // 1 connector ---------------------------
        TestTCPConnector connector1;
        bool connector1Received = false;
        connector1.onMessage = [&](const std::string& message) {
            std::lock_guard<std::mutex> lock(gTestMutex);
            UNIT_ASSERT_VALUES_EQUAL(message, "hello");
            connector1Received = true;
            gTestCV.notify_all();
        };

        connector1.init("localhost", port);

        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&connectedCount]() {
                return connectedCount == 1;
            });
        }

        testEndpoint.sendToAll("hello\n");

        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&connector1Received]() {
                return connector1Received;
            });
        }

        // 2 connectors -----------------------------
        connector1Received = false;
        TestTCPConnector connector2;
        bool connector2Received = false;
        connector2.onMessage = [&](const std::string& message) {
            std::lock_guard<std::mutex> lock(gTestMutex);
            UNIT_ASSERT_VALUES_EQUAL(message, "hello");
            connector2Received = true;
            gTestCV.notify_all();
        };

        connector2.init("localhost", port);

        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&connectedCount]() {
                return connectedCount == 2;
            });
        }

        testEndpoint.sendToAll("hello\n");

        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&connector1Received, &connector2Received]() {
                return connector1Received && connector2Received;
            });
        }

        // connector1 goes down
        connector1.shutdown();
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&connectedCount]() {
                return connectedCount == 1;
            });
            connector1Received = false;
            connector2Received = false;
        }
        testEndpoint.sendToAll("hello\n");
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&connector2Received]() {
                return connector2Received;
            });
            UNIT_ASSERT(!connector1Received);
            UNIT_ASSERT(connector2Received);
        }

        // connector2 goes down
        connector2.shutdown();
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&connectedCount]() {
                return connectedCount == 0;
            });
        }
        testEndpoint.sendToAll("hello\n"); // Nothing should break
    }
}
