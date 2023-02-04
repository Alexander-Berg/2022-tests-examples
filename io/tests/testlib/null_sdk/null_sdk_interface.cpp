#include "null_sdk_interface.h"

#include <yandex_io/tests/testlib/null_alice_capability/null_alice_capability.h>
#include <yandex_io/tests/testlib/null_file_player_capability/null_file_player_capability.h>
#include <yandex_io/tests/testlib/null_playback_control_capability/null_playback_control_capability.h>
#include <yandex_io/tests/testlib/null_spotter_capability/null_spotter_capability.h>
#include <yandex_io/tests/testlib/null_device_state_capability/null_device_state_capability.h>
#include <yandex_io/tests/testlib/null_endpoint_storage/null_endpoint_storage.h>

using namespace YandexIO;

NullSDKInterface::NullSDKInterface()
    : aliceCapability_(std::make_shared<NullAliceCapability>())
    , filePlayerCapability_(std::make_shared<NullFilePlayerCapability>())
    , playbackControlCapability_(std::make_shared<NullPlaybackControlCapability>())
    , activationSpotterCapability_(std::make_shared<NullSpotterCapability>())
    , commandSpotterCapability_(std::make_shared<NullSpotterCapability>())
    , naviOldSpotterCapability_(std::make_shared<NullSpotterCapability>())
    , deviceStateCapability_(std::make_shared<NullDeviceStateCapability>())
    , endpointStorage_(std::make_shared<NullEndpointStorage>())
{
}

void NullSDKInterface::allowInitConfigurationState() {
}

void NullSDKInterface::addDeviceModeObserver(std::weak_ptr<DeviceModeObserver> /*observer*/) {
}

void NullSDKInterface::addAlarmObserver(std::weak_ptr<AlarmObserver> /*observer*/) {
}

void NullSDKInterface::addMediaObserver(std::weak_ptr<MediaObserver> /*observer*/) {
}

void NullSDKInterface::addUpdateObserver(std::weak_ptr<UpdateObserver> /*observer*/) {
}

void NullSDKInterface::addDirectiveObserver(std::weak_ptr<DirectiveObserver> /*observer*/) {
}

void NullSDKInterface::addBackendConfigObserver(std::weak_ptr<BackendConfigObserver> /*observer*/) {
}

void NullSDKInterface::addAuthObserver(std::weak_ptr<AuthObserver> /*observer*/) {
}

void NullSDKInterface::addSDKStateObserver(std::weak_ptr<SDKStateObserver> /*observer*/) {
}

void NullSDKInterface::addNotificationObserver(std::weak_ptr<NotificationObserver> /*observer*/) {
}

void NullSDKInterface::addSpectrumObserver(std::weak_ptr<SpectrumObserver> /*observer*/) {
}

void NullSDKInterface::addSoundCommandObserver(std::weak_ptr<SoundCommandObserver> /*observer*/) {
}

void NullSDKInterface::addMusicStateObserver(std::weak_ptr<MusicStateObserver> /*observer*/) {
}

void NullSDKInterface::addAudioClientEventObserver(std::weak_ptr<AudioClientEventObserver> /*observer*/) {
}

void NullSDKInterface::addBluetoothMediaObserver(std::weak_ptr<BluetoothMediaObserver> /*observer*/) {
}

void NullSDKInterface::addBrickStatusObserver(std::weak_ptr<BrickStatusObserver> /*observer*/) {
}

void NullSDKInterface::addPushNotificationObserver(std::weak_ptr<PushNotificationObserver> /*observer*/) {
}

void NullSDKInterface::addDeviceGroupStateObserver(std::weak_ptr<DeviceGroupStateObserver> /*observer*/) {
}

void NullSDKInterface::addDiscoveryObserver(std::weak_ptr<DiscoveryObserver> /*observer*/) {
}

void NullSDKInterface::registerDeviceInBackend(std::string /*oauthToken*/, std::string /*uid*/) {
}

RegistrationResult NullSDKInterface::registerDeviceInBackendSync(std::string /*oauthToken*/, std::string /*uid*/) {
    return RegistrationResult();
}

void NullSDKInterface::provideUserAccountInfo(std::string /*oauthToken*/, std::string /*uid*/) {
}

void NullSDKInterface::revokeUserAccountInfo() {
}

void NullSDKInterface::subscribeToDeviceConfig(const std::string& /*configName*/) {
}

void NullSDKInterface::unsubscribeFromDeviceConfig(const std::string& /*configName*/) {
}

void NullSDKInterface::subscribeToSystemConfig(const std::string& /*configName*/) {
}

void NullSDKInterface::unsubscribeFromSystemConfig(const std::string& /*configName*/) {
}

void NullSDKInterface::subscribeToAccountConfig(const std::string& /*configName*/) {
}

