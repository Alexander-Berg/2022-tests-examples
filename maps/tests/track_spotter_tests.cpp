#include <library/cpp/testing/unittest/registar.h>
#include <iostream>
#include <maps/automotive/parking/lib/track_spotter/include/track_spotter.h>

using Point2 = maps::geolib3::Point2;

namespace {

using namespace maps::automotive::parking::track_spotter;

Position pos(TimestampSec ts, double lon, double lat) {
    return Position{ts, Point2{lon, lat}};
}

Path slowPath(TimestampSec pts = 112538) {
    return Path{{pos(pts-5, 1.0, 1.0), pos(pts, 1.0, 1.0)}, pts};
}

Path fastPath(TimestampSec pts = 112538) {
    return Path{{pos(pts-5, 1.0, 1.0), pos(pts, 100.0, 100.0)}, pts};
}

Telemetry tel(IgnitionState is, TimestampMs ts, Version v) {
    TelemetryEvent te;
    te.timestamp_ = ts;
    te.ignitionState_ = is;

    return Telemetry{{te}, v};
}

} // anonymous namespace
namespace maps::automotive::parking::track_spotter {


Y_UNIT_TEST_SUITE(test_parking_lib_track_spotter) {

    Y_UNIT_TEST(unknown_state_when_too_few_points)
    {
        VehicleData vd {
            Path{{pos(112433, 2., 2.)}, 112433},
            tel(IgnitionState::LOCK, 112430000, 1),
            VehicleStatus{CarState::UNKNOWN, Point2{}, 0, 0}
        };
        auto status = calcStatus(vd);
        UNIT_ASSERT_EQUAL(CarState::UNKNOWN, status.state_);
    }

    Y_UNIT_TEST(stopped_when_slow_but_engine_is_working)
    {
        VehicleData vd {
            slowPath(112538),
            tel(IgnitionState::ON, 112400000, 1),
            VehicleStatus{CarState::MOVING, Point2{1.0, 1.0}, 112330, 1}
        };
        auto status = calcStatus(vd);
        UNIT_ASSERT_EQUAL(CarState::STOPPED, status.state_);
        UNIT_ASSERT_EQUAL(112538, status.pathTimestamp_);
    }

    Y_UNIT_TEST(moving_when_fast)
    {
        VehicleData vd {
            fastPath(112538),
            tel(IgnitionState::ON, 112400000, 1),
            VehicleStatus{CarState::STOPPED, Point2{1.0, 1.0}, 112330, 1}
        };
        auto status = calcStatus(vd);
        UNIT_ASSERT_EQUAL(CarState::MOVING, status.state_);
        UNIT_ASSERT_EQUAL(112538, status.pathTimestamp_);
    }

    Y_UNIT_TEST(parked_when_ignition_is_off_or_lock)
    {
        VehicleData vd {
            slowPath(112430),
            tel(IgnitionState::LOCK, 112430000, 1),
            VehicleStatus{CarState::STOPPED, Point2{1.0, 1.0}, 112430, 0}
        };
        auto status = calcStatus(vd);
        UNIT_ASSERT_EQUAL(CarState::PARKED, status.state_);
        UNIT_ASSERT_EQUAL(1, status.telemetryVersion_);
    }

    Y_UNIT_TEST(ignition_signal_is_ignored_when_path_stale)
    {
        VehicleData vd {
            slowPath(112400),
            tel(IgnitionState::LOCK, 112430000, 1),
            VehicleStatus{CarState::STOPPED, Point2{1.0, 1.0}, 112400, 0}
        };
        auto status = calcStatus(vd);
        UNIT_ASSERT_EQUAL(CarState::STOPPED, status.state_);
        UNIT_ASSERT_EQUAL(1, status.telemetryVersion_);
    }

}

} // maps::automotive::parking::track_spotter
