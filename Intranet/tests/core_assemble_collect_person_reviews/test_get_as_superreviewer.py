# coding: utf-8

from collections import defaultdict

from review.core import const
from review.core import models
from review.core.logic.assemble import collect_person_reviews
from tests import helpers

EXPECTED_NUM_QUERIES = 1


def _collect_for_review_roles(*args, **kwargs):
    collected = collect_person_reviews.collect_for_review_roles(*args, **kwargs)
    res = defaultdict(list)
    for role, data in collected:
        res[role].append(data)
    return res


def test_get_as_superreviewer_no_reviews_no_role(person):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_review_roles(
            subject=person,
            filters={},
            role_types=[const.ROLE.REVIEW.SUPERREVIEWER],
        ).get(const.ROLE.REVIEW.SUPERREVIEWER, set())
    assert len(person_reviews) == 0


def test_get_as_superreviewer_with_reviews_and_no_role(person, person_review):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_review_roles(
            subject=person,
            filters={},
            role_types=[const.ROLE.REVIEW.SUPERREVIEWER],
        ).get(const.ROLE.REVIEW.SUPERREVIEWER, set())
    helpers.assert_ids_equal(models.PersonReview.objects.all(), [person_review])
    assert len(person_reviews) == 0


def test_get_as_superreviewer_with_review_and_role(
    person,
    person_review_builder,
    review_role_builder,
):
    someones_person_review = person_review_builder()
    review_role_builder(
        person=person,
        review=someones_person_review.review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_review_roles(
            subject=person,
            filters={},
            role_types=[const.ROLE.REVIEW.SUPERREVIEWER],
        ).get(const.ROLE.REVIEW.SUPERREVIEWER, set())

    helpers.assert_ids_equal(person_reviews, [someones_person_review])


def test_get_as_superreviewer_with_review_and_role_except_own(
    person_review_builder,
    review_role_builder,
):
    own_person_review = person_review_builder()
    subject = own_person_review.person
    review = own_person_review.review
    someones_person_review = person_review_builder(review=review)
    review_role_builder(
        person=subject,
        review=review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_review_roles(
            subject=subject,
            filters={},
            role_types=[const.ROLE.REVIEW.SUPERREVIEWER],
        ).get(const.ROLE.REVIEW.SUPERREVIEWER, set())

    helpers.assert_ids_equal(person_reviews, [someones_person_review])


def test_get_as_superreviewer_with_many_reviews_and_role(
    person,
    review_builder,
    person_review_builder,
    review_role_builder,
):
    person_review_one = person_review_builder(review=review_builder())
    person_review_builder(review=review_builder())
    review_role_builder(
        person=person,
        review=person_review_one.review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_review_roles(
            subject=person,
            filters={},
            role_types=[const.ROLE.REVIEW.SUPERREVIEWER],
        ).get(const.ROLE.REVIEW.SUPERREVIEWER, set())
    assert models.PersonReview.objects.count() == 2
    helpers.assert_ids_equal(person_reviews, [person_review_one])
