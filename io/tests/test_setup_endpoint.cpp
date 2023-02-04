#include <yandex_io/services/setupd/setup_service.h>

#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/http_client/http_client.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/setup_parser/credentials.h>
#include <yandex_io/libs/setup_parser/setup_parser.h>
#include <yandex_io/libs/setup_parser/wifi_type.h>
#include <yandex_io/protos/enum_names/enum_names.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <library/cpp/testing/unittest/env.h>
#include <library/cpp/testing/unittest/registar.h>

#include <future>
#include <iostream>
#include <memory>
#include <string>
#include <thread>

namespace {
    using namespace quasar;
    using namespace quasar::SetupParser;
    using namespace quasar::proto;
    using namespace quasar::TestUtils;
    using GetPortFunc = std::function<int()>;

    struct ConnectParameters {
        std::vector<std::string> ssids;
        std::string password;
        std::string authCode;
        quasar::proto::WifiType wifiType = quasar::proto::WifiType::UNKNOWN_WIFI_TYPE;
        bool stopAccessPoint = true;
    };

    class MockFirstrund {
    private:
        YandexIO::Configuration::TestGuard testGuard;
        std::shared_ptr<ipc::IServer> protobuf_server_;
        TestHttpServer http_server_;
        std::unique_ptr<std::promise<bool>> connected_;

        static ConnectParameters parseParams(const std::string& payload) {
            ConnectParameters parameters;
            auto request = parseJson(payload);

            const auto& id = request["ssid"];
            if (id.isString()) {
                parameters.ssids.push_back(request["ssid"].asString());
            } else { // id is array of ssids
                for (const auto& ssid : id)
                {
                    parameters.ssids.push_back(ssid.asString());
                }
            }
            parameters.password = request["password"].asString();
            parameters.authCode = request["xtoken_code"].asString();

            if (request.isMember("wifi_type"))
            {
                parameters.wifiType = quasar::proto::WifiType::UNKNOWN_WIFI_TYPE;
                wifiTypeParse(request["wifi_type"].asString(), &parameters.wifiType);
            }

            if (request.isMember("stop_access_point")) {
                parameters.stopAccessPoint = request["stop_access_point"].asBool();
            }

            return parameters;
        }

    public:
        MockFirstrund(std::shared_ptr<YandexIO::IDevice> device, std::shared_ptr<ipc::IIpcFactory> ipcFactory, GetPortFunc getPort) {
            auto& config = device->configuration()->getMutableConfig(testGuard);

            connected_ = std::make_unique<std::promise<bool>>();
            http_server_.onHandlePayload = [&](const TestHttpServer::Headers& /*headers*/, const std::string& payload, TestHttpServer::HttpConnection& handler) {
                YIO_LOG_INFO("MockFirstrund::http_server_ received: " << payload);
                std::this_thread::sleep_for(std::chrono::seconds(2)); // Emulating a small delay while connecting to the network
                ConnectParameters params = parseParams(payload);
                UNIT_ASSERT_VALUES_EQUAL(params.ssids[0], "MobDevInternet");
                UNIT_ASSERT_VALUES_EQUAL(params.password, "password");
                UNIT_ASSERT_VALUES_EQUAL(params.authCode, "xToken");
                UNIT_ASSERT_EQUAL(params.wifiType, quasar::proto::WifiType::WPA);
                handler.doReplay(200, "application/json", "{\"status\":\"ok\"}");
                connected_->set_value(true);
                YIO_LOG_INFO("MockFirstrund::http_server_ received: DONE");
            };

            protobuf_server_ = ipcFactory->createIpcServer("firstrund");
            protobuf_server_->listenService();

            config["firstrund"]["httpPort"] = http_server_.start(getPort());
            YIO_LOG_INFO("Started http firstrun server on port " << config["firstrund"]["httpPort"]);
        }
        void startInit() {
            quasar::proto::QuasarMessage message;
            message.set_configuration_state(quasar::proto::ConfigurationState::CONFIGURING);
            protobuf_server_->sendToAll(std::move(message));
        }
        void stopInit() {
            quasar::proto::QuasarMessage message;
            message.set_configuration_state(quasar::proto::ConfigurationState::CONFIGURED);
            protobuf_server_->sendToAll(std::move(message));
        }
        bool connected() {
            bool connect = connected_->get_future().get();
            connected_ = std::make_unique<std::promise<bool>>();
            return connect;
        }
    };
} // namespace

Y_UNIT_TEST_SUITE_F(TestSetupEndpoint, QuasarUnitTestFixture) {
    Y_UNIT_TEST(testSetup) {
        MockFirstrund mockFirstrund(getDeviceForTests(), ipcFactoryForTests(), std::bind(&QuasarUnitTestFixture::getPort, this));

        auto mockMetrica = createIpcServerForTests("metricad");
        auto mockUpdatesd = createIpcServerForTests("updatesd");
        auto mockAuthd = createIpcServerForTests("authd");
        auto mockYandexIO = createIpcServerForTests("iohub_services");

        auto mockWifid = createIpcServerForTests("wifid");
        std::mutex wifiMutex_;
        mockWifid->setMessageHandler([&](const auto& request, auto& connection) {
            std::unique_lock<std::mutex> lock(wifiMutex_);
            quasar::proto::QuasarMessage response;
            response.set_request_id(request->request_id());
            Y_VERIFY(request->has_wifi_list_request());

            auto wifiList = response.mutable_wifi_list();
            auto hotspot = wifiList->add_hotspots();
            hotspot->set_ssid("MobDevInternet");
            hotspot->set_secure(true);
            hotspot = wifiList->add_hotspots();
            hotspot->set_ssid("myquasar");
            hotspot->set_secure(false);

            connection.send(std::move(response));
        });

        mockMetrica->listenService();
        mockWifid->listenService();
        mockUpdatesd->listenService();
        mockAuthd->listenService();
        mockYandexIO->listenService();

        auto filePlayerCapabilityMock = std::make_shared<YandexIO::MockIFilePlayerCapability>();
        SetupEndpoint setupEndpoint(getDeviceForTests(), ipcFactoryForTests(), filePlayerCapabilityMock);
        std::vector<std::string> ssids;
        ssids.push_back("MobDevInternet");
        const Credentials credentials(quasar::WifiType::WIFI_TYPE_WPA, ssids, javaStyleStringHash("MobDevInternet"), "password", "xToken");

        mockFirstrund.startInit();
        mockFirstrund.startInit();
        mockFirstrund.startInit();
        mockFirstrund.startInit();

        setupEndpoint.connectWithCredentials(credentials, quasar::proto::SetupSource::SOUND);

        UNIT_ASSERT(mockFirstrund.connected());
    }
}
