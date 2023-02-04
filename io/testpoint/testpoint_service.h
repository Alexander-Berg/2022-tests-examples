#pragma once
#include "bluetooth_emulator.h"
#include "testpoint_endpoint.h"

#include <yandex_io/modules/volume_manager/base/volume_manager.h>

#include <yandex_io/libs/base/quasar_service.h>
#include <yandex_io/libs/device/i_device.h>
#include <yandex_io/libs/ipc/i_ipc_factory.h>

#include <memory>
#include <string>

namespace quasar {
    class TestpointService: public QuasarService {
    public:
        TestpointService(std::shared_ptr<YandexIO::IDevice> device,
                         std::shared_ptr<YandexIO::SDKInterface> sdk,
                         std::shared_ptr<ipc::IIpcFactory> ipcFactory,
                         std::shared_ptr<VolumeManager> volumeManager,
                         std::shared_ptr<YandexIO::BluetoothEmulator> bluetoothEmulator = nullptr);
        std::string getServiceName() const override;
        void start() override;

    private:
        std::shared_ptr<YandexIO::IDevice> device_;
        std::shared_ptr<ipc::IIpcFactory> ipcFactory_;
        std::shared_ptr<VolumeManager> volumeManager_;
        const std::shared_ptr<YandexIO::SDKInterface> sdk_;
        std::shared_ptr<YandexIO::BluetoothEmulator> bluetoothEmulator_;
        std::shared_ptr<TestpointEndpoint> endpointPtr_;
    };
} // namespace quasar
