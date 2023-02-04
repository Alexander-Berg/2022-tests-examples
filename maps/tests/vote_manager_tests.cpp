#include <maps/infopoint/lib/point/vote_manager.h>
#include <maps/infopoint/tests/common/time_io.h>

#include <maps/libs/common/include/exception.h>

#include <library/cpp/testing/common/env.h>
#include <library/cpp/testing/gtest/gtest.h>

#include <time.h>
#include <optional>

using namespace infopoint;

const std::string SUPPLIERS_CONFIG = ArcadiaSourceRoot() + "/maps/infopoint/tests/data/suppliers.conf";

class VoteManagerFixture : public ::testing::Test {
public:
    VoteManagerFixture()
        : pointDurations_(makeDurations())
        , voteManager_(pointDurations_)
    {
        EXPECT_EQ(
            pointDurations_.minimalAfterUpvote(PointType("accident")),
            std::chrono::minutes(20));
    }

    VoteManager& voteManager() { return voteManager_; }

private:
    static PointDurations makeDurations()
    {
        DurationConfig minimalAfterUpvote(std::chrono::seconds(0));
        minimalAfterUpvote.setDurationForTag("accident",
            std::chrono::minutes(20));

        DurationConfig empty(std::chrono::seconds(0));

        return PointDurations(empty, empty, minimalAfterUpvote, empty, empty);
    }

    PointDurations pointDurations_;
    VoteManager voteManager_;
};

TimePoint time(int y, int m, int d, int h, int mi, int s = 0)
{
    std::tm tm;
    tm.tm_year = y;
    tm.tm_mon = m;
    tm.tm_mday = d;
    tm.tm_hour = h;
    tm.tm_min = mi;
    tm.tm_sec = s;
    return std::chrono::system_clock::from_time_t(timegm(&tm));
}

Infopoint makePoint()
{
    auto point = Infopoint(
        generateRandomPointUuid(),
        PointType("accident"),
        {38.5, 50.5});
    point.author = UserURI("foo");
    point.owner = UserURI("bar");
    point.rating = 1.0;
    return point;
}

TEST_F(VoteManagerFixture, apply_positive_vote)
{
    Infopoint point = makePoint();
    point.begin = time(2012, 4, 18, 18, 52);
    point.end = time(2012, 4, 18, 19, 12);
    point.autoEnd = true;
    point.rating = 0.3;

    voteManager().applyVote(
        &point, 0.3, std::nullopt, time(2012, 4, 18, 19, 5));

    EXPECT_EQ(point.end, time(2012, 4, 18, 19, 25));
    EXPECT_NEAR(point.rating, 0.51, 1);
}

TEST_F(VoteManagerFixture, apply_negative_vote)
{
    Infopoint point = makePoint();
    point.begin = time(2012, 4, 19, 10, 40);
    point.end = time(2012, 4, 19, 11, 10);
    point.autoEnd = true;
    point.rating = 0.3;

    voteManager().applyVote(
        &point, -0.3, std::nullopt, time(2012, 4, 19, 10, 50));

    EXPECT_EQ(point.end, time(2012, 4, 19, 10, 55));
    EXPECT_NEAR(point.rating, 0.0, 0.01); // == 0
}

TEST_F(VoteManagerFixture, upvote_then_downvote)
{
    Infopoint point = makePoint();
    point.begin = time(2012, 4, 19, 12, 19);
    point.end = time(2012, 4, 19, 12, 39);
    point.autoEnd = true;
    point.rating = 0.3;

    voteManager().applyVote(
        &point, 0.3, std::nullopt, time(2012, 4, 19, 12, 25));

    voteManager().applyVote(
        &point, -0.3, 0.3, time(2012, 4, 19, 12, 26));

    EXPECT_EQ(point.end, time(2012, 4, 19, 12, 30, 45));
    EXPECT_NEAR(point.rating, 0.0, 0.01); // == 0
}
