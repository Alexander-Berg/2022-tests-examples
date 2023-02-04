#pragma once

#include "bluetooth_emulator.h"

#include <yandex_io/modules/volume_manager/base/volume_manager.h>

#include <yandex_io/libs/device/i_device.h>
#include <yandex_io/libs/ipc/i_ipc_factory.h>
#include <yandex_io/protos/quasar_proto.pb.h>
#include <yandex_io/sdk/sdk_interface.h>

#include <string>

namespace quasar {
    class TestpointEndpoint: public IVolumeManagerListener {
    public:
        static const std::string SERVICE_NAME;
        TestpointEndpoint(std::shared_ptr<YandexIO::IDevice> device,
                          std::shared_ptr<YandexIO::SDKInterface> sdk,
                          std::shared_ptr<ipc::IIpcFactory> ipcFactory,
                          std::shared_ptr<VolumeManager> volumeManager,
                          std::shared_ptr<YandexIO::BluetoothEmulator> bluetoothEmulator = nullptr);
        void processQuasarMessage(const ipc::SharedMessage& message);

    private:
        proto::QuasarMessage prepareVolumeState(int platformVolume, int aliceVolume, bool isMuted) const;
        void sendVolumeState(int platformVolume, int aliceVolume, bool isMuted) const;

        void onVolumeChange(int platformVolume, int aliceVolume, bool isMuted, const std::string& source, bool setBtVolume) override;

    private:
        const std::shared_ptr<YandexIO::IDevice> device_;
        const std::shared_ptr<VolumeManager> volumeManager_;
        const std::shared_ptr<ipc::IServer> server_;
        const std::shared_ptr<YandexIO::SDKInterface> sdk_;
        const std::shared_ptr<YandexIO::BluetoothEmulator> bluetoothEmulator_;
    };
} // namespace quasar
