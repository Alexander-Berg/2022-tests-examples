#pragma once

#include <infra/pod_agent/libs/porto_client/client.h>

namespace NInfra::NPodAgent {

/*
    Client for tests with retries
    Calls Client_ methods, but does DEFAULT_RETRIES retries
    upon getting SocketError, SocketUnavailable or SocketTimeout errors
    WARNING: This client does not switch LogFrame
*/

const size_t DEFAULT_RETRIES = 6;
const TDuration DEFAULT_SLEEP = TDuration::MilliSeconds(100);

template<class TReturnType>
TExpected<TReturnType, TPortoError> RunWithRetries(
    std::function<TExpected<TReturnType, TPortoError>()>&& functionToCall
    , std::function<TExpected<TReturnType, TPortoError>(const TExpected<TReturnType, TPortoError>&)>&& patchResultAfterRetry
        = [](const TExpected<TReturnType, TPortoError>& result) {
        return result;
    }
) {
    Y_ENSURE(DEFAULT_RETRIES > 0, "number of retries should be at least 1");
    for (size_t iter = 0; iter < DEFAULT_RETRIES - 1; ++iter) {
        auto returnValue = functionToCall();

        if (returnValue.IsError() && (
            returnValue.Error().Code == EPortoError::SocketError
            || returnValue.Error().Code == EPortoError::SocketUnavailable
            || returnValue.Error().Code == EPortoError::SocketTimeout
        )) {
            Sleep(DEFAULT_SLEEP);
        } else {
            return iter == 0 ? returnValue : patchResultAfterRetry(returnValue);
        }
    }

    auto lastResult = functionToCall();
    return DEFAULT_RETRIES == 1 ? lastResult : patchResultAfterRetry(lastResult);
}

class TPortoClientWithRetries: public IPortoClient {
public:
    TPortoClientWithRetries(TPortoClientPtr client)
        : Client_(client)
    {}

    virtual TPortoClientPtr SwitchLogFrame(TLogFramePtr /*logFrame*/) override {
        // WARNING: Client does not switch LogFrame
        return this;
    }

    virtual TExpected<void, TPortoError> Create(const TPortoContainerName& name) override {
        return RunWithRetries<void>([=](){ return Client_->Create(name); });
    }

    virtual TExpected<void, TPortoError> CreateRecursive(const TPortoContainerName& name) override {
        return RunWithRetries<void>([=](){ return Client_->CreateRecursive(name); });
    }

    virtual TExpected<void, TPortoError> Destroy(const TPortoContainerName& name) override {
        return RunWithRetries<void>([=](){ return Client_->Destroy(name); });
    }

    virtual TExpected<int, TPortoError> IsContainerExists(const TPortoContainerName& name) override {
        return RunWithRetries<int>([=](){ return Client_->IsContainerExists(name); });
    }

    virtual TExpected<void, TPortoError> Start(const TPortoContainerName& name) override {
        return RunWithRetries<void>(
            [=](){ return Client_->Start(name); }
            , [](const TExpected<void, TPortoError>& result) {
                // if container started after socket error
                if (!result
                    && result.Error().Code == EPortoError::InvalidState
                    && (result.Error().Message.Contains("in state meta") || result.Error().Message.Contains("in state running"))
                ) {
                    return TExpected<void, TPortoError>::DefaultSuccess();
                }

                return result;
            }
        );
    }

    virtual TExpected<void, TPortoError> Stop(const TPortoContainerName& name, TDuration timeout) override {
        return RunWithRetries<void>([=](){ return Client_->Stop(name, timeout); });
    }

    virtual TExpected<void, TPortoError> Kill(const TPortoContainerName& name, int sig) override {
        return RunWithRetries<void>([=](){ return Client_->Kill(name, sig); });
    }

    virtual TExpected<void, TPortoError> Pause(const TPortoContainerName& name) override {
        return RunWithRetries<void>([=](){ return Client_->Pause(name); });
    }

    virtual TExpected<void, TPortoError> Resume(const TPortoContainerName& name) override {
        return RunWithRetries<void>([=](){ return Client_->Resume(name); });
    }

    virtual TExpected<TString, TPortoError> WaitContainers(const TVector<TPortoContainerName>& containers, TDuration timeout) override {
        return RunWithRetries<TString>([=](){ return Client_->WaitContainers(containers, timeout); });
    }

    virtual TExpected<TVector<TPortoContainerName>, TPortoError> List(const TString& mask) override {
        return RunWithRetries<TVector<TPortoContainerName>>([=](){ return Client_->List(mask); });
    }

    virtual TExpected<TMap<TPortoContainerName, TMap<EPortoContainerProperty, TPortoGetResponse>>, TPortoError> Get(const TVector<TPortoContainerName>& name, const TVector<EPortoContainerProperty>& variable) override {
        return RunWithRetries<TMap<TPortoContainerName, TMap<EPortoContainerProperty, TPortoGetResponse>>>([=](){ return Client_->Get(name, variable); });
    }

    virtual TExpected<TString, TPortoError> GetProperty(const TPortoContainerName& name, EPortoContainerProperty property, int flags) override {
        return RunWithRetries<TString>([=](){ return Client_->GetProperty(name, property, flags); });
    }

    virtual TExpected<void, TPortoError> SetProperty(const TPortoContainerName& name, EPortoContainerProperty property, const TString& value) override {
        return RunWithRetries<void>([=](){ return Client_->SetProperty(name, property, value); });
    }

