#include <yandex_io/capabilities/alice/interfaces/mocks/mock_i_alice_request_events.h>
#include <yandex_io/libs/base/directives.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/ipc/i_connector.h>
#include <yandex_io/libs/ipc/i_server.h>
#include <yandex_io/libs/ipc/mock/server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/protobuf_utils/proto_trace.h>
#include <yandex_io/libs/spotter_types/spotter_types.h>
#include <yandex_io/libs/telemetry/null/null_metrica.h>
#include <yandex_io/services/aliced/speechkit_endpoint.h>
#include <yandex_io/services/aliced/tests/testlib/ske_test_base_fixture.h>
#include <yandex_io/tests/testlib/test_http_server.h>
#include <yandex_io/tests/testlib/test_spotter.h>
#include <yandex_io/tests/testlib/test_utils.h>

#include <json/json.h>

#include <util/folder/path.h>
#include <util/generic/scope.h>

#include <algorithm>
#include <concepts>
#include <cstdio>
#include <deque>
#include <memory>
#include <mutex>
#include <optional>
#include <type_traits>
#include <vector>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace quasar::proto;
using namespace testing;

using namespace std::literals;

namespace {
    bool messageHasSoundFile(const ipc::SharedMessage& msg, TFsPath fileName) {
        if (!msg->has_media_request() || !msg->media_request().has_play_audio() || !msg->media_request().play_audio().has_file_path()) {
            YIO_LOG_WARN("No file_path in message");
            return false;
        }

        TFsPath path(msg->media_request().play_audio().file_path());

        if (path.GetName() != fileName) {
            YIO_LOG_WARN("Unexpected sound_file. Got " << path.GetName() << ", expected " << fileName);
            return false;
        }
        return true;
    }
} // namespace

namespace {

    class SKETestFixture: public SKETestBaseFixture {
    public:
        using Base = SKETestBaseFixture;

        SKETestFixture(bool initDefaultMocks = true)
            : SKETestBaseFixture(initDefaultMocks)
        {
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        void initialize(const std::set<std::string>& disabledServices) {
            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["common"]["useBluetoothd"] = true;

            mockAudioClientd->setMessageHandler([this](const auto& msg, auto& conn) {
                if (audioClientMessageHandler_) {
                    audioClientMessageHandler_(msg, conn);
                }
            });

            SKETestBaseFixture::initialize(disabledServices);
        }

        void sendAppState(AppState::ScreenType screenType) const {
            NAlice::TDeviceState::TVideo state;
            const auto* desc = AppState::ScreenType_descriptor();
            std::string type = desc->FindValueByNumber(screenType)->name();
            std::transform(type.begin(), type.end(), type.begin(), ::tolower);
            state.SetCurrentScreen(TString(type));
            mockSdk_->getDeviceStateCapability()->setVideoState(state);
        }

        void setAudioClientMessageHandler(OnMessageHandler handler) {
            audioClientMessageHandler_ = std::move(handler);
        }

    public:
        OnMessageHandler audioClientMessageHandler_;
    };

    class SKETestConnectedServicesFixture: public SKETestBaseFixture {
    public:
        using Base = SKETestBaseFixture;

        SKETestConnectedServicesFixture()
            : SKETestBaseFixture(true)
        {
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            initialize({});
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }
    };

    class SKETestAutoInitFixture: public SKETestFixture {
    public:
        using Base = SKETestFixture;

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            initialize({});
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }
    };

    class SKETestAutoInitWithTelemetryFixture: public SKETestFixture {
        class MockTelemetry: public NullMetrica {
        public:
            struct Event {
                std::string name;
                std::string json;
            };

            using RequestUUIDCallback = std::function<void(UUIDCallback)>;
            void setRequestUUIDCallback(RequestUUIDCallback cb) {
                requestUUIDCallback_ = std::move(cb);
            }

            std::mutex& getEventsMutex() {
                return eventsMutex_;
            }

            SteadyConditionVariable& getEventsCv() {
                return eventsCv_;
            }

            const std::vector<Event>& getEvents() const {
                return events_;
            }

            void requestUUID(UUIDCallback cb) override {
                if (requestUUIDCallback_ != nullptr) {
                    requestUUIDCallback_(cb);
                }
            }

            void reportEvent(const std::string& event, const std::string& json, ITelemetry::Flags /*flags*/) override {
                std::scoped_lock lock{eventsMutex_};
                YIO_LOG_INFO("Add event " << event << ", " << json);
                events_.push_back({.name = event, .json = json});
                eventsCv_.notify_one();
            }

            void putAppEnvironmentValue(const std::string& key, const std::string& value) override {
                std::scoped_lock<std::mutex> scopedLock_(envMapMutex_);
                YIO_LOG_INFO("putAppEnvironmentValue. key: " << key << ", value: " << value);
                env_[key] = value;
            };

            void deleteAppEnvironmentValue(const std::string& key) override {
                YIO_LOG_INFO("deleteAppEnvironmentValue. key: " << key);
                std::scoped_lock<std::mutex> scopedLock_(envMapMutex_);
                env_.erase(key);
            };

            std::unordered_map<std::string, std::string> getEnv() {
                std::scoped_lock<std::mutex> scopedLock_(envMapMutex_);
                return env_;
            };

        private:
            RequestUUIDCallback requestUUIDCallback_;

            std::mutex envMapMutex_;
            std::unordered_map<std::string, std::string> env_;

            std::mutex eventsMutex_;
            SteadyConditionVariable eventsCv_;
            std::vector<Event> events_;
        };

