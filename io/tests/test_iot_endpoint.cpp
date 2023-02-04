#include <yandex_io/services/iot/iot_endpoint.h>

#include <yandex_io/capabilities/file_player/interfaces/mocks/mock_i_file_player_capability.h>
#include <yandex_io/libs/configuration/configuration.h>
#include <yandex_io/libs/iot/i_iot_discovery.h>
#include <yandex_io/libs/iot/i_iot_discovery_provider.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>
#include <yandex_io/libs/threading/steady_condition_variable.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>

#include <fstream>
#include <future>
#include <mutex>
#include <vector>

using namespace quasar;

class IotActionsList {
public:
    enum class Action {
        START,
        STOP
    };

    void checkActionsList(const std::vector<Action>& actions) {
        std::unique_lock<std::mutex> lock(mutex_);
        YIO_LOG_INFO("Checking action list");
        CV_.wait(lock, [&actions, this]() {
            return actions == actions_;
        });
        YIO_LOG_INFO("Action list successfully checked");
    }

    void start() {
        YIO_LOG_DEBUG("Starting IOT discovery");
        std::lock_guard<std::mutex> guard(mutex_);
        actions_.push_back(Action::START);
        CV_.notify_all();
    }

    void stop() {
        YIO_LOG_DEBUG("Stopping IOT discovery");
        std::lock_guard<std::mutex> guard(mutex_);
        actions_.push_back(Action::STOP);
        CV_.notify_all();
    }

private:
    std::mutex mutex_;
    std::vector<Action> actions_;
    SteadyConditionVariable CV_;
};

class TestIIotDiscovery: public YandexIO::IIotDiscovery {
public:
    TestIIotDiscovery(std::shared_ptr<IotActionsList> actionList,
                      YandexIO::IIotDiscovery::PairingInfo pairingInfo)
        : actionList_(actionList)
        , requiredPairingInfo_(pairingInfo)
    {
    }

    void start(const YandexIO::IIotDiscovery::PairingInfo& pairingInfo) override {
        YIO_LOG_DEBUG("Starting IOT discovery");
        UNIT_ASSERT_EQUAL(pairingInfo.token, requiredPairingInfo_.token);
        UNIT_ASSERT_EQUAL(pairingInfo.cipher, requiredPairingInfo_.cipher);
        UNIT_ASSERT_EQUAL(pairingInfo.wifiSSID, requiredPairingInfo_.wifiSSID);
        UNIT_ASSERT_EQUAL(pairingInfo.wifiPassword, requiredPairingInfo_.wifiPassword);
        if (actionList_) {
            actionList_->start();
        }
    }

    void stop() noexcept override {
        YIO_LOG_DEBUG("Stopping IOT discovery");
        if (actionList_) {
            actionList_->stop();
        }
    }

private:
    std::shared_ptr<IotActionsList> actionList_;
    YandexIO::IIotDiscovery::PairingInfo requiredPairingInfo_;
};

class TestIIotDiscoveryProvider: public YandexIO::IIotDiscoveryProvider {
public:
    TestIIotDiscoveryProvider(YandexIO::IIotDiscovery::PairingInfo pairingInfo)
        : pairingInfo_(pairingInfo)
    {
    }

    std::shared_ptr<YandexIO::IIotDiscovery> createDiscovery() override {
        return std::make_shared<TestIIotDiscovery>(actionsList, pairingInfo_);
    }

    // Must be set only before starting fixture, after starting will be reachable from IotEndpoint's thread
    std::shared_ptr<IotActionsList> actionsList;

private:
    YandexIO::IIotDiscovery::PairingInfo pairingInfo_;
};

struct Fixture: public QuasarUnitTestFixture {
    using Base = QuasarUnitTestFixture;

