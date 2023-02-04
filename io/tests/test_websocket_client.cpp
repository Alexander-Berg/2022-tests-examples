#include <yandex_io/libs/websocket/websocket_client.h>
#include <yandex_io/libs/websocket/websocket_server.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>

#include <yandex_io/tests/testlib/mock_ws_server.h>
#include <yandex_io/tests/testlib/test_certificates.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <future>
#include <memory>
#include <thread>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE_F(TestWebsocketClient, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testWebsocketClient)
    {
        std::condition_variable wakeupVar_;
        MockWSServer mockXiva(getPort());
        int port = mockXiva.getPort();
        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;

        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        WebsocketClient client(std::make_shared<NullMetrica>());

        client.setOnMessageHandler([&](std::string msg) {
            messagePromise->set_value(msg);
        });
        client.connectSync(wsSettings);
        mockXiva.waitUntilConnections(1, 0);
        messagePromise = std::make_unique<std::promise<std::string>>();
        mockXiva.send("test");

        UNIT_ASSERT_VALUES_EQUAL("test", messagePromise->get_future().get());
    }

    Y_UNIT_TEST(testWebsocketClientReconnect)
    {
        std::condition_variable wakeupVar_;

        MockWSServer mockXiva(getPort());
        int port = mockXiva.getPort();
        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;

        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        WebsocketClient client(std::make_shared<NullMetrica>());

        client.setOnMessageHandler([&](std::string msg) {
            messagePromise->set_value(msg);
            wakeupVar_.notify_one();
        });

        constexpr int retries = 10;
        for (int i = 0; i < retries; ++i) {
            client.connectSync(wsSettings);
            std::cout << "WAITING " << i << std::endl;
            mockXiva.waitUntilConnections(i + 1, i);
            std::cout << "WAITING DONE " << i << std::endl;
            std::promise<void> disconnectPromise;
            client.disconnectAsync([&disconnectPromise]() {
                disconnectPromise.set_value();
            });
            disconnectPromise.get_future().get();
        }
        mockXiva.waitUntilConnections(retries, retries);
        UNIT_ASSERT_VALUES_EQUAL((unsigned long)0, mockXiva.getCurrentConnectionCnt());
        messagePromise = std::make_unique<std::promise<std::string>>();
        client.connectSync(wsSettings);
        mockXiva.waitUntilConnections(retries + 1, retries);
        mockXiva.send("test");
        UNIT_ASSERT_VALUES_EQUAL("test", messagePromise->get_future().get());
    }

    Y_UNIT_TEST(testWebsocketClientAutoReconnect)
    {
        std::condition_variable wakeupVar_;

        MockWSServer mockXiva(getPort());
        int port = mockXiva.getPort();
        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;

        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        WebsocketClient client(std::make_shared<NullMetrica>());

        client.setOnMessageHandler([&](std::string msg) {
            messagePromise->set_value(msg);
            wakeupVar_.notify_one();
        });
        client.connectSync(wsSettings);

        constexpr int retries = 10;
        for (int i = 0; i < retries; ++i) {
            std::cout << "WAITING " << i << std::endl;
            mockXiva.waitUntilConnections(i + 1, i);
            std::cout << "WAITING DONE " << i << std::endl;
            mockXiva.closeConnections();
        }
        mockXiva.waitUntilConnections(retries + 1, retries);
        UNIT_ASSERT_VALUES_EQUAL((unsigned long)1, mockXiva.getCurrentConnectionCnt());
        messagePromise = std::make_unique<std::promise<std::string>>();
        mockXiva.send("test");
        UNIT_ASSERT_VALUES_EQUAL("test", messagePromise->get_future().get());
    }

    Y_UNIT_TEST(testWebsocketClientSend)
    {
        MockWSServer mockXiva(getPort());
        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;
        mockXiva.onMessage = [&](std::string msg) {
            messagePromise->set_value(msg);
        };
        int port = mockXiva.getPort();

        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        WebsocketClient client(std::make_shared<NullMetrica>());

        client.connectSync(wsSettings);
        mockXiva.waitUntilConnections(1, 0);
        messagePromise = std::make_unique<std::promise<std::string>>();
        client.unsafeSend("test");

        UNIT_ASSERT_VALUES_EQUAL("test", messagePromise->get_future().get());
    }

    Y_UNIT_TEST(testWebsocketClientConnectionInfo)
    {
        WebsocketServer::Settings serverSettings;
        serverSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM;
        serverSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
        serverSettings.port = getPort();
        auto server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
        int port = server->start();

        WebsocketClient client(std::make_shared<NullMetrica>());
        WebsocketClient::Settings clienstSettings;
        clienstSettings.tls.crtBuffer = TestKeys().INCORRECT_PUBLIC_PEM;
        clienstSettings.tls.verifyHostname = false;
        clienstSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        std::promise<Websocket::ConnectionInfo> connectionInfoPromise;

        client.setOnFailHandler([&connectionInfoPromise](const Websocket::ConnectionInfo& connectionInfo) {
            YIO_LOG_INFO("\nConnectionInfo: \n"
                         << connectionInfo.toString());
            connectionInfoPromise.set_value(connectionInfo);
        });
        std::atomic<bool> connected{false};
        client.setOnConnectHandler([&connected]() {
            connected = true;
        });
        client.connectAsync(clienstSettings);
        Websocket::ConnectionInfo connectionInfo = connectionInfoPromise.get_future().get();
        UNIT_ASSERT(connectionInfo.error == Websocket::Error::TLS_HANDSHAKE_FAILED);
        UNIT_ASSERT(!connected);
    }

    Y_UNIT_TEST(testWebsocketClientPing)
    {
        WebsocketServer::Settings serverSettings;
        serverSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM;
        serverSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
        serverSettings.port = getPort();
        SteadyConditionVariable condVar;
        std::atomic_int pingCount{0};
        auto server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
        server->setOnPingHandler([&](WebsocketServer::ConnectionHdl /* hdl */, const std::string& /* payload */) {
            ++pingCount;
            condVar.notify_one();
            return true; // send pong
        });
        int port = server->start();

        WebsocketClient client(std::make_shared<NullMetrica>());
        WebsocketClient::Settings clienstSettings;
        clienstSettings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        clienstSettings.tls.verifyHostname = false;
        clienstSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        clienstSettings.ping.interval = std::chrono::seconds(1);
        clienstSettings.ping.enabled = true;
        std::promise<Websocket::ConnectionInfo> connectionInfoPromise;
        std::atomic<int> disconnectsCnt{0};
        client.setOnDisconnectHandler([&disconnectsCnt](const Websocket::ConnectionInfo& /* connectionInfo */) {
            ++disconnectsCnt;
        });
        client.connectSync(clienstSettings);
        /* Make sure that Server And Client send Ping/Pongs */
        TestUtils::waitUntil(condVar, [&]() {
            return (pingCount == 3);
        });
        /* Make sure that there was not any disconnect */
        UNIT_ASSERT_VALUES_EQUAL(disconnectsCnt.load(), 0);
    }

    Y_UNIT_TEST(testWebsocketClientPingTimeOut)
    {
        WebsocketServer::Settings serverSettings;
        serverSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM;
        serverSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
        serverSettings.port = getPort();
        serverSettings.pong.enabled = false;
        auto server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
        int port = server->start();

        WebsocketClient client(std::make_shared<NullMetrica>());
        WebsocketClient::Settings clienstSettings;
        clienstSettings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        clienstSettings.tls.verifyHostname = false;
        clienstSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        clienstSettings.ping.interval = std::chrono::milliseconds(100);
        clienstSettings.ping.enabled = true;
        clienstSettings.reconnect.enabled = false;
        std::promise<Websocket::ConnectionInfo> connectionInfoPromise;

        client.setOnDisconnectHandler([&connectionInfoPromise](const Websocket::ConnectionInfo& connectionInfo) {
            YIO_LOG_INFO("\nConnectionInfo: \n"
                         << connectionInfo.toString());
            connectionInfoPromise.set_value(connectionInfo);
        });
        client.connectSync(clienstSettings);

        Websocket::ConnectionInfo connectionInfo = connectionInfoPromise.get_future().get();
        UNIT_ASSERT(connectionInfo.local.closeCode == Websocket::StatusCode::CLIENT_PONG_TIMEOUT);
    }

    Y_UNIT_TEST(testWebsocketClientRecieveBinary)
    {
        char dataArray[3];
        dataArray[0] = 1;
        dataArray[1] = 0;
        dataArray[2] = -1;
        std::string data(dataArray, 3);
        std::condition_variable wakeupVar_;
        MockWSServer mockXiva(getPort());
        int port = mockXiva.getPort();
        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;

        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        WebsocketClient client(std::make_shared<NullMetrica>());

        client.setOnBinaryMessageHandler([&](std::string msg) {
            messagePromise->set_value(msg);
        });
        client.connectSync(wsSettings);
        mockXiva.waitUntilConnections(1, 0);
        messagePromise = std::make_unique<std::promise<std::string>>();
        mockXiva.sendBinary(data);

        UNIT_ASSERT_VALUES_EQUAL(data, messagePromise->get_future().get());
    }

    Y_UNIT_TEST(testWebsocketClientSendBinary)
    {
        char dataArray[3];
        dataArray[0] = 1;
        dataArray[1] = 0;
        dataArray[2] = -1;
        std::string data(dataArray, 3);
        MockWSServer mockXiva(getPort());
        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;
        mockXiva.onBinaryMessage = [&](std::string msg) {
            messagePromise->set_value(msg);
        };
        mockXiva.onMessage = [&](std::string /* msg */) {
            UNIT_FAIL("No text message must be sent");
        };
        int port = mockXiva.getPort();

        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        WebsocketClient client(std::make_shared<NullMetrica>());

        client.connectSync(wsSettings);
        mockXiva.waitUntilConnections(1, 0);
        messagePromise = std::make_unique<std::promise<std::string>>();
        client.unsafeSendBinary(data);

        UNIT_ASSERT_VALUES_EQUAL(data, messagePromise->get_future().get());
    }

    Y_UNIT_TEST(testWebsocketClientConnectionHeaders)
    {
        std::condition_variable wakeupVar_;
        MockWSServer mockXiva(getPort());
        int port = mockXiva.getPort();
        std::unique_ptr<std::promise<std::string>> header1Promise = nullptr;
        std::unique_ptr<std::promise<std::string>> header2Promise = nullptr;
        mockXiva.onOpen = [&](MockWSServer::ConnectionPtr conn) {
            if (conn) {
                header1Promise->set_value(conn->get_request_header("Header1"));
                header2Promise->set_value(conn->get_request_header("Header2"));
            }
        };
        header1Promise = std::make_unique<std::promise<std::string>>();
        header2Promise = std::make_unique<std::promise<std::string>>();

        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        wsSettings.connectionHeaders["Header1"] = "Header 1 value";
        wsSettings.connectionHeaders["Header2"] = "Header 2 value";
        WebsocketClient client(std::make_shared<NullMetrica>());
        client.connectSync(wsSettings);

        UNIT_ASSERT_VALUES_EQUAL("Header 1 value", header1Promise->get_future().get());
        UNIT_ASSERT_VALUES_EQUAL("Header 2 value", header2Promise->get_future().get());
    }

    Y_UNIT_TEST(testClientConnectSyncWithTimeout)
    {
        MockWSServer mockServer(getPort());
        int port = mockServer.getPort();

        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        wsSettings.connectTimeoutMs = 3000;
        WebsocketClient client(std::make_shared<NullMetrica>());

        std::unique_ptr<std::promise<std::string>> messagePromise = nullptr;
        client.setOnMessageHandler([&](const std::string& msg) {
            messagePromise->set_value(msg);
        });

        // check opened connections
        bool connectResult = client.connectSyncWithTimeout(wsSettings);
        UNIT_ASSERT(connectResult);
        mockServer.waitUntilConnections(1, 0);
        UNIT_ASSERT_VALUES_EQUAL(1, mockServer.getCurrentConnectionCnt());
        UNIT_ASSERT_VALUES_EQUAL(1, mockServer.getConnectionOpenedCnt());
        messagePromise = std::make_unique<std::promise<std::string>>();
        mockServer.send("test");
        UNIT_ASSERT_VALUES_EQUAL("test", messagePromise->get_future().get());

        // check that connection will be properly closed
        bool disconnectResult = client.disconnectSyncWithTimeout(3000);
        UNIT_ASSERT(disconnectResult);
        mockServer.waitUntilConnections(0, 1);
        UNIT_ASSERT_VALUES_EQUAL(1, mockServer.getConnectionClosedCnt());
    }

    Y_UNIT_TEST(testClientMultipleConnectWithTimeout)
    {
        MockWSServer mockServer(getPort());
        int port = mockServer.getPort();

        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        wsSettings.connectTimeoutMs = 3000;
        WebsocketClient client(std::make_shared<NullMetrica>());

        // check that we cannot open multiple connections and one of them will be interrupted
        bool connectResult = true;
        for (int i = 0; i < 100; i++) {
            connectResult &= client.connectSyncWithTimeout(wsSettings);
        }
        UNIT_ASSERT(connectResult);

        waitUntil([&]() {
            return mockServer.getCurrentConnectionCnt() == 1;
        });
        YIO_LOG_INFO("Current connections: " << mockServer.getCurrentConnectionCnt());
        YIO_LOG_INFO("Opened connections: " << mockServer.getConnectionOpenedCnt());
        UNIT_ASSERT_VALUES_EQUAL(1, mockServer.getCurrentConnectionCnt());
        UNIT_ASSERT_VALUES_EQUAL(1, mockServer.getConnectionOpenedCnt());
    }

    Y_UNIT_TEST(testClientConnectWithTimeoutFail)
    {
        WebsocketClient::Settings wsSettings;
        wsSettings.tls.disabled = true;
        wsSettings.url = "wss://some_wrong_host:6666/";
        wsSettings.connectTimeoutMs = 3000;
        WebsocketClient client(std::make_shared<NullMetrica>());

        YIO_LOG_INFO("Try to connect to " << wsSettings.url);
        bool connectResult = client.connectSyncWithTimeout(wsSettings);
        YIO_LOG_INFO("got connect result " << connectResult);
        UNIT_ASSERT(!connectResult);
    }

    Y_UNIT_TEST(testWaitForStateWithTimeout)
    {
        // initial state is STOPPED
        WebsocketClient client(std::make_shared<NullMetrica>());

        // wait for some not stopped state
        YIO_LOG_INFO("waiting for CONNECTED");
        bool waitResult = client.waitForStateWithTimeout(WebsocketClient::State::CONNECTED, 1000);
        YIO_LOG_INFO("wait for CONNECTED end");
        UNIT_ASSERT(!waitResult);

        YIO_LOG_INFO("waiting for CONNECTING");
        waitResult = client.waitForStateWithTimeout(WebsocketClient::State::CONNECTING, 1000);
        YIO_LOG_INFO("wait for CONNECTING end");
        UNIT_ASSERT(!waitResult);

        // wait for initial state - STOPPED
        YIO_LOG_INFO("waiting for STOPPED");
        waitResult = client.waitForStateWithTimeout(WebsocketClient::State::STOPPED, 1000);
        YIO_LOG_INFO("wait for STOPPED end");
        UNIT_ASSERT(waitResult);
    }
}