    virtual TExpected<void, TPortoError> SetProperties(const TPortoContainerName& name, const TMap<EPortoContainerProperty, TString>& properties) override {
        return RunWithRetries<void>([=](){ return Client_->SetProperties(name, properties); });
    }

    virtual TExpected<TString, TPortoError> GetStdout(const TPortoContainerName& name, int offset, int length, int flags) override {
        return RunWithRetries<TString>([=](){ return Client_->GetStdout(name, offset, length, flags); });
    }

    virtual TExpected<TString, TPortoError> GetStderr(const TPortoContainerName& name, int offset, int length, int flags) override {
        return RunWithRetries<TString>([=](){ return Client_->GetStderr(name, offset, length, flags); });
    }

    TExpected<TString, TPortoError> GetStream(const TPortoContainerName& name, const TString& stream, int offset, int length, int flags) override {
        return RunWithRetries<TString>([=](){ return Client_->GetStream(name, stream, offset, length, flags); });
    }

    virtual TExpected <TString, TPortoError> CreateVolume(
        const TString& path
        , const TString& storage
        , const TString& place
        , const TVector<TString>& layers
        , unsigned long long quotaBytes
        , const TString& privateValue
        , const EPortoVolumeBackend backend
        , const TPortoContainerName& containerName
        , const TVector<TPortoVolumeShare>& staticResources
        , bool readOnly
    ) override {
        return RunWithRetries<TString>(
            [=](){ return Client_->CreateVolume(
                    path
                    , storage
                    , place
                    , layers
                    , quotaBytes
                    , privateValue
                    , backend
                    , containerName
                    , staticResources
                    , readOnly
                );
            }
            , [=](const TExpected<TString, TPortoError>& result) {
                // if volume already created after socket error
                if (!result
                    && result.Error().Code == EPortoError::VolumeAlreadyExists
                ) {
                    return TExpected<TString, TPortoError>(path);
                }

                return result;
            }
        );
    }

    virtual TExpected<void, TPortoError> LinkVolume(const TString& path, const TPortoContainerName& container, const TString& target, bool readOnly, bool required) override {
        return RunWithRetries<void>([=](){ return Client_->LinkVolume(path, container, target, readOnly, required); });
    }

    virtual TExpected<void, TPortoError> UnlinkVolume(const TString& path, const TPortoContainerName& container, const TString& target, bool strict) override {
        return RunWithRetries<void>([=](){ return Client_->UnlinkVolume(path, container, target, strict); });
    }

    virtual TExpected<TVector<TString>, TPortoError> ListVolumesPaths(const TString& path, const TPortoContainerName& container) override {
        return RunWithRetries<TVector<TString>>([=](){ return Client_->ListVolumesPaths(path, container); });
    }

    virtual TExpected<TVector<TPortoVolume>, TPortoError> ListVolumes(const TString& path, const TPortoContainerName& container) override {
        return RunWithRetries<TVector<TPortoVolume>>([=](){ return Client_->ListVolumes(path, container); });
    }

    virtual TExpected<int, TPortoError> IsVolumeExists(const TString& path) override {
        return RunWithRetries<int>([=](){ return Client_->IsVolumeExists(path); });
    }

    virtual TExpected<void, TPortoError> ImportLayer(const TString& layer, const TString& tarball, bool merge, const TString& place, const TString& privateValue) override {
        return RunWithRetries<void>([=](){ return Client_->ImportLayer(layer, tarball, merge, place, privateValue); });
    }

    virtual TExpected<void, TPortoError> RemoveLayer(const TString& layer, const TString& place) override {
        return RunWithRetries<void>([=](){ return Client_->RemoveLayer(layer, place); });
    }

    virtual TExpected<TVector<TPortoLayer>, TPortoError> ListLayers(const TString& place, const TString& mask) override {
        return RunWithRetries<TVector<TPortoLayer>>([=](){ return Client_->ListLayers(place, mask); });
    }

    virtual TExpected<TString, TPortoError> GetLayerPrivate(const TString& layer, const TString& place) override {
        return RunWithRetries<TString>([=](){ return Client_->GetLayerPrivate(layer, place); });
    }

    virtual TExpected<void, TPortoError> SetLayerPrivate(const TString& privateValue, const TString& layer, const TString& place) override {
        return RunWithRetries<void>([=](){ return Client_->SetLayerPrivate(privateValue, layer, place); });
    }

    virtual TExpected<TVector<TPortoStorage>, TPortoError> ListStorages(const TString& place, const TString& mask) override {
        return RunWithRetries<TVector<TPortoStorage>>([=](){ return Client_->ListStorages(place, mask); });
    }

    virtual TExpected<TString, TPortoError> GetStoragePrivate(const TString& place, const TString& name) override {
        return RunWithRetries<TString>([=](){ return Client_->GetStoragePrivate(place, name); });
    }

    virtual TExpected<void, TPortoError> RemoveStorage(const TString& name, const TString& place) override {
        return RunWithRetries<void>([=](){ return Client_->RemoveStorage(name, place); });
    }

    virtual TExpected<void, TPortoError> ImportStorage(const TString& name, const TString& archive, const TString& place, const TString& compression, const TString& privateValue) override {
        return RunWithRetries<void>([=](){ return Client_->ImportStorage(name, archive, place, compression, privateValue); });
    }

    virtual TExpected<bool, TPortoError> IsStorageExists(const TString& place, const TString& name) override {
        return RunWithRetries<bool>([=](){ return Client_->IsStorageExists(place, name); });
    }

private:
    TPortoClientPtr Client_;
};

} // namespace NInfra::NPodAgent
