#include "null_endpoint_storage.h"

#include <yandex_io/sdk/private/endpoint_storage/endpoint.h>

using namespace YandexIO;

namespace {

    class NullEndpoint: public EndpointBase,
                        public std::enable_shared_from_this<NullEndpoint> {
    public:
        NullEndpoint(std::string id, NAlice::TEndpoint::EEndpointType type, NAlice::TEndpoint::TDeviceInfo deviceInfo, std::shared_ptr<IDirectiveHandler> handler)
            : EndpointBase(std::move(id), type, std::move(deviceInfo), std::move(handler))
        {
        }

        std::list<std::shared_ptr<ICapability>> getCapabilities() const override {
            return {};
        }

        void addCapability(const std::shared_ptr<ICapability>& /*capability*/) override {
        }

        void removeCapability(const std::shared_ptr<ICapability>& /*capability*/) override {
        }

    private:
        std::shared_ptr<IEndpoint> sharedFromThis() override {
            return shared_from_this();
        }
    };

} // namespace

NullEndpointStorage::NullEndpointStorage() {
    localEndpoint_ = createEndpoint("local", NAlice::TEndpoint::SpeakerEndpointType, {}, nullptr);
}

void NullEndpointStorage::addEndpoint(const std::shared_ptr<IEndpoint>& endpoint)
{
    Y_UNUSED(endpoint);
}

void NullEndpointStorage::removeEndpoint(const std::shared_ptr<IEndpoint>& endpoint)
{
    Y_UNUSED(endpoint);
}

std::list<std::shared_ptr<IEndpoint>> NullEndpointStorage::getEndpoints() const {
    return endpoints_;
}

void NullEndpointStorage::addListener(std::weak_ptr<IListener> wlistener)
{
    Y_UNUSED(wlistener);
}

void NullEndpointStorage::removeListener(std::weak_ptr<IListener> wlistener)
{
    Y_UNUSED(wlistener);
}

std::shared_ptr<IEndpoint> NullEndpointStorage::createEndpoint(std::string id, NAlice::TEndpoint::EEndpointType type, NAlice::TEndpoint::TDeviceInfo deviceInfo, std::shared_ptr<IDirectiveHandler> directiveHandler) {
    return std::make_shared<NullEndpoint>(std::move(id), type, std::move(deviceInfo), std::move(directiveHandler));
}

std::shared_ptr<IEndpoint> NullEndpointStorage::getLocalEndpoint() const {
    return localEndpoint_;
}
