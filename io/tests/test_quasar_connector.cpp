#include "test_tcp_endpoints.h"

#include <yandex_io/libs/ipc/datacratic/length_value_tokenizer.h>
#include <yandex_io/libs/ipc/datacratic/quasar_connector.h>
#include <yandex_io/libs/ipc/datacratic/quasar_server.h>

#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>

#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <mutex>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::proto;
using namespace quasar::ipc;
using namespace quasar::ipc::detail::datacratic;

namespace {
    std::mutex gTestMutex;
    SteadyConditionVariable gTestCV;
} // Anonymous namespace

Y_UNIT_TEST_SUITE_F(QuasarConnector, QuasarUnitTestFixtureWithoutIpc) {
    Y_UNIT_TEST(testQuasarConnectorRequestResponse)
    {
        const auto device = getDeviceForTests();
        QuasarServer quasarEndpoint("test", device->sharedConfiguration());
        QuasarMessage requestToSend;
        QuasarMessage responseToSend;
        int onQuasarMessageCalled = 0;
        quasarEndpoint.setMessageHandler([&](const auto& request, auto& connection) {
            ++onQuasarMessageCalled;
            UNIT_ASSERT(request->has_request_id());
            UNIT_ASSERT(request->has_wifi_list_request());
            UNIT_ASSERT_VALUES_EQUAL(request->SerializeAsString(), requestToSend.SerializeAsString());
            responseToSend.set_request_id(request->request_id());
            connection.send(QuasarMessage{responseToSend});
        });

        const int port = quasarEndpoint.listenTcpLocal(getPort());

        QuasarConnector connector("test", device->sharedConfiguration());
        connector.connectToTcpHost("localhost", port);
        connector.waitUntilConnected();

        requestToSend.mutable_wifi_list_request(); // Just set "has" flag

        responseToSend.mutable_wifi_list()->add_hotspots()->set_ssid("myssid");
        responseToSend.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);

        bool resultCalled = false;
        auto onDone = [&](const SharedMessage& response) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_ASSERT_VALUES_EQUAL(response->SerializeAsString(), responseToSend.SerializeAsString());
            UNIT_ASSERT_VALUES_EQUAL(response->wifi_list().hotspots(0).ssid(), "myssid");
            resultCalled = true;
            gTestCV.notify_all();
        };

        auto onError = [&](const std::string& errorMessage) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_FAIL(errorMessage);
            resultCalled = true;
            gTestCV.notify_all();
        };

        connector.sendRequest(std::move(requestToSend), onDone, onError, std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled;
            });
        }

        UNIT_ASSERT_VALUES_EQUAL(onQuasarMessageCalled, 1);
    }

    Y_UNIT_TEST(testQuasarConnectorRequestSync)
    {
        const auto device = getDeviceForTests();
        QuasarConnector connector("test", device->sharedConfiguration());
        QuasarMessage requestToSend;
        requestToSend.mutable_wifi_list_request(); // Just set "has" flag

        UNIT_ASSERT_EXCEPTION(connector.sendRequestSync(QuasarMessage{requestToSend}, std::chrono::milliseconds(100)), std::runtime_error); // Not connected

        QuasarServer quasarEndpoint("test", device->sharedConfiguration());
        QuasarMessage responseToSend;

        responseToSend.mutable_wifi_list()->add_hotspots()->set_ssid("myssid");
        responseToSend.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);
        int port = quasarEndpoint.listenTcpLocal(getPort());
        int onQuasarMessageCalled = 0;
        quasarEndpoint.setMessageHandler([&](const auto& request, auto& connection) {
            ++onQuasarMessageCalled;
            responseToSend.set_request_id(request->request_id());
            if (1 == onQuasarMessageCalled) {
                connection.send(QuasarMessage{responseToSend});
            }
        });

        connector.connectToTcpHost("localhost", port);
        connector.waitUntilConnected();

        SharedMessage response = connector.sendRequestSync(QuasarMessage{requestToSend}, std::chrono::seconds(600));
        UNIT_ASSERT_VALUES_EQUAL(response->wifi_list().hotspots(0).ssid(), "myssid");

        UNIT_ASSERT_EXCEPTION(connector.sendRequestSync(QuasarMessage{requestToSend}, std::chrono::milliseconds(100)), std::runtime_error); // Timeout
    }

    Y_UNIT_TEST(testQuasarConnectorTimeout)
    {
        const auto device = getDeviceForTests();
        QuasarServer quasarEndpoint("test", device->sharedConfiguration());
        QuasarMessage requestToSend;
        QuasarMessage responseToSend;
        int onMessageCalled = 0;
        quasarEndpoint.setMessageHandler([&](const auto& request, auto& connection) {
            ++onMessageCalled;
            if (2 == onMessageCalled)
            {
                responseToSend.set_request_id(request->request_id());
                connection.send(QuasarMessage{responseToSend});
            }
        });

        const int port = quasarEndpoint.listenTcpLocal(getPort());

        QuasarConnector quasarConnector("test", device->sharedConfiguration());
        quasarConnector.connectToTcpHost("localhost", port);
        UNIT_ASSERT(quasarConnector.waitUntilConnected(std::chrono::seconds(1)));

        bool resultCalled = false;
        int onDoneCalled = 0;
        int onErrorCalled = 0;
        auto onDone = [&](const SharedMessage& /* response */) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            ++onDoneCalled;
            resultCalled = true;
            gTestCV.notify_all();
        };

        auto onError = [&](const std::string& errorMessage) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            ++onErrorCalled;
            YIO_LOG_INFO(errorMessage);
            resultCalled = true;
            gTestCV.notify_all();
        };

        // expect timeout
        quasarConnector.sendRequest(QuasarMessage{requestToSend}, onDone, onError, std::chrono::milliseconds(100));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled;
            });
        }

        UNIT_ASSERT_VALUES_EQUAL(onDoneCalled, 0);
        UNIT_ASSERT_VALUES_EQUAL(onErrorCalled, 1);
        UNIT_ASSERT_VALUES_EQUAL(onMessageCalled, 1);

        resultCalled = false;
        // expect done
        quasarConnector.sendRequest(QuasarMessage{requestToSend}, onDone, onError, std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled;
            });
        }
        UNIT_ASSERT_VALUES_EQUAL(onDoneCalled, 1);
        UNIT_ASSERT_VALUES_EQUAL(onErrorCalled, 1);
        UNIT_ASSERT_VALUES_EQUAL(onMessageCalled, 2);
    }

    Y_UNIT_TEST(testQuasarConnectorWrongOrder)
    {
        const auto device = getDeviceForTests();
        QuasarServer quasarEndpoint("test", device->sharedConfiguration());
        QuasarMessage request1;
        QuasarMessage request2;
        QuasarMessage response1;
        QuasarMessage response2;
        int onMessageCalled = 0;
        quasarEndpoint.setMessageHandler([&](const auto& request, auto& connection) {
            ++onMessageCalled;
            if (1 == onMessageCalled)
            {
                response1.set_request_id(request->request_id());
            } else {
                Y_VERIFY(2 == onMessageCalled);
                response2.set_request_id(request->request_id());
                connection.send(QuasarMessage{response2});
                connection.send(QuasarMessage{response1});
            }
        });

        const int port = quasarEndpoint.listenTcpLocal(getPort());

        QuasarConnector quasarConnector("test", device->sharedConfiguration());
        quasarConnector.connectToTcpHost("localhost", port);
        UNIT_ASSERT(quasarConnector.waitUntilConnected(std::chrono::seconds(1)));

        request1.mutable_wifi_list_request();
        request2.mutable_wifi_list_request();

        response1.mutable_wifi_list()->add_hotspots()->set_ssid("ssid1");
        response1.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);
        response2.mutable_wifi_list()->add_hotspots()->set_ssid("ssid2");
        response2.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);

        int responseReceived = 0;
        bool done = false;
        auto onDone = [&](const SharedMessage& response, int requestNumber) {
            if (1 == requestNumber)
            {
                UNIT_ASSERT_VALUES_EQUAL(response->wifi_list().hotspots(0).ssid(), "ssid1");
            } else {
                Y_VERIFY(2 == requestNumber);
                UNIT_ASSERT_VALUES_EQUAL(response->wifi_list().hotspots(0).ssid(), "ssid2");
            }
            ++responseReceived;
            if (2 == responseReceived)
            {
                std::lock_guard<std::mutex> guard(gTestMutex);
                done = true;
                gTestCV.notify_all();
            }
        };

        quasarConnector.sendRequest(std::move(request1), std::bind(onDone, std::placeholders::_1, 1), QuasarConnector::OnError(), std::chrono::seconds(600));
        quasarConnector.sendRequest(std::move(request2), std::bind(onDone, std::placeholders::_1, 2), QuasarConnector::OnError(), std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&done]() {
                return done;
            });
        }
    }

    Y_UNIT_TEST(testQuasarConnectorDeadLock)
    {
        const auto device = getDeviceForTests();
        QuasarMessage response;
        std::unique_ptr<QuasarServer> quasarEndpoint(new QuasarServer("test", device->sharedConfiguration()));
        const int port = quasarEndpoint->listenTcpLocal(getPort());
        QuasarConnector quasarConnector("test", device->sharedConfiguration());
        quasarConnector.connectToTcpHost("localhost", port);
        quasarEndpoint->setMessageHandler([&](const auto& request, auto& connection) {
            response.set_request_id(request->request_id());
            connection.send(QuasarMessage{response});
            connection.scheduleClose();
        });

        quasarConnector.waitUntilConnected();
        quasarConnector.setSilentMode(true);
        bool stopped = false;
        auto sendRequestThread = std::thread([&]() {
            while (!stopped)
            {
                auto onDone = [&](const SharedMessage& /* response */) {

                };
                auto onError = [&](const std::string& /* errorMessage */) {

                };
                QuasarMessage request;
                quasarConnector.sendRequest(std::move(request), onDone, onError, std::chrono::seconds(600));
            }
        });

        std::this_thread::sleep_for(std::chrono::seconds(1));

        stopped = true;

        sendRequestThread.join();
    }

    Y_UNIT_TEST(testQuasarConnectorParseError)
    {
        const auto device = getDeviceForTests();
        QuasarServer quasarEndpoint("test", device->sharedConfiguration());
        QuasarMessage requestToSend;
        int onMessageCalled = 0;
        quasarEndpoint.setMessageHandler([&](const auto& /*msg*/, auto& connection) {
            ++onMessageCalled;
            connection.unsafeSendBytes(LengthValueTokenizer::getLengthValue("abracadabra"));
        });

        const int port = quasarEndpoint.listenTcpLocal(getPort());

        QuasarConnector quasarConnector("test", device->sharedConfiguration());
        quasarConnector.connectToTcpHost("localhost", port);
        quasarConnector.waitUntilConnected();

        requestToSend.mutable_wifi_list_request();

        bool resultCalled = false;
        auto onDone = [&](const SharedMessage& /* response */) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_FAIL("onDone shouldn't be called");
            resultCalled = true;
            gTestCV.notify_all();
        };

        auto onError = [&](const std::string& errorMessage) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            YIO_LOG_INFO(errorMessage);
            resultCalled = true;
            gTestCV.notify_all();
        };

        quasarConnector.sendRequest(QuasarMessage{requestToSend}, onDone, onError, std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled;
            });
        }

        UNIT_ASSERT_VALUES_EQUAL(onMessageCalled, 1);

        quasarConnector.waitUntilDisconnected();
        quasarConnector.waitUntilConnected();

        QuasarMessage responseToSend;
        quasarEndpoint.setMessageHandler([&](const auto& request, auto& connection) {
            ++onMessageCalled;
            responseToSend.set_request_id(request->request_id());
            connection.send(QuasarMessage{responseToSend});
        });

        resultCalled = 0;
        auto onDone2 = [&](const SharedMessage& response) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_ASSERT_VALUES_EQUAL(response->SerializeAsString(), responseToSend.SerializeAsString());
            resultCalled = true;
            gTestCV.notify_all();
        };

        auto onError2 = [&](const std::string& errorMessage) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_FAIL(errorMessage);
            resultCalled = true;
            gTestCV.notify_all();
        };

        quasarConnector.sendRequest(QuasarMessage{requestToSend}, onDone2, onError2, std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled;
            });
        }
        UNIT_ASSERT_VALUES_EQUAL(onMessageCalled, 2);
    }

    Y_UNIT_TEST(testQuasarConnectorShutdownConnected)
    {
        const auto device = getDeviceForTests();
        MockTCPEndpoint endpoint("testSpeechKitConnectorShutdownConnected");
        int port = endpoint.init(getPort());
        QuasarConnector connector("test", device->sharedConfiguration());
        connector.connectToTcpHost("localhost", port);
        connector.waitUntilConnected();
    }

    Y_UNIT_TEST(testQuasarConnectorTryConnectToServiceWithoutEntryInConfig)
    {
        const auto device = getDeviceForTests();
        QuasarConnector connector("test", device->sharedConfiguration());
        const bool res = connector.tryConnectToService();
        /* no such entry in config. method should return false */
        UNIT_ASSERT(!res);
        QuasarMessage stub;
        connector.sendMessage(QuasarMessage{stub});
        connector.sendRequest(
            QuasarMessage{stub}, [](const SharedMessage& /*msg*/) {}, [](const std::string& /*error*/) {}, std::chrono::seconds(600));
        try {
            connector.sendRequestSync(QuasarMessage{stub}, std::chrono::milliseconds(3));
        } catch (const std::runtime_error& e) {
            /* connector throw runtime_error on timeout */
        }
        /* Make sure that reached this code */
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST(testQuasarConnectorTryConnectToServiceWithEntryInConfig)
    {
        const auto device = getDeviceForTests();
        QuasarServer mockServer("random", device->sharedConfiguration());
        std::promise<void> sendMessagePromise;
        mockServer.setMessageHandler([&](const auto& quasarMessage, auto& connection) {
            if (quasarMessage->has_request_id()) {
                QuasarMessage message;
                message.set_request_id(quasarMessage->request_id());
                /* set some flag */
                message.mutable_external_command_response()->set_processed(true);
                connection.send(QuasarMessage{message});
            }
            if (quasarMessage->has_io_control() && quasarMessage->io_control().has_toggle_setup_mode()) {
                sendMessagePromise.set_value();
            }
        });
        YandexIO::Configuration::TestGuard testGuard;
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["random"]["port"] = mockServer.listenTcpLocal(getPort());

        QuasarConnector connector("random", device->sharedConfiguration());
        const bool res = connector.tryConnectToService();
        /* There is such entry in config. method should return true */
        UNIT_ASSERT(res);

        connector.waitUntilConnected();
        QuasarMessage sendMessageCheckMsg;
        /* Set some message "flag", so server will set up promise */
        sendMessageCheckMsg.mutable_io_control()->mutable_toggle_setup_mode();
        connector.sendMessage(std::move(sendMessageCheckMsg));
        /* Make sure that sendMessage works */
        sendMessagePromise.get_future().get();

        std::promise<bool> asyncRequestPromise;
        auto onDone = [&](const SharedMessage& msg) {
            asyncRequestPromise.set_value(msg->external_command_response().processed());
        };
        auto onError = [](const std::string& msg) {
            UNIT_FAIL(std::string("Send Request fail!: ") + msg);
        };

        QuasarMessage requestMessageCheckMsg; /* empty stub message for requests */
        connector.sendRequest(QuasarMessage{requestMessageCheckMsg}, std::move(onDone), std::move(onError), std::chrono::seconds(600));

        /* Check that async request work */
        const auto result = asyncRequestPromise.get_future().get();
        UNIT_ASSERT(result);

        /* Check that sync request work */
        const auto response = connector.sendRequestSync(QuasarMessage{requestMessageCheckMsg}, std::chrono::seconds(600));
        UNIT_ASSERT(response->external_command_response().processed());
    }
}
