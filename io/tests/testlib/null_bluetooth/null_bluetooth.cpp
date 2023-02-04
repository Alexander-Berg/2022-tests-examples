#include "null_bluetooth.h"

using namespace YandexIO;

NullBluetooth::NullBluetooth(const std::string& name)
    : Bluetooth(name)
{
}

int NullBluetooth::setName(const std::string& /*name*/) {
    return 0;
}

int NullBluetooth::scanNetworks() {
    return 0;
}

int NullBluetooth::stopScanNetworks() {
    return 0;
}

int NullBluetooth::disconnectAll(BtRole /*role*/) {
    return 0;
}

int NullBluetooth::pairWithSink(const BtNetwork& /*network*/) {
    return 0;
}

int NullBluetooth::setVisibility(bool /*isDiscoverable*/, bool /*isConnectable*/) {
    return 0;
}

int NullBluetooth::asSinkPlayNext(const BtNetwork& /*network*/) {
    return 0;
}

int NullBluetooth::asSinkPlayPrev(const BtNetwork& /*network*/) {
    return 0;
}

int NullBluetooth::asSinkPlayPause(const BtNetwork& /*network*/) {
    return 0;
}

int NullBluetooth::asSinkPlayStart(const BtNetwork& /*network*/) {
    return 0;
}

int NullBluetooth::asSinkSetVolumeAbs(int /*volume*/) {
    return 0;
}

int NullBluetooth::powerOn() {
    return 0;
}

int NullBluetooth::powerOff() {
    return 0;
}

void NullBluetooth::freeAudioFocus() {
}

void NullBluetooth::takeAudioFocus() {
}

Bluetooth::PowerState NullBluetooth::getPowerState() const {
    return PowerState::OFF;
}

bool NullBluetooth::isAsSinkPlaying() const {
    return false;
}

void NullBluetooth::factoryReset() {
}
