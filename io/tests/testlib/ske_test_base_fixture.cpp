#include "ske_test_base_fixture.h"

#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/delay_timings_policy/delay_timings_policy.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/libs/logging/logging.h>
#include <yandex_io/libs/self_destroyer/self_destroyer_utils.h>

#include <speechkit/Logger.h>

using namespace quasar;
using namespace quasar::TestUtils;

SKETestBaseFixture::IoHubWrapper::IoHubWrapper(std::shared_ptr<ipc::IIpcFactory> ipcFactory)
    : server_(ipcFactory->createIpcServer("iohub_services"))
    , alicedConnector_(ipcFactory->createIpcConnector("aliced"))
    , aliceRemoteObjectId_("AliceCapability")
{
    server_->listenService();
    alicedConnector_->connectToService();
}

void SKETestBaseFixture::IoHubWrapper::toggleConversation(const YandexIO::VinsRequest::EventSource& eventSource) {
    auto message = quasar::ipc::buildMessage([&](auto& msg) {
        auto method = msg.mutable_remoting()->mutable_alice_capability_method();
        msg.mutable_remoting()->set_remote_object_id(TString(aliceRemoteObjectId_));

        method->set_method(quasar::proto::Remoting::AliceCapabilityMethod::TOGGLE_CONVERSATION);
        method->mutable_vins_request()->mutable_event_source()->CopyFrom(eventSource);
    });

    alicedConnector_->sendMessage(message);
}

void SKETestBaseFixture::IoHubWrapper::startConversation(const YandexIO::VinsRequest::EventSource& eventSource) {
    auto message = quasar::ipc::buildMessage([&](auto& msg) {
        auto method = msg.mutable_remoting()->mutable_alice_capability_method();
        msg.mutable_remoting()->set_remote_object_id(TString(aliceRemoteObjectId_));

        method->set_method(quasar::proto::Remoting::AliceCapabilityMethod::START_CONVERSATION);
        method->mutable_vins_request()->mutable_event_source()->CopyFrom(eventSource);
    });

    alicedConnector_->sendMessage(message);
}

void SKETestBaseFixture::IoHubWrapper::stopConversation() {
    auto message = quasar::ipc::buildMessage([&](auto& msg) {
        auto method = msg.mutable_remoting()->mutable_alice_capability_method();
        msg.mutable_remoting()->set_remote_object_id(TString(aliceRemoteObjectId_));

        method->set_method(quasar::proto::Remoting::AliceCapabilityMethod::STOP_CONVERSATION);
    });

    alicedConnector_->sendMessage(message);
}

void SKETestBaseFixture::IoHubWrapper::blockVoiceAssistant(const std::string& source, const std::optional<std::string>& errorSound) {
    proto::QuasarMessage message;
    message.mutable_io_control()->mutable_assistant_blocking()->set_source(TString(source));
    if (errorSound) {
        message.mutable_io_control()->mutable_assistant_blocking()->set_error_sound(TString(*errorSound));
    }
    server_->sendToAll(std::move(message));
}

void SKETestBaseFixture::IoHubWrapper::unblockVoiceAssistant(const std::string& source) {
    proto::QuasarMessage message;
    message.mutable_io_control()->mutable_assistant_unblocking()->set_source(TString(source));
    server_->sendToAll(std::move(message));
}

void SKETestBaseFixture::IoHubWrapper::bluetoothSinkConnected(const std::string& networkAddr, const std::string& networkName) {
    proto::QuasarMessage message;
    auto connectionEvent = message.mutable_io_control()->mutable_bluetooth_sink_event()->mutable_connection_event();
    auto messageNetwork = connectionEvent->mutable_network();
    messageNetwork->set_address(TString(networkAddr));
    messageNetwork->set_name(TString(networkName));
    messageNetwork->set_role(proto::BluetoothNetwork::SINK);
    connectionEvent->set_connection_event(proto::BtConnection::CONNECTED);
    server_->sendToAll(std::move(message));
}

void SKETestBaseFixture::IoHubWrapper::bluetoothSinkDisconnected(const std::string& networkAddr, const std::string& networkName) {
    proto::QuasarMessage message;
    auto connectionEvent = message.mutable_io_control()->mutable_bluetooth_sink_event()->mutable_connection_event();
    auto messageNetwork = connectionEvent->mutable_network();
    messageNetwork->set_address(TString(networkAddr));
    messageNetwork->set_name(TString(networkName));
    messageNetwork->set_role(proto::BluetoothNetwork::SINK);
    connectionEvent->set_connection_event(proto::BtConnection::DISCONNECTED);
    server_->sendToAll(std::move(message));
}