    void SetUp(NUnitTest::TTestContext& context) override {
        Base::SetUp(context);

        setDeviceForTests(std::make_shared<YandexIO::Device>(makeTestDeviceId(),
                                                             makeTestConfiguration(), std::make_unique<NullMetrica>(), makeTestHAL()));

        YandexIO::IIotDiscovery::PairingInfo pairingInfo;
        // Pairing token consists of concatenation of region, token and secret
        pairingInfo.token = backendRegion + backendToken + backendSecret;
        pairingInfo.cipher = backendCipher;
        pairingInfo.wifiSSID = wifiSSID;
        pairingInfo.wifiPassword = wifiPassword;
        iotDiscoveryProvider = std::make_shared<TestIIotDiscoveryProvider>(pairingInfo);

        mockCredentials = createIpcConnectorForTests("iot");
        mockCredentials->setMessageHandler([this](const auto& message) {
            if (message->has_iot_state() && message->iot_state().current_state() == proto::IotState::STARTING_DISCOVERY && message->iot_state().current_state() != message->iot_state().prev_state()) {
                proto::QuasarMessage credentialsMessage;
                auto credentials = credentialsMessage.mutable_iot_request()->mutable_credentials();
                credentials->set_ssid(TString(wifiSSID));
                credentials->set_password(TString(wifiPassword));
                credentials->set_token(TString(pairingToken));
                credentials->set_cipher(TString(backendCipher));
                mockCredentials->sendMessage(std::move(credentialsMessage));
            }
        });

        filePlayerCapabilityMock = std::make_shared<YandexIO::MockIFilePlayerCapability>();
        iotConnector = createIpcConnectorForTests("iot");

        auto& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        wifiStoragePath = config["firstrund"]["wifiStoragePath"].asString();
        {
            std::ofstream file(wifiStoragePath);
            if (!file.good()) {
                throw std::runtime_error("Cannot open file " + wifiStoragePath + " for writing");
            }

            quasar::proto::WifiConnect wifiInfo;
            wifiInfo.set_wifi_id(TString(wifiSSID));
            wifiInfo.set_password(TString(wifiPassword));
            file << wifiInfo.SerializeAsString();
        }
    }

    void start() {
        iotEndpoint = std::make_unique<IotEndpoint>(getDeviceForTests(), ipcFactoryForTests(), iotDiscoveryProvider, filePlayerCapabilityMock);
        iotEndpoint->start();

        iotConnector->connectToService();
        mockCredentials->connectToService();

        iotConnector->waitUntilConnected();
        mockCredentials->waitUntilConnected();
    }

    void TearDown(NUnitTest::TTestContext& context) override {
        mockCredentials->shutdown();
        iotConnector->shutdown();
        ::unlink(wifiStoragePath.c_str());

        Base::TearDown(context);
    }

    static const std::string wifiSSID;
    static const std::string wifiPassword;
    static const std::string backendRegion;
    static const std::string backendToken;
    static const std::string backendSecret;
    static const std::string backendCipher;
    static const std::string pairingToken;

    YandexIO::Configuration::TestGuard testGuard;
    std::shared_ptr<TestIIotDiscoveryProvider> iotDiscoveryProvider;
    std::unique_ptr<IotEndpoint> iotEndpoint;
    std::shared_ptr<ipc::IConnector> iotConnector;
    std::shared_ptr<ipc::IConnector> mockCredentials;
    std::shared_ptr<YandexIO::MockIFilePlayerCapability> filePlayerCapabilityMock;
    std::string wifiStoragePath;
};

const std::string Fixture::wifiSSID = "TEST_WIFI_SSID";
const std::string Fixture::wifiPassword = "TEST_WIFI_PWD";
const std::string Fixture::backendRegion = "TEST_REGION";
const std::string Fixture::backendToken = "TEST_TOKEN";
const std::string Fixture::backendSecret = "TEST_SECRET";
const std::string Fixture::backendCipher = "TEST_CIPHER";
const std::string Fixture::pairingToken = Fixture::backendRegion + Fixture::backendToken + Fixture::backendSecret;

