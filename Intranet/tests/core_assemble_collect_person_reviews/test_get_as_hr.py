# coding: utf-8

from collections import defaultdict

import pytest

from review.core import const
from review.core import models
from review.core.logic.assemble import collect_person_reviews
from review.staff import const as staff_const
from tests import helpers

EXPECTED_NUM_QUERIES = 1


def _collect_for_review_roles(*args, **kwargs):
    collected = collect_person_reviews.collect_for_hr(*args, **kwargs)
    res = defaultdict(list)
    for role, data in collected:
        res[role].append(data)
    return res


@pytest.mark.parametrize(
    'role', [
        const.ROLE.DEPARTMENT.HR_ANALYST,
        const.ROLE.DEPARTMENT.HR_PARTNER,
    ]
)
def test_hr_has_person_reviews(
    department_role_builder,
    person_review,
    role,
):
    hr = department_role_builder(
        type=role,
        department=person_review.person.department,
    ).person
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_review_roles(
            subject=hr,
            filters={},
            role_types=[role],
        ).get(role, set())
    assert len(person_reviews) == 1
    assert person_reviews.pop()['id'] == person_review.id


@pytest.mark.parametrize(
    'role', [
        const.ROLE.DEPARTMENT.HR_ANALYST,
        const.ROLE.DEPARTMENT.HR_PARTNER,
    ]
)
def test_hr_has_no_person_reviews(
    department_root_builder,
    department_role_builder,
    person_review,
    role,
):
    hr = department_role_builder(
        type=role,
        department=department_root_builder(),
    ).person
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_review_roles(
            subject=hr,
            filters={},
            role_types=[role],
        ).get(role, set())
    assert len(person_reviews) == 0
