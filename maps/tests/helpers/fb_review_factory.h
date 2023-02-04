#pragma once

#include <maps/wikimap/mapspro/libs/social/include/yandex/maps/wiki/social/feedback/review.h>
#include <maps/libs/json/include/value.h>

namespace maps::wiki::social::feedback::tests {

class FbReviewFactory {
public:
    static Review makeReview(
        TUid createdBy,
        TUid reviewee,
        TId regionId,
        TId regionCommitId,
        ReviewState state = ReviewState::Draft);

private:
    static TId nextReviewId_;
};

} // namespace maps::wiki::social::feedback::tests
