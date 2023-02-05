#pragma once

// FIXME: remove this file in MAPSMOBILE-2009

#include <yandex/maps/navikit/advert/billboard_logger.h>

#include <yandex/maps/mapkit/search/billboard_logger.h>

namespace yandex::maps::navikit::advert {

/**
 * Wrapper, needed for A/B testing.
 */
class BillboardLoggerWrapper : public mapkit::search::BillboardLogger {
public:
    BillboardLoggerWrapper(std::shared_ptr<navikit::advert::BillboardLogger> impl) : impl_(impl)
    {
    }

    virtual void logRouteVia(const std::shared_ptr<mapkit::GeoObject>& geoObject) override
    {
        impl_->logRouteVia(geoObject);
    }

    virtual void logAdvertAction(
        const std::string& type,
        const std::shared_ptr<mapkit::GeoObject>& geoObject) override
    {
        impl_->logAdvertAction(type, geoObject);
    }

    virtual void logBannerShow(const std::shared_ptr<mapkit::GeoObject>& geoObject) override
    {
        impl_->logBannerShow(geoObject);
    }

    virtual void logBannerClick(const std::shared_ptr<mapkit::GeoObject>& geoObject) override
    {
        impl_->logBannerClick(geoObject);
    }

private:
    const std::shared_ptr<navikit::advert::BillboardLogger> impl_;
};

} // namespace yandex::maps::navikit::advert
