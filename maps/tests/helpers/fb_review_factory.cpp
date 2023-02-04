#include <maps/wikimap/mapspro/libs/social/tests/helpers/fb_review_factory.h>

namespace maps::wiki::social::feedback::tests {

TId FbReviewFactory::nextReviewId_ = 1;

Review FbReviewFactory::makeReview(
    TUid createdBy,
    TUid reviewee,
    TId regionId,
    TId regionCommitId,
    ReviewState state)
{
    return Review(nextReviewId_++, state, createdBy, reviewee, regionId, regionCommitId);
}

} // namespace maps::wiki::social::feedback::tests
