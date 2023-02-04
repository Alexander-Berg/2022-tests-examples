#include <library/cpp/testing/unittest/registar.h>
#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/reviews_gateway.h>
#include "maps/wikimap/mapspro/libs/social/tests/medium/helpers.h"
#include <maps/wikimap/mapspro/libs/social/tests/helpers/fb_task_creator.h>

namespace maps::wiki::social::tests {

using namespace feedback;
namespace {

void fillSampleReviewExternData(pqxx::work& txn)
{
    txn.exec(
        "INSERT INTO social.feedback_review (created_by, reviewee, region_id, region_commit_id)"
        " VALUES(1, 2, 1, 1) RETURNING region_id");
}

const TId REGION_ID = 1;
const TId REGION_COMMIT_ID = 1;
const TUid AUTHOR = 1;
const TUid REVIEWEE = 2;

const geolib3::Polygon2 REGION_POLY {
        geolib3::PointsVector{
            {0, 0},
            {0, 1},
            {1, 1},
            {1, 0},
            {0, 0}
        }};

void createReview(ReviewsGateway& gw)
{
    gw.createReview(
            REGION_ID,
            REGION_COMMIT_ID,
            REVIEWEE,
            AUTHOR,
            REGION_POLY,
            ReviewsGateway::CreationPolicy::NewTasks);
}
} // namespace

Y_UNIT_TEST_SUITE(feedback_review) {

Y_UNIT_TEST_F(create, DbFixture)
{
    pqxx::work txn(conn);
    ReviewsGateway gw(txn);
    //empty region
    UNIT_ASSERT_EXCEPTION(createReview(gw), NothingToReview);
    feedback::tests::FbTaskCreator fbCreator(txn, TaskState::Opened);
    //out of region
    fbCreator.source("partner-pedestrian-onfoot").position({-0.5, -0.5}).create();
    UNIT_ASSERT_EXCEPTION(
        createReview(gw),
        NothingToReview);
    //success
    fbCreator.source("partner-pedestrian-onfoot").position({0.5, 0.5}).create();
    UNIT_ASSERT_NO_EXCEPTION(createReview(gw));
    //in progress
    fbCreator.source("partner-pedestrian-onfoot").position({0.5, 0.5}).create();
    UNIT_ASSERT_EXCEPTION(createReview(gw), AnotherReviewInProgress);
}

Y_UNIT_TEST_F(readAndSave, DbFixture)
{
    pqxx::work txn(conn);
    fillSampleReviewExternData(txn);
    ReviewsGateway gw(txn);
    const auto reviews = gw.getRecentRegionReviews(1, 0);
    UNIT_ASSERT_VALUES_EQUAL(reviews.size(), 1);
    const auto id = reviews[0].id();
    {
        auto review = gw.getReview(id);
        UNIT_ASSERT_VALUES_EQUAL(review.createdBy(), 1);
        UNIT_ASSERT_VALUES_EQUAL(review.reviewee(), 2);
        UNIT_ASSERT_VALUES_EQUAL(review.regionId(), 1);
        UNIT_ASSERT_VALUES_EQUAL(review.regionCommitId(), 1);
        UNIT_ASSERT_VALUES_EQUAL(review.state(), ReviewState::Draft);
        UNIT_ASSERT(!review.publishedAt());

        review.setComment("comm");
        UNIT_ASSERT_EXCEPTION(review.setTaskComment(1, "task_comm", std::nullopt),
            InvalidReviewOperation);
        review.setTaskComment(1, "task_comm", ReviewTaskComment::Topic::BadPhoto);
        review.setPublished("url");
        gw.saveReview(review);
        review = gw.saveReview(review);
        UNIT_ASSERT(review.publishedAt());
    }
    {
        auto review = gw.getReview(id);
        UNIT_ASSERT_VALUES_EQUAL(review.state(), ReviewState::Published);
        UNIT_ASSERT(review.publishedAt());
        UNIT_ASSERT_VALUES_EQUAL(review.reportData(), "url");
        UNIT_ASSERT_VALUES_EQUAL(review.comment(), "comm");
        UNIT_ASSERT_VALUES_EQUAL(review.tasksComments().size(), 1);
    }
}

Y_UNIT_TEST_F(deleteReview, DbFixture)
{
    pqxx::work txn(conn);
    fillSampleReviewExternData(txn);
    ReviewsGateway gw(txn);
    const auto reviews = gw.getRecentRegionReviews(1, 0);
    UNIT_ASSERT_VALUES_EQUAL(reviews.size(), 1);
    const auto id = reviews[0].id();
    gw.deleteReview(id);
    UNIT_ASSERT(gw.getRecentRegionReviews(1, 0).empty());
    UNIT_ASSERT_EXCEPTION(gw.getReview(id), ReviewDoesntExist);
}

}

} // namespace maps::wiki::social::feedback::tests
