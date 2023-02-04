# coding: utf-8

import pytest

from review.core import const
from review.staff import const as staff_const

from tests import helpers


SUBJECT_LOGIN = 'subject'
DUMMY_LOGIN = 'dummy'

CHECKING_URL = '/frontend/person-reviews-mode-review/'

TESTS_PARAMETRIZATION = [
    (
        {"mark": 'A'},
        {"mark": 'A'},
        {"mark": 'B'}
    ),
    (
        {"flagged": True},
        {"flagged": True},
        {"flagged": False}
    ),
    (
        {"flagged_positive": True},
        {"flagged": True},
        {"flagged": False},
    ),
    (
        {"status": const.PERSON_REVIEW_STATUS.EVALUATION},
        {"status": const.PERSON_REVIEW_STATUS.VERBOSE[const.PERSON_REVIEW_STATUS.EVALUATION]},
        {"status": const.PERSON_REVIEW_STATUS.VERBOSE[const.PERSON_REVIEW_STATUS.WAIT_ANNOUNCE]},
    ),
    (
        {"goldstar": const.GOLDSTAR.OPTION_AND_BONUS},
        {"goldstar": const.GOLDSTAR.VERBOSE[const.GOLDSTAR.OPTION_AND_BONUS]},
        {"goldstar": const.GOLDSTAR.VERBOSE[const.GOLDSTAR.OPTION_ONLY]},
    ),
    (
        {"bonus": 5},
        {"bonus": True},
        {"bonus": False},
    ),
    (
        {"level_change": 5},
        {"level_change_from": 3},
        {"level_change_from": 6},
    ),
    (
        {"level_change": 5},
        {"level_change_to": 6},
        {"level_change_to": 4},
    ),
    (
        {"level_change": 5},
        {"level_changed": True},
        {"level_changed": False},
    ),
    (
        {"level_change": 0},
        {"level_changed": False},
        {"level_changed": True},
    ),
    (
        {"salary_change": 5},
        {"salary_change_from": 3},
        {"salary_change_from": 6},
    ),
    (
        {"salary_change": 5},
        {"salary_change_to": 6},
        {"salary_change_to": 4},
    ),
    (
        {"options_rsu": 5},
        {"options_rsu": True},
        {"options_rsu": False},
    ),
    (
        {},
        {'reviewer': [SUBJECT_LOGIN]},
        {'reviewer': [DUMMY_LOGIN]},
    ),
    (
        {"approve_level": 0},
        {"action_at": [SUBJECT_LOGIN]},
        {"action_at": [DUMMY_LOGIN]},
    ),
    # CIA-897: должен быть замкнутый интервал
    (
        {"level_change": 5},
        {"level_change_from": 5},
        {"level_change_from": 6},
    ),
    (
        {"level_change": 5},
        {"level_change_to": 5},
        {"level_change_to": 4},
    ),
    (
        {"taken_in_average": True},
        {"taken_in_average": True},
        {"taken_in_average": False}
    ),
]


@pytest.mark.parametrize(
    "params, filter_positive, filter_negative",
    TESTS_PARAMETRIZATION,
)
def test_person_review_filters(
    params,
    filter_positive,
    filter_negative,
    client,
    person_builder,
    review_builder,
    review_role_builder,
    person_review_builder,
    person_review_role_builder,
):
    dummy_person = person_builder(login=DUMMY_LOGIN)
    subject_person = person_builder(login=SUBJECT_LOGIN)
    review = review_builder(**{
        key: const.REVIEW_MODE.MODE_MANUAL
        for key in list(const.REVIEW_MODE.FIELDS_TO_MODIFIERS.values())
    })
    review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
        person=subject_person,
    )
    person_review = person_review_builder(
        review=review,
        **params
    )
    top_reviewer_role = person_review_role_builder(
        person=subject_person,
        person_review=person_review,
        position=0,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    )
    positive_result = helpers.get_json(
        client=client,
        login=subject_person.login,
        path=CHECKING_URL,
        request=filter_positive,
    )
    negative_result = helpers.get_json(
        client=client,
        login=subject_person.login,
        path=CHECKING_URL,
        request=filter_negative
    )
    assert len(positive_result['person_reviews']) == 1
    assert positive_result['person_reviews'][0]['id'] == person_review.id
    assert len(negative_result['person_reviews']) == 0


