import pytest

from review.core import const
from review.core import models
from review.core.logic.assemble import collect_person_reviews
from tests import helpers

EXPECTED_NUM_QUERIES = 1


def _collect_for_head(*args, **kwargs):
    res = collect_person_reviews.collect_for_head(*args, **kwargs)
    return [it for _, it in res]


def test_no_reviews_no_role(person, fetch_root_departments_path):
    assert fetch_root_departments_path()
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_head(
            subject=person,
            filters={},
        )
    assert models.PersonReview.objects.count() == 0
    assert len(person_reviews) == 0


def test_some_reviews_no_role(person, person_review, fetch_root_departments_path):
    """
    Очень важно, чтобы все не стало доступно, когда нет руководительства вообще.
    """
    assert fetch_root_departments_path()
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_head(
            subject=person,
            filters={},
        )
    helpers.assert_ids_equal(models.PersonReview.objects.all(), [person_review])
    assert len(person_reviews) == 0


def test_some_reviews_no_subordinates(
    department_role_head,
    person_review,
    fetch_root_departments_path,
):
    assert fetch_root_departments_path()
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_head(
            subject=department_role_head.person,
            filters={},
        )

    helpers.assert_ids_equal(models.PersonReview.objects.all(), [person_review])
    assert len(person_reviews) == 0


def test_some_reviews_one_subordinate(
    department_role_head,
    person_review,
    person_builder,
    person_review_builder,
    fetch_root_departments_path,
):
    subordinate = person_builder(department=department_role_head.department)
    subordinate_review = person_review_builder(
        person=subordinate,
        review=person_review.review,
    )
    assert fetch_root_departments_path()
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_head(
            subject=department_role_head.person,
            filters={},
        )
    assert len(models.PersonReview.objects.all()) > 1
    helpers.assert_ids_equal(person_reviews, [subordinate_review])


def test_shouldnt_see_own_reviews(
    department_role_head,
    person_builder,
    person_review_builder,
    fetch_root_departments_path,
):
    head_himself = department_role_head.person
    employee = person_builder(department=department_role_head.department)
    head_review = person_review_builder(person=head_himself)
    employee_review = person_review_builder(person=employee)

    assert fetch_root_departments_path()
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_head(
            subject=head_himself,
            filters={},
        )
    helpers.assert_ids_equal(
        models.PersonReview.objects.all(),
        [employee_review, head_review]
    )
    helpers.assert_ids_equal(
        person_reviews,
        [employee_review]
    )


def test_shouldnt_see_external_subordinates(
    department_root_builder,
    department_head_role_builder,
    person_builder,
    review_builder,
    person_review_builder,
    fetch_root_departments_path,
):
    yandex_dep = department_root_builder(slug='yandex')  # internal
    ext_dep = department_root_builder(slug='ext')  # external
    outstaff_dep = department_root_builder(slug='outstaff')  # external

    chief = person_builder()

    department_head_role_builder(person=chief, department=yandex_dep)
    department_head_role_builder(person=chief, department=ext_dep)
    department_head_role_builder(person=chief, department=outstaff_dep)

    internal_subordinate = person_builder(department=yandex_dep)
    ext_subordinate = person_builder(department=ext_dep)
    outstaff_subordinate = person_builder(department=outstaff_dep)

    review = review_builder()
    subordinate_review = person_review_builder(person=internal_subordinate, review=review)
    person_review_builder(person=ext_subordinate, review=review)
    person_review_builder(person=outstaff_subordinate, review=review)

    assert {yandex_dep.slug, ext_dep.slug, outstaff_dep.slug}.issubset(
        set(fetch_root_departments_path())
    )
    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_head(subject=chief, filters={})

    assert models.PersonReview.objects.count() == 3
    assert len(person_reviews) == 1
    helpers.assert_ids_equal(person_reviews, [subordinate_review])


@pytest.mark.parametrize(
    'role_type',
    [
        None,
        *(const.ROLE.PERSON_REVIEW.ALL - const.ROLE.PERSON_REVIEW.ALL_REVIEWER_ROLES),
    ]
)
@pytest.mark.parametrize('root_department', ['ext', 'outstaff'])
def test_should_not_see_external_subordinates_if_he_is_not_reviewer(
    role_type,
    root_department,
    department_root_builder,
    department_head_role_builder,
    person_builder,
    review_builder,
    person_review_builder,
    person_review_role_builder,
    fetch_root_departments_path,
):
    chief = person_builder()
    department = department_root_builder(slug=root_department)  # external
    department_head_role_builder(person=chief, department=department)

    subordinate = person_builder(department=department)

    person_review = person_review_builder(person=subordinate)
    if role_type is not None:
        person_review_role_builder(person_review=person_review, person=chief, type=role_type, skip_denormalization=True)

    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_head(subject=chief, filters={})

    assert len(person_reviews) == 0


@pytest.mark.parametrize('role_type', const.ROLE.PERSON_REVIEW.ALL_REVIEWER_ROLES)
@pytest.mark.parametrize('root_department', ['ext', 'outstaff'])
def test_should_see_external_subordinates_if_he_is_reviewer(
    role_type,
    root_department,
    department_root_builder,
    department_head_role_builder,
    person_builder,
    review_builder,
    person_review_builder,
    person_review_role_builder,
    fetch_root_departments_path,
):
    chief = person_builder()
    department = department_root_builder(slug=root_department)  # external
    department_head_role_builder(person=chief, department=department)

    subordinate = person_builder(department=department)

    person_review = person_review_builder(person=subordinate)
    person_review_role_builder(person_review=person_review, person=chief, type=role_type, skip_denormalization=True)

    with helpers.assert_num_queries(EXPECTED_NUM_QUERIES):
        person_reviews = _collect_for_head(subject=chief, filters={})

    assert len(person_reviews) == 1
    helpers.assert_ids_equal(person_reviews, [person_review])
