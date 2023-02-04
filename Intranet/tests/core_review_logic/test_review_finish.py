from review.core.logic import review_actions
from review.core.const import REVIEW_STATUS
from review.core.models import Review
from review.lib import datetimes


def test_finish_reviews(review_builder):
    today = datetimes.now().date()
    for date in (
        datetimes.shifted(today, months=-1),
        today,
        datetimes.shifted(today, months=1),
    ):
        review_builder(finish_date=date, status=REVIEW_STATUS.IN_PROGRESS)

    review_actions.finish_reviews()

    review_before, review_today, review_after = Review.objects.all().order_by('finish_date')
    assert review_before.status == REVIEW_STATUS.FINISHED
    assert review_today.status == REVIEW_STATUS.IN_PROGRESS
    assert review_after.status == REVIEW_STATUS.IN_PROGRESS
