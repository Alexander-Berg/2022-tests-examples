from collections import OrderedDict
from functools import partial
from itertools import product

import pytest

from review.core.logic.review_actions import add_global_admins, delete_from_global_admins
from review.lib import datetimes
from review.core import const, models
from tests.helpers import get_json


REVIEWS_URL = '/frontend/reviews/'


def prepare_reviews(test_person, review_builder, review_role_builder):
    reviews = [
        review_builder(status=const.REVIEW_STATUS.DRAFT),
        review_builder(status=const.REVIEW_STATUS.IN_PROGRESS)
    ]
    review_roles = [
        review_role_builder(
            review=review,
            person=test_person,
            type=const.ROLE.REVIEW.ADMIN
        ) for review in reviews
    ]
    return reviews, review_roles


def test_review_list_get_draft(client, test_person, review_builder, review_role_builder):
    prepare_reviews(test_person, review_builder, review_role_builder)
    request = {"statuses": [const.REVIEW_STATUS.VERBOSE[const.REVIEW_STATUS.DRAFT]]}
    result = get_json(client, REVIEWS_URL, request)
    assert len(result["reviews"]) == 1
    assert result["reviews"][0]["status"] == const.REVIEW_STATUS.VERBOSE[const.REVIEW_STATUS.DRAFT]


def test_review_list_get_in_progress(client, test_person, review_builder, review_role_builder):
    prepare_reviews(test_person, review_builder, review_role_builder)
    request = {"statuses": [const.REVIEW_STATUS.VERBOSE[const.REVIEW_STATUS.IN_PROGRESS]]}
    result = get_json(client, REVIEWS_URL, request)
    assert len(result["reviews"]) == 1
    assert result["reviews"][0]["status"] == const.REVIEW_STATUS.VERBOSE[const.REVIEW_STATUS.IN_PROGRESS]


def test_review_list_get_all(client, test_person, review_builder, review_role_builder):
    reviews, _ = prepare_reviews(test_person, review_builder, review_role_builder)
    result = get_json(client, REVIEWS_URL)
    assert len(result["reviews"]) == len(reviews)


@pytest.mark.parametrize(
    'checking_role,target_field',
    [
        (const.ROLE.REVIEW.ADMIN, 'admins'),
        (const.ROLE.REVIEW.SUPERREVIEWER, 'super_reviewers'),
    ]
)
def test_review_list_superreviewers_admins_fields(
    checking_role,
    target_field,
    client,
    test_person,
    review,
    review_role_builder,
    global_role_builder,
):
    role = review_role_builder(
        review=review,
        person=test_person,
        type=checking_role
    )

    response = get_json(
        client,
        '{url}?fields=admins&fields=super_reviewers'.format(url=REVIEWS_URL),
        login=role.person.login,
    )
    review = response['reviews'][0]
    assert any(
        person['login'] == test_person.login
        for person in review[target_field]
    )


@pytest.mark.parametrize(
    'role_type', [
        const.ROLE.DEPARTMENT.HR_PARTNER,
        const.ROLE.DEPARTMENT.HR_ANALYST,
    ]
)
def test_review_list_available_for_hr(
    client,
    case_reviewers_dep_roles,
    role_type,
):
    case = case_reviewers_dep_roles
    person = {
        const.ROLE.DEPARTMENT.HR_PARTNER: case.hr_partner,
        const.ROLE.DEPARTMENT.HR_ANALYST: case.hr_analyst,
    }[role_type]
    res = get_json(
        client,
        path=REVIEWS_URL,
        login=person.login,
    )
    reviews = res['reviews']
    assert len(reviews) == 1 and reviews[0]['id'] == case_reviewers_dep_roles.review.id


