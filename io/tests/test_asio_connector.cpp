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

    std::mutex gTestMutex;
    SteadyConditionVariable gTestCV;

    class AsioConnectorTestFixture: public QuasarUnitTestFixtureWithoutIpc {
    public:
        using Base = QuasarUnitTestFixtureWithoutIpc;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            ipcFactory = std::make_shared<ipc::AsioIpcFactory>(device_->sharedConfiguration());
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            if (ipcFactory) {
                YIO_LOG_INFO("Reseting ipc factory.");
                ipcFactory.reset();
                YIO_LOG_INFO("Factory reseted.");
            }

            Base::TearDown(context);
        }

        std::shared_ptr<ipc::AsioIpcFactory> ipcFactory;
    };

} // Anonymous namespace

Y_UNIT_TEST_SUITE_F(AsioConnector, AsioConnectorTestFixture) {
    Y_UNIT_TEST(testConnectorRequestResponse)
    {
        QuasarMessage requestToSend;
        QuasarMessage responseToSend;
        int onQuasarMessageCalled = 0;

        auto server = ipcFactory->createIpcServer("test");
        server->setMessageHandler([&](const auto& request, auto& connection) {
            ++onQuasarMessageCalled;
            UNIT_ASSERT(request->has_request_id());
            UNIT_ASSERT(request->has_wifi_list_request());
            // UNIT_ASSERT_VALUES_EQUAL(request->SerializeAsString(), requestToSend.SerializeAsString());
            responseToSend.set_request_id(request->request_id());
            connection.send(QuasarMessage{responseToSend});
        });
        const int port = server->listenTcpLocal(getPort());

        auto connector = ipcFactory->createIpcConnector("test");
        connector->connectToTcpHost("localhost", port);
        connector->waitUntilConnected();

        requestToSend.mutable_wifi_list_request(); // Just set "has" flag

        responseToSend.mutable_wifi_list()->add_hotspots()->set_ssid("myssid");
        responseToSend.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);

        bool resultCalled = false;
        auto onDone = [&](const auto& response) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            // UNIT_ASSERT_VALUES_EQUAL(response->SerializeAsString(), responseToSend.SerializeAsString());
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

        connector->sendRequest(QuasarMessage{requestToSend}, onDone, onError, std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled;
            });
        }

        UNIT_ASSERT_VALUES_EQUAL(onQuasarMessageCalled, 1);
    }

    Y_UNIT_TEST(testConnectorRequestSync)
    {
        QuasarMessage requestToSend;
        requestToSend.mutable_wifi_list_request(); // Just set "has" flag

        QuasarMessage responseToSend;
        responseToSend.mutable_wifi_list()->add_hotspots()->set_ssid("myssid");
        responseToSend.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);
        int onQuasarMessageCalled = 0;

        auto connector = ipcFactory->createIpcConnector("test");
        UNIT_ASSERT_EXCEPTION(connector->sendRequestSync(QuasarMessage{requestToSend}, std::chrono::milliseconds(100)), std::runtime_error); // Not connected

        auto server = ipcFactory->createIpcServer("test");
        server->setMessageHandler([&](const auto& request, auto& connection) {
            ++onQuasarMessageCalled;
            responseToSend.set_request_id(request->request_id());
            if (1 == onQuasarMessageCalled) {
                connection.send(QuasarMessage{responseToSend});
            }
        });
        const int port = server->listenTcpLocal(getPort());

        connector->connectToTcpHost("localhost", port);
        connector->waitUntilConnected();

        auto response = connector->sendRequestSync(QuasarMessage{requestToSend}, std::chrono::seconds(600));
        UNIT_ASSERT_VALUES_EQUAL(response->wifi_list().hotspots(0).ssid(), "myssid");

        UNIT_ASSERT_EXCEPTION(connector->sendRequestSync(QuasarMessage{requestToSend}, std::chrono::milliseconds(100)), std::runtime_error); // Timeout
    }

    Y_UNIT_TEST(testConnectorTimeout)
    {
        QuasarMessage requestToSend;
        QuasarMessage responseToSend;
        std::atomic<int> onMessageCalled = 0;

        auto server = ipcFactory->createIpcServer("test");
        server->setMessageHandler([&](const auto& request, auto& connection) {
            ++onMessageCalled;
            if (2 == onMessageCalled)
            {
                responseToSend.set_request_id(request->request_id());
                connection.send(QuasarMessage{responseToSend});
            }
        });
        const int port = server->listenTcpLocal(getPort());

        auto connector = ipcFactory->createIpcConnector("test");
        connector->connectToTcpHost("localhost", port);
        UNIT_ASSERT(connector->waitUntilConnected(std::chrono::seconds(1)));

        std::atomic_bool resultCalled = false;
        std::atomic<int> onDoneCalled = 0;
        std::atomic<int> onErrorCalled = 0;
        auto onDone = [&](const auto& /* response */) {
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
        connector->sendRequest(QuasarMessage{requestToSend}, onDone, onError, std::chrono::milliseconds(100));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled.load();
            });
        }

        UNIT_ASSERT_VALUES_EQUAL(onDoneCalled.load(), 0);
        UNIT_ASSERT_VALUES_EQUAL(onErrorCalled.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(onMessageCalled.load(), 1);

        resultCalled = false;
        // expect done
        connector->sendRequest(QuasarMessage{requestToSend}, onDone, onError, std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled.load();
            });
        }
        UNIT_ASSERT_VALUES_EQUAL(onDoneCalled.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(onErrorCalled.load(), 1);
        UNIT_ASSERT_VALUES_EQUAL(onMessageCalled.load(), 2);
    }

    Y_UNIT_TEST(testConnectorWrongOrder)
    {
        QuasarMessage request1;
        QuasarMessage request2;
        QuasarMessage response1;
        QuasarMessage response2;
        std::atomic<int> onMessageCalled = 0;

        auto server = ipcFactory->createIpcServer("test");
        server->setMessageHandler([&](const auto& request, auto& connection) {
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

        const int port = server->listenTcpLocal(getPort());

        auto connector = ipcFactory->createIpcConnector("test");
        connector->connectToTcpHost("localhost", port);
        UNIT_ASSERT(connector->waitUntilConnected(std::chrono::seconds(1)));

        request1.mutable_wifi_list_request();
        request2.mutable_wifi_list_request();

        response1.mutable_wifi_list()->add_hotspots()->set_ssid("ssid1");
        response1.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);
        response2.mutable_wifi_list()->add_hotspots()->set_ssid("ssid2");
        response2.mutable_wifi_list()->mutable_hotspots(0)->set_secure(true);

        int responseReceived = 0;
        bool done = false;
        auto onDone = [&](const auto& response, int requestNumber) {
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

        auto onError = [](const auto& /*error*/) {};

        connector->sendRequest(QuasarMessage{request1}, std::bind(onDone, std::placeholders::_1, 1), onError, std::chrono::seconds(600));
        connector->sendRequest(QuasarMessage{request2}, std::bind(onDone, std::placeholders::_1, 2), onError, std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&done]() {
                return done;
            });
        }
    }

    Y_UNIT_TEST(testConnectorDeadLock)
    {
        QuasarMessage response;

        auto server = ipcFactory->createIpcServer("test");
        server->setMessageHandler([&](const auto& request, auto& connection) {
            response.set_request_id(request->request_id());
            connection.send(QuasarMessage{response});
            connection.scheduleClose();
        });
        const int port = server->listenTcpLocal(getPort());

        auto connector = ipcFactory->createIpcConnector("test");
        connector->connectToTcpHost("localhost", port);
        connector->waitUntilConnected();
        connector->setSilentMode(true);

        std::atomic_bool stopped = false;
        auto sendRequestThread = std::thread([&]() {
            YIO_LOG_INFO("Spammer thread started...");
            while (!stopped)
            {
                auto onDone = [&](const auto& /* response */) {};
                auto onError = [&](const std::string& /* errorMessage */) {};
                QuasarMessage request;
                connector->sendRequest(QuasarMessage{request}, onDone, onError, std::chrono::seconds(600));
            }
            YIO_LOG_INFO("Spammer thread stopped...");
        });

        std::this_thread::sleep_for(std::chrono::seconds(1));

        YIO_LOG_INFO("Schdule stopping of Spammer thread...");
        stopped = true;

        sendRequestThread.join();
        YIO_LOG_INFO("Spammer thread joined. Test finished.");
    }

    Y_UNIT_TEST(testConnectorParseError)
    {
        QuasarMessage requestToSend;
        std::atomic<int> onMessageCalled = 0;

        auto server = ipcFactory->createIpcServer("test");
        server->setMessageHandler([&](const auto& /*msg*/, auto& connection) {
            ++onMessageCalled;
            // UNIT_ASSERT_VALUES_EQUAL(request->SerializeAsString(), requestToSend.SerializeAsString());
            connection.unsafeSendBytes("abracadabra");
        });
        const int port = server->listenTcpLocal(getPort());

        auto connector = ipcFactory->createIpcConnector("test");
        connector->connectToTcpHost("localhost", port);
        connector->waitUntilConnected();

        requestToSend.mutable_wifi_list_request();

        bool resultCalled = false;
        auto onDone = [&](const auto& /* response */) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_FAIL("onDone shouldn't be called");
            resultCalled = true;
            gTestCV.notify_all();
        };

        auto onError = [&](const std::string& errorMessage) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            YIO_LOG_INFO("onError: " << errorMessage);
            resultCalled = true;
            gTestCV.notify_all();
        };

        connector->sendRequest(QuasarMessage{requestToSend}, onDone, onError, std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled;
            });
        }

        UNIT_ASSERT_VALUES_EQUAL(onMessageCalled.load(), 1);

        server = nullptr;
        connector->waitUntilDisconnected();

        QuasarMessage responseToSend;
        server = ipcFactory->createIpcServer("test");
        server->setMessageHandler([&](const auto& request, auto& connection) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            ++onMessageCalled;
            // UNIT_ASSERT_VALUES_EQUAL(request->SerializeAsString(), requestToSend.SerializeAsString());
            responseToSend.set_request_id(request->request_id());
            connection.send(QuasarMessage{responseToSend});
        });
        server->listenTcpLocal(port);

        server->waitConnectionsAtLeast(1);
        connector->waitUntilConnected();

        resultCalled = 0;
        auto onDone2 = [&](const auto& /*response*/) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            // UNIT_ASSERT_VALUES_EQUAL(response->SerializeAsString(), responseToSend.SerializeAsString());
            resultCalled = true;
            gTestCV.notify_all();
        };

        auto onError2 = [&](const std::string& errorMessage) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_FAIL(errorMessage);
            resultCalled = true;
            gTestCV.notify_all();
        };

        connector->sendRequest(QuasarMessage{requestToSend}, onDone2, onError2, std::chrono::seconds(600));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&resultCalled]() {
                return resultCalled;
            });
        }
        UNIT_ASSERT_VALUES_EQUAL(onMessageCalled.load(), 2);
    }

    Y_UNIT_TEST(testConnectorShutdownConnected)
    {
        auto server = ipcFactory->createIpcServer("test");
        int port = server->listenTcpLocal(getPort());

        auto connector = ipcFactory->createIpcConnector("test");
        connector->connectToTcpHost("localhost", port);
        connector->waitUntilConnected();
    }

    Y_UNIT_TEST(testConnectorTryConnectToServiceWithoutEntryInConfig)
    {
        auto connector = ipcFactory->createIpcConnector("random");
        const bool res = connector->tryConnectToService();

        /* no such entry in config. method should return false */
        UNIT_ASSERT(!res);
        QuasarMessage stub;
        connector->sendMessage(QuasarMessage{stub});
        connector->sendRequest(
            QuasarMessage{stub}, [](const auto& /*response*/) {}, [](const std::string& /*error*/) {}, std::chrono::seconds(600));

        try {
            connector->sendRequestSync(QuasarMessage{stub}, std::chrono::milliseconds(3));
        } catch (const std::runtime_error& e) {
            /* connector throw runtime_error on timeout */
        }

        /* Make sure that reached this code */
        UNIT_ASSERT(true);
    }

    Y_UNIT_TEST(testConnectorTryConnectToServiceWithEntryInConfig)
    {
        std::promise<void> sendMessagePromise;

        auto server = ipcFactory->createIpcServer("test");
        server->setMessageHandler([&](const auto& quasarMessage, auto& connection) {
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
        config["random"]["port"] = server->listenTcpLocal(getPort());

        auto connector = ipcFactory->createIpcConnector("random");
        const bool res = connector->tryConnectToService();
        /* There is such entry in config. method should return true */
        UNIT_ASSERT(res);

        connector->waitUntilConnected();

        QuasarMessage sendMessageCheckMsg;
        /* Set some message "flag", so server will set up promise */
        sendMessageCheckMsg.mutable_io_control()->mutable_toggle_setup_mode();
        connector->sendMessage(QuasarMessage{sendMessageCheckMsg});

        /* Make sure that sendMessage works */
        sendMessagePromise.get_future().get();

        std::promise<bool> asyncRequestPromise;
        auto onDone = [&](const ipc::SharedMessage& msg) {
            asyncRequestPromise.set_value(msg->external_command_response().processed());
        };
        auto onError = [](const std::string& msg) {
            UNIT_FAIL(std::string("Send Request fail!: ") + msg);
        };

        QuasarMessage requestMessageCheckMsg; /* empty stub message for requests */
        connector->sendRequest(QuasarMessage{requestMessageCheckMsg}, std::move(onDone), std::move(onError), std::chrono::seconds(600));

        /* Check that async request work */
        const auto result = asyncRequestPromise.get_future().get();
        UNIT_ASSERT(result);

        /* Check that sync request work */
        const auto response = connector->sendRequestSync(QuasarMessage{requestMessageCheckMsg}, std::chrono::seconds(600));
        UNIT_ASSERT(response->external_command_response().processed());
    }

    Y_UNIT_TEST(testBurstScenario1)
    {
        std::mt19937 generator((std::random_device())());
        std::uniform_int_distribution<size_t> msgSize(16, 512);

        auto server = ipcFactory->createIpcServer("test");
        server->setMessageHandler([&](const auto& quasarMessage, auto& connection) {
            connection.send(ipc::buildMessage([&](auto& msg) {
                msg.set_bug_report_id(quasarMessage->bug_report_id());
            }));
        });
        int port = server->listenTcpLocal(getPort());

        std::atomic<int> spawnCounter{0};
        std::atomic<int> demolishCounter{0};
        std::atomic<int> msgCounter{0};
        std::vector<std::shared_ptr<ipc::IConnector>> connectors;

        std::mutex setMutex;
        std::set<int> cids;
        auto spawnConnector = [&] {
            YIO_LOG_INFO("Spawn connector: " << spawnCounter.load());
            auto connector = ipcFactory->createIpcConnector("test");
            auto lastMessage = std::make_shared<TString>();
            auto wconn = std::weak_ptr{connector};
            connector->setConnectHandler([&, lastMessage, wconn] {
                if (auto me = wconn.lock()) {
                    ++msgCounter;
                    *lastMessage = rndMark(msgSize(generator));
                    me->sendMessage(ipc::buildMessage([&](auto& msg) {
                        msg.set_bug_report_id(*lastMessage);
                    }));
                }
            });
            auto cid = ++spawnCounter;
            connector->setMessageHandler([&, lastMessage, wconn, cid](const auto& quasarMessage) {
                if (auto me = wconn.lock()) {
                    UNIT_ASSERT_VALUES_EQUAL(quasarMessage->bug_report_id(), *lastMessage);
                    ++msgCounter;
                    *lastMessage = rndMark(msgSize(generator));
                    me->sendMessage(ipc::buildMessage([&](auto& msg) {
                        msg.set_bug_report_id(*lastMessage);
                    }));
                    std::lock_guard lock(setMutex);
                    cids.insert(cid);
                }
            });
            connector->connectToTcpLocal(port);
            connectors.push_back(connector);
        };
        auto demolishConnector = [&] {
            YIO_LOG_INFO("Demolish connector: " << demolishCounter.load());
            if (connectors.empty()) {
                return;
            }

            size_t lastIndex = connectors.size() - 1;
            std::uniform_int_distribution<size_t> index(0, lastIndex);
            std::swap(connectors[index(generator)], connectors[lastIndex]);
            connectors.resize(lastIndex);
            ++demolishCounter;
        };

        YIO_LOG_INFO("Begin of spawn 10 connectors");
        for (size_t i = 0; i < 10; ++i) {
            spawnConnector();
        }
        YIO_LOG_INFO("End of spawn 10 connectors");

        std::this_thread::sleep_for(std::chrono::milliseconds(1000));
        YIO_LOG_INFO("Try demolish one connector");
        demolishConnector();
        YIO_LOG_INFO("Do random loop with spawn and demolish");

        auto until = std::chrono::steady_clock::now() + std::chrono::seconds{10};
        auto maxMessageCounter = 1000000;
        std::uniform_int_distribution<size_t> command(0, 1);
        do {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            switch (connectors.size() < 5 ? 0 : command(generator)) {
                case 0:
                    spawnConnector();
                    break;
                case 1:
                    demolishConnector();
                    break;
            }
            YIO_LOG_INFO("msgCounter=" << msgCounter.load());
            YIO_LOG_INFO("spawnCounter=" << spawnCounter.load());
            YIO_LOG_INFO("demolishCounter=" << demolishCounter.load());
            YIO_LOG_INFO("active connector counter=" << (spawnCounter - demolishCounter));

            std::lock_guard lock(setMutex);
            YIO_LOG_INFO("cids.size()=" << cids.size());
        } while (msgCounter < maxMessageCounter && std::chrono::steady_clock::now() < until);

        connectors.clear();
        server.reset();

        YIO_LOG_INFO("== msgCounter=" << msgCounter.load());
        YIO_LOG_INFO("== spawnCounter=" << spawnCounter.load());
        YIO_LOG_INFO("== demolishCounter=" << demolishCounter.load());
        YIO_LOG_INFO("== active connector counter=" << (spawnCounter - demolishCounter));

        std::unique_lock lock(setMutex);
        YIO_LOG_INFO("== cids.size()=" << cids.size());
        lock.unlock();

        ipcFactory.reset();
        YIO_LOG_INFO("== FIN ==");
    }
}
