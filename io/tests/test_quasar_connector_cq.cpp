#include <yandex_io/libs/ipc/datacratic/length_value_tokenizer.h>
#include <yandex_io/libs/ipc/datacratic/quasar_connector.h>
#include <yandex_io/libs/ipc/datacratic/quasar_server.h>

#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/threading/callback_queue.h>
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

    class MockCallbackQueue: public CallbackQueue {
    public:
        MockCallbackQueue()
        {
        }

        void add(std::function<void()> callback) override {
            ++scheduleCounter;
            CallbackQueue::add([this, callback{std::move(callback)}] { callback(); ++doneCounter; });
        }

        void add(std::function<void()> callback, Lifetime::Tracker tracker) override {
            ++scheduleCounter;
            CallbackQueue::add([this, callback{std::move(callback)}] { callback(); ++doneCounter; }, tracker);
        }
        void addDelayed(std::function<void()> callback, std::chrono::milliseconds timeOut) override {
            ++scheduleCounter;
            CallbackQueue::addDelayed([this, callback{std::move(callback)}] { callback(); ++doneCounter; }, timeOut);
        }
        void addDelayed(std::function<void()> callback, std::chrono::milliseconds timeOut, Lifetime::Tracker tracker) override {
            ++scheduleCounter;
            CallbackQueue::addDelayed([this, callback{std::move(callback)}] { callback(); ++doneCounter; }, timeOut, tracker);
        }

        std::atomic<int> scheduleCounter{0};
        std::atomic<int> doneCounter{0};
    };

} // Anonymous namespace

