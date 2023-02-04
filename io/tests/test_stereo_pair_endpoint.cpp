#include <yandex_io/services/stereo_paird/stereo_pair_endpoint.h>

#include <yandex_io/interfaces/clock_tower/mock/clock_tower_provider.h>
#include <yandex_io/interfaces/glagol/mock/glagol_cluster_provider.h>
#include <yandex_io/interfaces/spectrogram_animation/mock/spectrogram_animation_provider.h>
#include <yandex_io/interfaces/user_config/mock/user_config_provider.h>
#include <yandex_io/interfaces/volume_manager/mock/volume_manager_provider.h>
#include <yandex_io/libs/base/named_callback_queue.h>
#include <yandex_io/libs/base/utils.h>
#include <yandex_io/libs/device/device.h>
#include <yandex_io/libs/ipc/mock/connector.h>
#include <yandex_io/libs/ipc/mock/server.h>
#include <yandex_io/libs/json_utils/json_utils.h>
#include <yandex_io/tests/testlib/test_utils.h>
#include <yandex_io/tests/testlib/unittest_helper/unit_test_fixture.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>

using namespace quasar;
using namespace quasar::TestUtils;
using namespace testing;

namespace {
    struct Fixture: public QuasarUnitTestFixture {
        Fixture() {
            mockClockTowerProvider = std::make_shared<mock::ClockTowerProvider>();
            mockGlagolCluster = std::make_shared<mock::GlagolClusterProvider>();
            mockUserConfigProvider = std::make_shared<mock::UserConfigProvider>(nullptr);
            mockSpectrogramAnimationProvider = std::make_shared<mock::SpectrogramAnimationProvider>();
            mockVolumeManagerProvider = std::make_shared<mock::VolumeManagerProvider>();
            mockServer = std::make_shared<ipc::mock::Server>();
            mockClientConnection = std::make_shared<ipc::mock::ClientConnection>();
            lifecycle = std::make_shared<NamedCallbackQueue>("test");

            userConfigStandalone = UserConfig{.auth = UserConfig::Auth::SUCCESS, .system = parseJson(R"({"stereo_pair":{}})")};
            userConfigLeader = UserConfig{.auth = UserConfig::Auth::SUCCESS, .system = parseJson(R"({"stereo_pair":{"role":"leader","partnerDeviceId":"FOLLOWER","channel":"left"}})")};
            userConfigFollower = UserConfig{.auth = UserConfig::Auth::SUCCESS, .system = parseJson(R"({"stereo_pair":{"role":"follower","partnerDeviceId":"LEADER","channel":"right"}})")};

            mockUserConfigProvider->setUserConfig(userConfigStandalone);
        }

        void setupDefaultExpectCalls()
        {
            EXPECT_CALL(*mockServer, setMessageHandler(_)).WillOnce(Invoke([&](ipc::IServer::MessageHandler arg) {
                mockServerMessageHandler = arg;
            }));

            EXPECT_CALL(*mockServer, setClientConnectedHandler(_)).WillOnce(Invoke([&](ipc::IServer::ClientHandler arg) {
                mockServerClientConnectedHandler = arg;
            }));

            EXPECT_CALL(*mockServer, listenService()).Times(1);
        }

        void setupDefaultNotFollowerExpectCalls() const {
        }

        void setupDefaultFollowerExpectCalls() const {
        }

        std::unique_ptr<StereoPairEndpoint> crteateStereoPairEndpoint()
        {
            auto spe = std::make_unique<StereoPairEndpoint>(
                getDeviceForTests(),
                mockClockTowerProvider,
                mockGlagolCluster,
                mockSpectrogramAnimationProvider,
                mockUserConfigProvider,
                mockVolumeManagerProvider,
                mockServer,
                lifecycle,
                std::make_shared<YandexIO::NullSDKInterface>());
            flushCallbackQueue(lifecycle);
            UNIT_ASSERT(mockServerMessageHandler);
            UNIT_ASSERT(mockServerClientConnectedHandler);
            return spe;
        }

        YandexIO::Configuration::TestGuard testGuard;

