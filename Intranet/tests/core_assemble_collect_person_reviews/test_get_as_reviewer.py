# coding: utf-8
from collections import defaultdict

from review.core import const
from review.core import models
from review.core.logic.assemble import collect_person_reviews
from review.lib import datetimes
from tests import helpers

EXPECTED_NUM_QUERIES = 1
REVIEWER = const.ROLE.PERSON_REVIEW.REVIEWER
REVIEWER_INHERITED = const.ROLE.PERSON_REVIEW.REVIEWER_INHERITED


def _get_person_reviews(*args, **kwargs):
    collected = collect_person_reviews.collect_for_person_review_roles(*args, **kwargs)
    res = defaultdict(list)
    for role, data in collected:
        res[role].append(data)
    return res


def test_only_linked_allowed(
    person_review_role_reviewer,
    person_review
):
    reviewer = person_review_role_reviewer.person
    person_review = person_review_role_reviewer.person_review

    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _get_person_reviews(subject=reviewer)[REVIEWER]

    assert models.PersonReview.objects.count() == 2
    helpers.assert_ids_equal(
        person_reviews,
        [person_review],
    )


def test_historical_allowed(
    person_review_role_reviewer,
    review_builder,
    person_review_builder,
):
    reviewer = person_review_role_reviewer.person
    person_review = person_review_role_reviewer.person_review
    review = person_review.review
    previous_review = review_builder(
        start_date=datetimes.shifted(review.start_date, months=-6),
        finish_date=datetimes.shifted(review.start_date, months=-6),
    )
    previous_person_review = person_review_builder(
        person=person_review.person,
        review=previous_review,
    )
    from review.core.logic import roles
    roles.denormalize_person_review_roles()

    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _get_person_reviews(subject=reviewer)
    helpers.assert_ids_equal(
        person_reviews[REVIEWER],
        [person_review],
    )
    helpers.assert_ids_equal(
        person_reviews[REVIEWER_INHERITED],
        [previous_person_review],
    )
