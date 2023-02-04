#include "null_device_state_capability.h"

using namespace YandexIO;

NAlice::TCapabilityHolder NullDeviceStateCapability::getState() const {
    return NAlice::TCapabilityHolder();
}

std::shared_ptr<IDirectiveHandler> NullDeviceStateCapability::getDirectiveHandler()
{
    return nullptr;
}

void NullDeviceStateCapability::addListener(std::weak_ptr<IListener> wlistener)
{
    Y_UNUSED(wlistener);
}

void NullDeviceStateCapability::removeListener(std::weak_ptr<IListener> wlistener)
{
    Y_UNUSED(wlistener);
}

void NullDeviceStateCapability::setBluetoothState(const NAlice::TDeviceState::TBluetooth& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setAudioPlayerState(const NAlice::TDeviceState::TAudioPlayer& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setMusicState(const NAlice::TDeviceState::TMusic& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setRadioState(const google::protobuf::Struct& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setLastWatched(const NAlice::TDeviceState::TLastWatched& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::addSupportedFeature(const TString& /*feature*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setSupportedFeatures(const std::unordered_set<TString>& /*features*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setUnsupportedFeatures(const std::unordered_set<TString>& /*features*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setExperiments(const NAlice::TExperimentsProto& /*experiments*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setEnrollmentHeaders(const NAlice::TEnrollmentHeaders& /*enrollmentHeaders*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setVideoState(const NAlice::TDeviceState::TVideo& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setRcuState(const NAlice::TDeviceState::TRcuState& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setScreenState(const NAlice::TDeviceState::TScreen& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setMultiroomState(const NAlice::TDeviceState::TMultiroom& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setPackagesState(const NAlice::TDeviceState::TPackagesState& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setEnvironmentDeviceInfo(const NAlice::TEnvironmentDeviceInfo& /*deviceInfo*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setIsTvPluggedIn(bool /*isTvPluggedIn*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setICalendar(const TString& /*ical*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setAlarmState(const NAlice::TDeviceState::TAlarmState& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setTimersState(const NAlice::TDeviceState::TTimers& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setMicsMuted(bool /*muted*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setNetworkState(const NAlice::TDeviceStateCapability::TState::TNetworkState& /*state*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setVolumeState(bool /*soundMuted*/, int /*soundLevel*/, std::optional<int> /*soundMaxLevel*/) {
    // ¯\_(ツ)_/¯
}

void NullDeviceStateCapability::setBattery(const NAlice::TDeviceState::TBattery& /*battery*/) {
    // ¯\_(ツ)_/¯
}
