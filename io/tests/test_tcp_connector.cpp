#include "test_tcp_endpoints.h"

#include <yandex_io/libs/ipc/datacratic/tcp_connector.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/logging/logging.h>

#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>

using namespace Datacratic;
using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::ipc::detail::datacratic;

namespace {
    struct QueuedTCPConnector: public TCPConnector {
    public:
        using OnDone = std::function<void(const std::string& message)>;
        using OnError = std::function<void(const std::string& errorMessage)>;

    private:
        const size_t maxQueueSize = 65000;

        std::mutex requestQueueMutex_;
        std::queue<std::pair<OnDone, OnError>> requestQueue_;

    public:
        struct Request {
            Request() = default;
            Request(std::string message, OnDone onDone, OnError onError)
                : message(std::move(message))
                , onDone(std::move(onDone))
                , onError(std::move(onError))
            {
                // No operations.
            }
            Request(Request&& other) = default;
            std::string message;
            OnDone onDone;
            OnError onError;
        };

        void sendRequest(Request request) {
            std::unique_lock<std::mutex> connectionLock(connectionLock_);
            std::unique_lock<std::mutex> requestQueueLock(requestQueueMutex_);

            if (requestQueue_.size() >= maxQueueSize)
            {
                requestQueueLock.unlock();
                connectionLock.unlock();
                request.onError("Cannot send request: queue overflow.");
                return;
            }

            bool sent = TCPConnector::sendMessageUnlocked(std::move(request.message));
            if (!sent)
            {
                requestQueueLock.unlock();
                connectionLock.unlock();
                request.onError("Cannot send request: not connected.");
                return;
            }

            requestQueue_.push(std::make_pair(std::move(request.onDone), std::move(request.onError)));
        }

        ~QueuedTCPConnector() {
            shutdown();
        }

    private:
        void handleMessageReceived(const std::string& response) override {
            std::unique_lock<std::mutex> lock(requestQueueMutex_);
            if (requestQueue_.empty())
            {
                lock.unlock();
                doError("Received result '" + response + "' when request queue is empty.");
                return;
            }
            auto handlers = requestQueue_.front();
            requestQueue_.pop();
            lock.unlock();
            if (handlers.first) {
                handlers.first(response);
            }
        }

        std::function<void()> doBeforeDisconnect(std::shared_ptr<TCPConnectionHandler> connection) override {
            (void)connection;

            std::unique_lock<std::mutex> queueLock(requestQueueMutex_);
            auto uncompletedRequests = std::move(requestQueue_);
            queueLock.unlock();
            auto doAfterDisconnect = [=](std::queue<std::pair<OnDone, OnError>>& requests) mutable {
                runErrorHandlers(requests, "Connection has been lost");
            };

            return std::bind(doAfterDisconnect, std::move(uncompletedRequests));
        }

        static void runErrorHandlers(std::queue<std::pair<OnDone, OnError>>& requestQueue, const std::string& errorMessage) {
            while (!requestQueue.empty())
            {
                if (requestQueue.front().second) {
                    requestQueue.front().second(errorMessage);
                }
                requestQueue.pop();
            }
        }
    };

    std::mutex gTestMutex;
    SteadyConditionVariable gTestCV;
} // Anonymous namespace

