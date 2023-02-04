# coding: utf-8

import itertools
from collections import defaultdict

import pytest

from review.core import const
from review.core import models
from review.core.logic.assemble import (
    get_calibration_person_reviews,
    collect_person_reviews,
)
from review.core.logic import calibration_actions
from tests import helpers

EXPECTED_NUM_QUERIES = 1


def get_person_reviews_for_roles_grouped(*args, **kwargs):
    collected = collect_person_reviews.collect_for_person_review_roles(*args, **kwargs)
    res = defaultdict(list)
    for role, data in collected:
        res[role].append(data)
    return res


def get_person_reviews_for_roles_flat(*args, **kwargs):
    return list(
        itertools.chain.from_iterable(
            list(get_person_reviews_for_roles_grouped(*args, **kwargs).values())
        )
    )


def test_get_for_roles_no_reviews_no_roles(person):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = get_person_reviews_for_roles_flat(
            subject=person,
            filters={},
        )
    assert models.PersonReview.objects.count() == 0
    assert len(person_reviews) == 0


def test_get_for_roles_with_reviews_no_roles(person, person_review):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = get_person_reviews_for_roles_flat(
            subject=person,
            filters={},
        )
    helpers.assert_ids_equal(models.PersonReview.objects.all(), [person_review])
    assert len(person_reviews) == 0


def test_get_for_role_reviewer_with_reviews(
    person_review,
    person_review_role_reviewer
):
    _test_for_some_role_with_reviews(role=person_review_role_reviewer)


def test_get_for_role_top_reviewer_with_reviews(
    person_review,
    person_review_role_top_reviewer,
):
    _test_for_some_role_with_reviews(role=person_review_role_top_reviewer)


def test_get_for_role_reader_with_reviews(
    person_review,
    person_review_role_reader,
):
    _test_for_some_role_with_reviews(role=person_review_role_reader)


def test_get_for_role_superreader_with_reviews(
    person_review,
    person_review_role_superreader,
):
    _test_for_some_role_with_reviews(role=person_review_role_superreader)


def _test_for_some_role_with_reviews(role):
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = get_person_reviews_for_roles_grouped(
            subject=role.person,
            role_types=[role.type],
            filters={},
        )[role.type]
    assert models.PersonReview.objects.count() > 1
    helpers.assert_ids_equal(person_reviews, [role.person_review])


SOME_TEST_ROLES = [
    const.ROLE.PERSON_REVIEW.REVIEWER,
    const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    const.ROLE.PERSON_REVIEW.READER,
    const.ROLE.PERSON_REVIEW.SUPERREADER,
]


@pytest.mark.parametrize('role_type', SOME_TEST_ROLES)
def test_for_several_roles_for_one_person_with_reviews(
    role_type,
    person_review,
    person,
    person_review_role_builder,
):
    created_roles = {}
    for role_type in SOME_TEST_ROLES:
        created_roles[role_type] = person_review_role_builder(
            person=person,
            type=role_type,
        )
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        grouped_person_reviews = get_person_reviews_for_roles_grouped(
            subject=person,
            filters={},
        )

    created_person_review = created_roles[role_type].person_review
    selected_person_reviews = grouped_person_reviews[role_type]
    helpers.assert_ids_equal(selected_person_reviews, [created_person_review])


def test_for_two_roles_for_same_review(
    person,
    person_review_role_builder,
):
    created_roles = {}
    TWO_ROLES = {
        const.ROLE.PERSON_REVIEW.READER,
        const.ROLE.PERSON_REVIEW.SUPERREADER,
    }
    for role_type in TWO_ROLES:
        created_roles[role_type] = person_review_role_builder(
            person=person,
            type=role_type,
        ).person_review
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        selected = get_person_reviews_for_roles_grouped(
            subject=person,
            filters={},
        )

    for role_type in TWO_ROLES:
        helpers.assert_ids_equal(selected[role_type], [created_roles[role_type]])


def test_calibration_person_reviews_for_admin(
    calibration_person_review_builder,
    test_person,
    calibration,
    calibration_role_builder,
):
    for _ in range(2):
        calibration_person_review_builder(calibration=calibration)

    calibration_person_reviews = get_calibration_person_reviews(
        subject=test_person,
        filters_chosen={'calibration_id': calibration.id},
        requested_person_review_fields={const.FIELDS.ID},
    )

    assert not calibration_person_reviews

    calibration_role_builder(
        calibration=calibration,
        person=test_person,
        type=const.ROLE.CALIBRATION.ADMIN,
    )

    calibration_person_reviews = get_calibration_person_reviews(
        subject=test_person,
        filters_chosen={'calibration_id': calibration.id},
        requested_person_review_fields={const.FIELDS.ID},
    )

    assert len(calibration_person_reviews) == 2
    assert all(pc.actions['discuss'] == const.OK for pc in calibration_person_reviews)


def test_calibration_person_reviews_for_calibrator(
    calibration_person_review_builder,
    test_person,
    calibration_builder,
):
    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    for _ in range(2):
        calibration_person_review_builder(calibration=calibration)

    calibration_person_reviews = get_calibration_person_reviews(
        subject=test_person,
        filters_chosen={'calibration_id': calibration.id},
        requested_person_review_fields={const.FIELDS.ID},
    )

    assert not calibration_person_reviews
    calibration_actions.add_calibrators(
        calibration=calibration,
        persons=[test_person]
    )
    calibration_person_reviews = get_calibration_person_reviews(
        subject=test_person,
        filters_chosen={'calibration_id': calibration.id},
        requested_person_review_fields={const.FIELDS.ID},
    )

    assert len(calibration_person_reviews) == 2
    assert all(pc.actions['discuss'] == const.NO_ACCESS for pc in calibration_person_reviews)