def test_person_review_filter_by_scale(
    client,
    person_builder,
    review_builder,
    marks_scale_builder,
    review_role_builder,
    person_review_builder,
    person_review_role_builder,
):
    subject_person = person_builder(login=SUBJECT_LOGIN)
    scale_right = marks_scale_builder()
    scale_wrong = marks_scale_builder()
    scale_just_for_multi = marks_scale_builder()
    review = review_builder(scale=scale_right, **{
        key: const.REVIEW_MODE.MODE_MANUAL
        for key in list(const.REVIEW_MODE.FIELDS_TO_MODIFIERS.values())
    })
    review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
        person=subject_person,
    )
    person_review = person_review_builder(review=review)
    positive_result = helpers.get_json(
        client=client,
        login=subject_person.login,
        path=CHECKING_URL,
        request={'scale': scale_right.id},
    )
    negative_result = helpers.get_json(
        client=client,
        login=subject_person.login,
        path=CHECKING_URL,
        request={'scale': [scale_wrong.id, scale_just_for_multi.id]},
    )
    assert len(positive_result['person_reviews']) == 1
    assert positive_result['person_reviews'][0]['id'] == person_review.id
    assert len(negative_result['person_reviews']) == 0


def test_person_review_filter_by_calibration(
    client,
    person_builder,
    review_builder,
    review_role_builder,
    person_review_builder,
    calibration_builder,
    calibration_person_review_builder,
):
    subject_person = person_builder(login=SUBJECT_LOGIN)
    review = review_builder()
    review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
        person=subject_person,
    )
    person_review = person_review_builder(review=review)
    calibration = calibration_builder()
    calibration_person_review_builder(
        person_review=person_review,
        calibration=calibration,
    )
    # another calibration for person_review in the same review
    calibration_person_review_builder(
        person_review=person_review_builder(review=review)
    )

    result = helpers.get_json(
        client=client,
        login=subject_person.login,
        path=CHECKING_URL,
        request={'reviews': [review.id], 'calibrations': [calibration.id]},
    )
    assert len(result['person_reviews']) == 1
    assert result['person_reviews'][0]['id'] == person_review.id


def test_filter_by_tag(
    client,
    person_review_builder,
    person_review_role_top_reviewer,
):
    person_review = person_review_role_top_reviewer.person_review
    helpers.update_model(person_review, tag_average_mark='asd')
    person_review_builder(tag_average_mark='bsd')
    result = helpers.get_json(
        client=client,
        login=person_review_role_top_reviewer.person.login,
        path=CHECKING_URL,
        request={'tag_average_mark': ['asd', 'bsd']},
    )
    assert len(result['person_reviews']) == 1
    assert result['person_reviews'][0]['id'] == person_review.id


@pytest.mark.parametrize(
    'filter_params,what_is_found',
    [
        ({'umbrella__isnull': True}, ['without_umbrella']),
        ({'umbrella__isnull': False}, ['with_umbrella1', 'with_umbrella2']),
        ({'umbrella': 100501}, ['with_umbrella1']),
        ({'umbrella': 100502}, ['with_umbrella2']),
        ({'umbrella': [100501, 100502]}, ['with_umbrella1', 'with_umbrella2']),
        ({'umbrella': 100501, 'umbrella__isnull': True}, ['with_umbrella1', 'without_umbrella']),
        ({'umbrella': 100501, 'umbrella__isnull': False}, ['with_umbrella1', 'with_umbrella2']),
    ],
)
def test_filter_by_umbrella(
    filter_params,
    what_is_found,
    client,
    person_builder,
    person_review_builder,
    umbrella_builder,
):
    person = person_builder()

    person_reviews = {
        'with_umbrella1': person_review_builder(
            status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
            person=person,
            umbrella=umbrella_builder(id=100501),
        ),
        'with_umbrella2': person_review_builder(
            status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
            person=person,
            umbrella=umbrella_builder(id=100502),
        ),
        'without_umbrella': person_review_builder(
            status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
            person=person,
            umbrella=None,
        ),
    }

    result = helpers.get_json(
        client=client,
        path=CHECKING_URL,
        login=person.login,
        request=filter_params,
    )
    assert len(result['person_reviews']) == len(what_is_found)
    expected_ids = sorted(person_reviews[name].id for name in what_is_found)
    assert sorted(pr['id'] for pr in result['person_reviews']) == expected_ids