void SKETestBaseFixture::OnQuasarMessageReceivedCallback::onMessage(const proto::QuasarMessage& message) {
    if (isReady_.load()) {
        return;
    }

    std::scoped_lock lock{mutex_};

    isReady_.store(google::protobuf::util::MessageDifferencer::Equals(message, message_));
    if (isReady_) {
        cv_.notify_one();
    }
}

void SKETestBaseFixture::OnQuasarMessageReceivedCallback::setMessage(const proto::QuasarMessage& message) {
    std::scoped_lock lock{mutex_};
    isReady_.store(false);
    message_ = message;
}

void SKETestBaseFixture::OnQuasarMessageReceivedCallback::waitUntilDelivered() const {
    std::unique_lock lock{mutex_};
    cv_.wait(lock, [this] {
        return isReady_.load();
    });
}

SKETestBaseFixture::SKETestBaseFixture(bool initDefaultMocks)
    : initDefaultMocks_{initDefaultMocks}
{
}

void SKETestBaseFixture::SetUp(NUnitTest::TTestContext& context) {
    Base::SetUp(context);

    pathSpotterAlice = JoinFsPaths(tryGetRamDrivePath(), "activation/alisa");
    pathSpotterYandex = JoinFsPaths(tryGetRamDrivePath(), "activation/yandex");
    pathSpotterAdditional = JoinFsPaths(tryGetRamDrivePath(), "additional");
    pathSpotterNaviOld = JoinFsPaths(tryGetRamDrivePath(), "navi_old");
    pathTemp = JoinFsPaths(tryGetRamDrivePath(), "temp-" + makeUUID());
    soundsDir = JoinFsPaths(tryGetRamDrivePath(), "sounds-" + makeUUID());
    soundsDir.ForceDelete();
    soundsDir.MkDirs();
    createSoundFile(soundsDir, "vins_error.wav");
    createSoundFile(soundsDir, "no_internet.wav");
    createSoundFile(soundsDir, "auth_failed.wav");
    createSoundFile(soundsDir, "brick.wav");
    createSoundFile(soundsDir, "guest_enrollment_failed.mp3");

    // FIXME: This should be moved to initialize(), however some tests set up
    // their own non-empty service mocks before initialize()
    if (initDefaultMocks_) {
        initDefaultMocks();
    }
}

void SKETestBaseFixture::createSoundFile(const TFsPath& dir, const std::string& fileName) {
    TFsPath path = JoinFsPaths(dir, fileName);
    path.Touch();
}

void SKETestBaseFixture::TearDown(NUnitTest::TTestContext& context) {
    if (queue) {
        queue->destroy();
    }

    if (testVoiceDialog != nullptr) {
        testVoiceDialog->stop();
    }

    /* Make sure SpeechkitEndpoint won't try make any requests to other servers */
    endpoint.reset();
    testVins.reset();
    testVoiceDialog.reset();

    /* Clean up after test */
    soundsDir.ForceDelete();
    pathTemp.ForceDelete();
    pathSpotterAlice.ForceDelete();
    pathSpotterYandex.ForceDelete();
    pathSpotterAdditional.ForceDelete();
    pathSpotterNaviOld.ForceDelete();

    SpeechKit::Logger::setInstance(nullptr);

    Base::TearDown(context);
}

