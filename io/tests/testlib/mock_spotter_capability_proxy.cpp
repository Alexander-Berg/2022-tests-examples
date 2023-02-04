#include "mock_spotter_capability_proxy.h"

using namespace quasar::TestUtils;
using namespace YandexIO;

MockSpotterCapabilityProxy::MockSpotterCapabilityProxy(std::weak_ptr<IRemotingRegistry> remotingRegistry,
                                                       const std::string& remoteObjectId,
                                                       std::shared_ptr<quasar::ICallbackQueue> worker)
    : SpotterCapabilityProxy(std::move(remotingRegistry), std::move(worker), remoteObjectId)
{
    ON_CALL(*this, setModelPaths).WillByDefault([this](const auto& spotterTypeToModelPath) {
        setModelPathsCalled_ = true;
        SpotterCapabilityProxy::setModelPaths(spotterTypeToModelPath);
    });

    ON_CALL(*this, setSpotterWord).WillByDefault([this](const auto& spotterWord) {
        setSpotterWordCalled_ = true;
        SpotterCapabilityProxy::setSpotterWord(spotterWord);
    });
}

void MockSpotterCapabilityProxy::resetExpectations() {
    setModelPathsCalled_ = setSpotterWordCalled_ = false;
}
