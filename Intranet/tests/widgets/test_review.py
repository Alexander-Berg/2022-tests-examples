# coding: utf-8

from __future__ import unicode_literals

import pytest

from cab.utils import datetimes
from cab.widgets.review import endpoints as review_endpoints


month_ago = datetimes.today_shifted(months=-1)
two_months_ago = datetimes.today_shifted(months=-2)
shifted_isoformat = lambda *a, **kw: datetimes.shifted_date(*a, **kw).isoformat()


REVIEW_LIST_DATA = {
    "reviews": [
        {
            "name": "Поисковый портал сентябрь 2017",
            "id": 109,
            "status": "in_progress",
            "type": "normal",
            "start_date": month_ago.isoformat(),
            "finish_feedback_date": shifted_isoformat(month_ago, days=1),
            "finish_submission_date": shifted_isoformat(month_ago, days=2),
            "finish_calibration_date": shifted_isoformat(month_ago, days=3),
            "finish_approval_date": shifted_isoformat(month_ago, days=4),
            "finish_date": shifted_isoformat(month_ago, days=5),
        },
        {
            "name": "епартамент инфраструктуры март 2017",
            "id": 82,
            "status": "in_progress",
            "type": "normal",
            "start_date": two_months_ago.isoformat(),
            "finish_feedback_date": shifted_isoformat(two_months_ago, days=1),
            "finish_submission_date": shifted_isoformat(two_months_ago, days=2),
            "finish_calibration_date": shifted_isoformat(two_months_ago, days=3),
            "finish_approval_date": shifted_isoformat(two_months_ago, days=4),
            "finish_date": shifted_isoformat(two_months_ago, days=5),
        },
    ]
}

REVIEW_STATS = {
    109: {
        "all": 10,
        "waiting_for_evaluation_total": 1,
        "waiting_for_approve_total": 3,
        "waiting_for_announce_total": 4,
        "discuss": 3
    }
}


@pytest.mark.skip
def test_get_reviews_data(mocker):
    mocker.patch(
        'cab.widgets.review.endpoints.review.get_review_list',
        lambda *a, **kw: REVIEW_LIST_DATA
    )
    mocker.patch(
        'cab.widgets.review.endpoints.review.get_review_stats',
        lambda auth, id: REVIEW_STATS[id]
    )
    review_data = review_endpoints.get_reviews_data(auth=None)

    assert len(review_data) == 1
    only_review = review_data[0]

    assert only_review['id'] == 109
