import datetime
import json
from pretend import stub
import pytest

from review.lib import (
    datetimes,
    encryption,
)
from review.core import const
from review.staff import const as staff_const
from review.oebs import const as oebs_const
from tests import helpers
from django.utils.dateparse import parse_datetime


@pytest.fixture
def person_review_data(
    department_child_builder,
    person_builder,
    person_review_builder,
    review_role_builder,
    review_builder,
):
    super_reviewer = person_builder()

    employee = person_builder()
    review = review_builder(
        mark_mode=const.REVIEW_MODE.MODE_MANUAL,
        goldstar_mode=const.REVIEW_MODE.MODE_MANUAL,
        level_change_mode=const.REVIEW_MODE.MODE_MANUAL,
        salary_change_mode=const.REVIEW_MODE.MODE_MANUAL,
        bonus_mode=const.REVIEW_MODE.MODE_MANUAL,
        options_rsu_mode=const.REVIEW_MODE.MODE_MANUAL,
        deferred_payment_mode=const.REVIEW_MODE.MODE_MANUAL,
    )
    review_role_builder(
        person=super_reviewer,
        review=review,
        type=const.ROLE.REVIEW.SUPERREVIEWER,
    )
    person_review = person_review_builder(
        status=const.PERSON_REVIEW_STATUS.ANNOUNCED,
        review=review,
        person=employee,
        tag_average_mark='test tag_average_mark',
        bonus_rsu=500,
        taken_in_average=True,
        deferred_payment=100500,
    )
    return stub(
        super_reviewer=super_reviewer,
        person_review=person_review,
        review=review,
    )


@pytest.fixture
def department_chain(
    department_child_builder,
    person_builder,
    person_review_data,
):
    upper_person = person_builder()
    parent_department = upper_person.department
    child_department = department_child_builder(parent=parent_department)
    employee = person_review_data.person_review.person
    employee.department = child_department
    employee.save()
    return [parent_department, child_department]


@pytest.fixture
def finance_info(
    person_review_data,
    finance_builder,
):
    today = datetimes.today().strftime('%Y-%m-%d')
    value = 100
    currency = 'RUB'
    level = 10
    profession = 'Other'
    person_review = person_review_data.person_review
    finance = finance_builder(
        person=person_review.person,
        salary_history=[dict(
            salarySum=value,
            currency=currency,
            basis='MONTHLY',
            dateFrom='2010-01-01',
            dateTo=today,
        )],
        grade_history=[dict(
            gradeName='{}.{}.3'.format(profession, level),
            dateFrom='2010-01-01',
            dateTo=today,
        )],
        generate_fields=[oebs_const.CURRENT_SALARY]
    )
    cur_salary = json.loads(encryption.decrypt(finance.current_salary))
    return dict(
        value=value,
        currency=currency,
        level=level,
        profession=profession,
        fte=cur_salary['factFTE'],
    )


def _fetch_person_review(client, person_review_data, fields=(), login=None):
    url = ''.join((
        '/frontend/person-reviews-mode-review/?ids=',
        str(person_review_data.person_review.id),
        ''.join('&fields={}'.format(f) for f in fields),
    ))

    if login is None:
        login = person_review_data.super_reviewer.login

    response = helpers.get_json(
        client,
        login=login,
        path=url,
    )
    return response['person_reviews'][0]


def _fetch_person_review_subobject(
    client,
    person_review_data,
    subobject,
    fields=(),
):
    fields = ('_'.join((subobject, f)) for f in fields)
    pr = _fetch_person_review(client, person_review_data, fields)
    return pr[subobject]


def test_get_id(
    client,
    person_review_data,
):
    pr = _fetch_person_review(client, person_review_data)
    assert pr['id'] == person_review_data.person_review.id


@pytest.mark.parametrize(
    'field,translator', [
        ('status', const.PERSON_REVIEW_STATUS.VERBOSE),
        ('goldstar', const.GOLDSTAR.VERBOSE),
        ('bonus_type', const.SALARY_DEPENDENCY_TYPE.VERBOSE),
        ('salary_change_type', const.SALARY_DEPENDENCY_TYPE.VERBOSE),
    ]
)
def test_get_translatable(
    client,
    person_review_data,
    field,
    translator,
):
    received_pr = _fetch_person_review(client, person_review_data)
    expecting = translator[getattr(person_review_data.person_review, field)]
    assert received_pr[field] == expecting


@pytest.mark.parametrize(
    'field', [
        'bonus',
        'bonus_absolute',
        'salary_change',
        'salary_change_absolute',
        'level_change',
        'flagged',
        'flagged_positive',
        'bonus_rsu',
        'deferred_payment',
        'options_rsu',
        'tag_average_mark',
        'taken_in_average',
    ]
)
def test_get_simple_field(
    client,
    person_review_data,
    field,
):
    received_pr = _fetch_person_review(client, person_review_data)
    expecting = getattr(person_review_data.person_review, field)
    assert received_pr[field] == expecting