    public:
        using Base = SKETestFixture;

        SKETestAutoInitWithTelemetryFixture()
            : SKETestFixture(false)
        {
        }

        void SetUp(NUnitTest::TTestContext& context) override {
            Base::SetUp(context);

            auto mockTelemetry = std::make_unique<MockTelemetry>();
            telemetry_ = mockTelemetry.get();
            setDeviceForTests(makeDevice(std::move(mockTelemetry)));

            // init default mocks after setting up fixture device
            initDefaultMocks();

            Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
            config["aliced"]["startDelaySec"] = Json::Value{1};

            initialize({});

            mockSpotterStorage.onHandlePayload = [](
                                                     const TestHttpServer::Headers& /*headers*/, const std::string& /*payload*/,
                                                     TestHttpServer::HttpConnection& handler) {
                Spotter spotter = createSpotter();
                YIO_LOG_DEBUG("Sent spotter data, size=" << spotter.gzipData.size());
                handler.doReplay(200, "application/x-tar", spotter.gzipData);
            };
            mockSpotterStorage.start(getPort());
        }

        void TearDown(NUnitTest::TTestContext& context) override {
            Base::TearDown(context);
        }

        static std::unique_ptr<YandexIO::Device> makeDevice(std::unique_ptr<YandexIO::ITelemetry> telemetry) {
            return std::make_unique<YandexIO::Device>(
                QuasarUnitTestFixture::makeTestDeviceId(),
                QuasarUnitTestFixture::makeTestConfiguration(),
                std::move(telemetry),
                QuasarUnitTestFixture::makeTestHAL());
        }

    public:
        MockTelemetry* telemetry_;
        TestHttpServer mockSpotterStorage;
    };

} // namespace

