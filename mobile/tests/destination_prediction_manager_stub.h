#pragma once
#include <yandex/maps/navikit/destination_suggest/destination_prediction_manager.h>
#include <yandex/maps/runtime/subscription/subscription.h>

namespace yandex::maps::navikit::destination_suggest::test {

class DestinationPredictionManagerStub : public DestinationPredictionManager {
public:
    void resetTimeout() override {}

    virtual std::vector<Destination> destinations() const override { return destinations_; }

    void setDestinations(std::vector<Destination> destinations)
    {
        destinations_ = std::move(destinations);
    }

    virtual void addListener(const std::shared_ptr<DestinationListener>& listener) override
    {
        subscription_.subscribe(listener);
    }
    virtual void removeListener(const std::shared_ptr<DestinationListener>& listener) override
    {
        subscription_.unsubscribe(listener);
    }

    void notify() { subscription_.notify(&DestinationListener::onDestinationsChanged); }

    virtual void suspend() override { }
    virtual void resume() override { }

private:
    std::vector<Destination> destinations_;
    runtime::subscription::Subscription<DestinationListener> subscription_;
};

}