def test_get_umbrella(
    client,
    person_review_data,
    main_product_builder,
    umbrella_builder,
):
    person_review = person_review_data.person_review
    mp = main_product_builder()
    umbrella = umbrella_builder(main_product=mp)
    person_review.umbrella = umbrella
    person_review.save()

    received_pr = _fetch_person_review(client, person_review_data)
    assert received_pr['umbrella'] == {
        'id': umbrella.id,
        'name': umbrella.name,
        'main_product': {
            'id': mp.id,
            'name': mp.name,
            'abc_service_id': mp.abc_service_id,
        }
    }


def test_get_main_product(
    client,
    person_review_data,
    main_product_builder,
):
    person_review = person_review_data.person_review
    mp = main_product_builder()
    person_review.main_product = mp
    person_review.save()

    received_pr = _fetch_person_review(client, person_review_data)
    assert received_pr['main_product'] == {
        'id': mp.id,
        'name': mp.name,
        'abc_service_id': mp.abc_service_id,
    }


@pytest.mark.parametrize(
    'action_name',
    [
        const.PERSON_REVIEW_ACTIONS.UMBRELLA,
        const.PERSON_REVIEW_ACTIONS.MAIN_PRODUCT,
    ],
)
@pytest.mark.parametrize('role_type', [const.ROLE.REVIEW.ACCOMPANYING_HR, const.ROLE.REVIEW.SUPERREVIEWER])
@pytest.mark.parametrize('review_status', [const.REVIEW_STATUS.IN_PROGRESS, const.REVIEW_STATUS.FINISHED])
def test_get_gradient_actions_ok(
    action_name,
    role_type,
    review_status,
    client,
    person_builder,
    person_review_builder,
    review_role_builder,
    review_builder,
):
    person = person_builder()
    review = review_builder(status=review_status)
    person_review = person_review_builder(review=review)
    review_role_builder(person=person, review=review, type=role_type)

    response = helpers.get_json(
        client,
        login=person.login,
        path=''.join((
            '/frontend/person-reviews-mode-review/?ids=',
            str(person_review.id),
            '&fields=_all_',
        ))
    )

    assert response['person_reviews'][0]['actions'][action_name] == const.OK


@pytest.mark.parametrize(
    'action_name',
    [
        const.PERSON_REVIEW_ACTIONS.UMBRELLA,
        const.PERSON_REVIEW_ACTIONS.MAIN_PRODUCT,
    ],
)
@pytest.mark.parametrize('role_type', const.ROLE.REVIEW.ALL)
@pytest.mark.parametrize('review_status', [const.REVIEW_STATUS.ARCHIVE])
def test_get_gradient_actions_wrong_status(
    action_name,
    role_type,
    review_status,
    client,
    person_builder,
    person_review_builder,
    review_role_builder,
    review_builder,
):
    person = person_builder()
    review = review_builder(status=review_status)
    person_review = person_review_builder(review=review)
    review_role_builder(person=person, review=review, type=role_type)

    response = helpers.get_json(
        client,
        login=person.login,
        path=''.join((
            '/frontend/person-reviews-mode-review/?ids=',
            str(person_review.id),
            '&fields=_all_',
        ))
    )

    assert response['person_reviews'][0]['actions'][action_name] == const.NO_ACCESS


@pytest.mark.parametrize(
    'action_name',
    [
        const.PERSON_REVIEW_ACTIONS.UMBRELLA,
        const.PERSON_REVIEW_ACTIONS.MAIN_PRODUCT,
    ],
)
@pytest.mark.parametrize('role_type', [const.ROLE.REVIEW.ADMIN])
@pytest.mark.parametrize(
    'review_status',
    [
        const.REVIEW_STATUS.IN_PROGRESS,
        const.REVIEW_STATUS.FINISHED,
        const.REVIEW_STATUS.ARCHIVE,
    ],
)
def test_get_gradient_actions_wrong_role(
    action_name,
    role_type,
    review_status,
    client,
    person_builder,
    person_review_builder,
    review_role_builder,
    review_builder,
):
    person = person_builder()
    review = review_builder(status=review_status)
    person_review = person_review_builder(review=review)
    review_role_builder(person=person, review=review, type=role_type)

    response = helpers.get_json(
        client,
        login=person.login,
        path=''.join((
            '/frontend/person-reviews-mode-review/?ids=',
            str(person_review.id),
            '&fields=_all_',
        ))
    )

    assert response['person_reviews'][0]['actions'][action_name] == const.NO_ACCESS