Y_UNIT_TEST_SUITE(SpeechkitEndpointTest) {
    Y_UNIT_TEST_F(testAlicedDeviceContextConversation, SKETestAutoInitFixture) {
        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        /* Check toggle conversation start and stop Dialog */
        ioSDK->toggleConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        ioSDK->toggleConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        // Start conversation with toggle and cancel it with "stopConversation"
        ioSDK->toggleConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        ioSDK->stopConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        // Test that "startConversation" can start Dialog
        ioSDK->startConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("привет");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPEAKING_WITH_SPOTTER));
        // Test that "stopConversation" can stop voiceDialog when Alice speak
        ioSDK->stopConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
    }

    Y_UNIT_TEST_F(testAlicedSimple, SKETestAutoInitFixture)
    {
        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        auto makeDummyChannels = []() {
            using ChannelData = YandexIO::ChannelData;
            auto makeChannel = [](ChannelData::Type type, std::string name, bool reco) {
                YandexIO::ChannelData rval;
                rval.type = type;
                rval.isForRecognition = reco;
                rval.name = std::move(name);
                rval.data.resize(40000, 100); // 2.5 seconds
                return rval;
            };
            YandexIO::ChannelsData rval;
            rval.push_back(makeChannel(ChannelData::Type::VQE, "vqe", true));
            rval.push_back(makeChannel(ChannelData::Type::RAW, "raw_mic", false));
            return rval;
        };

        testAudioSourceClient->pushChannelsData(makeDummyChannels());
        testVoiceDialog->Say("алиса");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("привет");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPEAKING_WITH_SPOTTER));

        const auto& options = testVins->getLastPayload()["request"]["additional_options"];
        UNIT_ASSERT(options.isMember("spotter_rms"));

        UNIT_ASSERT_EQUAL(testVoiceDialog->getSpeech(), "Хеллоу.");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("хватит");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->Say("алиса");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("привет", false);
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("как", false);
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("дела", true); // End of phrase
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPEAKING_WITH_SPOTTER));
        UNIT_ASSERT_EQUAL(testVoiceDialog->getSpeech(), "Здравствуйте.");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("хватит");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
    }

    Y_UNIT_TEST_F(testAlicedConfigUpdate, SKETestAutoInitFixture)
    {
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->waitForYandexUid();

        testVoiceDialog->Say("яндекс");
        UNIT_ASSERT(!testVoiceDialog->waitForState(State::LISTENING, std::chrono::milliseconds(500)));
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->Say("алиса");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("хватит");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        std::unique_ptr<std::promise<void>> msgReceived;
        endpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& /*msg*/) {
            msgReceived->set_value();
        };
        Y_DEFER {
            endpoint->onQuasarMessageReceivedCallback = nullptr;
        };

        Json::Value config;
        Json::Value account_config;
        account_config["contentAccess"] = "medium";
        config["account_config"] = account_config;
        proto::QuasarMessage msg;
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        msg.mutable_user_config_update()->set_passport_uid("123");

        msgReceived = std::make_unique<std::promise<void>>();
        mockSyncd->sendToAll(proto::QuasarMessage{msg});
        msgReceived->get_future().get();

        changeSpotterWord("yandex");

        testVoiceDialog->Say("алиса");
        UNIT_ASSERT(!testVoiceDialog->waitForState(State::LISTENING, std::chrono::milliseconds(500)));
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->Say("яндекс");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("хватит");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        msgReceived = std::make_unique<std::promise<void>>();
        mockSyncd->sendToAll(proto::QuasarMessage{msg});
        msgReceived->get_future().get();

        Json::Value system_config;
        system_config["unused_field"] = "lol";
        account_config["unused_field"] = "lol";
        config["system_config"] = system_config;
        config["account_config"] = account_config;
        msg.mutable_user_config_update()->set_config(jsonToString(config));

        msgReceived = std::make_unique<std::promise<void>>();
        mockSyncd->sendToAll(proto::QuasarMessage{msg});
        msgReceived->get_future().get();

        // New VQE type and preset changes sound logging settings, so dialog settings should be changed.
        YandexIO::ChannelData::VqeInfo vqeInfo{"new_vqe", "new_preset"};
        endpoint->setVqeInfo(vqeInfo);

        waitUntil([this, &vqeInfo] {
            auto settings = testVoiceDialog->getSettings();
            auto extra = parseJson(settings.activationPhraseSpotter.soundLoggerSettings.payloadExtra);
            return extra["VQEType"].asString() == vqeInfo.type && extra["VQEPreset"].asString() == vqeInfo.preset;
        });

        // Add voiceDialogSettings/recognizer section -> one more settings update.
        const auto forceReconnectTimeout = 5000ms;
        system_config["voiceDialogSettings"]["recognizer"]["forceReconnectTimeout"] = static_cast<int>(forceReconnectTimeout.count());
        config["system_config"] = system_config;
        msg.mutable_user_config_update()->set_config(jsonToString(config));

        msgReceived = std::make_unique<std::promise<void>>();
        mockSyncd->sendToAll(proto::QuasarMessage{msg});
        msgReceived->get_future().get();

        waitUntil([this, forceReconnectTimeout] {
            auto recognizerSettings = testVoiceDialog->getSettings().recognizer;
            return recognizerSettings.forceReconnectTimeout == forceReconnectTimeout;
        });
    }

    Y_UNIT_TEST_F(testAdditionalOptionsWithAuxilaryDeviceConfig, SKETestAutoInitFixture)
    {
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        std::promise<void> msgReceived;
        endpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& msg) {
            if (msg.has_user_config_update()) {
                msgReceived.set_value();
            }
        };
        Y_DEFER {
            endpoint->onQuasarMessageReceivedCallback = nullptr;
        };

        Json::Value config;
        Json::Value accountConfig;
        Json::Value auxiliaryDeviceConfig;
        accountConfig["spotter"] = "alisa";
        accountConfig["contentAccess"] = "medium";

        auxiliaryDeviceConfig["alice4Buisness"]["max_volume"] = 4;
        auxiliaryDeviceConfig["alice4Buisness"]["smart_home_uid"] = 1234567890;

        config["auxiliary_device_config"] = auxiliaryDeviceConfig;
        config["account_config"] = accountConfig;

        proto::QuasarMessage msg;
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        msg.mutable_user_config_update()->set_passport_uid("123");

        mockSyncd->sendToAll(std::move(msg));
        msgReceived.get_future().get();

        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->Say("алиса");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));

        Json::Value lastPayload = parseJson(testVoiceDialog->getLastPayload());
        YIO_LOG_INFO(jsonToString(lastPayload));
        Json::Value quasarAuxiliaryConfig = lastPayload["request"]["additional_options"]["quasar_auxiliary_config"];
        UNIT_ASSERT_EQUAL(quasarAuxiliaryConfig, auxiliaryDeviceConfig);
    }

    Y_UNIT_TEST_F(testAdditionalOptionsWithoutAuxilaryDeviceConfig, SKETestAutoInitFixture)
    {
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        {
            std::promise<void> msgReceived;
            endpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& /*msg*/) {
                msgReceived.set_value();
            };
            Y_DEFER {
                endpoint->onQuasarMessageReceivedCallback = nullptr;
            };

            Json::Value config;
            Json::Value accountConfig;
            accountConfig["spotter"] = "alisa";
            accountConfig["contentAccess"] = "medium";
            config["account_config"] = accountConfig;

            proto::QuasarMessage msg;
            msg.mutable_user_config_update()->set_config(jsonToString(config));
            msg.mutable_user_config_update()->set_passport_uid("123");

            mockSyncd->sendToAll(std::move(msg));
            msgReceived.get_future().get();
        }

        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->Say("алиса");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));

        Json::Value lastPayload = parseJson(testVoiceDialog->getLastPayload());
        YIO_LOG_INFO(jsonToString(lastPayload));
        Json::Value additionalOptions = lastPayload["request"]["additional_options"];
        UNIT_ASSERT(!additionalOptions.isMember("quasar_auxiliary_config"));
    }

    Y_UNIT_TEST_F(testAlicedConfigUpdateNoSpotterFile, SKETestAutoInitFixture)
    {
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->waitForYandexUid();

        std::unique_ptr<std::promise<void>> msgReceived;
        endpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& /*msg*/) {
            if (msgReceived) {
                msgReceived->set_value();
            }
        };

        TFsPath(JoinFsPaths(tryGetRamDrivePath(), "activation/yandex")).ForceDelete();

        Json::Value config;
        Json::Value account_config;
        account_config["contentAccess"] = "medium";
        config["account_config"] = account_config;

        proto::QuasarMessage msg;
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        msg.mutable_user_config_update()->set_passport_uid("123");

        msgReceived = std::make_unique<std::promise<void>>();
        mockSyncd->sendToAll(std::move(msg));
        msgReceived->get_future().get();

        changeSpotterWord("yandex");

        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->Say("алиса");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        testVoiceDialog->Say("хватит");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
    }

    Y_UNIT_TEST_F(testSingleDialogMode, SKETestAutoInitFixture)
    {
        startEndpoint();

        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        // set single dialog mode
        proto::QuasarMessage msg;
        Json::Value config;
        config["system_config"]["singleDialogMode"] = "a5d219e5-4ae9-4852-bed1-43fe3d946704";
        config["account_config"]["contentAccess"] = "medium";
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        sendSync(mockSyncd, msg);

        changeSpotterWord("yandex");

        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPEAKING_WITH_SPOTTER));
        UNIT_ASSERT_EQUAL(testVoiceDialog->getSpeech(), "Привет из залипающего навыка!.");
    }

    Y_UNIT_TEST_F(testAlicedEnableSpottersFlag, SKETestAutoInitFixture)
    {
        const auto enableSpotters = [this](bool spotters, bool longListening, bool commandSpotters) {
            Json::Value config;
            config["system_config"]["enableSpotters"] = spotters;
            config["system_config"]["longListeningEnabled"] = longListening;
            config["system_config"]["enableCommandSpotters"] = commandSpotters;
            config["account_config"]["spotter"] = "alice";
            config["account_config"]["contentAccess"] = "medium";

            proto::QuasarMessage msg;
            msg.mutable_user_config_update()->set_config(jsonToString(config));
            sendSync(mockSyncd, msg);
        };

        TFsPath navigationSpotterPath{JoinFsPaths(tryGetRamDrivePath(), SpotterTypes::NAVIGATION)};
        TFsPath naviOldSpotterPath{JoinFsPaths(tryGetRamDrivePath(), SpotterTypes::NAVIGATION_OLD)};
        navigationSpotterPath.MkDirs();
        naviOldSpotterPath.MkDirs();
        Y_DEFER {
            navigationSpotterPath.ForceDelete();
            naviOldSpotterPath.ForceDelete();
        };

        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        // Enable long listening for the next check.
        enableSpotters(true, true, false);
        sendAppState(AppState::GALLERY);

        // Check that an 'enableSpotters' flag has a priority over a 'longListeningEnabled' flag.
        enableSpotters(false, true, false);

        // Enable command spotters for the next check.
        enableSpotters(true, false, true);
        testVoiceDialog->waitForCommandSpotter(SpotterTypes::NAVIGATION);

        // Check that an 'enableSpotters' flag has a priority over an 'enableCommandSpotters' flag.
        enableSpotters(false, false, true);
        testVoiceDialog->waitForCommandSpotter("");

        // Enable spotters for the next check.
        enableSpotters(true, false, false);
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        // Check that disabling spotters sets dialog settings with empty spotters settings.
        enableSpotters(false, false, false);
        waitUntil([this]() {
            const auto& settings = testVoiceDialog->getSettings();
            const auto defaultSpotterSettings = ::SpeechKit::PhraseSpotterSettings{""};
            return settings.activationPhraseSpotter == defaultSpotterSettings && settings.interruptionPhraseSpotter == defaultSpotterSettings && settings.additionalPhraseSpotter == defaultSpotterSettings;
        });
    }

    Y_UNIT_TEST_F(testAlicedSpotterRejected, SKETestAutoInitFixture)
    {
        std::atomic_bool received;
        received = false;
        std::promise<void> msgReceived;
        setAudioClientMessageHandler([&](const auto& msg, auto& /*connection*/) {
            if (messageHasSoundFile(msg, "vins_error.wav")) {
                received = true;
                msgReceived.set_value();
            }
        });
        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->Say("алиса");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
        SpeechKit::Error error(SpeechKit::Error::ErrorSpottingRejected, "spotter rejected");
        testVoiceDialog->GenVinsError(error);

        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
        UNIT_ASSERT_EQUAL(received, false);

        testVoiceDialog->Say("алиса");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));

        error = SpeechKit::Error(SpeechKit::Error::ErrorInvalidApiKey, "spotter rejected");
        testVoiceDialog->GenVinsError(error);

        msgReceived.get_future().get();

        UNIT_ASSERT_EQUAL(received, true);
    }

    Y_UNIT_TEST_F(testWifiErrorMessage, SKETestAutoInitFixture)
    {
        std::promise<std::string> soundFilePromise;
        setAudioClientMessageHandler([&](const auto& msg, auto& /*connection*/) {
            if (msg->has_media_request() && msg->media_request().has_play_audio() && msg->media_request().play_audio().has_file_path()) {
                soundFilePromise.set_value(msg->media_request().play_audio().file_path());
            }
        });

        std::promise<void> networkStatusReceivedPromise;
        endpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& message) {
            if (message.has_network_status()) {
                networkStatusReceivedPromise.set_value();
            }
        };
        Y_DEFER {
            endpoint->onQuasarMessageReceivedCallback = nullptr;
        };

        std::promise<void> networkdClientConnectedPromise;
        setNetworkdClientConnectedHandler([&](auto& /*connection*/) {
            networkdClientConnectedPromise.set_value();
        });

        startEndpoint();
        networkdClientConnectedPromise.get_future().wait();

        auto checkWifiError = [&](SpeechKit::Error::Code errorCode, proto::NetworkStatus::Status status, const std::string& errorWav) {
            networkStatusReceivedPromise = std::promise<void>();
            proto::QuasarMessage message;
            message.mutable_network_status()->set_status(status);
            mockNetworkd->sendToAll(std::move(message));

            networkStatusReceivedPromise.get_future().wait();

            UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
            testVoiceDialog->Say("алиса");
            UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));

            SpeechKit::Error error(errorCode, "Some error");
            testVoiceDialog->genRecognitionError(error);

            soundFilePromise = std::promise<std::string>();
            if (errorWav.empty()) {
                UNIT_ASSERT_EQUAL(soundFilePromise.get_future().wait_for(50ms), std::future_status::timeout); // No voice error message
            } else {
                std::string wavToPlay = TFsPath(soundFilePromise.get_future().get()).GetName();
                YIO_LOG_INFO("wavToPlay " << wavToPlay << " errorWav " << errorWav);
                UNIT_ASSERT_EQUAL(wavToPlay, errorWav);
            }
        };

        checkWifiError(SpeechKit::Error::ErrorNoSpeechDetected, proto::NetworkStatus::CONNECTED, "");
        checkWifiError(SpeechKit::Error::ErrorNoSpeechDetected, proto::NetworkStatus::NOT_CONNECTED, "");
        checkWifiError(SpeechKit::Error::ErrorNetwork, proto::NetworkStatus::CONNECTED, "no_internet.wav");
        checkWifiError(SpeechKit::Error::ErrorNetwork, proto::NetworkStatus::NOT_CONNECTED, "no_internet.wav");
        checkWifiError(SpeechKit::Error::ErrorTimeout, proto::NetworkStatus::CONNECTED, "vins_error.wav");
        checkWifiError(SpeechKit::Error::ErrorTimeout, proto::NetworkStatus::CONNECTING, "no_internet.wav");
        checkWifiError(SpeechKit::Error::ErrorTimeout, proto::NetworkStatus::NOT_CONNECTED, "no_internet.wav");
        checkWifiError(SpeechKit::Error::ErrorPongTimeoutElapsed, proto::NetworkStatus::NOT_CONNECTED, "no_internet.wav");
        checkWifiError(SpeechKit::Error::ErrorPongTimeoutElapsed, proto::NetworkStatus::CONNECTED, "no_internet.wav");
        checkWifiError(SpeechKit::Error::ErrorPongTimeoutElapsed, proto::NetworkStatus::CONNECTED_NO_INTERNET, "no_internet.wav");
        checkWifiError(SpeechKit::Error::ErrorServer, proto::NetworkStatus::CONNECTED, "vins_error.wav");
        checkWifiError(SpeechKit::Error::ErrorServer, proto::NetworkStatus::NOT_CONNECTED, "vins_error.wav");
    }

    Y_UNIT_TEST_F(testAlicedVoiceDialogSettingsConfigFromQuasmodrom, SKETestAutoInitFixture)
    {
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        // required fields
        {
            const auto& settings = testVoiceDialog->getSettings();
            // required fields fallback to default values
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthAfterTrigger, 500ms);
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 1500ms);
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundFormat, SpeechKit::SoundFormat::OPUS);
            UNIT_ASSERT_EQUAL(settings.recognizer.enableCapitalization, false);
            UNIT_ASSERT_EQUAL(settings.recognizer.enablePunctuation, true);
            // additional spotter logginng is disabled by default
            UNIT_ASSERT_EQUAL(settings.additionalPhraseSpotter.soundLoggerSettings.soundLengthAfterTrigger, 0ms);
            UNIT_ASSERT_EQUAL(settings.additionalPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 0ms);
        }

        Json::Value config;
        Json::Value account_config;
        account_config["contentAccess"] = "medium";
        config["account_config"] = account_config;

        proto::QuasarMessage msg;

        config["system_config"]["voiceDialogSettings"]["connectionTimeout"] = 300;
        config["system_config"]["voiceDialogSettings"]["recognizer"]["startingSilenceTimeout"] = 300;
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        mockSyncd->sendToAll(proto::QuasarMessage{msg});

        changeSpotterWord("yandex");

        waitUntil([this] {
            return testVoiceDialog->getSettings().connectionTimeout == 300ms;
        });

        {
            const auto& settings = testVoiceDialog->getSettings();
            UNIT_ASSERT_EQUAL(settings.connectionTimeout, 300ms);
            UNIT_ASSERT_EQUAL(settings.recognizer.startingSilenceTimeout, 300ms);
            // required fields shouldn't change
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthAfterTrigger, 500ms);
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 1500ms);
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundFormat, SpeechKit::SoundFormat::OPUS);
            UNIT_ASSERT_EQUAL(settings.recognizer.enableCapitalization, false);
            UNIT_ASSERT_EQUAL(settings.recognizer.enablePunctuation, true);
        }

        config["system_config"]["voiceDialogSettings"]["spotterLoggingTailMillis"] = 777;
        config["system_config"]["voiceDialogSettings"]["recognizer"]["enableCapitalization"] = true;
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        mockSyncd->sendToAll(proto::QuasarMessage{msg});

        waitUntil([this] {
            const auto& activationSpotter = testVoiceDialog->getSettings().activationPhraseSpotter;
            return activationSpotter.soundLoggerSettings.soundLengthAfterTrigger == 777ms;
        });

        {
            const auto& settings = testVoiceDialog->getSettings();
            UNIT_ASSERT_EQUAL(settings.connectionTimeout, 300ms);
            UNIT_ASSERT_EQUAL(settings.recognizer.startingSilenceTimeout, 300ms);
            UNIT_ASSERT_EQUAL(settings.recognizer.enableCapitalization, true);
            // required field shouldn't change
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 1500ms);
            UNIT_ASSERT_EQUAL(settings.recognizer.enablePunctuation, true);
        }

        config["system_config"]["voiceDialogSettings"]["additionalSpotterLoggingTailMillis"] = 200;
        config["system_config"]["voiceDialogSettings"]["additionalSpotterLoggingHeadMillis"] = 100;
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        mockSyncd->sendToAll(proto::QuasarMessage{msg});

        waitUntil([this] {
            const auto& additionalSpotter = testVoiceDialog->getSettings().additionalPhraseSpotter;
            return additionalSpotter.soundLoggerSettings.soundLengthAfterTrigger == 200ms && additionalSpotter.soundLoggerSettings.soundLengthBeforeTrigger == 100ms;
        });

        {
            const auto& settings = testVoiceDialog->getSettings();
            UNIT_ASSERT_EQUAL(settings.additionalPhraseSpotter.soundLoggerSettings.soundLengthAfterTrigger, 200ms);
            UNIT_ASSERT_EQUAL(settings.additionalPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 100ms);
            // required field shouldn't change
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 1500ms);
            UNIT_ASSERT_EQUAL(settings.recognizer.enablePunctuation, true);
        }

        config["system_config"]["voiceDialogSettings"]["soundLoggingFormat"] = "opus";
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        mockSyncd->sendToAll(proto::QuasarMessage{msg});

        auto getSoundLogFormat = [](const SpeechKit::SoundLoggingSettings& settings) {
            return settings.soundFormat;
        };

        waitUntil([this, &getSoundLogFormat] {
            const auto settings = testVoiceDialog->getSettings();
            return getSoundLogFormat(settings.activationPhraseSpotter.soundLoggerSettings) == SpeechKit::SoundFormat::OPUS && getSoundLogFormat(settings.interruptionPhraseSpotter.soundLoggerSettings) == SpeechKit::SoundFormat::OPUS && getSoundLogFormat(settings.additionalPhraseSpotter.soundLoggerSettings) == SpeechKit::SoundFormat::OPUS;
        });

        {
            const auto& settings = testVoiceDialog->getSettings();
            UNIT_ASSERT_EQUAL(getSoundLogFormat(settings.activationPhraseSpotter.rareEventSoundLoggerSettings),
                              SpeechKit::SoundFormat::PCM);
            UNIT_ASSERT_EQUAL(getSoundLogFormat(settings.interruptionPhraseSpotter.rareEventSoundLoggerSettings),
                              SpeechKit::SoundFormat::PCM);
            UNIT_ASSERT_EQUAL(getSoundLogFormat(settings.additionalPhraseSpotter.rareEventSoundLoggerSettings),
                              SpeechKit::SoundFormat::PCM);
            // required field shouldn't change
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 1500ms);
            UNIT_ASSERT_EQUAL(settings.recognizer.enablePunctuation, true);
        }

        config["system_config"]["voiceDialogSettings"]["soundLoggingFormat"] = "wrong_format";
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        mockSyncd->sendToAll(proto::QuasarMessage{msg});

        waitUntil([this, &getSoundLogFormat] {
            const auto settings = testVoiceDialog->getSettings();
            return getSoundLogFormat(settings.activationPhraseSpotter.soundLoggerSettings) == SpeechKit::SoundFormat::OPUS && getSoundLogFormat(settings.interruptionPhraseSpotter.soundLoggerSettings) == SpeechKit::SoundFormat::OPUS && getSoundLogFormat(settings.additionalPhraseSpotter.soundLoggerSettings) == SpeechKit::SoundFormat::OPUS;
        });

        {
            const auto& settings = testVoiceDialog->getSettings();
            UNIT_ASSERT_EQUAL(getSoundLogFormat(settings.activationPhraseSpotter.rareEventSoundLoggerSettings),
                              SpeechKit::SoundFormat::PCM);
            UNIT_ASSERT_EQUAL(getSoundLogFormat(settings.interruptionPhraseSpotter.rareEventSoundLoggerSettings),
                              SpeechKit::SoundFormat::PCM);
            UNIT_ASSERT_EQUAL(getSoundLogFormat(settings.additionalPhraseSpotter.rareEventSoundLoggerSettings),
                              SpeechKit::SoundFormat::PCM);
            // required field shouldn't change
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 1500ms);
            UNIT_ASSERT_EQUAL(settings.recognizer.enablePunctuation, true);
        }

        config["system_config"]["voiceDialogSettings"]["spotterLoggingRareEventPercent"] = 5;
        config["system_config"]["voiceDialogSettings"]["spotterLoggingRareEventTailMillis"] = 1000;
        config["system_config"]["voiceDialogSettings"]["spotterLoggingRareEventHeadMillis"] = 500;
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        mockSyncd->sendToAll(proto::QuasarMessage{msg});

        waitUntil([this] {
            const auto& settings = testVoiceDialog->getSettings();
            return settings.activationPhraseSpotter.rareEventPercent == 5 && settings.interruptionPhraseSpotter.rareEventPercent == 5 && settings.additionalPhraseSpotter.rareEventPercent == 5;
        });

        {
            const auto& settings = testVoiceDialog->getSettings();
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.rareEventSoundLoggerSettings.soundLengthBeforeTrigger, 500ms);
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.rareEventSoundLoggerSettings.soundLengthAfterTrigger, 1000ms);
            UNIT_ASSERT_EQUAL(settings.interruptionPhraseSpotter.rareEventSoundLoggerSettings.soundLengthBeforeTrigger, 500ms);
            UNIT_ASSERT_EQUAL(settings.interruptionPhraseSpotter.rareEventSoundLoggerSettings.soundLengthAfterTrigger, 1000ms);
            UNIT_ASSERT_EQUAL(settings.additionalPhraseSpotter.rareEventSoundLoggerSettings.soundLengthBeforeTrigger, 500ms);
            UNIT_ASSERT_EQUAL(settings.additionalPhraseSpotter.rareEventSoundLoggerSettings.soundLengthAfterTrigger, 1000ms);
            // required field shouldn't change
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 1500ms);
            UNIT_ASSERT_EQUAL(settings.recognizer.enablePunctuation, true);
        }

        config["system_config"]["voiceDialogSettings"]["sendPingPongTime"] = 10;
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        mockSyncd->sendToAll(proto::QuasarMessage{msg});

        waitUntil([this] {
            const auto& settings = testVoiceDialog->getSettings();
            return settings.sendPingPongTime == 10;
        });

        config["system_config"].clear();
        msg.mutable_user_config_update()->set_config(jsonToString(config));
        mockSyncd->sendToAll(proto::QuasarMessage{msg});

        waitUntil([this] {
            const auto& settings = testVoiceDialog->getSettings();
            return settings.connectionTimeout == 6s; // default value
        });

        {
            const auto& settings = testVoiceDialog->getSettings();
            // required fields fallback to default values
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthAfterTrigger, 500ms);
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundLengthBeforeTrigger, 1500ms);
            UNIT_ASSERT_EQUAL(settings.activationPhraseSpotter.soundLoggerSettings.soundFormat, SpeechKit::SoundFormat::OPUS);
            UNIT_ASSERT_EQUAL(settings.recognizer.enableCapitalization, false);
            UNIT_ASSERT_EQUAL(settings.recognizer.enablePunctuation, true);
        }
    }

    Y_UNIT_TEST_F(testBtConnectionInDeviceState, SKETestAutoInitFixture)
    {
        const std::string BT_NAME = "Sic Transit Gloria Mundi";
        const std::string BT_ADDR = "VE:RY:DE:AD:BE:AF";

        /* Trigger alice and get last payload for test purposes */
        auto triggerAlice = [&]() -> Json::Value {
            testVoiceDialog->Say("алиса");
            UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));
            testVoiceDialog->Say("хватит");
            auto lastPayload = testVins->getLastPayload();
            UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
            return lastPayload;
        };

        auto checkDefault = [&]() -> bool {
            // do not send any message. check default state
            const auto lastPayload = triggerAlice();
            YIO_LOG_INFO(lastPayload);
            return lastPayload["request"]["device_state"].isMember("bluetooth") && (lastPayload["request"]["device_state"]["bluetooth"] == Json::Value(Json::objectValue));
        };

        auto checkBtConnected = [&]() -> bool {
            const auto lastPayload = triggerAlice();
            YIO_LOG_INFO(lastPayload);
            const auto& deviceState = lastPayload["request"]["device_state"];
            return deviceState.isMember("bluetooth") && deviceState["bluetooth"].isMember("current_connections") && deviceState["bluetooth"]["current_connections"].isArray() && deviceState["bluetooth"]["current_connections"].size() == 1 && deviceState["bluetooth"]["current_connections"][0]["name"].asString() == BT_NAME;
        };

        startEndpoint();
        YIO_LOG_INFO("SpeechkitEndpoint started");
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        /* Check that default values (when we didn't connect bt at all) are correct */
        waitUntil(checkDefault);
        ioSDK->bluetoothSinkConnected(BT_ADDR, BT_NAME);
        /* Check that after bt connection there is expected json in device_state */
        waitUntil(checkBtConnected);
        ioSDK->bluetoothSinkDisconnected(BT_ADDR, BT_NAME);
        /* Check that after bt disconnect device_state json fallback to default */
        waitUntil(checkDefault);
    }

    Y_UNIT_TEST_F(testMetricaUUIDReceivingDelayEvent, SKETestAutoInitWithTelemetryFixture) {
        telemetry_->setRequestUUIDCallback([](YandexIO::ITelemetry::UUIDCallback cb) {
            cb(std::string{"some_metrica_uuid"});
        });

        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        std::unique_lock lock{telemetry_->getEventsMutex()};
        telemetry_->getEventsCv().wait(lock, [this] {
            const auto& events = telemetry_->getEvents();
            const auto foundEvent = std::find_if(events.cbegin(), events.cend(),
                                                 [](const auto& event) {
                                                     return event.name == "MetricaUUIDReceivingDelay";
                                                 });
            if (foundEvent != events.cend()) {
                UNIT_ASSERT(parseJson(foundEvent->json).isMember("delay_ms"));
                return true;
            }
            return false;
        });
    }

    Y_UNIT_TEST_F(testMetricaUUIDReceivingTimeoutEvent, SKETestAutoInitWithTelemetryFixture) {
        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        std::unique_lock lock{telemetry_->getEventsMutex()};
        telemetry_->getEventsCv().wait(lock, [this] {
            const auto& events = telemetry_->getEvents();
            const auto foundEvent = std::find_if(events.cbegin(), events.cend(),
                                                 [](const auto& event) {
                                                     return event.name == "MetricaUUIDReceivingTimeout";
                                                 });
            if (foundEvent != events.cend()) {
                UNIT_ASSERT(parseJson(foundEvent->json).isMember("timeout_ms"));
                return true;
            }
            return false;
        });
    }

    Y_UNIT_TEST_F(testOnInvalidOAuthToken, SKETestConnectedServicesFixture) {
        std::promise<std::string> authTokenUpdatePromise;

        mockAuthProvider->setRequestAuthTokenUpdate(
            [&](const std::string& authToken) {
                YIO_LOG_INFO("Received request to update auth token: " << authToken);
                UNIT_ASSERT_NO_EXCEPTION(authTokenUpdatePromise.set_value(authToken));
            });

        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        testVoiceDialog->raiseOnInvalidOAuthToken();

        // 123 - hard-coded value in SKETestBaseFixture
        UNIT_ASSERT_VALUES_EQUAL(authTokenUpdatePromise.get_future().get(), "123");
    }

    Y_UNIT_TEST_F(testStartRequestWithoutTokenByVoice, SKETestAutoInitFixture) {
        std::promise<void> wasSoundError;
        setAudioClientMessageHandler([&wasSoundError](const auto& msg, auto& /*connection*/) {
            if (messageHasSoundFile(msg, "auth_failed.wav")) {
                wasSoundError.set_value();
            }
        });

        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        {
            proto::QuasarMessage message;
            message.mutable_auth_failed();
            sendSync(mockSyncd, message);
        }

        YIO_LOG_INFO("Check voice");
        testVoiceDialog->Say("алиса", false);
        wasSoundError.get_future().wait();
    }

    Y_UNIT_TEST_F(testStartRequestWithoutTokenByButton, SKETestAutoInitFixture) {
        std::promise<void> wasSoundError;
        setAudioClientMessageHandler([&wasSoundError](const auto& msg, auto& /*connection*/) {
            if (messageHasSoundFile(msg, "auth_failed.wav")) {
                wasSoundError.set_value();
            }
        });

        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        {
            proto::QuasarMessage message;
            message.mutable_auth_failed();
            sendSync(mockSyncd, message);
        }

        YIO_LOG_INFO("Check button");
        ioSDK->startConversation();
        wasSoundError.get_future().wait();
    }

    Y_UNIT_TEST_F(testStartRequestWhenAuthorizationNotRequired, SKETestFixture) {
        Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);
        config["aliced"]["requireAuthorization"] = false;

        initialize({});

        std::promise<void> wasSoundError;
        setAudioClientMessageHandler([&wasSoundError](const auto& msg, auto& /*connection*/) {
            if (messageHasSoundFile(msg, "no_internet.wav")) {
                wasSoundError.set_value();
            }
        });

        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        mockDeviceStateProvider->setDeviceState(DeviceState{
            .configuration = DeviceState::Configuration::CONFIGURING,
            .networkStatus = mockDeviceStateProvider->deviceState().value()->networkStatus,
            .update = mockDeviceStateProvider->deviceState().value()->update,
        });

        YIO_LOG_INFO("Check requireAuthorization false");
        ioSDK->startConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));

        // Check that sound error hasn't been played.
        UNIT_ASSERT_EQUAL(std::future_status::timeout, wasSoundError.get_future().wait_for(1ms));
    }

    Y_UNIT_TEST_F(testStartRequestInBrickedState, SKETestAutoInitFixture) {
        std::unique_ptr<std::promise<void>> wasSoundError;
        setAudioClientMessageHandler([&wasSoundError](const auto& msg, auto& /*connection*/) {
            if (messageHasSoundFile(msg, "brick.wav")) {
                wasSoundError->set_value();
            }
        });

        UNIT_ASSERT(testVoiceDialog->waitForState(State::IDLE));
        startEndpoint();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));

        wasSoundError = std::make_unique<std::promise<void>>();

        {
            {
                proto::QuasarMessage message;
                message.set_brick_status(proto::BrickStatus::BRICK);
                sendSync(mockSyncd, message);
            }

            YIO_LOG_INFO("Check BRICK");
            ioSDK->startConversation();
            wasSoundError->get_future().wait();
        }

        ioSDK->stopConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
        wasSoundError = std::make_unique<std::promise<void>>();

        {
            {
                proto::QuasarMessage message;
                message.set_brick_status(proto::BrickStatus::BRICK_BY_TTL);
                sendSync(mockSyncd, message);
            }

            YIO_LOG_INFO("Check BRICK_BY_TTL");
            ioSDK->startConversation();
            wasSoundError->get_future().wait();
        }

        ioSDK->stopConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
        wasSoundError = std::make_unique<std::promise<void>>();

        {
            {
                proto::QuasarMessage message;
                message.set_brick_status(proto::BrickStatus::NOT_BRICK);
                sendSync(mockSyncd, message);
            }

            YIO_LOG_INFO("Check NOT_BRICK");
            ioSDK->startConversation();
            UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));

            // Check that sound error hasn't been played.
            UNIT_ASSERT_EQUAL(std::future_status::timeout, wasSoundError->get_future().wait_for(1ms));
        }

        ioSDK->stopConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
        wasSoundError = std::make_unique<std::promise<void>>();

        {
            {
                proto::QuasarMessage message;
                message.set_brick_status(proto::BrickStatus::UNKNOWN_BRICK_STATUS);
                sendSync(mockSyncd, message);
            }

            YIO_LOG_INFO("Check UNKNOWN_BRICK_STATUS");
            ioSDK->startConversation();
            UNIT_ASSERT(testVoiceDialog->waitForState(State::LISTENING));

            // Check that sound error hasn't been played.
            UNIT_ASSERT_EQUAL(std::future_status::timeout, wasSoundError->get_future().wait_for(1ms));
        }

        ioSDK->stopConversation();
        UNIT_ASSERT(testVoiceDialog->waitForState(State::SPOTTER));
    }
}
