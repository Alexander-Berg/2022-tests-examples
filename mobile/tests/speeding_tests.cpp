#include "../speeding_utils.h"

#include <boost/test/unit_test.hpp>

namespace yandex::maps::navikit::road_events::tests {

BOOST_AUTO_TEST_CASE(DistanceForSpeedLimit)
{
    const struct {
        float cameraSpeedLimit;  // m/s
        float expectedDistance;
    }
    tests[] = {
        { -1.0,     300.0 },
        {0.0,       300.0},

        {11.1,      300.0},
        {11.111111, 300.0},  // 40 km/h
        {11.2,      300.0},

        {11.4,      500.0},  // 41 km/h

        {16.6,      500.0},
        {16.666666, 500.0},  // 60 km/h
        {16.7,      500.0},

        {16.95,    1000.0},  // 61 km/h

        {24.9,     1000.0},
        {25.0,     1000.0},  // 90 km/h
        {25.1,     1000.0},

        {25.3,     1500.0},  // 91 km/h

        {299792458.0, 1500.0},  // speed of light
    };

    for (const auto& test : tests) {
        float distance = distanceForSpeedLimit(test.cameraSpeedLimit);
        BOOST_TEST(distance == test.expectedDistance,
            "distanceForSpeedLimit(" << test.cameraSpeedLimit << ") -> "
            << distance << " != " << test.expectedDistance);
    }
}

}