Y_UNIT_TEST_SUITE(IotEndpointTest) {

    Y_UNIT_TEST_F(testIotStates, Fixture) {
        std::vector<proto::IotState_State> requiredStates{
            proto::IotState::IDLE,
            proto::IotState::STARTING_DISCOVERY,
            proto::IotState::DISCOVERY_IN_PROGRESS,
            proto::IotState::IDLE,
        };
        std::vector<proto::IotState_State>::iterator requiredStatesIterator = requiredStates.begin();
        std::promise<void> allStatesAreReceived;
        std::promise<void> credentialsReceived;
        iotConnector->setMessageHandler([&requiredStates,
                                         &requiredStatesIterator,
                                         &allStatesAreReceived,
                                         &credentialsReceived](const auto& request) {
            UNIT_ASSERT(request->has_iot_state());
            UNIT_ASSERT(request->iot_state().current_state() == *requiredStatesIterator);
            if (requiredStatesIterator != requiredStates.begin()) {
                UNIT_ASSERT(request->iot_state().prev_state() == *(requiredStatesIterator - 1));
            }
            if (*requiredStatesIterator == proto::IotState::DISCOVERY_IN_PROGRESS) {
                credentialsReceived.set_value();
            }
            ++requiredStatesIterator;
            if (requiredStatesIterator == requiredStates.end()) {
                allStatesAreReceived.set_value();
            }
        });
        start();
        proto::QuasarMessage startMessage;
        startMessage.mutable_iot_request()->mutable_start_discovery();
        iotConnector->sendMessage(std::move(startMessage));

        credentialsReceived.get_future().get(); // Waiting for credentials

        proto::QuasarMessage stopMessage;
        stopMessage.mutable_iot_request()->mutable_stop_discovery();
        iotConnector->sendMessage(std::move(stopMessage));

        allStatesAreReceived.get_future().get();
    }

    Y_UNIT_TEST_F(testIotStatesTimeout, Fixture) {
        std::vector<proto::IotState_State> requiredStates{
            proto::IotState::IDLE,
            proto::IotState::STARTING_DISCOVERY,
            proto::IotState::DISCOVERY_IN_PROGRESS,
            proto::IotState::IDLE,
        };
        std::vector<proto::IotState_State>::iterator requiredStatesIterator = requiredStates.begin();
        std::promise<void> allStatesAreReceived;
        iotConnector->setMessageHandler([&requiredStates, &requiredStatesIterator, &allStatesAreReceived](const auto& request) {
            UNIT_ASSERT(request->has_iot_state());
            UNIT_ASSERT(request->iot_state().current_state() == *requiredStatesIterator);
            if (requiredStatesIterator != requiredStates.begin()) {
                UNIT_ASSERT(request->iot_state().prev_state() == *(requiredStatesIterator - 1));
            }
            ++requiredStatesIterator;
            if (requiredStatesIterator == requiredStates.end()) {
                allStatesAreReceived.set_value();
            }
        });
        start();
        proto::QuasarMessage startMessage;
        startMessage.mutable_iot_request()->mutable_start_discovery()->set_timeout_ms(1);
        iotConnector->sendMessage(std::move(startMessage));
        allStatesAreReceived.get_future().get();
    }

    Y_UNIT_TEST_F(testIotDiscoveryActions, Fixture) {
        iotDiscoveryProvider->actionsList = std::make_shared<IotActionsList>();
        start();
        proto::QuasarMessage startMessage;
        startMessage.mutable_iot_request()->mutable_start_discovery();
        iotConnector->sendMessage(std::move(startMessage));
        iotDiscoveryProvider->actionsList->checkActionsList({IotActionsList::Action::START});
        proto::QuasarMessage stopMessage;
        stopMessage.mutable_iot_request()->mutable_stop_discovery();
        iotConnector->sendMessage(std::move(stopMessage));
        iotDiscoveryProvider->actionsList->checkActionsList({
            IotActionsList::Action::START,
            IotActionsList::Action::STOP,
        });
    }
}