@pytest.mark.parametrize(
    'filter_params,what_is_found',
    [
        ({'main_product__isnull': True}, ['without_product']),
        ({'main_product__isnull': False}, ['with_product1', 'with_product2']),
        ({'main_product': 100501}, ['with_product1']),
        ({'main_product': 100502}, ['with_product2']),
        ({'main_product': [100501, 100502]}, ['with_product1', 'with_product2']),
        ({'main_product': 100501, 'main_product__isnull': True}, ['with_product1', 'without_product']),
        ({'main_product': 100501, 'main_product__isnull': False}, ['with_product1', 'with_product2']),
    ],
)
def test_filter_by_main_product(
    filter_params,
    what_is_found,
    client,
    person_builder,
    person_review_builder,
    main_product_builder,
):
    person = person_builder()

    person_reviews = {
        'with_product1': person_review_builder(
            status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
            person=person,
            main_product=main_product_builder(id=100501),
        ),
        'with_product2': person_review_builder(
            status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
            person=person,
            main_product=main_product_builder(id=100502),
        ),
        'without_product': person_review_builder(
            status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
            person=person,
            main_product=None,
        ),
    }

    result = helpers.get_json(
        client=client,
        path=CHECKING_URL,
        login=person.login,
        request=filter_params,
    )
    assert len(result['person_reviews']) == len(what_is_found)
    expected_ids = sorted(person_reviews[name].id for name in what_is_found)
    assert sorted(pr['id'] for pr in result['person_reviews']) == expected_ids


@pytest.fixture
def person_review_with_reviewer(
    person_builder,
    person_review_builder,
    person_review_role_builder,
    review_role_builder,
):
    target_person_review = person_review_builder()
    target_review = target_person_review.review
    review_admin = person_builder()
    review_role_builder(
        review=target_review,
        type=const.ROLE.REVIEW.ADMIN,
        person=review_admin,
    )
    reviewer = person_builder()
    person_review_role_builder(
        person=reviewer,
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
        person_review=target_person_review,
    )
    person_review_builder(
        person=reviewer,
        review=target_review
    )

    return dict(
        admin=review_admin,
        target_person_review=target_person_review,
        reviewer=reviewer,
    )


def test_filter_by_existing_reviewer(
    client,
    person_review_with_reviewer,
):
    review_admin = person_review_with_reviewer['admin']
    reviewer = person_review_with_reviewer['reviewer']
    response = helpers.get_json(
        client,
        path=CHECKING_URL,
        login=review_admin.login,
        request=dict(reviewer=reviewer.login),
    )
    person_reviews = response['person_reviews']
    target_person_review = person_review_with_reviewer['target_person_review']
    assert len(person_reviews) == 1 and person_reviews[0]['id'] == target_person_review.id


def test_filter_by_non_existing_reviewer(
    client,
    person_review_with_reviewer,
):
    review_admin = person_review_with_reviewer['admin']
    response = helpers.get_json(
        client,
        path=CHECKING_URL,
        login=review_admin.login,
        request=dict(reviewer=review_admin.login),
    )
    assert not response['person_reviews']