void NullSDKInterface::unsubscribeFromAccountConfig(const std::string& /*configName*/) {
}

void NullSDKInterface::setSetupMode() {
}

void NullSDKInterface::stopSetupMode() {
}

void NullSDKInterface::toggleSetupMode() {
}

void NullSDKInterface::reportTvPolicyInfo(const TvPolicyInfo& /*tvPolicyInfo*/) {
}

void NullSDKInterface::setActiveActions(const NAlice::TDeviceState::TActiveActions& /*activeActions*/) {
}

void NullSDKInterface::setActiveActionsSemanticFrames(const std::optional<std::string>& /*payload*/) {
}

void NullSDKInterface::notifyPreparedForNotification() {
}

void NullSDKInterface::setAllowUpdate(bool /*allowUpdateAll*/, bool /*allowCritical*/) {
}

void NullSDKInterface::approveAlarm(const std::string& /*alarmId*/) {
}

void NullSDKInterface::authenticate(const std::string& /*oauthCode*/) {
}

void NullSDKInterface::bluetoothMediaSinkPause() {
}

void NullSDKInterface::bluetoothMediaSinkStart() {
}

void NullSDKInterface::bluetoothSinkConnected(const std::string& /*networkAddr*/, const std::string& /*networkName*/) {
}

void NullSDKInterface::bluetoothSinkDisconnected(const std::string& /*networkAddr*/, const std::string& /*networkName*/) {
}

void NullSDKInterface::bluetoothMediaSinkTrackInfo(const std::string& /*title*/, const std::string& /*artist*/, const std::string& /*album*/, const std::string& /*genre*/, int /*songLenMs*/, int /*currPosMs*/) {
}

void NullSDKInterface::sendNumericMetrics(const std::string& /*key*/, double /*value*/) {
}

void NullSDKInterface::sendCategoricalMetrics(const std::string& /*key*/, const std::string& /*value*/) {
}

void NullSDKInterface::blockVoiceAssistant(const std::string& /*sourceId*/, const std::optional<std::string>& /*errorSound*/) {
}

void NullSDKInterface::unblockVoiceAssistant(const std::string& /*sourceId*/) {
}

void NullSDKInterface::reportScreenActive(bool /*isScreenActive*/) {
}

void NullSDKInterface::reportClockDisplayState(ClockDisplayState /*clockDisplayState*/) {
}

void NullSDKInterface::setLocation(double /*latitude*/, double /*longitude*/, std::optional<double> /*accuracy*/) {
}

void NullSDKInterface::setTimezone(const std::string& /*timezone*/, int32_t /*offsetSec*/) {
}

void NullSDKInterface::setWifiList(const std::vector<WifiInfo>& /*wifiList*/) {
}

void NullSDKInterface::acceptIncomingCall() {
}

void NullSDKInterface::declineIncomingCall() {
}

void NullSDKInterface::declineCurrentCall() {
}

void NullSDKInterface::provideState(const yandex_io::proto::TDeviceStatePart& /*statePart*/) {
}

void NullSDKInterface::provideMediaDeviceIdentifier(const NAlice::TClientInfoProto::TMediaDeviceIdentifier& /*identifier*/) {
}

void NullSDKInterface::setVinsResponsePreprocessorHandler(VinsResponsePreprocessorHandler /*value*/) {
}

void NullSDKInterface::sendVinsResponse(const std::string& /*responseJson*/) {
}

const std::shared_ptr<IEndpointStorage>& NullSDKInterface::getEndpointStorage() const {
    return endpointStorage_;
}

void NullSDKInterface::setEqualizerConfig(const EqualizerInfo& /*equalizerInfo*/) {
}

std::shared_ptr<IAlarmCapability> NullSDKInterface::getAlarmCapability() const {
    return nullptr;
}

std::shared_ptr<IAliceCapability> NullSDKInterface::getAliceCapability() const {
    return aliceCapability_;
}

std::shared_ptr<IFilePlayerCapability> NullSDKInterface::getFilePlayerCapability() const {
    return filePlayerCapability_;
}

std::shared_ptr<IPlaybackControlCapability> NullSDKInterface::getPlaybackControlCapability() const {
    return playbackControlCapability_;
}

std::shared_ptr<ISpotterCapability> NullSDKInterface::getActivationSpotterCapability() const {
    return activationSpotterCapability_;
}

std::shared_ptr<ISpotterCapability> NullSDKInterface::getCommandSpotterCapability() const {
    return commandSpotterCapability_;
}

std::shared_ptr<ISpotterCapability> NullSDKInterface::getNaviOldSpotterCapability() const {
    return naviOldSpotterCapability_;
}

std::shared_ptr<IDeviceStateCapability> NullSDKInterface::getDeviceStateCapability() const {
    return deviceStateCapability_;
}
