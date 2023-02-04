#include "test_hal.h"

#include <yandex_io/libs/bluetooth/bluetooth_capabilities_impl.h>
#include <yandex_io/libs/device_cryptography/plain_file/plain_file_device_cryptography.h>
#include <yandex_io/libs/json_utils/json_utils.h>

using namespace quasar;

std::optional<YandexIO::HALInfo> TestHAL::getHALInfo() {
    return std::nullopt;
}

std::shared_ptr<BluetoothLE> TestHAL::createBluetoothLE() {
    return nullptr;
}

BluetoothCapabilities& TestHAL::getBluetoothCapabilities() {
    static BluetoothCapabilitiesImpl singleton(/*streamIn*/ false, /*streamOut*/ false, /*ble*/ false);
    return singleton;
}

std::unique_ptr<YandexIO::DeviceCryptography> TestHAL::createDeviceCryptography(const Json::Value& config) {
    return std::make_unique<YandexIO::PlainFileDeviceCryptography>(Cryptography::KeyPair::fromFiles(
        getString(config, "devicePublicKeyPath"),
        getString(config, "devicePrivateKeyPath")));
}

std::shared_ptr<YandexIO::IIotDiscoveryProvider> TestHAL::createIotDiscoveryProvider() {
    if (iotDiscoveryProviderFactory_) {
        return iotDiscoveryProviderFactory_();
    } else {
        return nullptr;
    }
}
