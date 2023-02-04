#include <library/cpp/testing/unittest/registar.h>

#include <maps/automotive/parking/lib/track_spotter/include/proto_codec.h>

#include <algorithm>

using Point2 = maps::geolib3::Point2;

namespace {

using namespace maps::automotive::parking::track_spotter;

Position pos(TimestampSec ts, double lon, double lat) {
    return Position{ts, Point2{lon, lat}};
}

Path generatePath(TimestampSec pts = 112538) {
    return Path{{pos(pts-5, 1.0, 2.0), pos(pts, 101.0, 102.0)}, pts};
}

} // anonymous namespace

namespace maps::automotive::parking::track_spotter {

Y_UNIT_TEST_SUITE(test_parking_lib_track_spotter_proto_codec) {

    Y_UNIT_TEST(check_encode_path_to_proto)
    {
        auto path = generatePath();
        auto pathProto = encode(path);
        UNIT_ASSERT_EQUAL(pathProto.points_size(), static_cast<int>(path.path_.size()));
        for (int i = 0; i < pathProto.points_size(); i++) {
            auto position = path.path_[i];
            auto positionProto = pathProto.points(i);
            UNIT_ASSERT_EQUAL(position.position_.x(), positionProto.point().lon());
            UNIT_ASSERT_EQUAL(position.position_.y(), positionProto.point().lat());
            UNIT_ASSERT_EQUAL(position.timestamp_, positionProto.timestamp());

        }
    }

    Y_UNIT_TEST(check_encode_telemetry_event_to_proto)
    {
        TelemetryEvent event;
        event.timestamp_ = 112430;
        event.ignitionState_ = IgnitionState::LOCK;
        auto eventProto = encode(event);
        UNIT_ASSERT_EQUAL(eventProto.event_time(), 112430);
        UNIT_ASSERT(eventProto.has_ignition_event());
        UNIT_ASSERT_EQUAL(eventProto.ignition_event().state(), ::yandex::automotive::proto::datacollect::IgnitionState::IGNITION_LOCK);
    }

}

} // maps::automotive::parking::track_spotter
