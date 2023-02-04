#pragma once

#include <yandex_io/capabilities/device_state/proxy/device_state_capability_proxy.h>
#include <yandex_io/libs/ipc/i_ipc_factory.h>
#include <yandex_io/sdk/private/remoting/remoting_connector_wrapper.h>
#include <yandex_io/services/aliced/tests/testlib/mock_spotter_capability_proxy.h>
#include <yandex_io/tests/testlib/null_sdk/null_sdk_interface.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace quasar::TestUtils {

    class MockSdk: public YandexIO::NullSDKInterface {
    public:
        MockSdk(std::shared_ptr<ipc::IIpcFactory> ipcFactory, std::shared_ptr<ICallbackQueue> queue);

        MOCK_METHOD(std::shared_ptr<YandexIO::ISpotterCapability>, getActivationSpotterCapability, (), (const, override));
        MOCK_METHOD(std::shared_ptr<YandexIO::ISpotterCapability>, getCommandSpotterCapability, (), (const, override));
        MOCK_METHOD(std::shared_ptr<YandexIO::ISpotterCapability>, getNaviOldSpotterCapability, (), (const, override));

        std::shared_ptr<testing::NiceMock<MockSpotterCapabilityProxy>> getActivationSpotterCapabilityProxy() const;

        std::shared_ptr<YandexIO::IDeviceStateCapability> getDeviceStateCapability() const override;

    private:
        std::shared_ptr<YandexIO::RemotingConnectorWrapper> remotingWrapper_;
        std::shared_ptr<YandexIO::IDeviceStateCapability> deviceStateCapability_;

        std::shared_ptr<testing::NiceMock<MockSpotterCapabilityProxy>> activationSpotterCapability_;
        std::shared_ptr<testing::NiceMock<MockSpotterCapabilityProxy>> commandSpotterCapability_;
        std::shared_ptr<testing::NiceMock<MockSpotterCapabilityProxy>> naviOldSpotterCapability_;
    };

} // namespace quasar::TestUtils
