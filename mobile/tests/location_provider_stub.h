#pragma once
#include <yandex/maps/navikit/location/location_provider.h>
#include <yandex/maps/runtime/subscription/subscription.h>

namespace yandex::maps::navikit::location::test{

class LocationProviderStub: public LocationProvider{
public:
    boost::optional<mapkit::directions::guidance::ClassifiedLocation> location() const override
    {
        return location_;
    }

    void setLocation(boost::optional<mapkit::directions::guidance::ClassifiedLocation> value)
    {
        location_ = std::move(value);
    }

    void addListener(const std::shared_ptr<LocationProviderListener>& handler) override
    {
        subscription_.subscribe(handler);
    }

    void removeListener(const std::shared_ptr<LocationProviderListener>& handler) override
    {
        subscription_.unsubscribe(handler);
    }

    void notify()
    {
        subscription_.notify(&LocationProviderListener::onLocationChanged);
    }

private:
    boost::optional<mapkit::directions::guidance::ClassifiedLocation> location_;
    runtime::subscription::Subscription<LocationProviderListener> subscription_;
};

}