Y_UNIT_TEST_SUITE_F(QuasarConnectorCQ, QuasarUnitTestFixtureWithoutIpc) {
    Y_UNIT_TEST(testServiceName)
    {
        const auto device = getDeviceForTests();
        auto callbackQueue = std::make_shared<MockCallbackQueue>();

        QuasarConnector connector("test", device->sharedConfiguration(), Lifetime::immortal, callbackQueue);
        UNIT_ASSERT_VALUES_EQUAL(connector.serviceName(), "test");
    }

    Y_UNIT_TEST(testQuasarConnectorRequestResponseCQ)
    {
        const auto device = getDeviceForTests();
        auto callbackQueue = std::make_shared<MockCallbackQueue>();

        QuasarServer quasarEndpoint("test", device->sharedConfiguration());
        QuasarMessage requestToSend;
        QuasarMessage responseToSend;
        int onQuasarMessageCalled = 0;
        quasarEndpoint.setMessageHandler([&](const SharedMessage& request, auto& connection) {
            ++onQuasarMessageCalled;
            UNIT_ASSERT(request->has_request_id());
            UNIT_ASSERT(request->has_wifi_list_request());
            responseToSend.set_request_id(request->request_id());
            connection.send(QuasarMessage{responseToSend});
        });

        const int port = quasarEndpoint.listenTcpLocal(getPort());

        QuasarConnector connector("test", device->sharedConfiguration(), Lifetime::immortal, callbackQueue);
        connector.connectToTcpHost("localhost", port);
        UNIT_ASSERT(connector.waitUntilConnected(std::chrono::seconds(1)));

        requestToSend.mutable_wifi_list_request(); // Just set "has" flag

        responseToSend.mutable_wifi_list()->add_hotspots()->set_ssid("myssid");
        responseToSend.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);

        bool resultCalled = false;
        auto onDone = [&](const SharedMessage& response) {
            UNIT_ASSERT(callbackQueue->isWorkingThread());

            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_ASSERT_VALUES_EQUAL(response->wifi_list().hotspots(0).ssid(), "myssid");
            resultCalled = true;
            gTestCV.notify_all();
        };

        auto onError = [&](const std::string& errorMessage) {
            UNIT_ASSERT(callbackQueue->isWorkingThread());

            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_FAIL(errorMessage);
            resultCalled = true;
            gTestCV.notify_all();
        };

        connector.sendRequest(QuasarMessage{requestToSend}, onDone, onError, std::chrono::seconds(5));
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
        auto callbackQueue = std::make_shared<MockCallbackQueue>();

        QuasarConnector connector("test", device->sharedConfiguration(), Lifetime::immortal, callbackQueue);
        QuasarMessage requestToSend;
        requestToSend.mutable_wifi_list_request(); // Just set "has" flag

        UNIT_ASSERT_EXCEPTION(connector.sendRequestSync(QuasarMessage{requestToSend}, std::chrono::milliseconds(100)), std::runtime_error); // Not connected

        QuasarServer quasarEndpoint("test", device->sharedConfiguration());
        QuasarMessage responseToSend;

        responseToSend.mutable_wifi_list()->add_hotspots()->set_ssid("myssid");
        responseToSend.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);
        int port = quasarEndpoint.listenTcpLocal(getPort());
        int onQuasarMessageCalled = 0;
        quasarEndpoint.setMessageHandler([&](const SharedMessage& request, auto& connection) {
            ++onQuasarMessageCalled;
            responseToSend.set_request_id(request->request_id());
            if (1 == onQuasarMessageCalled) {
                connection.send(QuasarMessage{responseToSend});
            }
        });

        connector.connectToTcpHost("localhost", port);
        connector.waitUntilConnected();

        int doneCounter = callbackQueue->doneCounter.load();
        SharedMessage response = connector.sendRequestSync(QuasarMessage{requestToSend}, std::chrono::milliseconds(100));
        UNIT_ASSERT_VALUES_EQUAL(response->wifi_list().hotspots(0).ssid(), "myssid");
        UNIT_ASSERT(callbackQueue->doneCounter.load() >= doneCounter); // OnDone event will be masrshaled via callbackQueue

        UNIT_ASSERT_EXCEPTION(connector.sendRequestSync(QuasarMessage{requestToSend}, std::chrono::milliseconds(100)), std::runtime_error); // Timeout
    }

    Y_UNIT_TEST(testQuasarConnectorTimeout)
    {
        const auto device = getDeviceForTests();
        auto callbackQueue = std::make_shared<MockCallbackQueue>();

        QuasarServer quasarEndpoint("test", device->sharedConfiguration());
        QuasarMessage requestToSend;
        QuasarMessage responseToSend;
        int onMessageCalled = 0;
        quasarEndpoint.setMessageHandler([&](const SharedMessage& request, auto& connection) {
            ++onMessageCalled;
            if (2 == onMessageCalled)
            {
                responseToSend.set_request_id(request->request_id());
                connection.send(QuasarMessage{responseToSend});
            }
        });

        const int port = quasarEndpoint.listenTcpLocal(getPort());

        QuasarConnector quasarConnector("test", device->sharedConfiguration(), Lifetime::immortal, callbackQueue);
        quasarConnector.connectToTcpHost("localhost", port);
        UNIT_ASSERT(quasarConnector.waitUntilConnected(std::chrono::seconds(1)));

        bool resultCalled = false;
        int onDoneCalled = 0;
        int onErrorCalled = 0;
        auto onDone = [&](const SharedMessage& /* response */) {
            UNIT_ASSERT(callbackQueue->isWorkingThread());

            std::lock_guard<std::mutex> guard(gTestMutex);
            ++onDoneCalled;
            resultCalled = true;
            gTestCV.notify_all();
        };

        auto onError = [&](const std::string& errorMessage) {
            UNIT_ASSERT(callbackQueue->isWorkingThread());

            std::lock_guard<std::mutex> guard(gTestMutex);
            ++onErrorCalled;
            YIO_LOG_INFO(errorMessage);
            resultCalled = true;
            gTestCV.notify_all();
        };

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
        quasarConnector.sendRequest(QuasarMessage{requestToSend}, onDone, onError, std::chrono::milliseconds(100));
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
        auto callbackQueue = std::make_shared<MockCallbackQueue>();

        QuasarServer quasarEndpoint("test", device->sharedConfiguration());
        QuasarMessage request1;
        QuasarMessage request2;
        QuasarMessage response1;
        QuasarMessage response2;
        int onMessageCalled = 0;
        quasarEndpoint.setMessageHandler([&](const SharedMessage& request, auto& connection) {
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

        QuasarConnector quasarConnector("test", device->sharedConfiguration(), Lifetime::immortal, callbackQueue);
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
            UNIT_ASSERT(callbackQueue->isWorkingThread());

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

        quasarConnector.sendRequest(std::move(request1), std::bind(onDone, std::placeholders::_1, 1), QuasarConnector::OnError(), std::chrono::seconds(5));
        quasarConnector.sendRequest(std::move(request2), std::bind(onDone, std::placeholders::_1, 2), QuasarConnector::OnError(), std::chrono::seconds(5));
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
        auto callbackQueue = std::make_shared<MockCallbackQueue>();

        QuasarMessage response;
        auto quasarEndpoint = std::make_unique<QuasarServer>("test", device->sharedConfiguration());
        const int port = quasarEndpoint->listenTcpLocal(getPort());
        QuasarConnector quasarConnector("test", device->sharedConfiguration(), Lifetime::immortal, callbackQueue);
        quasarConnector.connectToTcpHost("localhost", port);
        quasarEndpoint->setMessageHandler([&](const SharedMessage& request, auto& connection) {
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
                auto onError = [&](const std::string& /*error*/) {
                };
                QuasarMessage request;
                quasarConnector.sendRequest(std::move(request), onDone, onError, std::chrono::seconds(5));
                std::this_thread::sleep_for(std::chrono::milliseconds(100));
            }
        });

        std::this_thread::sleep_for(std::chrono::seconds(1));

        stopped = true;

        sendRequestThread.join();
        callbackQueue->destroy();
        int scheduleCounter = callbackQueue->scheduleCounter;
        int doneCounter = callbackQueue->doneCounter;
        UNIT_ASSERT_VALUES_EQUAL(scheduleCounter, doneCounter);
    }
}
