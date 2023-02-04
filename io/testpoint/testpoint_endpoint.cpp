#include "testpoint_endpoint.h"

#include <yandex_io/protos/quasar_proto_forward.h>

#include <yandex_io/libs/configuration/configuration.h>
#include <yandex_io/libs/logging/logging.h>

namespace quasar {
    const std::string TestpointEndpoint::SERVICE_NAME = "testpoint";

    TestpointEndpoint::TestpointEndpoint(std::shared_ptr<YandexIO::IDevice> device,
                                         std::shared_ptr<YandexIO::SDKInterface> sdk,
                                         std::shared_ptr<ipc::IIpcFactory> ipcFactory,
                                         std::shared_ptr<VolumeManager> volumeManager,
                                         std::shared_ptr<YandexIO::BluetoothEmulator> bluetoothEmulator)
        : device_(device)
        , volumeManager_(volumeManager)
        , server_(ipcFactory->createIpcServer(SERVICE_NAME))
        , sdk_(std::move(sdk))
        , bluetoothEmulator_(std::move(bluetoothEmulator))
    {
        auto serviceConfig = device->configuration()->getServiceConfig(SERVICE_NAME);
        if (!serviceConfig.isNull()) {
            server_->setMessageHandler([this](const auto& message, auto& /*connection*/) {
                processQuasarMessage(message);
            });
            server_->setClientConnectedHandler([this](auto& connection) {
                const int platformVolume = volumeManager_->manualGetCurrentVolume();
                const int aliceVolume = volumeManager_->scaleToAlice(platformVolume);
                connection.send(prepareVolumeState(platformVolume, aliceVolume, volumeManager_->manualGetIsMuted()));
            });
            server_->listenService();
        }
    }

    void TestpointEndpoint::processQuasarMessage(const ipc::SharedMessage& message) {
        if (message->has_testpoint_message()) {
            const auto& testpointMsg = message->testpoint_message();

            if (testpointMsg.incoming_directive_size() > 0) {
                proto::QuasarMessage copyMessage;
                *copyMessage.mutable_testpoint_message() = testpointMsg;
                server_->sendToAll(std::move(copyMessage));
            }

            if (testpointMsg.has_test_event()) {
                server_->sendToAll(message);
            }

            if (testpointMsg.has_emulated_bluetooth() && bluetoothEmulator_) {
                const auto& command = testpointMsg.emulated_bluetooth();
                if (command.has_connect()) {
                    YIO_LOG_INFO("Connecting to bluetooth emulator via testpoint");
                    bluetoothEmulator_->connect();
                }
                if (command.has_disconnect()) {
                    YIO_LOG_INFO("Disconnecting from bluetooth emulator via testpoint");
                    bluetoothEmulator_->disconnect();
                }
                if (command.has_play()) {
                    YIO_LOG_INFO("Starting to play bluetooth music via testpoint");
                    bluetoothEmulator_->asSinkPlayStart(Bluetooth::BtNetwork{});
                }
                if (command.has_pause()) {
                    YIO_LOG_INFO("Pausong bluetooth music via testpoint");
                    bluetoothEmulator_->asSinkPlayPause(Bluetooth::BtNetwork{});
                }
            }

            if (testpointMsg.has_emulated_button()) {
                const auto& button = testpointMsg.emulated_button();
                if (button.has_activate()) {
                    YIO_LOG_INFO("Conversation toggling requested via testpoint");
                    sdk_->getAliceCapability()->toggleConversation(YandexIO::VinsRequest::createHardwareButtonClickEventSource());
                } else if (button.has_volume_up()) {
                    YIO_LOG_INFO("Volume up requested via testpoint");
                    volumeManager_->manualVolumeUp();
                } else if (button.has_volume_down()) {
                    YIO_LOG_INFO("Volume down requested via testpoint");
                    volumeManager_->manualVolumeDown();
                } else if (button.has_volume_mute()) {
                    YIO_LOG_INFO("Mute requested via testpoint");
                    volumeManager_->manualMute();
                } else if (button.has_volume_unmute()) {
                    YIO_LOG_INFO("Unmute requested via testpoint");
                    volumeManager_->manualUnmute();
                } else if (button.has_volume_toggle_mute()) {
                    YIO_LOG_INFO("Toggle mute requested via testpoint");
                    if (volumeManager_->manualGetIsMuted()) {
                        volumeManager_->manualUnmute();
                    } else {
                        volumeManager_->manualMute();
                    }
                } else if (button.has_start_conversation()) {
                    YIO_LOG_INFO("Conversation starting requested via testpoint");
                    sdk_->getAliceCapability()->startConversation(YandexIO::VinsRequest::createHardwareButtonClickEventSource());
                } else if (button.has_toggle_play_pause()) {
                    YIO_LOG_INFO("Toggling play pause via testpoint");
                    sdk_->getPlaybackControlCapability()->togglePlayPause(true);
                } else if (button.has_next()) {
                    YIO_LOG_INFO("Processing next request via testpoint");
                    sdk_->getPlaybackControlCapability()->next();
                } else if (button.has_prev()) {
                    YIO_LOG_INFO("Processing prev request via testpoint");
                    sdk_->getPlaybackControlCapability()->prev();
                }
            } else if (testpointMsg.has_start_configuration_mode()) {
                YIO_LOG_INFO("Start configuration mode via testpoint");
                sdk_->setSetupMode();
            }

            if (testpointMsg.has_authenticate_request()) {
                YIO_LOG_INFO("Changing account via testpoint");
                sdk_->authenticate(testpointMsg.authenticate_request().xcode());
            }
        } else {
            if (message->has_simple_player_state()) {
                server_->sendToAll(message);
            }
        }
    }

    void TestpointEndpoint::onVolumeChange(int platformVolume, int aliceVolume, bool isMuted, const std::string& source, bool setBtVolume) {
        Y_UNUSED(source);
        Y_UNUSED(setBtVolume);

        sendVolumeState(platformVolume, aliceVolume, isMuted);
    }

    proto::QuasarMessage TestpointEndpoint::prepareVolumeState(int platformVolume, int aliceVolume, bool isMuted) const {
        proto::QuasarMessage message;
        {
            auto platformVolumeState = message.mutable_testpoint_message()->mutable_platform_volume_state();
            platformVolumeState->set_max_volume(volumeManager_->maxVolume());
            platformVolumeState->set_min_volume(volumeManager_->minVolume());
            platformVolumeState->set_cur_volume(platformVolume);
            platformVolumeState->set_muted(isMuted);
        }
        {
            auto aliceVolumeState = message.mutable_testpoint_message()->mutable_alice_volume_state();
            aliceVolumeState->set_cur_volume(aliceVolume);
            aliceVolumeState->set_muted(isMuted);
        }
        return message;
    }

    void TestpointEndpoint::sendVolumeState(int platformVolume, int aliceVolume, bool isMuted) const {
        server_->sendToAll(prepareVolumeState(platformVolume, aliceVolume, isMuted));
    }
} // namespace quasar
