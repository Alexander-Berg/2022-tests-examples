#include "testpoint_service.h"

namespace quasar {
    TestpointService::TestpointService(std::shared_ptr<YandexIO::IDevice> device,
                                       std::shared_ptr<YandexIO::SDKInterface> sdk,
                                       std::shared_ptr<ipc::IIpcFactory> ipcFactory,
                                       std::shared_ptr<VolumeManager> volumeManager,
                                       std::shared_ptr<YandexIO::BluetoothEmulator> bluetoothEmulator)
        : device_(std::move(device))
        , ipcFactory_(std::move(ipcFactory))
        , volumeManager_(std::move(volumeManager))
        , sdk_(std::move(sdk))
        , bluetoothEmulator_(std::move(bluetoothEmulator))
    {
    }

    std::string TestpointService::getServiceName() const {
        return TestpointEndpoint::SERVICE_NAME;
    }

    void TestpointService::start() {
        endpointPtr_ = std::make_shared<TestpointEndpoint>(device_, sdk_, ipcFactory_, volumeManager_, bluetoothEmulator_);
        volumeManager_->addListener(endpointPtr_);
    }
} // namespace quasar
