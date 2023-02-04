#pragma once

#include <yandex_io/sdk/interfaces/i_endpoint_storage.h>

namespace YandexIO {

    class NullEndpointStorage: public IEndpointStorage {
    public:
        NullEndpointStorage();
        std::shared_ptr<IEndpoint> createEndpoint(std::string id, NAlice::TEndpoint::EEndpointType type, NAlice::TEndpoint::TDeviceInfo deviceInfo, std::shared_ptr<IDirectiveHandler> directiveHandler) override;
        std::shared_ptr<IEndpoint> getLocalEndpoint() const override;

        void addEndpoint(const std::shared_ptr<IEndpoint>& endpoint) override;
        void removeEndpoint(const std::shared_ptr<IEndpoint>& endpoint) override;
        std::list<std::shared_ptr<IEndpoint>> getEndpoints() const override;

        void addListener(std::weak_ptr<IListener> wlistener) override;
        void removeListener(std::weak_ptr<IListener> wlistener) override;

    private:
        std::shared_ptr<IEndpoint> localEndpoint_;
        std::list<std::shared_ptr<IEndpoint>> endpoints_;
    };

} // namespace YandexIO
