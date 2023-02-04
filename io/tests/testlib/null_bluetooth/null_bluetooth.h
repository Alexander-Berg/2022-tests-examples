#pragma once

#include <yandex_io/libs/bluetooth/bluetooth.h>

namespace YandexIO {

    class NullBluetooth: public Bluetooth {
    public:
        explicit NullBluetooth(const std::string& name);

        int setName(const std::string& name) override;

        int scanNetworks() override;

        int stopScanNetworks() override;

        int disconnectAll(BtRole role) override;

        int pairWithSink(const BtNetwork& network) override;

        int setVisibility(bool isDiscoverable, bool isConnectable) override;

        int asSinkPlayNext(const BtNetwork& network) override;

        int asSinkPlayPrev(const BtNetwork& network) override;

        int asSinkPlayPause(const BtNetwork& network) override;

        int asSinkPlayStart(const BtNetwork& network) override;

        int asSinkSetVolumeAbs(int volume) override;

        int powerOn() override;

        int powerOff() override;

        void freeAudioFocus() override;

        void takeAudioFocus() override;

        PowerState getPowerState() const override;

        bool isAsSinkPlaying() const override;

        void factoryReset() override;
    };

} // namespace YandexIO