@pytest.mark.parametrize(
    'product_schema_loaded_review_field,expected',
    [
        (datetime.date(2020, 1, 1), True),
        (None, False),
    ]
)
def test_product_schema_loaded(
    product_schema_loaded_review_field,
    expected,
    client,
    person_review_data,
):
    person_review_data.review.product_schema_loaded = product_schema_loaded_review_field
    person_review_data.review.save()

    received_pr = _fetch_person_review(client, person_review_data)

    assert received_pr['product_schema_loaded'] == expected


def test_get_kpi_loaded(
    client,
    person_review_data,
):
    person_review = person_review_data.person_review
    review = person_review.review

    received_pr = _fetch_person_review(client, person_review_data)

    received_date = parse_datetime(received_pr['review']['kpi_loaded']).replace(microsecond=0)
    expected = review.kpi_loaded.replace(microsecond=0)
    assert received_date == expected


def test_get_kpi_loaded_none(
    client,
    person_review_data,
):
    helpers.update_model(
        person_review_data.review,
        kpi_loaded=None,
    )

    received_pr = _fetch_person_review(client, person_review_data)

    assert received_pr['review']['kpi_loaded'] is None


@pytest.mark.parametrize(
    'field', [
        'tag_average_mark',
        'mark',
        'goldstar',
        'level_change',
        'salary_change',
        'salary_change_absolute',
        'bonus',
        'bonus_absolute',
        'bonus_rsu',
        'options_rsu',
        'flagged',
        'flagged_positive',
        'comment',
        'reviewers',
        'taken_in_average',
    ]
)
def test_get_action_field_ok(
    client,
    person_review_data,
    field,
):
    received_pr = _fetch_person_review(client, person_review_data)
    assert received_pr['actions'][field] == const.OK


@pytest.mark.parametrize(
    'field', [
        'approve',
        'unapprove',
        'announce',
        'allow_announce',
    ]
)
def test_get_action_field_no_access(
    client,
    person_review_data,
    field,
):
    received_pr = _fetch_person_review(client, person_review_data)
    assert received_pr['actions'][field] == const.NO_ACCESS


@pytest.mark.parametrize(
    'field,role_const,has_access', [
        ('taken_in_average', const.ROLE.REVIEW.SUPERREVIEWER, True),
        ('taken_in_average', const.ROLE.REVIEW.ACCOMPANYING_HR, True),
        ('taken_in_average', const.ROLE.REVIEW.ADMIN, False),
    ]
)
def test_get_action_field_access_for_role(
    client,
    person_review_data,
    person_builder,
    review_role_builder,
    field,
    role_const,
    has_access,
):
    person = person_builder()
    review_role_builder(
        person=person,
        review=person_review_data.review,
        type=role_const,
    )
    received_pr = _fetch_person_review(client, person_review_data, login=person.login)
    if has_access:
        assert received_pr['actions'][field] == const.OK
    else:
        assert received_pr['actions'][field] == const.NO_ACCESS


@pytest.mark.parametrize(
    'field', [
        'id',
        'name',
        'start_date',
        'finish_date',
        'evaluation_from_date',
        'evaluation_to_date',
        'scale_id',
        'options_rsu_unit',
    ]
)
def test_get_review_field(
    client,
    person_review_data,
    field,
):
    received_review = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='review', fields=[field],
    )
    expected = getattr(person_review_data.person_review.review, field)
    received = received_review[field]
    if isinstance(expected, datetime.date) and not isinstance(received, datetime.date):
        received = datetime.datetime.strptime(received, '%Y-%m-%d').date()
    if field == 'options_rsu_unit':
        expected = const.REVIEW_OPTIONS_RSU_UNIT.VERBOSE[expected]
    assert received == expected


@pytest.mark.parametrize(
    'field', [
        'id',
        'slug',
        'path',
    ]
)
def test_get_department_field(
    client,
    person_review_data,
    field,
):
    target_field = 'department_{}'.format(field)
    received_person = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='person', fields=[target_field],
    )
    person = person_review_data.person_review.person
    expecting = getattr(person.department, field)
    assert received_person[target_field] == expecting


def test_get_department_name(
    client,
    person_review_data,
):
    target_field = 'department_name'
    received_person = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='person', fields=[target_field],
    )
    person = person_review_data.person_review.person
    expecting = [
        getattr(person.department, 'name_{}'.format(locale))
        for locale in ('en', 'ru')
    ]
    assert received_person[target_field] in expecting


def test_get_salary_field(
    client,
    person_review_data,
    finance_info,
):
    salary = _fetch_person_review_subobject(client, person_review_data, 'salary')
    assert salary['currency'] == finance_info['currency']
    assert salary['value'] == finance_info['value']


def test_get_salary_after_review_field(
    client,
    person_review_data,
    finance_info,
):
    salary = _fetch_person_review_subobject(client, person_review_data, 'salary_after_review')
    salary_after_review = finance_info['value'] + person_review_data.person_review.salary_change_absolute
    assert salary['currency'] == finance_info['currency']
    assert salary['value'] == salary_after_review