Y_UNIT_TEST_SUITE_F(TestTCPConnector, QuasarUnitTestFixtureWithoutIpc) {
    Y_UNIT_TEST(testTCPConnectorConnecting)
    {
        MockTCPEndpoint endpoint("test predictor endpoint");
        int port = endpoint.init(getPort());
        QueuedTCPConnector connector;
        std::atomic<bool> connectHandlerCalled(false);
        connector.setConnectHandler([&]() {
            connectHandlerCalled.store(true);
        });
        connector.init("localhost", port);

        UNIT_ASSERT_VALUES_EQUAL(connector.hostname(), "localhost");
        UNIT_ASSERT_VALUES_EQUAL(connector.port(), port);

        connector.waitUntilConnected();
        TestUtils::waitUntil([&]() { return connector.isConnected(); });
        TestUtils::waitUntil([&]() { return connectHandlerCalled.load(); });

        endpoint.shutdown();
        connector.shutdown();

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorDisconnecting)
    {
        MockTCPEndpoint endpoint("test predictor endpoint");
        int port = endpoint.init(getPort());

        QueuedTCPConnector connector;
        connector.init("localhost", port);

        connector.waitUntilConnected();
        TestUtils::waitUntil([&]() { return connector.isConnected(); });

        endpoint.shutdown();
        connector.waitUntilDisconnected();
        TestUtils::waitUntil([&]() { return !connector.isConnected(); });
        endpoint.shutdown();
        connector.shutdown();

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorCallHandlersOnDisconnect)
    {
        MockTCPEndpoint endpoint("test predictor endpoint");
        endpoint.setRequestHandler([&](const std::string& /*request*/, std::shared_ptr<TCPConnectionHandler> /*handler*/) {
            // No operations.
        });

        int port = endpoint.init(getPort());

        QueuedTCPConnector connector;
        connector.init("localhost", port);

        connector.waitUntilConnected();
        TestUtils::waitUntil([&]() { return connector.isConnected(); });

        int onDoneCalled = 0;
        int onErrorCalled = 0;
        auto onDone = [&](const std::string& /* message */) {
            ++onDoneCalled;
        };

        auto onError = [&](const std::string& /* message */) {
            ++onErrorCalled;
        };

        connector.sendRequest(QueuedTCPConnector::Request("message", onDone, onError));
        connector.sendRequest(QueuedTCPConnector::Request("message", onDone, onError));
        endpoint.shutdown();
        connector.waitUntilDisconnected();
        TestUtils::waitUntil([&]() { return !connector.isConnected(); });
        endpoint.shutdown();
        connector.shutdown();

        UNIT_ASSERT_VALUES_EQUAL(onDoneCalled, 0);
        UNIT_ASSERT_VALUES_EQUAL(onErrorCalled, 2);

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorDisconnectionFailure)
    {
        BoundSocket listeningSocket(getPort());
        listeningSocket.listen();
        YIO_LOG_INFO("listening on port " + std::to_string(listeningSocket.port()));
        bool connected = false;
        std::thread acceptThread(
            [&]()
            {
                sockaddr_in sin;
                socklen_t addrLen = sizeof(sin);
                int acceptedSock = accept(listeningSocket.socket(), reinterpret_cast<sockaddr*>(&sin), &addrLen);
                if (acceptedSock < 0)
                {
                    UNIT_FAIL(strerror(errno));
                    listeningSocket.close();
                    return;
                }
                {
                    std::unique_lock<std::mutex> lock(gTestMutex);
                    gTestCV.wait(lock, [&connected]() {
                        return connected;
                    });
                }
                close(acceptedSock);
                listeningSocket.close();
            });

        QueuedTCPConnector connector;
        connector.init("localhost", listeningSocket.port());

        connector.waitUntilConnected();
        TestUtils::waitUntil([&]() { return connector.isConnected(); });
        {
            std::lock_guard<std::mutex> guard(gTestMutex);
            connected = true;
            gTestCV.notify_all();
        }

        connector.waitUntilDisconnected();
        TestUtils::waitUntil([&]() { return !connector.isConnected(); });
        acceptThread.join();
        connector.shutdown();

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorReconnecting)
    {
        BoundSocket listenningSocket(getPort());
        listenningSocket.listen();
        YIO_LOG_INFO("listening on port " + std::to_string(listenningSocket.port()));
        bool connected = false;
        bool waitAfterCycle = false;
        std::thread acceptThread(
            [&]()
            {
                sockaddr_in sin;
                socklen_t addrLen = sizeof(sin);

                for (int i = 0; i < 2; ++i)
                {
                    int acceptedSock = accept(listenningSocket.socket(), reinterpret_cast<sockaddr*>(&sin), &addrLen);
                    if (acceptedSock < 0)
                    {
                        UNIT_FAIL(strerror(errno));
                        listenningSocket.close();
                        return;
                    }
                    {
                        std::unique_lock<std::mutex> lock(gTestMutex);
                        gTestCV.wait(lock, [&connected]() {
                            return connected;
                        });
                    }

                    close(acceptedSock);
                    {
                        std::unique_lock<std::mutex> lock(gTestMutex);
                        gTestCV.wait(lock, [&waitAfterCycle]() {
                            return waitAfterCycle;
                        });
                    }
                }

                listenningSocket.close();
            });

        QueuedTCPConnector connector;
        connector.init("localhost", listenningSocket.port());

        connector.waitUntilConnected();
        TestUtils::waitUntil([&]() { return connector.isConnected(); });
        YIO_LOG_INFO("Connected first time");
        {
            std::lock_guard<std::mutex> guard(gTestMutex);
            connected = true;
            gTestCV.notify_all();
        }

        connector.waitUntilDisconnected();
        TestUtils::waitUntil([&]() { return !connector.isConnected(); });
        YIO_LOG_INFO("Disconnected first time");
        {
            std::lock_guard<std::mutex> guard(gTestMutex);
            connected = false;
            waitAfterCycle = true;
            gTestCV.notify_all();
        }

        connector.waitUntilConnected();
        TestUtils::waitUntil([&]() { return connector.isConnected(); });
        YIO_LOG_INFO("Connected second time");
        {
            std::lock_guard<std::mutex> guard(gTestMutex);
            connected = true;
            gTestCV.notify_all();
        }

        connector.waitUntilDisconnected();
        TestUtils::waitUntil([&]() { return !connector.isConnected(); });
        YIO_LOG_INFO("Disconnected second time");
        {
            std::lock_guard<std::mutex> guard(gTestMutex);
            waitAfterCycle = true;
            gTestCV.notify_all();
        }

        acceptThread.join();
        connector.shutdown();

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorShutdownWhileReconnecting)
    {
        BoundSocket listenningSocket(getPort());
        listenningSocket.listen();
        YIO_LOG_INFO("listenning on port " + std::to_string(listenningSocket.port()));
        bool connected = false;
        bool waitAfterCycle = false;
        std::thread acceptThread(
            [&]()
            {
                sockaddr_in sin;
                socklen_t addrLen = sizeof(sin);
                int acceptedSock = accept(listenningSocket.socket(), reinterpret_cast<sockaddr*>(&sin), &addrLen);
                if (acceptedSock < 0)
                {
                    UNIT_FAIL(strerror(errno));
                    listenningSocket.close();
                    return;
                }
                {
                    std::unique_lock<std::mutex> lock(gTestMutex);
                    gTestCV.wait(lock, [&connected]() {
                        return connected;
                    });
                }
                close(acceptedSock);
                listenningSocket.close();
            });

        QueuedTCPConnector connector;
        connector.init("localhost", listenningSocket.port());

        connector.waitUntilConnected();
        TestUtils::waitUntil([&]() { return connector.isConnected(); });
        YIO_LOG_INFO("Connected first time");
        {
            std::lock_guard<std::mutex> guard(gTestMutex);
            connected = true;
            gTestCV.notify_all();
        }

        connector.waitUntilDisconnected();
        TestUtils::waitUntil([&]() { return !connector.isConnected(); });
        YIO_LOG_INFO("Disconnected first time");
        {
            std::lock_guard<std::mutex> guard(gTestMutex);
            connected = false;
            waitAfterCycle = true;
            gTestCV.notify_all();
        }

        connector.shutdown();
        acceptThread.join();

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorOnConnectionError)
    {
        QueuedTCPConnector connector;
        bool errorHappened = false;

        connector.setConnectionErrorHandler([&](const std::string& /* message */) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            errorHappened = true;
            gTestCV.notify_all();
        });

        BoundSocket bindedSocket(getPort());

        connector.init("localhost", bindedSocket.port());

        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&errorHappened]() {
                return errorHappened;
            });
        }

        UNIT_ASSERT(errorHappened);
        connector.shutdown();

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorReconnectingAfterError)
    {
        QueuedTCPConnector connector;
        BoundSocket bindedSocket(getPort());

        bool errorHappened = false;
        connector.setConnectionErrorHandler([&](const std::string& /* error */) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            errorHappened = true;
            gTestCV.notify_all();
        });

        connector.init("localhost", bindedSocket.port());

        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&errorHappened]() {
                return errorHappened;
            });
        }

        UNIT_ASSERT(errorHappened);

        bindedSocket.listen();
        connector.waitUntilConnected();
        waitUntil([&]() { return connector.isConnected(); });

        connector.shutdown();

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorTimedWait)
    {
        QueuedTCPConnector connector;

        UNIT_ASSERT_VALUES_EQUAL(connector.waitUntilConnected(std::chrono::milliseconds(500)), false);
        waitUntil([&]() { return !connector.isConnected(); });

        BoundSocket listenningSocket(getPort());
        listenningSocket.listen();
        connector.init("localhost", listenningSocket.port());

        connector.waitUntilConnected();
        waitUntil([&]() { return connector.isConnected(); });

        UNIT_ASSERT_VALUES_EQUAL(connector.waitUntilDisconnected(std::chrono::milliseconds(500)), false);
        UNIT_ASSERT(connector.isConnected());

        std::thread closeSocket([&]() {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            listenningSocket.close();
        });

        connector.waitUntilDisconnected();
        waitUntil([&]() { return !connector.isConnected(); });

        QueuedTCPConnector connector2;
        BoundSocket listenningSocket2(getPort());
        connector2.init("localhost", listenningSocket2.port());
        std::thread doListen([&]() {
            std::this_thread::sleep_for(std::chrono::milliseconds(100));
            listenningSocket2.listen();
        });

        connector2.waitUntilConnected();
        waitUntil([&]() { return connector2.isConnected(); });

        connector.shutdown();
        connector2.shutdown();
        closeSocket.join();
        doListen.join();

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorTwoTests)
    {
        { // testDMPConnectorOnConnectionError
            QueuedTCPConnector connector;
            bool errorHappened = false;

            connector.setConnectionErrorHandler([&](const std::string& /* message */) {
                std::lock_guard<std::mutex> guard(gTestMutex);
                errorHappened = true;
                gTestCV.notify_all();
            });

            BoundSocket bindedSocket(getPort());

            connector.init("localhost", bindedSocket.port());

            {
                std::unique_lock<std::mutex> lock(gTestMutex);
                gTestCV.wait(lock, [&errorHappened]() {
                    return errorHappened;
                });

                UNIT_ASSERT(errorHappened);
            }
        }
        { // testDMPConnectorReconnectingAfterError
            QueuedTCPConnector connector;
            BoundSocket bindedSocket(getPort());

            bool errorHappened = false;
            connector.setConnectionErrorHandler([&](const std::string& /* error */) {
                std::lock_guard<std::mutex> guard(gTestMutex);
                errorHappened = true;
                gTestCV.notify_all();
            });

            connector.init("localhost", bindedSocket.port());

            {
                std::unique_lock<std::mutex> lock(gTestMutex);
                gTestCV.wait(lock, [&errorHappened]() {
                    return errorHappened;
                });

                UNIT_ASSERT(errorHappened);
            }

            listen(bindedSocket.socket(), SOMAXCONN);
            connector.waitUntilConnected();
            waitUntil([&]() { return connector.isConnected(); });

            connector.shutdown();
        }

        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorSeveralReconnects)
    {
        QueuedTCPConnector connector;
        bool errorHappened = false;

        connector.setConnectionErrorHandler([&](const std::string& /* message */) {
            std::lock_guard<std::mutex> guard(gTestMutex);
            errorHappened = true;
            gTestCV.notify_all();
        });

        BoundSocket bindedSocket(getPort());

        connector.init("localhost", bindedSocket.port());

        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            // Wait for first error
            gTestCV.wait(lock, [&errorHappened]() {
                return errorHappened;
            });

            UNIT_ASSERT(errorHappened);
            errorHappened = false;

            // Wait for second error
            gTestCV.wait(lock, [&errorHappened]() {
                return errorHappened;
            });
        }

        bindedSocket.listen();
        //    UNIT_ASSERT(connector.waitUntilConnected(std::chrono::seconds(1)));
        connector.waitUntilConnected();
        waitUntil([&]() { return connector.isConnected(); });

        int acceptedSocket = bindedSocket.acceptSocket();
        close(acceptedSocket);

        connector.waitUntilDisconnected();
        waitUntil([&]() { return !connector.isConnected(); });

        connector.waitUntilConnected(); // Reconnection after closing connection
        waitUntil([&]() { return connector.isConnected(); });

        connector.shutdown();
        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorSendRequestNotConnected)
    {
        QueuedTCPConnector connector;
        bool errorHappened = false;
        connector.sendRequest(QueuedTCPConnector::Request(
            "test", [=](const std::string& /* message */) {},
            [&](const std::string& /* errorMessage */) {
                std::lock_guard<std::mutex> guard(gTestMutex);
                errorHappened = true;
                gTestCV.notify_all();
            }));

        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&errorHappened]() {
                return errorHappened;
            });

            UNIT_ASSERT(errorHappened);
        }

        connector.shutdown();
        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorSendRequest)
    {
        MockTCPEndpoint endpoint("test predictor endpoint");
        const int port = endpoint.init(getPort());

        QueuedTCPConnector connector;
        connector.init("localhost", port);
        connector.waitUntilConnected();
        waitUntil([&]() { return connector.isConnected(); });

        endpoint.answer = "0.9875\n";
        bool done = false;
        auto onDone = [&](const std::string& received)
        {
            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_ASSERT_VALUES_EQUAL(received + '\n', endpoint.answer);
            done = true;
            gTestCV.notify_all();
        };
        connector.sendRequest(QueuedTCPConnector::Request("test request\n", onDone, QueuedTCPConnector::OnError()));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&done]() {
                return done;
            });
        }
    }

    Y_UNIT_TEST(testTCPConnectorSendRequestAfterReconnect)
    {
        MockTCPEndpoint endpoint("test predictor endpoint");
        const int port = endpoint.init(getPort());

        QueuedTCPConnector connector;
        connector.init("localhost", port);
        connector.waitUntilConnected();
        waitUntil([&]() { return connector.isConnected(); });

        endpoint.shutdown();
        connector.waitUntilDisconnected();
        waitUntil([&]() { return !connector.isConnected(); });

        MockTCPEndpoint endpoint2("test predictor endpoint2");
        endpoint2.init(port);
        connector.waitUntilConnected();
        waitUntil([&]() { return connector.isConnected(); });

        endpoint2.answer = "0.9875\n";
        bool done = false;
        auto onDone = [&](const std::string& received)
        {
            std::lock_guard<std::mutex> guard(gTestMutex);
            UNIT_ASSERT_VALUES_EQUAL(received + '\n', endpoint2.answer);
            done = true;
            gTestCV.notify_all();
        };
        connector.sendRequest(QueuedTCPConnector::Request("test request\n", onDone, QueuedTCPConnector::OnError()));
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&done]() {
                return done;
            });
        }
    }

    Y_UNIT_TEST(testTCPConnectorMultipleRequests)
    {
        MockTCPEndpoint endpoint("test predictor endpoint");
        endpoint.setRequestHandler([&](const std::string& request, std::shared_ptr<TCPConnectionHandler> handler)
                                   {
                                       std::lock_guard lock(gTestMutex);
                                       int requestNumber = atoi(request.c_str());
                                       handler->send(std::to_string(requestNumber + 1) + "\n");
                                   });
        const int port = endpoint.init(getPort());
        QueuedTCPConnector connector;
        connector.init("localhost", port);
        connector.waitUntilConnected();
        waitUntil([&]() { return connector.isConnected(); });

        const int requestCount = 1000;
        std::vector<bool> done(requestCount, false);
        for (int i = 0; i < requestCount; ++i)
        {
            auto onDone = [&done, i](const std::string& received)
            {
                std::lock_guard lock(gTestMutex);
                UNIT_ASSERT_VALUES_EQUAL(received, std::to_string(i + 1));
                done[i] = true;
                gTestCV.notify_all();
            };
            auto onError = [&done, i](const std::string& errorMessage)
            {
                std::lock_guard lock(gTestMutex);
                UNIT_FAIL("Connection " + std::to_string(i) + ": " + errorMessage);
                done[i] = true;
                gTestCV.notify_all();
            };

            connector.sendRequest(QueuedTCPConnector::Request(std::to_string(i) + '\n', onDone, onError));
        }

        for (bool& requestDone : done)
        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&requestDone]() {
                return requestDone;
            });
        }

        connector.shutdown();
        endpoint.shutdown();
        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testQueuedTCPConnectorDeadLock)
    {
        std::unique_ptr<MockTCPEndpoint> tcpEndpoint(new MockTCPEndpoint("testQueuedTCPConnectorDeadLock"));
        const int port = tcpEndpoint->init(getPort());
        QueuedTCPConnector tcpConnector;
        tcpConnector.init("localhost", port);
        tcpEndpoint->setRequestHandler([&](const std::string& /* request */, std::shared_ptr<TCPConnectionHandler> handler) {
            handler->send("pong\n");
            handler->closeWhenHandlerFinished();
        });

        tcpConnector.waitUntilConnected();
        bool stopped = false;
        auto sendRequestThread = std::thread([&]() {
            while (!stopped)
            {
                auto onDone = [&](const std::string& /* received */) {

                };
                auto onError = [&](const std::string& /* errorMessage */) {

                };
                tcpConnector.sendRequest(QueuedTCPConnector::Request("ping\n", onDone, onError));
            }
        });

        std::this_thread::sleep_for(std::chrono::milliseconds(1000));

        stopped = true;

        sendRequestThread.join();
    }

    Y_UNIT_TEST(testQueuedTCPConnectorQueueOverflow)
    {
        MockTCPEndpoint endpoint("testQueuedTCPConnectorQueueOverflow");
        int port = endpoint.init(getPort());

        bool done = false;
        endpoint.setRequestHandler([&](const std::string& /* request */, std::shared_ptr<TCPConnectionHandler> handler) {
            {
                std::unique_lock<std::mutex> lock(gTestMutex);
                gTestCV.wait(lock, [&done]() {
                    return done;
                });
            }
            handler->closeWhenHandlerFinished();
        });

        QueuedTCPConnector connector;
        connector.init("localhost", port);
        connector.waitUntilConnected();
        waitUntil([&]() { return connector.isConnected(); });

        std::atomic<bool> doneHandlerCalled(false);
        auto onDone = [&](const std::string& /* received */) {
            doneHandlerCalled.store(true);
        };

        std::atomic<bool> errorHandlerCalled(false);
        auto onError = [&](const std::string& /* errorMessage */) {
            errorHandlerCalled.store(true);
        };

        for (int i = 0; i < 100000; i++)
        {
            connector.sendRequest(QueuedTCPConnector::Request("ping\n", onDone, onError));
        }

        UNIT_ASSERT(errorHandlerCalled.load());
        UNIT_ASSERT(!doneHandlerCalled.load());

        {
            std::lock_guard lock(gTestMutex);
            done = true;
            gTestCV.notify_all();
        }

        endpoint.shutdown();
        connector.shutdown();
        connector.waitUntilDisconnected();
        UNIT_ASSERT_VALUES_EQUAL(TransportBase::created.load(), TransportBase::destroyed.load());
    }

    Y_UNIT_TEST(testTCPConnectorBadHost)
    {
        MockTCPEndpoint endpoint("testBadHost");
        int port = endpoint.init(getPort());
        QueuedTCPConnector connector;
        bool errorCalled = false;
        connector.setConnectionErrorHandler([&](const std::string& /* error */) {
            std::lock_guard lock(gTestMutex);
            errorCalled = true;
            gTestCV.notify_all();
        });
        connector.init("abracadabra.zzz.ru", port);

        {
            std::unique_lock<std::mutex> lock(gTestMutex);
            gTestCV.wait(lock, [&errorCalled]() {
                return errorCalled;
            });
            UNIT_ASSERT(errorCalled);
        }
    }
}