void SKETestBaseFixture::initDefaultMocks() {
    ioSDK = std::make_unique<IoHubWrapper>(ipcFactoryForTests());
    mockAudioClientd = createIpcServerForTests("audioclient");
    mockAlarmd = createIpcServerForTests("alarmd");
    mockBrickd = createIpcServerForTests("brickd");
    mockSyncd = createIpcServerForTests("syncd");
    mockNetworkd = createIpcServerForTests("networkd");
    mockNetworkd->setClientConnectedHandler([this](auto& conn) {
        if (networkdClientConnectedHandler) {
            networkdClientConnectedHandler(conn);
        }
    });

    mockMediad = createIpcServerForTests("mediad");
    mockMediad->setMessageHandler([this](const auto& msg, auto& conn) {
        if (mediadMessageHandler) {
            mediadMessageHandler(msg, conn);
        }
    });

    mockInterfaced = createIpcServerForTests("interfaced");
    mockInterfaced->setMessageHandler([this](const auto& msg, auto& conn) {
        if (interfacedMessageHandler) {
            interfacedMessageHandler(msg, conn);
        }
    });

    // Pretend device is not brick
    mockBrickd->setClientConnectedHandler([&](auto& connection) {
        proto::QuasarMessage brickMessage;
        brickMessage.set_brick_status(proto::BrickStatus::NOT_BRICK);
        connection.send(std::move(brickMessage));
        YIO_LOG_INFO("brick message sent from brickd");
    });

    mockAuthProvider = std::make_shared<mock::AuthProvider>();
    mockClockTowerProvider = std::make_shared<mock::ClockTowerProvider>();
    mockDeviceStateProvider = std::make_shared<mock::DeviceStateProvider>();
    mockGlagolClusterProvider = std::make_shared<mock::GlagolClusterProvider>();
    mockMultiroomProvider = std::make_shared<mock::MultiroomProvider>();
    mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
}

void SKETestBaseFixture::initialize(const std::set<std::string>& disabledServices) {
    Json::Value& config = getDeviceForTests()->configuration()->getMutableConfig(testGuard);

    /* Pretend like device is authorized */
    mockAuthProvider->setOwner(
        AuthInfo2{
            .source = AuthInfo2::Source::AUTHD,
            .authToken = "123",
            .passportUid = "123",
            .tag = 1600000001});
    mockDeviceStateProvider->setDeviceState(mock::defaultDeviceState());

    auto initService = [&](auto service, const std::string& name) {
        if (disabledServices.find(name) == disabledServices.end()) {
            service->listenService();
        }
    };

    /* Actual mock servers */
    initService(mockAlarmd, "alarmd");
    initService(mockAudioClientd, "audioclient");
    initService(mockBrickd, "brickd");
    initService(mockSyncd, "syncd");
    initService(mockNetworkd, "networkd");
    initService(mockMediad, "mediad");
    initService(mockInterfaced, "interfaced");

    config["common"]["tempDir"] = pathTemp.GetPath();
    config["aliced"]["app_id"] = "app_id";
    config["aliced"]["app_version"] = "app_version";
    config["aliced"]["spotterModelsPath"] = tryGetRamDrivePath();
    config["aliced"]["customSpotterDir"] = std::string(JoinFsPaths(pathTemp, "data/quasar/data/spotter_model"));
    config["aliced"]["customSpotterConfigPath"] = std::string(JoinFsPaths(pathTemp, "data/quasar/data/spotter.json"));
    config["aliced"]["spotterLoggingTailMillis"] = 500;
    config["aliced"]["spotterLoggingHeadMillis"] = 1500;
    config["aliced"]["putBtNetworksToDeviceState"] = true;
    config["aliced"]["useVoiceInputDelayForAudioOutput"] = true;
    config["aliced"]["aliceAudioDeviceWaitTimeoutMs"] = AUDIO_DEVICE_WAIT_TIMEOUT;
    config["audiod"]["mainChannel"] = "main_channel";
    config["audioPlayer"] = "speechkit";

    config["soundd"]["soundsPath"] = soundsDir.GetPath();

    config["interfaced"]["port"] = 1;

    YIO_LOG_INFO("Config: " << jsonToString(config));

    // create dummy spotter models

    pathSpotterAlice.MkDirs();
    pathSpotterYandex.MkDirs();
    pathSpotterAdditional.MkDirs();
    pathSpotterNaviOld.MkDirs();

    testAudioSourceClient = std::make_shared<MockAudioSourceClient>();
    testAudioSourceClient->subscribeToChannels(YandexIO::RequestChannelType::ALL);
    testAudioPlayer = std::make_shared<TestAudioPlayer>();
    testVins = std::make_shared<TestVins>();

    queue = std::make_shared<NamedCallbackQueue>("test");

    aliceConfig = std::make_shared<AliceConfig>(
        getDeviceForTests(), getDeviceForTests()->configuration()->getServiceConfig(SpeechkitEndpoint::SERVICE_NAME));
    aliceDeviceState = std::make_shared<AliceDeviceState>(
        getDeviceForTests()->deviceId(), nullptr, mockDeviceStateProvider, EnvironmentStateHolder{getDeviceForTests()->deviceId(), getDeviceForTests()->telemetry()});

    mockSdk_ = std::make_shared<testing::NiceMock<MockSdk>>(ipcFactoryForTests(), queue);

    endpoint = std::make_shared<SpeechkitEndpoint>(
        getDeviceForTests(),
        NAlice::TEndpoint::SpeakerEndpointType,
        mockSdk_,
        ipcFactoryForTests(),
        mockAuthProvider,
        mockClockTowerProvider,
        mockDeviceStateProvider,
        mockGlagolClusterProvider,
        mockMultiroomProvider,
        nullptr,
        mockUserConfigProvider,
        testAudioPlayer,
        nullptr,
        testAudioSourceClient,
        VoiceStats::create(),
        std::make_shared<RandomSoundLogger>(queue, getDeviceForTests()->deviceId(), *aliceConfig, *aliceDeviceState),
        SelfDestroyerUtils::createStub(getDeviceForTests()),
        queue,
        std::make_unique<quasar::BackoffRetriesWithRandomPolicy>(getRandomSeed(getDeviceForTests()->deviceId())),
        *aliceConfig,
        aliceDeviceState);

    endpoint->onQuasarMessageReceivedCallback = [this](const proto::QuasarMessage& message) {
        onEndpointMessageReceivedCallback_.onMessage(message);
    };

    testVoiceDialog = std::make_shared<QuasarVoiceDialogTestImpl>(endpoint, testVins);

    mockSdk_ = std::make_shared<testing::NiceMock<MockSdk>>(ipcFactoryForTests(), queue);
    activationSpotterCapability_ = mockSdk_->getActivationSpotterCapabilityProxy();
}

