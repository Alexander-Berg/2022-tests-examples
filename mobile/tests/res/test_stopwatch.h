#pragma once

#include <yandex/maps/mapkit/directions/driving/stopwatch.h>

class TestStopwatch: public yandex::maps::mapkit::directions::driving::Stopwatch {
public:
    virtual yandex::maps::mapkit::directions::driving::Conditions conditions(
            yandex::maps::mapkit::directions::driving::RouteRepresentation* /*route*/,
            const std::vector<size_t>& /*sectionSizes */,
            size_t /*segment*/,
            yandex::maps::mapkit::directions::driving::VehicleType /*vehicleType*/) override
    {
         return {};
    }
    virtual std::chrono::seconds updateInterval() override { return std::chrono::seconds(300); }
    virtual std::chrono::seconds expirationInterval() override { return std::chrono::seconds(1200); }
};
