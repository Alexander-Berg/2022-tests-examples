#pragma once

#include <yandex_io/capabilities/device_state/interfaces/i_device_state_capability.h>

namespace YandexIO {

    class NullDeviceStateCapability: public IDeviceStateCapability {
    public:
        // ICapability
        NAlice::TCapabilityHolder getState() const override;
        std::shared_ptr<IDirectiveHandler> getDirectiveHandler() override;
        void addListener(std::weak_ptr<IListener> wlistener) override;
        void removeListener(std::weak_ptr<IListener> wlistener) override;

        void setBluetoothState(const NAlice::TDeviceState::TBluetooth& state) override;
        void setAudioPlayerState(const NAlice::TDeviceState::TAudioPlayer& state) override;
        void setMusicState(const NAlice::TDeviceState::TMusic& state) override;
        void setRadioState(const google::protobuf::Struct& state) override;
        void setLastWatched(const NAlice::TDeviceState::TLastWatched& state) override;
        void setVideoState(const NAlice::TDeviceState::TVideo& state) override;
        void setRcuState(const NAlice::TDeviceState::TRcuState& state) override;
        void setScreenState(const NAlice::TDeviceState::TScreen& state) override;
        void setMultiroomState(const NAlice::TDeviceState::TMultiroom& state) override;
        void setPackagesState(const NAlice::TDeviceState::TPackagesState& state) override;
        void setIsTvPluggedIn(bool isTvPluggedIn) override;
        void setICalendar(const TString& ical) override;
        void setAlarmState(const NAlice::TDeviceState::TAlarmState& state) override;
        void setTimersState(const NAlice::TDeviceState::TTimers& state) override;
        void setMicsMuted(bool muted) override;
        void setBattery(const NAlice::TDeviceState::TBattery& battery) override;

        void addSupportedFeature(const TString& feature) override;
        void setSupportedFeatures(const std::unordered_set<TString>& features) override;
        void setUnsupportedFeatures(const std::unordered_set<TString>& features) override;
        void setExperiments(const NAlice::TExperimentsProto& experiments) override;
        void setEnrollmentHeaders(const NAlice::TEnrollmentHeaders& enrollmentHeaders) override;
        void setEnvironmentDeviceInfo(const NAlice::TEnvironmentDeviceInfo& deviceInfo) override;
        void setNetworkState(const NAlice::TDeviceStateCapability::TState::TNetworkState& state) override;
        void setVolumeState(bool soundMuted, int soundLevel, std::optional<int> soundMaxLevel) override;
    };

} // namespace YandexIO