def test_review_list_sorting_order(
    client,
    test_person,
    review_builder,
    review_role_builder,
    person_review_role_builder,
):
    statuses = (
        const.REVIEW_STATUS.IN_PROGRESS,
        const.REVIEW_STATUS.FINISHED,
        const.REVIEW_STATUS.ARCHIVE,
    )

    now = datetimes.today()
    dates = (
        datetimes.shifted(now, months=1),
        now,
        datetimes.shifted(now, months=-1),
    )

    review_type, person_review_type = 'review', 'person_review'
    obj_type_to_builder = {
        review_type: review_role_builder,
        person_review_type: person_review_role_builder,
    }
    role_to_obj_type = OrderedDict((
        (const.ROLE.PERSON_REVIEW.REVIEWER, person_review_type),
        (const.ROLE.REVIEW.SUPERREVIEWER, review_type),
        (const.ROLE.REVIEW.ADMIN, review_type),
    ))

    reviews_order = []
    for status, date, (role, obj_type) in product(statuses, dates, role_to_obj_type.items()):
        review = review_builder(
            author=test_person,
            start_date=date,
            finish_date=datetimes.shifted(date, months=1),
            status=status,
        )
        role_builder = obj_type_to_builder[obj_type]
        role_builder(person=test_person, review=review, type=role)
        reviews_order.append(review.id)

    response = get_json(
        client,
        REVIEWS_URL,
        login=test_person.login,
    )
    received_ids = [r['id'] for r in response['reviews']]
    assert reviews_order == received_ids


def test_review_list_most_relevant_empty(
    client,
):
    response = get_json(client, REVIEWS_URL)
    assert response['most_relevant_id'] is None


@pytest.fixture
def review_builder_as_admin(db, test_person, review_builder, review_role_builder):
    def builder(**review_params):
        review = review_builder(**review_params)
        review_role_builder(
            review=review,
            person=test_person,
            type=const.ROLE.REVIEW.ADMIN,
        )
        return review
    return builder


def test_review_list_most_relevant_no_in_progress(
    client,
    review_builder_as_admin,
):
    now = datetimes.today()
    create_review = partial(review_builder_as_admin, status=const.REVIEW_STATUS.FINISHED)
    most_relevant = create_review(start_date=now)
    create_review(start_date=datetimes.shifted(now, months=-1))

    response = get_json(client, REVIEWS_URL)
    assert len(response['reviews']) == 2
    assert response['most_relevant_id'] == most_relevant.id


def test_review_list_most_relevant_alone_in_progress(
    client,
    review_builder_as_admin,
):
    most_relevant = review_builder_as_admin(status=const.REVIEW_STATUS.IN_PROGRESS)
    review_builder_as_admin(status=const.REVIEW_STATUS.FINISHED)
    review_builder_as_admin(status=const.REVIEW_STATUS.ARCHIVE)

    response = get_json(client, REVIEWS_URL)
    assert len(response['reviews']) == 3
    assert response['most_relevant_id'] == most_relevant.id


def test_review_list_most_relevant_several_in_progress(
    client,
    review_builder_as_admin,
):
    for _ in range(2):
        review_builder_as_admin(status=const.REVIEW_STATUS.IN_PROGRESS)

    response = get_json(client, REVIEWS_URL)
    assert len(response['reviews']) == 2
    assert response['most_relevant_id'] is None


def test_review_global_admins_add(global_role_builder, review_builder):
    global_admin = global_role_builder(type=const.ROLE.GLOBAL.ADMIN).person
    reviews = [review_builder() for _ in range(2)]

    add_global_admins()

    for review in reviews:
        assert (
            models.ReviewRole.objects
            .filter(
                type=const.ROLE.REVIEW.ADMIN,
                person=global_admin,
                review=review,
            )
            .exists()
        )


def test_delete_from_global_admins(global_role_builder, review_builder):
    global_admin_role = global_role_builder(type=const.ROLE.GLOBAL.ADMIN)
    reviews = [review_builder() for _ in range(2)]
    add_global_admins()

    delete_from_global_admins(global_admin_role)

    for review in reviews:
        assert not (
            models.ReviewRole.objects
            .filter(
                type=const.ROLE.REVIEW.ADMIN,
                person=global_admin_role.person,
                review=review,
            )
            .exists()
        )
