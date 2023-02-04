#pragma once

#include <yandex/maps/mapkit/directions/driving/stopwatch.h>

#include <gmock/gmock.h>

namespace yandex::maps::mapkit::directions::driving::unit_test {

class MockStopwatch: public Stopwatch {
public:
    MOCK_METHOD(Conditions, conditions, (
        RouteRepresentation*,
        const std::vector<size_t>&,
        size_t,
        VehicleType vehicleType), (override));

    MOCK_METHOD(std::chrono::seconds, updateInterval, (), (override));
    MOCK_METHOD(std::chrono::seconds, expirationInterval, (), (override));
};


}
