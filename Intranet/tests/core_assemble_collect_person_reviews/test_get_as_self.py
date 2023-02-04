# coding: utf-8

from review.core import const
from review.core.logic.assemble import collect_person_reviews
from tests import helpers

EXPECTED_NUM_QUERIES = 1


def _collect_for_self(*args, **kwargs):
    res = collect_person_reviews.collect_for_self(*args, **kwargs)
    return [it for _, it in res]


def test_get_as_self_no_reviews(person):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_self(
            subject=person,
            filters={},
        )
    assert len(person_reviews) == 0


def test_get_as_self_only_review(person_review):
    person_review.status = const.PERSON_REVIEW_STATUS.ANNOUNCED
    person_review.save()
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_self(
            subject=person_review.person,
            filters={},
        )
    helpers.assert_ids_equal(person_reviews, [person_review])


def test_get_as_self_filter_by_review(
    person,
    review_builder,
    person_review_builder,
):
    person_review_one = person_review_builder(
        review=review_builder(name='Review 1'),
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
        person=person,
    )
    person_review_builder(
        review=review_builder(name='Review 2'),
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
        person=person,
    )
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_self(
            subject=person,
            filters={
                const.FILTERS.REVIEWS: [person_review_one.review],
            },
        )
    helpers.assert_ids_equal(person_reviews, [person_review_one])
