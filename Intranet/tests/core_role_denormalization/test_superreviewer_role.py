# coding: utf-8
from unittest import mock

import pytest

from review.lib import datetimes
from review.core import const
from review.core import models
from review.core.logic import roles
from review.core.logic import review_actions

from tests import helpers


def test_superreviewer_denormalization_created(
    person_review,
    review_role_builder,
):
    review_role = review_role_builder(
        type=const.ROLE.REVIEW.SUPERREVIEWER,
        review=person_review.review,
        skip_denormalization=True,
    )

    roles.denormalize_person_review_roles()

    assert models.PersonReviewRole.objects.filter(
        type=const.ROLE.PERSON_REVIEW.SUPERREVIEWER_DENORMALIZED,
        person=review_role.person,
        person_review=person_review,
        from_calibration_role=None,
        from_review_role=review_role,
        inherited_from=None,
    ).exists()


def test_superreviewer_denormalization_created_if_other_roles_exist(
    person_review,
    review_role_builder,
    person_review_role_builder,
):
    review_role = review_role_builder(
        type=const.ROLE.REVIEW.SUPERREVIEWER,
        review=person_review.review,
        skip_denormalization=True,
    )

    # another PersonReviewRole
    person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        person_review=person_review,
        skip_denormalization=True,
    )

    roles.denormalize_person_review_roles()

    assert models.PersonReviewRole.objects.filter(
        type=const.ROLE.PERSON_REVIEW.SUPERREVIEWER_DENORMALIZED,
        person=review_role.person,
        person_review=person_review,
        from_calibration_role=None,
        from_review_role=review_role,
        inherited_from=None,
    ).exists()


def test_superreviewer_denormalization_created_for_many_review_roles(
    person_review_builder,
    review_role_builder,
):
    person_review_one = person_review_builder()
    review = person_review_one.review
    person_review_two = person_review_builder(review=review)
    review_role_one = review_role_builder(
        type=const.ROLE.REVIEW.SUPERREVIEWER,
        review=review,
        skip_denormalization=True,
    )
    review_role_two = review_role_builder(
        type=const.ROLE.REVIEW.SUPERREVIEWER,
        review=review,
        skip_denormalization=True,
    )

    roles.denormalize_person_review_roles()

    expected_roles = [
        (person_review_one.id, review_role_one.id, review_role_one.person_id),
        (person_review_two.id, review_role_one.id, review_role_one.person_id),
        (person_review_one.id, review_role_two.id, review_role_two.person_id),
        (person_review_two.id, review_role_two.id, review_role_two.person_id),
    ]

    fetched_roles = models.PersonReviewRole.objects.filter(
        type=const.ROLE.PERSON_REVIEW.SUPERREVIEWER_DENORMALIZED,
        from_calibration_role=None,
        inherited_from=None,
    ).values_list(
        'person_review_id',
        'from_review_role_id',
        'person_id',
    )
    helpers.assert_ids_equal(expected_roles, fetched_roles)


def test_superreviewer_inherited_created(
    person_review,
    person_review_builder,
    review_builder,
    review_role_builder,
):
    review = person_review.review
    person = person_review.person
    review_role_builder(
        type=const.ROLE.REVIEW.SUPERREVIEWER,
        review=review,
    )

    current_finish_date = review.finish_date
    PREVIOUS_REVIEWS_COUNT = 3
    for _ in range(PREVIOUS_REVIEWS_COUNT):
        finish_date = datetimes.shifted(current_finish_date, years=-1).isoformat()
        old_review = review_builder(
            finish_date=finish_date,
        )
        current_finish_date = finish_date
        person_review_builder(
            review=old_review,
            person=person,
        )

    roles.denormalize_person_review_roles()

    assert models.PersonReviewRole.objects.filter(
        person_review__person__login=person.login,
        type=const.ROLE.PERSON_REVIEW.SUPERREVIEWER_INHERITED,
    ).count() == PREVIOUS_REVIEWS_COUNT


@pytest.fixture
def history_for_draft(
    person_builder,
    review_builder,
    person_review_builder,
    review_role_builder,
):
    today = datetimes.today()
    old_review = review_builder(
        start_date=datetimes.shifted(today, months=-1, days=-1),
        finish_date=datetimes.shifted(today, months=-1),
    )
    employee = person_builder()
    old_person_review = person_review_builder(
        review=old_review,
        person=employee,
    )

    current_review = review_builder(
        start_date=datetimes.shifted(today, days=-1),
        finish_date=today,
        status=const.REVIEW_STATUS.DRAFT,
    )
    person_review_builder(
        review=current_review,
        person=employee,
    )

    super_reviewer = person_builder()
    review_role_builder(
        review=current_review,
        person=super_reviewer,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    return dict(
        old_person_review=old_person_review,
        current_review=current_review,
        super_reviewer=super_reviewer
    )


def test_create_draft_with_previous(history_for_draft):
    old_person_review = history_for_draft['old_person_review']
    super_reviewer = history_for_draft['super_reviewer']
    assert not old_person_review.roles.filter(
        person_id=super_reviewer.id,
        type=const.ROLE.PERSON_REVIEW.SUPERREVIEWER_INHERITED
    ).exists()


@mock.patch('review.core.tasks.freeze_gradient_data_task', mock.Mock())
def test_draft_to_in_progress(history_for_draft):
    old_person_review = history_for_draft['old_person_review']
    current_review = history_for_draft['current_review']
    super_reviewer = history_for_draft['super_reviewer']
    review_actions.follow_workflow(
        current_review,
        const.REVIEW_ACTIONS.STATUS_PUBLISH,
    )
    assert old_person_review.roles.filter(
        person_id=super_reviewer.id,
        type=const.ROLE.PERSON_REVIEW.SUPERREVIEWER_INHERITED
    ).exists()


@mock.patch('review.core.tasks.freeze_gradient_data_task', mock.Mock())
def test_in_progress_to_draft(history_for_draft):
    old_person_review = history_for_draft['old_person_review']
    current_review = history_for_draft['current_review']
    super_reviewer = history_for_draft['super_reviewer']
    review_actions.follow_workflow(
        current_review,
        const.REVIEW_ACTIONS.STATUS_PUBLISH,
    )
    review_actions.follow_workflow(
        current_review,
        const.REVIEW_ACTIONS.STATUS_IN_DRAFT,
    )
    assert not old_person_review.roles.filter(
        person_id=super_reviewer.id,
        type=const.ROLE.PERSON_REVIEW.SUPERREVIEWER_INHERITED
    ).exists()
