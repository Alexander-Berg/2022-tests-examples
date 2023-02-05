#pragma once

#include <yandex/maps/navi/profiling/histograms.h>

#include <yandex/maps/runtime/async/promise.h>

namespace yandex::maps::navi::profiling {

using namespace runtime;

struct MockHistRequest {
    std::string name;
    runtime::TimeInterval duration;
};

class MockHistManager : public HistManager {
public:
    bool initialized() const { return initialized_; }

    const std::map<std::string, std::string>& variations() const { return variations_; }

    runtime::async::MultiFuture<MockHistRequest> requests() { return requests_.future(); }

    // HistManager
    //
    virtual void init(
        const std::shared_ptr<bindings::StringDictionary<std::string>>& variations) override
    {
        ASSERT(!initialized_);
        initialized_ = true;
        variations_ = *variations;
    }

    virtual void createTimeHistogram(
        const std::string& name, TimeInterval /* minDuration */, TimeInterval /* maxDuration */,
        int /* numberOfBuckets */) override
    {
        ASSERT(!histograms_.count(name));
        histograms_.insert(name);
    }

    virtual void sendTime(const std::string& name, runtime::TimeInterval duration) override
    {
        ASSERT(initialized_);
        ASSERT(histograms_.count(name));
        requests_.yield({name, duration});
    }

private:
    bool initialized_ = false;
    std::map<std::string, std::string> variations_;
    std::set<std::string> histograms_;
    runtime::async::MultiPromise<MockHistRequest> requests_;
};

}  // namespace yandex
