#pragma once

#include <yandex_io/libs/hal/hal.h>

#include <functional>

class TestHAL: public YandexIO::HAL {
    using IotDiscoveryProviderFactory = std::function<std::shared_ptr<YandexIO::IIotDiscoveryProvider>()>;

    std::optional<YandexIO::HALInfo> getHALInfo() override;

    std::shared_ptr<BluetoothLE> createBluetoothLE() override;

    std::unique_ptr<YandexIO::DeviceCryptography> createDeviceCryptography(const Json::Value& config) override;

    quasar::BluetoothCapabilities& getBluetoothCapabilities() override;

    std::shared_ptr<YandexIO::IIotDiscoveryProvider> createIotDiscoveryProvider() override;
    void setIotDiscoveryProviderFactory(IotDiscoveryProviderFactory factory) {
        iotDiscoveryProviderFactory_ = std::move(factory);
    }

private:
    IotDiscoveryProviderFactory iotDiscoveryProviderFactory_;
};