def test_filter_with_several_reviewers_on_one_lvl(
    client,
    person_review_with_reviewer,
    person_review_role_builder,
):
    target_person_review = person_review_with_reviewer['target_person_review']
    review_admin = person_review_with_reviewer['admin']
    another_reviewer = person_review_role_builder(
        type=const.ROLE.PERSON_REVIEW.REVIEWER,
        position=0,
        person_review=target_person_review,
    ).person
    response = helpers.get_json(
        client,
        path=CHECKING_URL,
        login=review_admin.login,
        request=dict(reviewer=another_reviewer.login),
    )
    person_reviews = response['person_reviews']
    target_person_review = person_review_with_reviewer['target_person_review']
    assert len(person_reviews) == 1 and person_reviews[0]['id'] == target_person_review.id


@pytest.fixture
def reviewing_subordination_structure(
    department_child_builder,
    department_root_builder,
    department_role_builder,
    person_builder,
    person_review_builder,
    review_builder,
):
    root_dep = department_root_builder()
    child_dep = department_child_builder(parent=root_dep)
    root_chief = person_builder(department=root_dep)
    department_role_builder(
        department=root_dep,
        person=root_chief,
        type=staff_const.STAFF_ROLE.DEPARTMENT.HEAD,
    )
    root_empl = person_builder(department=root_dep)
    child_chief = person_builder(department=child_dep)
    department_role_builder(
        department=child_dep,
        person=child_chief,
        type=staff_const.STAFF_ROLE.DEPARTMENT.HEAD,
    )
    child_empl = person_builder(department=child_dep)

    review = review_builder()
    for person in (root_chief, root_empl, child_chief, child_empl):
        person_review_builder(review=review, person=person)

    return dict(
        root_chief=root_chief,
        root_empl=root_empl,
        child_chief=child_chief,
        child_empl=child_empl,
    )


def test_filter_by_subordination_subject_only(
    client,
    reviewing_subordination_structure,
):
    root_chief = reviewing_subordination_structure['root_chief']
    child_chief = reviewing_subordination_structure['child_chief']
    child_empl = reviewing_subordination_structure['child_empl']
    response = helpers.get_json(
        client,
        path=CHECKING_URL,
        login=root_chief.login,
        request=dict(subordination_subject=child_chief.login),
    )
    person_reviews = response['person_reviews']
    is_for_employee = person_reviews[0]['person']['login'] == child_empl.login
    assert len(person_reviews) == 1 and is_for_employee


SUBORDINATION = staff_const.SUBORDINATION


def test_filter_by_subordination_only(
    client,
    reviewing_subordination_structure,
):
    root_chief = reviewing_subordination_structure['root_chief']
    child_chief = reviewing_subordination_structure['child_chief']
    child_empl = reviewing_subordination_structure['child_empl']
    response = helpers.get_json(
        client,
        path=CHECKING_URL,
        login=root_chief.login,
        request=dict(subordination=SUBORDINATION.VERBOSE[SUBORDINATION.INDIRECT]),
    )
    person_reviews = response['person_reviews']
    expecting_logins = (child_empl.login, child_chief.login)
    received_logins = [pr['person']['login'] for pr in person_reviews]
    received_logins_correct = all(login in expecting_logins for login in received_logins)
    assert len(person_reviews) == len(received_logins) and received_logins_correct


@pytest.mark.parametrize(
    'subordination_type,subject,expecting_persons',
    [
        (SUBORDINATION.DIRECT, 'root_chief', ['root_empl']),
        (SUBORDINATION.INDIRECT, 'root_chief', ['child_chief', 'child_empl']),
        (SUBORDINATION.ANY, 'root_chief', ['root_empl', 'child_chief', 'child_empl']),
        (SUBORDINATION.DIRECT, 'child_chief', ['child_empl']),
        (SUBORDINATION.INDIRECT, 'child_chief', []),
        (SUBORDINATION.ANY, 'child_chief', ['child_empl']),
    ]
)
def test_filter_by_subordination_and_subject(
    client,
    reviewing_subordination_structure,
    subordination_type,
    subject,
    expecting_persons,
):
    root_chief = reviewing_subordination_structure['root_chief']
    response = helpers.get_json(
        client,
        path=CHECKING_URL,
        login=root_chief.login,
        request=dict(
            subordination=SUBORDINATION.VERBOSE[subordination_type],
            subordination_subject=reviewing_subordination_structure[subject].login,
        )
    )
    person_reviews = response['person_reviews']
    expecting_logins = [
        reviewing_subordination_structure[prsn].login
        for prsn in expecting_persons
    ]
    received_logins = [pr['person']['login'] for pr in person_reviews]
    received_logins_correct = all(login in expecting_logins for login in received_logins)
    assert len(person_reviews) == len(received_logins) and received_logins_correct


