#include <maps/infopoint/lib/misc/point_durations.h>
#include <maps/infopoint/tests/common/time_io.h>

#include <library/cpp/testing/gtest/gtest.h>

#include <memory>
#include <chrono>

#include <library/cpp/testing/common/env.h>

using namespace infopoint;

const std::string DURATIONS_CONFIG = ArcadiaSourceRoot() +
    "/maps/infopoint/tests/data/durations.conf";

TEST(point_duration_tests, load_from_file)
{
    using std::chrono::minutes;

    PointDurations durations = PointDurations::fromFile(DURATIONS_CONFIG);

    EXPECT_EQ(
        durations.initialWithDescription(PointType("accident")),
        minutes(30));

    EXPECT_EQ(
        durations.initialWithoutDescription(PointType("accident")),
        minutes(10));

    EXPECT_EQ(
        durations.initialWithoutDescription(PointType(PointTags{"chat"})),
        minutes(5));

    EXPECT_EQ(
        durations.minimalAfterUpvote(PointType("accident")),
        minutes(15));

    EXPECT_EQ(
        durations.minimalAfterUpvote(PointType(PointTags{"police"})),
        minutes(20));

    EXPECT_EQ(
        durations.minimalAfterUpvote(PointType("police")),
        durations.minimalAfterUpvote(
            PointType(PointTags{"police", "speed_control"})));

    EXPECT_EQ(
        durations.minimalAfterUpvote(
            PointType(PointTags{"police", "speed_control"})),
        minutes(25));

    EXPECT_EQ(
        durations.minimalAfterUpvote(
            PointType(PointTags{"police", "lane_control", "speed_control"})),
        minutes(30));

    EXPECT_EQ(
        durations.minimalAfterComment(PointType("accident")),
        minutes(20));

    EXPECT_EQ(
        durations.nearFuture().duration(PointType("accident")),
        minutes(0));
}
