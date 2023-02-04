#include <maps/indoor/libs/validation/include/indoor_track_validator.h>

#include <library/cpp/testing/gmock_in_unittest/gmock.h>
#include <library/cpp/testing/unittest/registar.h>
#include <boost/lexical_cast.hpp>

#include "data.h"

namespace ptracks = yandex::maps::sproto::offline::mrc::indoor;

namespace maps::mirc::validation {
using namespace ::testing;

const geolib3::Polyline2 TEST_POLYLINE_SINGLE_POINT{
    geolib3::PointsVector{
        {0, 0},
    }};

const geolib3::Polyline2 TEST_POLYLINE{
    geolib3::PointsVector{
        {37.348229, 55.691001},
        {37.347958, 55.693655},
        {37.346799, 55.693620},
        {37.347049, 55.690948}
    }};

Y_UNIT_TEST_SUITE(indoor_track_validator_tests) {

Y_UNIT_TEST(indoor_track_test_1)
{
    auto track = boost::lexical_cast<ptracks::IndoorTrack>(testdata::indoor_tracks_1_pb);
    const auto actions = IndoorTrackValidator(track, TEST_POLYLINE_SINGLE_POINT).actions();
    ASSERT_EQ(actions.size(), 1u);
    ASSERT_EQ(actions.front().type, IndoorTrackAction::Type::CHECK_POINT);
}

Y_UNIT_TEST(indoor_track_test_1_valid)
{
    auto track = boost::lexical_cast<ptracks::IndoorTrack>(testdata::indoor_tracks_1_valid_pb);
    auto validator = IndoorTrackValidator(track, TEST_POLYLINE_SINGLE_POINT);
    ASSERT_EQ(validator.actions().size(), 1u);
    ASSERT_TRUE(!validator.getValidationError());
}

Y_UNIT_TEST(indoor_track_test_1_invalid)
{
    auto track = boost::lexical_cast<ptracks::IndoorTrack>(testdata::indoor_tracks_1_invalid_pb);
    auto validator = IndoorTrackValidator(track, TEST_POLYLINE_SINGLE_POINT);
    ASSERT_EQ(validator.actions().size(), 1u);
    ASSERT_TRUE(validator.getValidationError());
}

Y_UNIT_TEST(indoor_track_test_2)
{
    auto track = boost::lexical_cast<ptracks::IndoorTrack>(testdata::indoor_tracks_2_pb);
    const auto actions = IndoorTrackValidator(track, TEST_POLYLINE).actions();
    ASSERT_EQ(actions.size(), 2u);
    ASSERT_EQ(actions[0].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[1].type, IndoorTrackAction::Type::CHECK_POINT);
}

Y_UNIT_TEST(indoor_track_test_3)
{
    auto track = boost::lexical_cast<ptracks::IndoorTrack>(testdata::indoor_tracks_3_pb);
    auto validator = IndoorTrackValidator(track, TEST_POLYLINE);
    const auto actions = validator.actions();
    const auto barriers = validator.barriers();
    ASSERT_EQ(actions.size(), 4u);
    ASSERT_EQ(actions[0].type, IndoorTrackAction::Type::CHECK_POINT_TERMINAL);
    ASSERT_EQ(actions[0].comment, "1");
    ASSERT_EQ(actions[1].type, IndoorTrackAction::Type::CHECK_POINT_UNREACHABLE);
    ASSERT_EQ(actions[1].comment, "2");
    ASSERT_EQ(actions[2].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[2].comment, std::nullopt);
    ASSERT_EQ(actions[3].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[3].comment, std::nullopt);

    ASSERT_EQ(barriers.size(), 2u);
}

Y_UNIT_TEST(indoor_track_test_4_valid)
{
    auto track = boost::lexical_cast<ptracks::IndoorTrack>(testdata::indoor_tracks_4_valid_pb);
    auto validator = IndoorTrackValidator(track, TEST_POLYLINE);
    const auto actions = validator.actions();
    ASSERT_EQ(actions.size(), 4u);
    ASSERT_EQ(actions[0].type, IndoorTrackAction::Type::CHECK_POINT_TERMINAL);
    ASSERT_EQ(actions[0].comment, "1");
    ASSERT_EQ(actions[1].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[1].comment, std::nullopt);
    ASSERT_EQ(actions[2].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[2].comment, std::nullopt);
    ASSERT_EQ(actions[3].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[3].comment, std::nullopt);
    ASSERT_TRUE(!validator.getValidationError());
}

Y_UNIT_TEST(indoor_track_test_4_invalid)
{
    auto track = boost::lexical_cast<ptracks::IndoorTrack>(testdata::indoor_tracks_4_invalid_pb);
    auto validator = IndoorTrackValidator(track, TEST_POLYLINE);
    const auto actions = validator.actions();
    ASSERT_EQ(actions.size(), 4u);
    ASSERT_EQ(actions[0].type, IndoorTrackAction::Type::CHECK_POINT_TERMINAL);
    ASSERT_EQ(actions[0].comment, "1");
    ASSERT_EQ(actions[1].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[1].comment, std::nullopt);
    ASSERT_EQ(actions[2].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[2].comment, std::nullopt);
    ASSERT_EQ(actions[3].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[3].comment, std::nullopt);
    ASSERT_TRUE(validator.getValidationError());
}

Y_UNIT_TEST(indoor_track_test_5)
{
    auto track = boost::lexical_cast<ptracks::IndoorTrack>(testdata::indoor_tracks_5_pb);
    const auto actions = IndoorTrackValidator(track, TEST_POLYLINE).actions();
    ASSERT_EQ(actions.size(), 4u);
    ASSERT_EQ(actions[0].type, IndoorTrackAction::Type::CHECK_POINT_TERMINAL);
    ASSERT_EQ(actions[0].comment, "1");
    ASSERT_EQ(actions[1].type, IndoorTrackAction::Type::CHECK_POINT_UNREACHABLE);
    ASSERT_EQ(actions[1].comment, "2");
    ASSERT_EQ(actions[2].type, IndoorTrackAction::Type::CHECK_POINT_UNREACHABLE);
    ASSERT_EQ(actions[2].comment, "3");
    ASSERT_EQ(actions[3].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[3].comment, std::nullopt);
}

Y_UNIT_TEST(indoor_track_test_6)
{
    auto track = boost::lexical_cast<ptracks::IndoorTrack>(testdata::indoor_tracks_6_pb);
    const auto actions = IndoorTrackValidator(track, TEST_POLYLINE).actions();
    ASSERT_EQ(actions.size(), 5u);
    ASSERT_EQ(actions[0].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[0].comment, std::nullopt);
    ASSERT_EQ(actions[1].type, IndoorTrackAction::Type::CHECK_POINT_TERMINAL);
    ASSERT_EQ(actions[1].comment, "1");
    ASSERT_EQ(actions[2].type, IndoorTrackAction::Type::CHECK_POINT_UNREACHABLE);
    ASSERT_EQ(actions[2].comment, "2");
    ASSERT_EQ(actions[3].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[3].comment, std::nullopt);
    ASSERT_EQ(actions[4].type, IndoorTrackAction::Type::CHECK_POINT);
    ASSERT_EQ(actions[4].comment, std::nullopt);
}

}

} // namespace maps::mirc::validation
