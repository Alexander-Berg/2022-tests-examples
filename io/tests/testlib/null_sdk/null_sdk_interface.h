#pragma once

#include <yandex_io/sdk/sdk_interface.h>
#include <yandex_io/sdk/proto/device_sdk.pb.h>

namespace YandexIO {

    class NullSDKInterface: public YandexIO::SDKInterface {
    public:
        NullSDKInterface();
        ~NullSDKInterface() = default;

        void allowInitConfigurationState() override;

        void addDeviceModeObserver(std::weak_ptr<DeviceModeObserver> observer) override;
        void addAlarmObserver(std::weak_ptr<AlarmObserver> observer) override;
        void addMediaObserver(std::weak_ptr<MediaObserver> observer) override;
        void addUpdateObserver(std::weak_ptr<UpdateObserver> observer) override;
        void addDirectiveObserver(std::weak_ptr<DirectiveObserver> observer) override;
        void addBackendConfigObserver(std::weak_ptr<BackendConfigObserver> observer) override;
        void addAuthObserver(std::weak_ptr<AuthObserver> observer) override;
        void addSDKStateObserver(std::weak_ptr<SDKStateObserver> observer) override;
        void addNotificationObserver(std::weak_ptr<NotificationObserver> observer) override;
        void addSpectrumObserver(std::weak_ptr<SpectrumObserver> observer) override;
        void addSoundCommandObserver(std::weak_ptr<SoundCommandObserver> observer) override;
        void addMusicStateObserver(std::weak_ptr<MusicStateObserver> observer) override;
        void addAudioClientEventObserver(std::weak_ptr<AudioClientEventObserver> observer) override;
        void addBluetoothMediaObserver(std::weak_ptr<BluetoothMediaObserver> observer) override;
        void addPushNotificationObserver(std::weak_ptr<PushNotificationObserver> observer) override;
        void addBrickStatusObserver(std::weak_ptr<BrickStatusObserver> observer) override;
        void addDeviceGroupStateObserver(std::weak_ptr<DeviceGroupStateObserver> observer) override;
        void addDiscoveryObserver(std::weak_ptr<DiscoveryObserver> observer) override;

        void registerDeviceInBackend(std::string oauthToken, std::string uid) override;
        RegistrationResult registerDeviceInBackendSync(std::string oauthToken, std::string uid) override;

        void provideUserAccountInfo(std::string oauthToken, std::string uid) override;

        void revokeUserAccountInfo() override;

        void subscribeToDeviceConfig(const std::string& configName) override;
        void unsubscribeFromDeviceConfig(const std::string& configName) override;

        void subscribeToSystemConfig(const std::string& configName) override;
        void unsubscribeFromSystemConfig(const std::string& configName) override;

        void subscribeToAccountConfig(const std::string& configName) override;
        void unsubscribeFromAccountConfig(const std::string& configName) override;

        void setSetupMode() override;
        void stopSetupMode() override;
        void toggleSetupMode() override;

        void reportTvPolicyInfo(const TvPolicyInfo& tvPolicyInfo) override;
        void setActiveActions(const NAlice::TDeviceState::TActiveActions& activeActions) override;
        void setActiveActionsSemanticFrames(const std::optional<std::string>& payload) override;

        void notifyPreparedForNotification() override;

        void setAllowUpdate(bool allowUpdateAll, bool allowCritical) override;

        void approveAlarm(const std::string& alarmId) override;

        void authenticate(const std::string& oauthCode) override;

        void acceptIncomingCall() override;
        void declineIncomingCall() override;
        void declineCurrentCall() override;

        void bluetoothMediaSinkPause() override;
        void bluetoothMediaSinkStart() override;
        void bluetoothSinkConnected(const std::string& networkAddr, const std::string& networkName) override;
        void bluetoothSinkDisconnected(const std::string& networkAddr, const std::string& networkName) override;
        void bluetoothMediaSinkTrackInfo(const std::string& title, const std::string& artist, const std::string& album, const std::string& genre, int songLenMs, int currPosMs) override;

        void sendNumericMetrics(const std::string& key, double value) override;
        void sendCategoricalMetrics(const std::string& key, const std::string& value) override;

        void blockVoiceAssistant(const std::string& sourceId, const std::optional<std::string>& errorSound) override;
        void unblockVoiceAssistant(const std::string& sourceId) override;

        void reportScreenActive(bool isScreenActive) override;

        void reportClockDisplayState(ClockDisplayState clockDisplayState) override;

        void provideState(const yandex_io::proto::TDeviceStatePart& statePart) override;

        void provideMediaDeviceIdentifier(const NAlice::TClientInfoProto::TMediaDeviceIdentifier& identifier) override;

        void setLocation(double latitude, double longitude, std::optional<double> accuracy) override;
        void setTimezone(const std::string& timezone, int32_t offsetSec) override;
        void setWifiList(const std::vector<WifiInfo>& wifiList) override;

        void setVinsResponsePreprocessorHandler(VinsResponsePreprocessorHandler value) override;
        void sendVinsResponse(const std::string& responseJson) override;

        const std::shared_ptr<IEndpointStorage>& getEndpointStorage() const override;

        void setEqualizerConfig(const EqualizerInfo& equalizerInfo) override;

        std::shared_ptr<IAlarmCapability> getAlarmCapability() const override;
        std::shared_ptr<IAliceCapability> getAliceCapability() const override;
        std::shared_ptr<IFilePlayerCapability> getFilePlayerCapability() const override;
        std::shared_ptr<IPlaybackControlCapability> getPlaybackControlCapability() const override;
        std::shared_ptr<ISpotterCapability> getActivationSpotterCapability() const override;
        std::shared_ptr<ISpotterCapability> getCommandSpotterCapability() const override;
        std::shared_ptr<ISpotterCapability> getNaviOldSpotterCapability() const override;
        std::shared_ptr<IDeviceStateCapability> getDeviceStateCapability() const override;

    private:
        const std::shared_ptr<IAliceCapability> aliceCapability_;
        const std::shared_ptr<IFilePlayerCapability> filePlayerCapability_;
        const std::shared_ptr<IPlaybackControlCapability> playbackControlCapability_;
        const std::shared_ptr<ISpotterCapability> activationSpotterCapability_;
        const std::shared_ptr<ISpotterCapability> commandSpotterCapability_;
        const std::shared_ptr<ISpotterCapability> naviOldSpotterCapability_;
        const std::shared_ptr<IDeviceStateCapability> deviceStateCapability_;
        const std::shared_ptr<IEndpointStorage> endpointStorage_;
    };

} // namespace YandexIO