void SKETestBaseFixture::startEndpoint() {
    endpoint->start(testVoiceDialog);

    // wait for capabilities init
    queue->wait();

    waitForSpotterModelsUpdated([this] {
        auto spotterConfigurerQueue = std::make_unique<quasar::NamedCallbackQueue>("SpotterConfigurerTest");
        spotterConfigurerQueue_ = spotterConfigurerQueue.get();
        spotterConfigurer_ = YandexIO::SpotterConfigurer::install(
            std::move(spotterConfigurerQueue), *mockSdk_, getDeviceForTests());
    });
}

void SKETestBaseFixture::sendSync(std::shared_ptr<ipc::IServer> server, const proto::QuasarMessage& message) {
    onEndpointMessageReceivedCallback_.setMessage(message);
    server->sendToAll(proto::QuasarMessage{message});
    onEndpointMessageReceivedCallback_.waitUntilDelivered();
}

void SKETestBaseFixture::setMediadMessageHandler(OnMessageHandler handler) {
    mediadMessageHandler = std::move(handler);
}

void SKETestBaseFixture::setInterfacedMessageHandler(OnMessageHandler handler) {
    interfacedMessageHandler = std::move(handler);
}

void SKETestBaseFixture::setNetworkdClientConnectedHandler(OnClientConnected handler) {
    networkdClientConnectedHandler = std::move(handler);
}

void SKETestBaseFixture::changeSpotterWord(const std::string& word) {
    waitForSpotterModelsUpdated([this, word] {
        spotterConfigurer_->onAccountConfig("spotter", "\"" + word + "\"");
    });
}

void SKETestBaseFixture::waitForSpotterModelsUpdated(std::function<void()> action) {
    auto oldCallback = endpoint->onQuasarMessageReceivedCallback;
    std::promise<void> modelPathReceived;
    std::promise<void> spotterWordReceived;
    endpoint->onQuasarMessageReceivedCallback = [&](const proto::QuasarMessage& msg) {
        if (msg.has_remoting() &&
            msg.remoting().has_spotter_capability_method())
        {
            auto method = msg.remoting().spotter_capability_method().method();
            if (method == quasar::proto::Remoting::SpotterCapabilityMethod::SET_MODEL_PATHS) {
                modelPathReceived.set_value();
            } else if (method == quasar::proto::Remoting::SpotterCapabilityMethod::SET_SPOTTER_WORD) {
                spotterWordReceived.set_value();
            }
        }
    };

    activationSpotterCapability_->resetExpectations();
    action();
    spotterConfigurerQueue_->wait();

    if (activationSpotterCapability_->setModelPathsCalled_) {
        modelPathReceived.get_future().get();
    }
    if (activationSpotterCapability_->setSpotterWordCalled_) {
        spotterWordReceived.get_future().get();
    }

    endpoint->onQuasarMessageReceivedCallback = oldCallback;

    // wait for config to be applied in ske
    queue->wait();
}
