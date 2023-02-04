# coding: utf-8
"""
Неизвестно насколько жизнеспособным окажется такой здоровенный тест
и есть ли смысл их писать, но давайте попробуем хотя бы минимальный.
"""

import pytest

from review.core import const
from review.core.logic import assemble
from review.shortcuts import models
from review.core.logic.assemble import fetched_obj, collect_person_reviews
from review.staff import const as staff_const
from tests import helpers
from tests.core_assemble_collect_person_reviews import (
    test_get_as_self,
    test_get_as_head,
    test_get_as_other_roles,
    test_get_as_superreviewer,
    test_get_as_hr,
)

FETCH_GLOBAL_ROLES_QUERIES = 1
FETCH_PR_FOR_GLOBAL_ROLES_QUERIES = 1

EXPECTED_NUM_QUERIES = sum([
    test_get_as_self.EXPECTED_NUM_QUERIES,
    test_get_as_head.EXPECTED_NUM_QUERIES,
    test_get_as_other_roles.EXPECTED_NUM_QUERIES,
    test_get_as_superreviewer.EXPECTED_NUM_QUERIES,
    FETCH_GLOBAL_ROLES_QUERIES,
    FETCH_PR_FOR_GLOBAL_ROLES_QUERIES,
    test_get_as_hr.EXPECTED_NUM_QUERIES,
])


@pytest.mark.parametrize(
    'role_type',
    [
        const.ROLE.PERSON_REVIEW.REVIEWER,
        const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
        const.ROLE.PERSON_REVIEW.READER,
        const.ROLE.PERSON_REVIEW.SUPERREADER,
    ],
)
def test_as_one_role_while_having_all(
    role_type,
    person_review,
    department_role_head,
    roles_to_person_reviews,
    fetch_root_departments_path,
):
    fetch_root_departments_path()
    person = department_role_head.person

    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        grouped_by_role = _get_allowed_grouped_by_roles(
            subject=person,
            filters={},
            role_types=const.ROLE.PERSON_REVIEW_LIST_RELATED,
        )
    fetched = grouped_by_role[role_type]
    created = roles_to_person_reviews[role_type]
    helpers.assert_ids_equal(fetched, [created])


@pytest.mark.parametrize(
    'role_type',
    [
        const.ROLE.PERSON_REVIEW.REVIEWER,
        const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
        const.ROLE.PERSON_REVIEW.READER,
        const.ROLE.PERSON_REVIEW.SUPERREADER,
    ],
)
def test_as_one_role_while_having_all_get_ids(
    role_type,
    person_review,
    department_role_head,
    roles_to_person_reviews,
):
    person = department_role_head.person

    ids = collect_person_reviews.get_allowed_person_review_ids(
        subject=person,
        filters={},
        role_types=[role_type],
    )
    created = roles_to_person_reviews[role_type]
    assert ids == {created.id}


def test_as_superreviewer_while_having_all(
    person_review,
    department_role_head,
    roles_to_person_reviews,
):
    # хелпер созадет роли так, что они попадают в одно ревью
    person = department_role_head.person

    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        allowed = _get_allowed_grouped_by_roles(
            subject=person,
            filters={},
            role_types=const.ROLE.PERSON_REVIEW_LIST_RELATED,
        )

    fetched = allowed[const.ROLE.REVIEW.SUPERREVIEWER]
    superreviewed_review = models.Review.objects.filter(
        role__person=person
    ).first()
    expected = models.PersonReview.objects.filter(
        review=superreviewed_review
    ).exclude(person=person)
    helpers.assert_ids_equal(fetched, expected)


def test_as_one_role_while_having_all_and_filter_works(
    person_review,
    department_role_head,
    roles_to_person_reviews,
):
    person = department_role_head.person

    related_roles = const.ROLE.PERSON_REVIEW_LIST_RELATED - const.ROLE.INHERITED
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        grouped_by_role = _get_allowed_grouped_by_roles(
            subject=person,
            filters={},
            role_types=related_roles,
        )
        fetched_roles = {
            role for role, group in grouped_by_role.items() if group
        }

    expected_roles = related_roles
    assert fetched_roles == expected_roles


