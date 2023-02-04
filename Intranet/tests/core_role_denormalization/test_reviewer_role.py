# coding: utf-8

import arrow
import pytest
from pretend import stub

from review.lib import datetimes
from review.core import const
from review.core import models
from review.core.logic import roles

from tests import helpers


DENORMALIZATION_CALL_TYPES = [
    'nothing',
    'by_person_review_ids',
    'by_review_ids',
]
REVIEWER_ROLES = [
    const.ROLE.PERSON_REVIEW.REVIEWER,
    const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
]


@pytest.mark.parametrize('call_params', DENORMALIZATION_CALL_TYPES)
@pytest.mark.parametrize('role_type', REVIEWER_ROLES)
def test_role_inherited_only_in_previous_reviews(
    call_params,
    role_type,
    case,
    person_review_role_builder,
):
    reviewer_role_in_second_review = person_review_role_builder(
        person_review=case.person_review_second,
        type=role_type,
        skip_denormalization=True,
    )

    params = {}
    if call_params == 'by_review_ids':
        params = {'review_id': case.review_second.id}
    if call_params == 'by_person_review_ids':
        params = {'id__in': [case.person_review_second.id]}
    roles.denormalize_person_review_roles(**params)

    assert role_exists(id=reviewer_role_in_second_review.id)
    assert is_inherited(
        role=reviewer_role_in_second_review,
        person_review=case.person_review_first,
    )
    assert not is_inherited(
        role=reviewer_role_in_second_review,
        person_review=case.person_review_last,
    )


@pytest.mark.parametrize('call_params', DENORMALIZATION_CALL_TYPES)
@pytest.mark.parametrize('role_type', REVIEWER_ROLES)
def test_two_roles_inherited_in_previous_reviews_independently(
    call_params,
    role_type,
    case,
    person_review_role_builder,
):
    reviewer_role_in_last_review = person_review_role_builder(
        person_review=case.person_review_last,
        type=role_type,
        skip_denormalization=True,
    )
    reviewer_role_in_second_review = person_review_role_builder(
        person_review=case.person_review_second,
        type=role_type,
        skip_denormalization=True,
    )

    params = {}
    if call_params == 'by_review_ids':
        params = {'review_id__in': [
            case.review_second.id,
            case.review_last.id,
        ]}
    if call_params == 'by_person_review_ids':
        params = {'id__in': [
            case.person_review_second.id,
            case.person_review_last.id,
        ]}
    roles.denormalize_person_review_roles(**params)

    assert role_exists(id=reviewer_role_in_second_review.id)
    assert is_inherited(
        role=reviewer_role_in_second_review,
        person_review=case.person_review_first,
    )
    assert not is_inherited(
        role=reviewer_role_in_second_review,
        person_review=case.person_review_last,
    )
    assert role_exists(id=reviewer_role_in_last_review.id)
    assert is_inherited(
        role=reviewer_role_in_last_review,
        person_review=case.person_review_first,
    )
    assert is_inherited(
        role=reviewer_role_in_last_review,
        person_review=case.person_review_second,
    )


@pytest.mark.parametrize('role_type', REVIEWER_ROLES)
def test_two_roles_delete_one_doesnt_affect_other(
    role_type,
    case,
    person_review_role_builder,
):
    reviewer_role_in_last_review = person_review_role_builder(
        person_review=case.person_review_last,
        type=role_type,
        skip_denormalization=True,
    )
    reviewer_role_in_second_review = person_review_role_builder(
        person_review=case.person_review_second,
        type=role_type,
        skip_denormalization=True,
    )

    roles.denormalize_person_review_roles()

    reviewer_role_in_last_review.delete()

    assert role_exists(id=reviewer_role_in_second_review.id)
    assert is_inherited(
        role=reviewer_role_in_second_review,
        person_review=case.person_review_first,
    )
    assert not role_exists(id=reviewer_role_in_last_review.id)
    assert not is_inherited(
        role=reviewer_role_in_second_review,
        person_review=case.person_review_last,
    )
    assert not is_inherited(
        role=reviewer_role_in_last_review,
        person_review=case.person_review_first,
    )
    assert not is_inherited(
        role=reviewer_role_in_last_review,
        person_review=case.person_review_second,
    )


@pytest.mark.parametrize('call_params', DENORMALIZATION_CALL_TYPES)
@pytest.mark.parametrize('role_type', REVIEWER_ROLES)
def test_review_dates_shuffle_changes_roles(
    call_params,
    role_type,
    case,
    person_review_role_builder,
):
    reviewer_role_in_last_review = person_review_role_builder(
        person_review=case.person_review_last,
        type=role_type,
        skip_denormalization=True,
    )
    reviewer_role_in_second_review = person_review_role_builder(
        person_review=case.person_review_second,
        type=role_type,
        skip_denormalization=True,
    )
    roles.denormalize_person_review_roles()

    # now second is latest
    helpers.update_model(
        case.review_second,
        start_date=datetimes.shifted(case.review_last.start_date, months=6),
        finish_date=datetimes.shifted(case.review_last.finish_date, months=6),
    )

    params = {}
    if call_params == 'by_review_ids':
        params = {'review_id': case.review_second.id}
    if call_params == 'by_person_review_ids':
        params = {'id__in': [case.person_review_second.id]}
    roles.denormalize_person_review_roles(**params)

    assert role_exists(id=reviewer_role_in_second_review.id)
    assert is_inherited(
        role=reviewer_role_in_second_review,
        person_review=case.person_review_first,
    )
    assert is_inherited(
        role=reviewer_role_in_second_review,
        person_review=case.person_review_last,
    )

    assert role_exists(id=reviewer_role_in_last_review.id)
    assert not is_inherited(
        role=reviewer_role_in_last_review,
        person_review=case.person_review_second,
    )
    assert is_inherited(
        role=reviewer_role_in_last_review,
        person_review=case.person_review_first,
    )


@pytest.fixture(name='case')
def case_with_three_same_person_reviews(
    person_review_builder,
):
    person_review_first = person_review_builder(
        review__name='first'
    )
    person = person_review_first.person
    person_review_second = person_review_builder(
        person=person,
        review__start_date=datetimes.shifted(
            person_review_first.review.start_date,
            months=6,
        ),
        review__finish_date=datetimes.shifted(
            person_review_first.review.start_date,
            months=7,
        ),
        review__name='second',
    )
    person_review_last = person_review_builder(
        person=person,
        review__start_date=datetimes.shifted(
            person_review_second.review.start_date,
            months=6,
        ),
        review__finish_date=datetimes.shifted(
            person_review_second.review.start_date,
            months=7,
        ),
        review__name='last',
    )

    return stub(
        person=person,
        person_review_first=person_review_first,
        review_first=person_review_first.review,
        person_review_second=person_review_second,
        review_second=person_review_second.review,
        person_review_last=person_review_last,
        review_last=person_review_last.review,
    )


def role_exists(**filter):
    return models.PersonReviewRole.objects.filter(
        **filter
    ).exists()


def is_inherited(role, person_review):
    return role_exists(
        person=role.person,
        person_review=person_review,
        type=const.ROLE.INHERITANCE[role.type],
        inherited_from=role.id,
    )