        std::shared_ptr<mock::ClockTowerProvider> mockClockTowerProvider;
        std::shared_ptr<mock::GlagolClusterProvider> mockGlagolCluster;
        std::shared_ptr<mock::UserConfigProvider> mockUserConfigProvider;
        std::shared_ptr<mock::SpectrogramAnimationProvider> mockSpectrogramAnimationProvider;
        std::shared_ptr<mock::VolumeManagerProvider> mockVolumeManagerProvider;
        std::shared_ptr<ipc::mock::Server> mockServer;
        std::shared_ptr<ipc::mock::ClientConnection> mockClientConnection;
        std::shared_ptr<NamedCallbackQueue> lifecycle;

        ipc::IServer::MessageHandler mockServerMessageHandler;
        ipc::IServer::ClientHandler mockServerClientConnectedHandler;
        ipc::IServer::ClientHandler mockServerClientDisconnectedHandler;

        UserConfig userConfigStandalone;
        UserConfig userConfigLeader;
        UserConfig userConfigFollower;
    };

} // namespace

Y_UNIT_TEST_SUITE_F(StereoPairEndpoint, Fixture) {
    Y_UNIT_TEST(testCtor)
    {
        setupDefaultExpectCalls();
        setupDefaultNotFollowerExpectCalls();
        auto spe = crteateStereoPairEndpoint();
    }

    Y_UNIT_TEST(testOnClientConnected)
    {
        setupDefaultExpectCalls();
        setupDefaultNotFollowerExpectCalls();
        auto spe = crteateStereoPairEndpoint();

        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_))).WillRepeatedly(Invoke([&](const ipc::SharedMessage& m) {
            UNIT_ASSERT(m->has_stereo_pair_message());
            UNIT_ASSERT(m->stereo_pair_message().has_state());
        }));
        EXPECT_CALL(*mockClientConnection, send(Matcher<const ipc::SharedMessage&>(_))).WillOnce(Invoke([&](const ipc::SharedMessage& m) {
            UNIT_ASSERT(m->has_stereo_pair_message());
            UNIT_ASSERT(m->stereo_pair_message().has_state());
            const auto& s = m->stereo_pair_message().state();
            UNIT_ASSERT_VALUES_EQUAL(s.device_id(), getDeviceForTests()->deviceId());
            UNIT_ASSERT_VALUES_EQUAL(s.platform(), getDeviceForTests()->platform());
            UNIT_ASSERT_VALUES_EQUAL((int)s.role(), (int)proto::StereoPair::STANDALONE);
            UNIT_ASSERT_VALUES_EQUAL((int)s.connectivity(), (int)proto::StereoPair::INAPPLICABLE);
        }));

        mockServerClientConnectedHandler(*mockClientConnection);

        flushCallbackQueue(lifecycle);
    }

    Y_UNIT_TEST(testOnUserConfigChanged)
    {
        setupDefaultExpectCalls();
        setupDefaultNotFollowerExpectCalls();
        /* BEGIN: Ignore State message */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_)));
        /* END: Ignore State message */
        auto spe = crteateStereoPairEndpoint();

        std::atomic<int> localCurrentRole{0};
        std::atomic<int> remoteCurrentRole{0};
        std::atomic<int> localChannel{-1};
        std::atomic<int> remoteChannel{-1};
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_))).WillRepeatedly(Invoke([&](const ipc::SharedMessage& m) {
            if (m->stereo_pair_message().has_state()) {
                localCurrentRole = (int)m->stereo_pair_message().state().role();
                localChannel = (int)m->stereo_pair_message().state().channel();
            }
        }));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _)).WillRepeatedly(Invoke([&](std::vector<std::string> d, std::string s, const quasar::ipc::SharedMessage& m) {
            UNIT_ASSERT_VALUES_EQUAL(d.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(d[0], "FOLLOWER");
            UNIT_ASSERT_VALUES_EQUAL(s, "stereo_pair");
            UNIT_ASSERT(m->has_stereo_pair_message());
            if (m->stereo_pair_message().has_state()) {
                remoteCurrentRole = (int)m->stereo_pair_message().state().role();
                remoteChannel = (int)m->stereo_pair_message().state().channel();
            }
        }));

        mockUserConfigProvider->setUserConfig(userConfigLeader);
        waitUntil([&] { return localCurrentRole.load() == (int)proto::StereoPair::LEADER; });
        waitUntil([&] { return remoteCurrentRole.load() == (int)proto::StereoPair::LEADER; });
        waitUntil([&] { return localChannel.load() == (int)proto::StereoPair::CH_LEFT; });
        waitUntil([&] { return remoteChannel.load() == (int)proto::StereoPair::CH_LEFT; });
        flushCallbackQueue(lifecycle);

        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _)).WillRepeatedly(Invoke([&](std::vector<std::string> d, std::string s, const quasar::ipc::SharedMessage& m) {
            UNIT_ASSERT_VALUES_EQUAL(d.size(), 1);
            UNIT_ASSERT_VALUES_EQUAL(d[0], "LEADER");
            UNIT_ASSERT_VALUES_EQUAL(s, "stereo_pair");
            UNIT_ASSERT(m->has_stereo_pair_message());
            if (m->stereo_pair_message().has_state()) {
                remoteCurrentRole = (int)m->stereo_pair_message().state().role();
                remoteChannel = (int)m->stereo_pair_message().state().channel();
            }
        }));

        mockUserConfigProvider->setUserConfig(userConfigFollower);
        waitUntil([&] { return localCurrentRole.load() == (int)proto::StereoPair::FOLLOWER; });
        waitUntil([&] { return remoteCurrentRole.load() == (int)proto::StereoPair::FOLLOWER; });
        waitUntil([&] { return localChannel.load() == (int)proto::StereoPair::CH_RIGHT; });
        waitUntil([&] { return remoteChannel.load() == (int)proto::StereoPair::CH_RIGHT; });
        flushCallbackQueue(lifecycle);

        EXPECT_CALL(*mockSpectrogramAnimationProvider, setExternalPresets(_, _, _)).WillRepeatedly(Invoke([&](const std::string& configs, const std::string& current, const std::string& extraData) {
            // Switch from FOLLOWER to another (STANDALONE) state
            UNIT_ASSERT_VALUES_EQUAL(configs, "");
            UNIT_ASSERT_VALUES_EQUAL(current, "");
            UNIT_ASSERT_VALUES_EQUAL(extraData, "");
        }));

        mockUserConfigProvider->setUserConfig(userConfigStandalone);
        waitUntil([&] { return localCurrentRole.load() == (int)proto::StereoPair::STANDALONE; });
        waitUntil([&] { return localCurrentRole.load() == (int)proto::StereoPair::CH_ALL; });
        flushCallbackQueue(lifecycle);
    }

    Y_UNIT_TEST(testSpectrogramAnimationState)
    {
        mockUserConfigProvider->setUserConfig(userConfigFollower);
        setupDefaultExpectCalls();
        setupDefaultFollowerExpectCalls();

        /* BEGIN: Ignore State message */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_)));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _)).WillRepeatedly(Invoke([&](std::vector<std::string> /*deviceIds*/, std::string /*serviceName*/, const ipc::SharedMessage& /*msg*/) {}));
        /* END: Ignore State message */
        auto spe = crteateStereoPairEndpoint();

        /*
         * Phase I
         * We check that the StereoPairEndpoint works correctly with the SpectrogramAnimationProvider
         * and correctly handles animation changes on the local device
         */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_))).WillOnce(Invoke([&](const ipc::SharedMessage& m) {
            UNIT_ASSERT(m->has_stereo_pair_message());
            UNIT_ASSERT(m->stereo_pair_message().has_state());
            UNIT_ASSERT(m->stereo_pair_message().state().has_spectrogram());
            UNIT_ASSERT_VALUES_EQUAL((int)m->stereo_pair_message().state().spectrogram().source(), (int)proto::SpectrogramAnimation::State::LOCAL);
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().spectrogram().configs(), "SOME_JSON");
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().spectrogram().current(), "CURRENT_KEY");
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().spectrogram().extra_data(), "EXTRA_DATA");
        }));

        mockSpectrogramAnimationProvider->setSpectrogramAnimationState({
            .source = SpectrogramAnimationState::Source::LOCAL,
            .configs = "SOME_JSON",
            .current = "CURRENT_KEY",
            .extraData = "EXTRA_DATA",
        });
        flushCallbackQueue(lifecycle);

        /*
         * Phase II
         * We check that the StereoPairEndpoint responds to the change in animation on the local device if
         * the partner has sent a new configuration.
         */
        int syncId = 1000000; // BIG VALUE
        EXPECT_CALL(*mockSpectrogramAnimationProvider, setExternalPresets(_, _, _)).WillOnce(Invoke([&](const std::string& configs, const std::string& current, const std::string& extraData) {
            /*
             * End point must change local anumation via SpectrogramAnimationProvider
             */
            UNIT_ASSERT_VALUES_EQUAL(configs, "NEW_SOME_JSON");
            UNIT_ASSERT_VALUES_EQUAL(current, "NEW_CURRENT_KEY");
            UNIT_ASSERT_VALUES_EQUAL(extraData, "NEW_EXTRA_DATA");
        }));
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_))).WillRepeatedly(Invoke([&](const ipc::SharedMessage& m) {
            /*
             * sync_id must be changed, but volume value will not until VolumeManagerProvider report about changes
             */
            UNIT_ASSERT(m->has_stereo_pair_message());
            UNIT_ASSERT(m->stereo_pair_message().has_state());
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().sync_id(), syncId);
        }));

        proto::QuasarMessage message;
        message.mutable_stereo_pair_message()->mutable_state()->set_device_id("LEADER");
        message.mutable_stereo_pair_message()->mutable_state()->set_platform(getDeviceForTests()->platform());
        message.mutable_stereo_pair_message()->mutable_state()->set_role(proto::StereoPair::LEADER);
        message.mutable_stereo_pair_message()->mutable_state()->set_connectivity(proto::StereoPair::ONEWAY);
        message.mutable_stereo_pair_message()->mutable_state()->set_sync_id(syncId);
        message.mutable_stereo_pair_message()->mutable_state()->mutable_spectrogram()->set_source(proto::SpectrogramAnimation::State::LOCAL);
        message.mutable_stereo_pair_message()->mutable_state()->mutable_spectrogram()->set_configs("NEW_SOME_JSON");
        message.mutable_stereo_pair_message()->mutable_state()->mutable_spectrogram()->set_current("NEW_CURRENT_KEY");
        message.mutable_stereo_pair_message()->mutable_state()->mutable_spectrogram()->set_extra_data("NEW_EXTRA_DATA");

        ipc::SharedMessage sharedMessage(message);
        mockServerMessageHandler(sharedMessage, *mockClientConnection);
        flushCallbackQueue(lifecycle);
    }

    Y_UNIT_TEST(testVolumeManagerStateChanged)
    {
        mockUserConfigProvider->setUserConfig(userConfigLeader);
        setupDefaultExpectCalls();
        setupDefaultNotFollowerExpectCalls();

        /* BEGIN: Ignore State message */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_)));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _));
        /* END: Ignore State message */
        auto spe = crteateStereoPairEndpoint();

        /*
         * Phase I
         * We check that the StereoPairEndpoint works correctly with the VolumeManagerProvider
         * and correctly handles volume changes on the local device
         */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_))).WillOnce(Invoke([&](const ipc::SharedMessage& m) {
            UNIT_ASSERT(m->has_stereo_pair_message());
            UNIT_ASSERT(m->stereo_pair_message().has_state());
            UNIT_ASSERT(m->stereo_pair_message().state().has_volume());
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().volume().platform_volume(), 333);
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().volume().alice_volume(), 3);
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().volume().is_muted(), true);
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().volume().source(), "test");
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().volume().set_bt_volume(), true);
        }));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _)).WillRepeatedly(Invoke([&](std::vector<std::string> /*deviceIds*/, std::string /*serviceName*/, const ipc::SharedMessage& /*msg*/) {}));

        mockVolumeManagerProvider->setVolumeManagerState(
            VolumeManagerState{
                .platformVolume = 333,
                .aliceVolume = 3,
                .isMuted = true,
                .source = "test",
                .setBtVolume = true,
            });
        flushCallbackQueue(lifecycle);

        /*
         * Phase II
         * We check that the StereoPairEndpoint responds to the change in volume on the local device if
         * the partner has sent a new value.
         */
        int syncId = 1000000; // BIG VALUE
        EXPECT_CALL(*mockVolumeManagerProvider, setPlatformVolume(_, _, _)).WillOnce(Invoke([&](int platformVolume, bool isMuted, const std::string& source) {
            /*
             * End point must change local volume via VolumeManagerProvider
             */
            UNIT_ASSERT_VALUES_EQUAL(platformVolume, 50);
            UNIT_ASSERT_VALUES_EQUAL(isMuted, false);
            UNIT_ASSERT_VALUES_EQUAL(source, "FOLLOWER"); // SPE mark source by partner device_id
        }));
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_))).WillRepeatedly(Invoke([&](const ipc::SharedMessage& m) {
            /*
             * sync_id must be changed, but volume value will not until VolumeManagerProvider report about changes
             */
            UNIT_ASSERT(m->has_stereo_pair_message());
            UNIT_ASSERT(m->stereo_pair_message().has_state());
            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().sync_id(), syncId);
        }));

        proto::QuasarMessage message;
        message.mutable_stereo_pair_message()->mutable_state()->set_device_id("FOLLOWER");
        message.mutable_stereo_pair_message()->mutable_state()->set_platform(getDeviceForTests()->platform());
        message.mutable_stereo_pair_message()->mutable_state()->set_role(proto::StereoPair::FOLLOWER);
        message.mutable_stereo_pair_message()->mutable_state()->set_connectivity(proto::StereoPair::ONEWAY);
        message.mutable_stereo_pair_message()->mutable_state()->set_sync_id(syncId);
        message.mutable_stereo_pair_message()->mutable_state()->mutable_volume()->set_platform_volume(50);
        message.mutable_stereo_pair_message()->mutable_state()->mutable_volume()->set_alice_volume(5);
        message.mutable_stereo_pair_message()->mutable_state()->mutable_volume()->set_is_muted(false);
        message.mutable_stereo_pair_message()->mutable_state()->mutable_volume()->set_source("BUTTON");
        message.mutable_stereo_pair_message()->mutable_state()->mutable_volume()->set_set_bt_volume(true);

        ipc::SharedMessage sharedMessage(message);
        mockServerMessageHandler(sharedMessage, *mockClientConnection);
        flushCallbackQueue(lifecycle);
    }

    Y_UNIT_TEST(testOnMessageRequestStateLocal)
    {
        mockUserConfigProvider->setUserConfig(userConfigLeader);
        setupDefaultExpectCalls();
        setupDefaultNotFollowerExpectCalls();

        /* BEGIN: Ignore State message */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_)));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _));
        /* END: Ignore State message */
        auto spe = crteateStereoPairEndpoint();

        EXPECT_CALL(*mockClientConnection, send(Matcher<const ipc::SharedMessage&>(_))).WillOnce(Invoke([&](const ipc::SharedMessage& m) {
            UNIT_ASSERT(m->has_stereo_pair_message());
            UNIT_ASSERT(m->stereo_pair_message().has_state());
        }));
        proto::QuasarMessage message;
        message.mutable_stereo_pair_message()->mutable_request_state();

        ipc::SharedMessage sharedMessage(message);
        mockServerMessageHandler(sharedMessage, *mockClientConnection);
        flushCallbackQueue(lifecycle);
    }

    Y_UNIT_TEST(testOnMessageRequestStateGC)
    {
        mockUserConfigProvider->setUserConfig(userConfigLeader);
        setupDefaultExpectCalls();
        setupDefaultNotFollowerExpectCalls();

        /* BEGIN: Ignore State message */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_)));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _));
        /* END: Ignore State message */
        auto spe = crteateStereoPairEndpoint();

        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _))
            .WillOnce(
                Invoke([&](std::vector<std::string> d, std::string s, const quasar::ipc::SharedMessage& m) {
                    UNIT_ASSERT_VALUES_EQUAL(d.size(), 1);
                    UNIT_ASSERT_VALUES_EQUAL(d[0], "BALALAIKA");
                    UNIT_ASSERT_VALUES_EQUAL(s, "stereo_pair");
                    UNIT_ASSERT(m->has_stereo_pair_message());
                    UNIT_ASSERT(m->stereo_pair_message().has_state());
                }));

        proto::QuasarMessage message;
        message.mutable_stereo_pair_message()->mutable_request_state()->set_requester("BALALAIKA");

        ipc::SharedMessage sharedMessage(message);
        mockServerMessageHandler(sharedMessage, *mockClientConnection);
        flushCallbackQueue(lifecycle);
    }

    Y_UNIT_TEST(testConversationSignalsOnLeader)
    {
        mockUserConfigProvider->setUserConfig(userConfigLeader);
        setupDefaultExpectCalls();
        setupDefaultNotFollowerExpectCalls();

        /* BEGIN: Ignore State message */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_)));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _));
        /* END: Ignore State message */
        auto spe = crteateStereoPairEndpoint();

        flushCallbackQueue(lifecycle);

        // NO ANY ACTIONS!! Leader ignore any signals about conversations
    }

    Y_UNIT_TEST(testInitialPairingRequest)
    {
        mockUserConfigProvider->setUserConfig(userConfigStandalone);
        setupDefaultExpectCalls();
        setupDefaultNotFollowerExpectCalls();

        std::atomic<int> stage = 0;
        std::atomic<bool> initialParingOk{false};
        std::string uuid;
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_)))
            .WillRepeatedly(
                Invoke([&](const ipc::SharedMessage& m) {
                    YIO_LOG_DEBUG("mockServer.sendToAll: message=" << m);
                    UNIT_ASSERT(m->has_stereo_pair_message());
                    if (m->stereo_pair_message().has_state()) {
                        if (stage != 2) {
                            UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().state().initial_pairing_time_ms(), 0);
                        } else {
                            if (m->stereo_pair_message().state().initial_pairing_time_ms() > 0) {
                                UNIT_ASSERT_VALUES_EQUAL(stage.load(), 2);
                                initialParingOk = true;
                            }
                        }
                    }
                }));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _))
            .WillRepeatedly(
                Invoke([&](std::vector<std::string> d, std::string s, const ipc::SharedMessage& m) {
                    YIO_LOG_DEBUG("mockGlagolCluster.send: service=" << s << ", message=" << m);
                    UNIT_ASSERT(stage.load() > 0);
                    UNIT_ASSERT_VALUES_EQUAL(d.size(), 1);
                    UNIT_ASSERT_VALUES_EQUAL(d[0], "FOLLOWER");
                    UNIT_ASSERT_VALUES_EQUAL(s, "stereo_pair");
                    UNIT_ASSERT(m->has_stereo_pair_message());

                    if (m->stereo_pair_message().has_initial_pairing_request()) {
                        UNIT_ASSERT_VALUES_EQUAL(stage.load(), 1);
                        const auto& r = m->stereo_pair_message().initial_pairing_request();
                        UNIT_ASSERT_VALUES_EQUAL(r.leader_device_id(), "DEVICE_ID");
                        UNIT_ASSERT_VALUES_EQUAL(r.follower_device_id(), "FOLLOWER");
                        UNIT_ASSERT(!r.uuid().empty());
                        uuid = r.uuid();
                        stage = 2;
                    }
                }));
        auto spe = crteateStereoPairEndpoint();

        stage = 1;
        setupDefaultNotFollowerExpectCalls();
        mockUserConfigProvider->setUserConfig(userConfigLeader);
        {
            auto message = ipc::buildMessage([&](auto& msg) {
                msg.mutable_stereo_pair_message()->mutable_state()->set_device_id("FOLLOWER");
                msg.mutable_stereo_pair_message()->mutable_state()->set_platform(getDeviceForTests()->platform());
                msg.mutable_stereo_pair_message()->mutable_state()->set_role(proto::StereoPair::FOLLOWER);
                msg.mutable_stereo_pair_message()->mutable_state()->set_connectivity(proto::StereoPair::TWOWAY);
                msg.mutable_stereo_pair_message()->mutable_state()->set_stereo_player_status(proto::StereoPair::PLAYER_READY);
                msg.mutable_stereo_pair_message()->mutable_state()->set_sync_id(444);
            });
            mockServerMessageHandler(message, *mockClientConnection);
            flushCallbackQueue(lifecycle);
        }
        waitUntil([&] { return stage == 2; });

        {
            auto message = ipc::buildMessage([&](auto& msg) {
                auto& answer = *msg.mutable_stereo_pair_message()->mutable_initial_pairing_answer();
                answer.set_follower_device_id("FOLLOWER");
                answer.set_uuid(TString(uuid));
            });
            mockServerMessageHandler(message, *mockClientConnection);
        }

        waitUntil([&] { return initialParingOk.load(); });
        flushCallbackQueue(lifecycle);
    }

    Y_UNIT_TEST(testOverrideChannelRequest)
    {
        mockUserConfigProvider->setUserConfig(userConfigFollower);
        setupDefaultExpectCalls();
        setupDefaultFollowerExpectCalls();

        /* BEGIN: Ignore State message */
        std::atomic<int> localCurrentRole{0};
        std::atomic<int> localChannel{-1};
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_))).WillRepeatedly(Invoke([&](const ipc::SharedMessage& m) {
            if (m->stereo_pair_message().has_state()) {
                localCurrentRole = (int)m->stereo_pair_message().state().role();
                localChannel = (int)m->stereo_pair_message().state().channel();
            }
        }));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _)).WillRepeatedly(Invoke([&](std::vector<std::string> /*deviceIds*/, std::string /*serviceName*/, const ipc::SharedMessage& /*msg*/) {}));
        /* END: Ignore State message */

        auto spe = crteateStereoPairEndpoint();
        waitUntil([&] { return localChannel.load() == (int)proto::StereoPair::CH_RIGHT; }); // Initial channel

        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_stereo_pair_message()->mutable_override_channel_request()->set_channel("left");
        });
        mockServerMessageHandler(message, *mockClientConnection);
        flushCallbackQueue(lifecycle);

        waitUntil([&] { return localChannel.load() == (int)proto::StereoPair::CH_LEFT; }); // Effective channel
    }

    Y_UNIT_TEST(testUserEventRequest)
    {
        mockUserConfigProvider->setUserConfig(userConfigFollower);
        setupDefaultExpectCalls();
        setupDefaultFollowerExpectCalls();

        std::atomic<int> req{0};
        /* BEGIN: Ignore State message */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_))).WillRepeatedly(Invoke([&](const ipc::SharedMessage& /*msg*/) {}));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _)).WillRepeatedly(Invoke([&](std::vector<std::string> /*deviceIds*/, std::string /*serviceName*/, const ipc::SharedMessage& m) {
            if (m->stereo_pair_message().has_user_event_signal()) {
                ++req;
                UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().user_event_signal().event_id(), "CUSTOM_USER_EVENT");
            }
        }));
        /* END: Ignore State message */

        auto spe = crteateStereoPairEndpoint();
        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_stereo_pair_message()->mutable_user_event_request()->set_event_id("CUSTOM_USER_EVENT");
        });
        mockServerMessageHandler(message, *mockClientConnection);
        flushCallbackQueue(lifecycle);

        waitUntil([&] { return req > 0; });
    }

    Y_UNIT_TEST(testUserEventSignal)
    {
        mockUserConfigProvider->setUserConfig(userConfigFollower);
        setupDefaultExpectCalls();
        setupDefaultFollowerExpectCalls();

        std::atomic<int> req{0};
        /* BEGIN: Ignore State message */
        EXPECT_CALL(*mockServer, sendToAll(Matcher<const ipc::SharedMessage&>(_))).WillRepeatedly(Invoke([&](const ipc::SharedMessage& m) {
            if (m->stereo_pair_message().has_user_event_signal()) {
                ++req;
                UNIT_ASSERT_VALUES_EQUAL(m->stereo_pair_message().user_event_signal().event_id(), "CUSTOM_USER_EVENT");
            }
        }));
        EXPECT_CALL(*mockGlagolCluster, send(Matcher<std::vector<std::string>>(_), _, _)).WillRepeatedly(Invoke([&](std::vector<std::string> /*deviceIds*/, std::string /*serviceName*/, const ipc::SharedMessage& /*msg*/) {}));
        /* END: Ignore State message */

        auto spe = crteateStereoPairEndpoint();
        auto message = ipc::buildMessage([](auto& msg) {
            msg.mutable_stereo_pair_message()->mutable_user_event_signal()->set_event_id("CUSTOM_USER_EVENT");
        });
        mockServerMessageHandler(message, *mockClientConnection);
        flushCallbackQueue(lifecycle);

        waitUntil([&] { return req > 0; });
    }

} // Y_UNIT_TEST_SUITE(AdbService)