def test_select_person_review_one_from_many(
    test_person,
    person_review_role_builder,
    person_review_builder
):
    person_reviews = [person_review_builder() for _ in range(5)]
    person_review_roles = [
        person_review_role_builder(
            person_review=pr,
            type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
            person=test_person,
        )
        for pr in person_reviews
    ]
    collected_person_review = assemble.get_person_review(
        subject=test_person,
        fields_requested=const.FIELDS.PERMISSIONS_WRITE,
        id=person_reviews[0].id
    )
    assert collected_person_review.id == person_reviews[0].id


@pytest.fixture
def roles_to_person_reviews(
    global_role_builder,
    department_role_head,
    department_role_builder,
    person_builder,
    person_review_builder,
    review_role_builder,
    person_review_role_builder,
    calibration_builder,
    calibration_role_builder,
    calibration_person_review_builder,
):
    created = {}

    person = department_role_head.person

    # own person_review
    person_review_self = person_review_builder(
        person=person,
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
    )
    created[const.ROLE.PERSON.SELF] = person_review_self
    some_review = person_review_self.review

    # person_review for subordinate
    department = department_role_head.department
    subordinate = person_builder(department=department)
    created[const.ROLE.DEPARTMENT.HEAD] = person_review_builder(
        person=subordinate,
        review=some_review,
    )

    # hrs
    hr_roles = [
        staff_const.STAFF_ROLE.HR.HR_ANALYST,
        staff_const.STAFF_ROLE.HR.HR_PARTNER,
    ]
    for role_type in hr_roles:
        department_role_builder(
            person=person,
            department=department,
            type=role_type,
        )
        created[role_type] = person_review_builder(
            person=person_builder(department=department),
            review=some_review,
        )

    # as superreviewer
    review_role_builder(
        person=person,
        review=some_review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    review_role_builder(
        person=person,
        review=some_review,
        type=const.ROLE.REVIEW.ADMIN,
    )
    review_role_builder(
        person=person,
        review=some_review,
        type=const.ROLE.REVIEW.ACCOMPANYING_HR,
    )

    calibration = calibration_builder(status=const.CALIBRATION_STATUS.IN_PROGRESS)
    cpr = calibration_person_review_builder(calibration=calibration)

    # as calibration roles
    calibration_role_builder(
        person=person,
        type=const.ROLE.CALIBRATION.ADMIN,
        calibration=cpr.calibration,
    )
    calibration_role_builder(
        person=person,
        type=const.ROLE.CALIBRATION.CALIBRATOR,
        calibration=cpr.calibration,
    )

    # as robot
    global_role_builder(
        person=person,
        type=const.ROLE.GLOBAL.ROBOT,
    )

    created[const.ROLE.CALIBRATION.ADMIN] = cpr.person_review
    created[const.ROLE.CALIBRATION.CALIBRATOR] = cpr.person_review

    roles_to_create = const.ROLE.PERSON_REVIEW.ALL - set(
        const.ROLE.DENORMALIZATION_REVERSED) - set(const.ROLE.INHERITED)
    for role_type in roles_to_create:
        created[role_type] = person_review_role_builder(
            person=person,
            type=role_type,
            review=some_review,
        ).person_review

    return created


def _get_allowed_grouped_by_roles(
    subject,
    role_types=None,
    filters=None,
    db_fields=None,
):
    fetched = fetched_obj.Fetched()
    collect_person_reviews.get_allowed_person_reviews_flat(
        subject=subject,
        fetched=fetched,
        role_types=role_types,
        filters=filters,
        db_fields=db_fields,
    )
    return fetched.grouped_by_roles
