#include "mock_sdk.h"

#include <yandex_io/capabilities/spotter/spotter_capability_proxy.h>

using namespace quasar::TestUtils;
using namespace YandexIO;
using namespace testing;

MockSdk::MockSdk(std::shared_ptr<ipc::IIpcFactory> ipcFactory, std::shared_ptr<ICallbackQueue> queue) {
    remotingWrapper_ = std::make_unique<YandexIO::RemotingConnectorWrapper>(*ipcFactory, "aliced", queue, "SdkRouterForTests");

    auto deviceStateCapability = std::make_shared<YandexIO::DeviceStateCapabilityProxy>(remotingWrapper_->getRemotingRegistry(), queue);
    deviceStateCapability->init();
    deviceStateCapability_ = std::move(deviceStateCapability);

    activationSpotterCapability_ = std::make_shared<testing::NiceMock<MockSpotterCapabilityProxy>>(
        remotingWrapper_->getRemotingRegistry(), "ActivationSpotterCapability", queue);
    activationSpotterCapability_->init();
    commandSpotterCapability_ = std::make_shared<testing::NiceMock<MockSpotterCapabilityProxy>>(
        remotingWrapper_->getRemotingRegistry(), "CommandSpotterCapability", queue);
    commandSpotterCapability_->init();
    naviOldSpotterCapability_ = std::make_shared<testing::NiceMock<MockSpotterCapabilityProxy>>(
        remotingWrapper_->getRemotingRegistry(), "NaviOldSpotterCapability", queue);
    naviOldSpotterCapability_->init();

    remotingWrapper_->start();

    ON_CALL(*this, getActivationSpotterCapability).WillByDefault(Return(activationSpotterCapability_));
    ON_CALL(*this, getCommandSpotterCapability).WillByDefault(Return(commandSpotterCapability_));
    ON_CALL(*this, getNaviOldSpotterCapability).WillByDefault(Return(naviOldSpotterCapability_));
}

std::shared_ptr<testing::NiceMock<MockSpotterCapabilityProxy>> MockSdk::getActivationSpotterCapabilityProxy() const {
    return activationSpotterCapability_;
}

std::shared_ptr<IDeviceStateCapability> MockSdk::getDeviceStateCapability() const {
    return deviceStateCapability_;
}
