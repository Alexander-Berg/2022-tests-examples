#include <library/cpp/testing/unittest/gtest.h>

#include <maps/libs/infopoints_client/include/client.h>

namespace maps {
namespace infopoints_client {
namespace tests {


Y_UNIT_TEST_SUITE(test_infopoints_client)
{

Y_UNIT_TEST(filter_test)
{
    InfopointsFilter filter;
    filter.setFuturePoints(FuturePoints::Current);

    TimePoint minBeginTime =
        std::chrono::time_point_cast<TimePoint::duration>(std::chrono::system_clock::from_time_t(0));
    TimePoint minModTime = minBeginTime;
    TimePoint maxBeginTime = minBeginTime + std::chrono::hours(1);
    TimePoint maxEndTime = maxBeginTime;

    filter
        .setMaxBeginTime(maxBeginTime)
        .setMaxEndTime(maxEndTime)
        .setMinBeginTime(minBeginTime)
        .setMinModTime(minModTime);

    filter.setConfidence(0.3);
    filter.setTypes({Type::Drawbridge, Type::Closed});

    maps::geolib3::BoundingBox bbox(
        maps::geolib3::Point2(33, 55),
        0.02,
        0.01);
    filter.setBoundingBox(bbox);

    filter.setModerationStates({
        InfopointModerationState::Pending,
        InfopointModerationState::Approved,
        InfopointModerationState::Disapproved});

    maps::http::URL url;
    filter.addParams(url);

    std::string expectedParams = "ll=33.000000%2C55.000000&spn=0.020000%2C0.010000"
        "&confidence=0.300000&future_points=current"
        "&min_mod_time=19700101T000000Z&max_end_time=19700101T010000Z"
        "&max_begin_time=19700101T010000Z&min_begin_time=19700101T000000Z"
        "&types=drawbridge%2Cclosed"
        "&moderated=pending%2Capproved%2Cdisapproved";


    EXPECT_EQ(url.params(), expectedParams);
};

} //Y_UNIT_TEST_SUITE(test_infopoints_client)

} //namespace tests
} //namespace infopoints_client
} //namespace maps
