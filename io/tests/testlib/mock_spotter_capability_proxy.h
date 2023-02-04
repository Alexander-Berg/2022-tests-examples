#pragma once

#include <yandex_io/capabilities/spotter/spotter_capability_proxy.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>

namespace quasar::TestUtils {

    class MockSpotterCapabilityProxy: public YandexIO::SpotterCapabilityProxy {
    public:
        MockSpotterCapabilityProxy(std::weak_ptr<YandexIO::IRemotingRegistry> remotingRegistry,
                                   const std::string& remoteObjectId,
                                   std::shared_ptr<quasar::ICallbackQueue> worker);

        MOCK_METHOD(void, setModelPaths, ((const std::map<std::string, std::string>&)), (override));
        MOCK_METHOD(void, setSpotterWord, (const std::string&), (override));

        void resetExpectations();

        bool setModelPathsCalled_{false};
        bool setSpotterWordCalled_{false};
    };

} // namespace quasar::TestUtils