@pytest.mark.parametrize(
    "params, filter_positive, filter_negative",
    TESTS_PARAMETRIZATION,
)
def test_person_review_filters_with_no_access_to_field(
    params,
    filter_positive,
    filter_negative,
    client,
    person_builder,
    review_builder,
    person_review_builder,
):
    dummy_person = person_builder(login=DUMMY_LOGIN)
    subject_person = person_builder(login=SUBJECT_LOGIN)
    review = review_builder(**{
        key: const.REVIEW_MODE.MODE_MANUAL
        for key in list(const.REVIEW_MODE.FIELDS_TO_MODIFIERS.values())
    })
    person_review = person_review_builder(
        review=review,
        **params
    )
    positive_result = helpers.get_json(
        client=client,
        login=dummy_person.login,
        path=CHECKING_URL,
        request=filter_positive,
    )
    negative_result = helpers.get_json(
        client=client,
        login=dummy_person.login,
        path=CHECKING_URL,
        request=filter_negative
    )

    assert len(positive_result['person_reviews']) == 0
    assert len(negative_result['person_reviews']) == 0


def test_b_and_b_star_are_together_cia_991(
    client,
    review_role_superreviewer,
    person_review_builder,
):
    review = review_role_superreviewer.review
    person_reviews = {}
    for mark in (
        'A',
        'B',
        'B*',
    ):
        person_reviews[mark] = person_review_builder(
            review=review,
            mark=mark
        )

    result = helpers.get_json(
        client=client,
        login=review_role_superreviewer.person.login,
        path=CHECKING_URL,
        request={'mark': ['B']},
    )

    helpers.assert_ids_equal(
        result['person_reviews'],
        [
            person_reviews['B'],
            person_reviews['B*'],
        ]
    )


@pytest.mark.parametrize(
    "params, filter_positive, filter_negative",
    TESTS_PARAMETRIZATION,
)
def test_person_review_filters_ids_only(
    params,
    filter_positive,
    filter_negative,
    client,
    person_builder,
    review_builder,
    review_role_builder,
    person_review_builder,
    person_review_role_builder,
):
    checking_url = '/frontend/person-reviews/ids-only/'
    dummy_person = person_builder(login=DUMMY_LOGIN)
    subject_person = person_builder(login=SUBJECT_LOGIN)
    review = review_builder(**{
        key: const.REVIEW_MODE.MODE_MANUAL
        for key in const.REVIEW_MODE.FIELDS_TO_MODIFIERS.values()
    })
    review_role_builder(
        review=review,
        type=const.ROLE.REVIEW.ADMIN,
        person=subject_person,
    )
    person_review = person_review_builder(
        review=review,
        **params
    )
    top_reviewer_role = person_review_role_builder(
        person=subject_person,
        person_review=person_review,
        position=0,
        type=const.ROLE.PERSON_REVIEW.TOP_REVIEWER,
    )
    positive_result = helpers.get_json(
        client=client,
        login=subject_person.login,
        path=checking_url,
        request=filter_positive,
    )
    negative_result = helpers.get_json(
        client=client,
        login=subject_person.login,
        path=checking_url,
        request=filter_negative
    )
    assert len(positive_result['person_review_ids']) == 1
    assert positive_result['person_review_ids'][0] == person_review.id
    assert len(negative_result['person_review_ids']) == 0
