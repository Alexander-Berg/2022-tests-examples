#pragma once

#include <yandex/maps/navi/ads/bitmaps_downloader.h>

#include <yandex/maps/runtime/async/utils/handle_session.h>
#include <yandex/maps/runtime/subscription/subscription.h>

#include <chrono>
#include <set>

using namespace std::chrono_literals;

namespace yandex::maps::navi::ads {

class MockBitmapsDownloader : public BitmapsDownloader {
public:
    MockBitmapsDownloader(const std::set<std::string>& cachedBitmaps)
        : cachedBitmaps_(cachedBitmaps)
    {
    }

    virtual void subscribe(const std::shared_ptr<BitmapsDownloaderListener>& listener) override
    {
        listeners_.subscribe(listener);
    }

    virtual void unsubscribe(const std::shared_ptr<BitmapsDownloaderListener>& listener) override
    {
        listeners_.unsubscribe(listener);
    }

    virtual void requestBitmap(const std::string& id, float /*scale*/) override
    {
        handles_.push_back(runtime::async::utils::makeHandleSession(
            [this] (const auto& id) {
                downloaded_.insert(id);
                listeners_.notify(&BitmapsDownloaderListener::onBitmapDownloaded, id);
            },
            [](auto* /*error*/) { ASSERT(false); },
            [id] {
                runtime::async::sleepFor(2s);
                return id;
            }
        ));
    }

    virtual runtime::PlatformBitmap bitmap(const std::string& /*id*/, float /*scale*/) const override
    {
        ASSERT(false);
    }

    virtual runtime::PlatformBitmap bitmapNow(
        const std::string& /*id*/) const override
    {
        ASSERT(false);
    }

    virtual std::shared_ptr<runtime::image::ImageProvider> imageProvider(
        const std::string& /*id*/,
        float /*scale*/) const override
    {
        ASSERT(false);
    }

    virtual bool cached(const std::string& id, float /*scale*/) const override
    {
        return cachedBitmaps_.count(id) != 0;
    }

    const std::set<std::string>& downloaded() const
    {
        return downloaded_;
    }

private:

    std::set<std::string> downloaded_;
    std::set<std::string> cachedBitmaps_;
    std::list<runtime::async::Handle> handles_;
    runtime::subscription::Subscription<BitmapsDownloaderListener> listeners_;
};

}  // namespace yandex