def test_get_finance_meta_field(
    client,
    person_review_data,
    finance_info,
):
    fields = ('profession', 'level', 'fte')
    received_pr = _fetch_person_review(client, person_review_data, fields)
    assert all(finance_info[f] == received_pr[f] for f in fields)


def test_get_person_chief(
    client,
    person_review_data,
    department_role_builder,
    department_chain,
):
    expecting_chief = department_chain[0].persons.get()
    department_role_builder(
        person=expecting_chief,
        type=staff_const.STAFF_ROLE.DEPARTMENT.HEAD,
    )

    received_person = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='person', fields=['chief'],
    )
    received_chief = received_person['chief']
    assert received_chief['is_dismissed'] == expecting_chief.is_dismissed
    assert received_chief['login'] == expecting_chief.login
    assert received_chief['gender'] == expecting_chief.gender
    assert received_chief['first_name'] in (expecting_chief.first_name_en,
                                            expecting_chief.first_name_ru)
    assert received_chief['last_name'] in (expecting_chief.last_name_en,
                                           expecting_chief.last_name_ru)


def test_get_department_chain_slugs(
    client,
    person_review_data,
    department_chain,
):

    expected_slugs_chain = [dep.slug for dep in department_chain]
    target_field = 'department_chain_slugs'
    received_person = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='person', fields=[target_field],
    )
    assert received_person[target_field] == expected_slugs_chain


def test_get_department_chain_names(
    client,
    person_review_data,
    department_chain,
):

    expected_names_chain = [
        [dep.slug for dep in department_chain]
        for locale in ('ru', 'en')
    ]
    target_field = 'department_chain_names'
    received_person = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='person', fields=[target_field],
    )
    assert received_person[target_field] in expected_names_chain


@pytest.mark.parametrize(
    'field,localized', [
        ('id', False),
        ('login', False),
        ('is_dismissed', False),
        ('first_name', True),
        ('last_name', True),
        ('city_name', True),
    ]
)
def test_get_person_field(
    client,
    person_review_data,
    field,
    localized,
):
    received_person = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='person', fields=[field],
    )
    person_review = person_review_data.person_review
    if localized:
        expecting = [
            getattr(person_review.person,
                    '_'.join((field, locale)))
            for locale in ('ru', 'en')
        ]
    else:
        expecting = [getattr(person_review.person, field)]
    assert received_person[field] in expecting


def test_get_person_gender(
    client,
    person_review_data,
):
    received_person = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='person', fields=['gender'],
    )
    db_gender = person_review_data.person_review.person.gender
    assert received_person['gender'] == staff_const.GENDER.VERBOSE[db_gender]


def test_get_goals_url(
    client,
    person_review_data,
):
    pr = _fetch_person_review(
        client,
        person_review_data,
        fields=(
            'goals_url',
            'st_goals_url',
        )
    )
    assert pr['goals_url'].startswith('https://goals')
    assert pr['st_goals_url'].startswith('https://st-api')


@pytest.mark.parametrize(
    'evaluation_type', [
        'goals',
        'feedback',
    ]
)
def get_fb_goals_from_to_dates_evaluation_dates_set(
    client,
    person_review_data,
    evaluation_type,
):
    dummy_from = '2010-01-01'
    dummy_to = datetimes.shifted(dummy_from, days=10)
    helpers.update_model(
        person_review_data.review,
        evaluation_date_from=dummy_from,
        evaluation_date_to=dummy_to,
    )
    received_review = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='review',
        fields=[
            'review_{}_from_date'.format(evaluation_type),
            'review_{}_to_date'.format(evaluation_type),
        ],
    )

    assert received_review['{}_from_date'.format(evaluation_type)] == dummy_from
    assert received_review['{}_to_date'.format(evaluation_type)] == dummy_to


@pytest.mark.parametrize(
    'evaluation_type', [
        'goals',
        'feedback',
    ]
)
def get_fb_goals_from_to_dates_evaluation_dates_not_set(
    client,
    person_review_data,
    evaluation_type,
):
    dummy_start = '2010-01-01'
    helpers.update_model(
        person_review_data.review,
        evaluation_date_from=None,
        evaluation_date_to=None,
        start_date=dummy_start
    )
    received_review = _fetch_person_review_subobject(
        client, person_review_data,
        subobject='review',
        fields=[
            'review_{}_from_date'.format(evaluation_type),
            'review_{}_to_date'.format(evaluation_type),
        ],
    )

    expected_from = datetimes.shifted(dummy_start, months=-6).isoformat()
    assert received_review['{}_from_date'.format(evaluation_type)] == expected_from
    assert received_review['{}_to_date'.format(evaluation_type)] == dummy_start
