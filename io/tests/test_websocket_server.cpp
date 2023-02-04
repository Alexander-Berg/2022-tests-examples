#include <yandex_io/libs/websocket/websocket_client.h>
#include <yandex_io/libs/websocket/websocket_server.h>

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>

#include <yandex_io/tests/testlib/test_certificates.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/registar.h>

#include <atomic>
#include <future>
#include <memory>
#include <thread>

using namespace quasar;
using namespace quasar::TestUtils;

Y_UNIT_TEST_SUITE_F(TestWebsocketServer, QuasarUnitTestFixture) {
    std::string reparseAddress(const std::string& src) {
        if (src.size() < 2) {
            return "NONE";
        }
        std::array<char, 1000> buf = {};
        if (src[0] == '[') { // ipv6
            auto srcTrimmed = src.substr(1, src.size() - 2);
            in6_addr addr;
            if (inet_pton(AF_INET6, srcTrimmed.c_str(), &addr) == -1) {
                return "WRONGIPV6";
            };
            return std::string("[") + inet_ntop(AF_INET6, &addr, buf.data(), buf.size()) + "]";
        }
        auto addr = inet_addr(src.c_str());
        return inet_ntop(AF_INET, &addr, buf.data(), buf.size());
    }

    Y_UNIT_TEST(testWebsocketServerBase)
    {
        WebsocketServer::Settings serverSettings;
        serverSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM;
        serverSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
        serverSettings.port = getPort();
        std::promise<std::string> serverMsgPromise;
        std::promise<void> clientConnectedToServerPromise;
        auto server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
        server->setOnOpenHandler([&](WebsocketServer::ConnectionHdl hdl) {
            const auto remoteAddr = server->getRemoteHost(hdl);
            const auto reparsedAddr = reparseAddress(remoteAddr);
            YIO_LOG_INFO("remote addr = '" << remoteAddr << "' reparsed back = '" << reparsedAddr << "'");
            UNIT_ASSERT_VALUES_EQUAL(remoteAddr, reparsedAddr);
            clientConnectedToServerPromise.set_value();
        });

        server->setOnMessageHandler([&serverMsgPromise](WebsocketServer::ConnectionHdl /*hdl*/, const std::string& msg) {
            serverMsgPromise.set_value(msg);
        });
        int port = server->start();

        WebsocketClient client(std::make_shared<NullMetrica>());
        WebsocketClient::Settings clienstSettings;
        clienstSettings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        clienstSettings.tls.verifyHostname = false;
        clienstSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        std::promise<std::string> msgPromise;
        std::atomic<bool> fail{false};
        client.setOnFailHandler([&fail](const Websocket::ConnectionInfo& /* connectionInfo */) {
            fail = true;
        });
        client.setOnMessageHandler([&msgPromise](const std::string& msg) {
            msgPromise.set_value(msg);
        });
        client.connectSync(clienstSettings);
        /* Make sure than client is connected */
        clientConnectedToServerPromise.get_future().get();
        server->sendAll("lol");
        client.unsafeSend("lol2");
        UNIT_ASSERT_VALUES_EQUAL("lol", msgPromise.get_future().get());
        UNIT_ASSERT_VALUES_EQUAL("lol2", serverMsgPromise.get_future().get());
        UNIT_ASSERT_VALUES_EQUAL(fail.load(), 0);
    }

    Y_UNIT_TEST(testWebsocketServerInvalidCert)
    {
        WebsocketServer::Settings serverSettings;
        serverSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM;
        serverSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
        serverSettings.port = getPort();
        auto server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
        int port = server->start();

        std::atomic_bool failed{false};
        std::promise<void> connectedPromise;
        WebsocketClient client(std::make_shared<NullMetrica>());
        WebsocketClient::Settings clienstSettings;
        clienstSettings.tls.crtBuffer = TestKeys().INCORRECT_PUBLIC_PEM;
        clienstSettings.tls.verifyHostname = false;
        clienstSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        client.setOnFailHandler([&failed](const Websocket::ConnectionInfo& connectionInfo) {
            YIO_LOG_INFO("\nConnectionInfo: \n"
                         << connectionInfo.toString());
            failed = true;
        });
        client.setOnConnectHandler([&connectedPromise]() {
            connectedPromise.set_value();
        });
        client.connectAsync(clienstSettings);
        /* Make sure that server reject clients with incorrect Certificates */
        const auto status = connectedPromise.get_future().wait_for(std::chrono::seconds(1));
        UNIT_ASSERT(status == std::future_status::timeout);
        /* Check that onFail callback was called */
        UNIT_ASSERT(failed.load());
    }

    Y_UNIT_TEST(testWebsocketServerNoStart)
    {
        WebsocketServer::Settings serverSettings;
        serverSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM;
        serverSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
        serverSettings.port = getPort();
        auto server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
    }

    Y_UNIT_TEST(testWebsocketServerPingClients)
    {
        WebsocketServer::Settings serverSettings;
        serverSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM;
        serverSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
        serverSettings.ping.interval = std::chrono::seconds(1);
        serverSettings.port = getPort();
        std::atomic<int> cnt{0};
        SteadyConditionVariable condVar;
        std::atomic<int> serverPingCnt{0};

        auto server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
        server->setOnCloseHandler([&](WebsocketServer::ConnectionHdl /*hdl*/, Websocket::ConnectionInfo /*info*/) {
            cnt++;
        });
        int port = server->start();

        WebsocketClient::DisposableClient::Settings clienstSettings;
        clienstSettings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        clienstSettings.tls.verifyHostname = false;

        auto client = WebsocketClient::DisposableClient::create("wss://localhost:" + std::to_string(port) + "/", clienstSettings, std::make_shared<NullMetrica>());
        client->onPingHandler = [&](std::weak_ptr<WebsocketClient::DisposableClient> /*client*/, const Websocket::ConnectionInfo& /*info*/, const std::string& /*payload*/) {
            ++serverPingCnt;
            condVar.notify_one();
            return true;
        };
        client->connectAsyncWithTimeout();
        /* Make sure that Server And Client send Ping/Pongs */
        TestUtils::waitUntil(condVar, [&]() {
            return (serverPingCnt == 3);
        });
        UNIT_ASSERT_VALUES_EQUAL(cnt.load(), 0);
        client.reset();
    }

    Y_UNIT_TEST(testWebsocketServerClientsNoPong)
    {
        WebsocketServer::Settings serverSettings;
        serverSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM;
        serverSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
        serverSettings.ping.interval = std::chrono::milliseconds(100);
        serverSettings.port = getPort();
        std::promise<WebsocketServer::ConnectionHdl> openConnectionPromise;
        std::promise<WebsocketServer::ConnectionHdl> closeConnectionPromise;
        auto server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
        server->setOnCloseHandler([&](WebsocketServer::ConnectionHdl hdl, Websocket::ConnectionInfo /*unused*/) {
            closeConnectionPromise.set_value(hdl);
            UNIT_ASSERT_VALUES_EQUAL(server->getConnectionsNumber(), 0);
        });
        server->setOnOpenHandler([&](WebsocketServer::ConnectionHdl hdl) {
            openConnectionPromise.set_value(hdl);
            UNIT_ASSERT_VALUES_EQUAL(server->getConnectionsNumber(), 1);
        });
        int port = server->start();

        WebsocketClient::DisposableClient::Settings clienstSettings;
        clienstSettings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        clienstSettings.tls.verifyHostname = false;
        clienstSettings.pong.enabled = false;

        auto client = WebsocketClient::DisposableClient::create("wss://localhost:" + std::to_string(port) + "/", clienstSettings, std::make_shared<NullMetrica>());
        client->connectAsyncWithTimeout();

        UNIT_ASSERT(openConnectionPromise.get_future().get().lock().get() == closeConnectionPromise.get_future().get().lock().get());
    }

    Y_UNIT_TEST(testWebsocketServerRestart)
    {
        WebsocketServer::Settings serverSettings;
        serverSettings.tls.crtPemBuffer = TestKeys().PUBLIC_PEM;
        serverSettings.tls.keyPemBuffer = TestKeys().PRIVATE_PEM;
        std::promise<void> connectPromise;
        serverSettings.port = getPort();
        auto server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
        int port = server->start();

        std::promise<std::string> msgPromise;
        WebsocketClient client(std::make_shared<NullMetrica>());
        client.setOnMessageHandler([&msgPromise](const std::string& msg) {
            msgPromise.set_value(msg);
        });
        WebsocketClient::Settings clientSettings;
        clientSettings.tls.crtBuffer = TestKeys().PUBLIC_PEM;
        clientSettings.tls.verifyHostname = false;
        clientSettings.url = "wss://localhost:" + std::to_string(port) + "/";
        client.connectSync(clientSettings);

        server = std::make_unique<WebsocketServer>(serverSettings, std::make_shared<NullMetrica>());
        server->setOnOpenHandler([&](WebsocketServer::ConnectionHdl /* hdl */) {
            connectPromise.set_value();
        });
        server->start();
        connectPromise.get_future().get();

        server->sendAll("lol");
        UNIT_ASSERT_VALUES_EQUAL("lol", msgPromise.get_future().get());
    }

    Y_UNIT_TEST(testExtractAsioHost)
    {
        auto fn = [](std::string src) {
            return WebsocketServer::extractAsioHost(std::move(src));
        };

        UNIT_ASSERT_VALUES_EQUAL(fn("[::ffff:1.2.3.4]:56"), "1.2.3.4");
        UNIT_ASSERT_VALUES_EQUAL(fn("[::ffff:host]:1234"), "host");
        UNIT_ASSERT_VALUES_EQUAL(fn("[ff::ff]:1234"), "[ff::ff]");
        UNIT_ASSERT_VALUES_EQUAL(fn("abracadabra"), "abracadabra");
    }
}
