#include "bluetooth_emulator.h"

#include <string>

using namespace YandexIO;

BluetoothEmulator::BluetoothEmulator()
    : Bluetooth("emulator")
{
}

int BluetoothEmulator::setName(const std::string& /*name*/) {
    return 0;
}

int BluetoothEmulator::scanNetworks() {
    return 0;
}
int BluetoothEmulator::stopScanNetworks() {
    return 0;
}

int BluetoothEmulator::disconnectAll(BtRole /*role*/) {
    return 0;
}

int BluetoothEmulator::pairWithSink(const BtNetwork& /*network*/) {
    return 0;
}

int BluetoothEmulator::setVisibility(bool /*isDiscoverable*/, bool /*isConnectable*/) {
    return 0;
}

int BluetoothEmulator::asSinkPlayNext(const BtNetwork& /*network*/) {
    currentTrack_++;
    metaDataCallback();
    return 0;
}

int BluetoothEmulator::asSinkPlayPrev(const BtNetwork& /*network*/) {
    currentTrack_--;
    metaDataCallback();
    return 0;
}

int BluetoothEmulator::asSinkPlayPause(const BtNetwork& /*network*/) {
    stopCallback();
    return 0;
}

int BluetoothEmulator::asSinkPlayStart(const BtNetwork& /*network*/) {
    playCallback();
    metaDataCallback();
    return 0;
}

int BluetoothEmulator::asSinkSetVolumeAbs(int /*volume*/) {
    return 0;
}

int BluetoothEmulator::powerOn() {
    return 0;
}

int BluetoothEmulator::powerOff() {
    return 0;
}

void BluetoothEmulator::freeAudioFocus() {
}

void BluetoothEmulator::takeAudioFocus() {
}

Bluetooth::PowerState BluetoothEmulator::getPowerState() const {
    return Bluetooth::PowerState::ON;
}

bool BluetoothEmulator::isAsSinkPlaying() const {
    return true;
}

void BluetoothEmulator::factoryReset() {
}

void BluetoothEmulator::connect() {
    connectCallback();
}

void BluetoothEmulator::disconnect() {
    disconnectCallback();
}

void BluetoothEmulator::connectCallback() {
    Bluetooth::EventResult res;
    res.network.name = "sample_app";
    res.network.addr = "11.22.33.44";
    Bluetooth::sinkEventCallbackLocked(SinkEvent::CONNECTED, res);
}

void BluetoothEmulator::disconnectCallback() {
    Bluetooth::EventResult res;
    res.network.name = "sample_app";
    res.network.addr = "11.22.33.44";
    Bluetooth::sinkEventCallbackLocked(SinkEvent::DISCONNECTED, res);
}

void BluetoothEmulator::metaDataCallback() {
    Bluetooth::EventResult res;
    res.avrcpEvent = Bluetooth::AVRCP::TRACK_META_INFO;
    res.trackInfo.title = std::to_string(currentTrack_);
    res.trackInfo.currPosMs = 1;
    res.trackInfo.songLenMs = 120;
    Bluetooth::sinkEventCallbackLocked(SinkEvent::AVRCP_IN, res);
}

void BluetoothEmulator::playCallback() {
    Bluetooth::EventResult res;
    res.avrcpEvent = Bluetooth::AVRCP::PLAY_START;
    Bluetooth::sinkEventCallbackLocked(SinkEvent::AVRCP_IN, res);
}

void BluetoothEmulator::stopCallback() {
    Bluetooth::EventResult res;
    res.avrcpEvent = Bluetooth::AVRCP::PLAY_STOP;
    Bluetooth::sinkEventCallbackLocked(SinkEvent::AVRCP_IN, res);
}
