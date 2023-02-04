import pytest
from dateutil import parser
from unittest import mock

from tests import helpers
from tests.fixtures_for_bi import (
    INCOME_PARSED_DATA,
    INCOME_PARSED_DATA_LONG_FORECAST,
)

from review.core import const
from review.lib import errors
from tests.helpers import waffle_switch


def test_api_bi_get_person_assignment(test_person, client, bi_assignment_builder):
    assignment = bi_assignment_builder(person=test_person)

    result = helpers.get_json(
        client=client,
        login=test_person.login,
        path='/v1/bi-assignments/',
    )

    expected = {
        'id': assignment.id,
        'currency': 'UAH',
        'rate': 0.7,
        'position': 'Журналист',
        'contract_number': '№ 011/12/0010 от 11.01.2012',
        'organization_id': 123,
        'organization': 'ООО Яндекс.Технологии',
        'report_date': '2020-01-31',
        'number': '2590-23664',
        'main': True,
        'rate_fact': 1.0,
        'contr_end_date': '2020-01-31',
    }
    assert len(result['assignments']) == 1
    assert result['assignments'][0] == expected


def test_api_bi_get_person_assignment_nothing(test_person, client):
    result = helpers.get_json(
        client=client,
        login=test_person.login,
        path='/v1/bi-assignments/',
    )
    assert len(result['assignments']) == 0


def test_api_bi_get_person_assignment_list(department_role_head, person_builder, department_root_builder,
                                           client, bi_assignment_builder, bi_income_builder):
    head = department_role_head.person

    employee = person_builder(department=department_role_head.department)
    assignment = bi_assignment_builder(person=employee)
    bi_income_builder(person=employee)

    employee_2 = person_builder(department=department_role_head.department)
    bi_assignment_builder(person=employee_2)
    bi_income_builder(person=employee_2)

    employee_from_another_department = person_builder(department=department_root_builder())
    bi_assignment_builder(person=employee_from_another_department)
    bi_income_builder(person=employee_from_another_department)

    with helpers.assert_num_queries(4):
        result = helpers.post_json(
            client=client,
            login=head.login,
            path='/v1/bi-assignments-list/',
            request={'logins': ','.join([
                employee.login,
                employee_2.login,
                employee_from_another_department.login,
                'non_existent_login',
            ])},
        )
    assert set(result.keys()) == {'_auth', employee.login, employee_2.login}

    expected = [{
        'organization_id': 123,
        'currency': 'UAH',
        'rate': 0.7,
        'position': 'Журналист',
        'main': True,
        'contract_number': '№ 011/12/0010 от 11.01.2012',
        'id': assignment.id,
        'organization': 'ООО Яндекс.Технологии',
        'report_date': '2020-01-31',
        'number': '2590-23664',
        'rate_fact': 1.0,
        'contr_end_date': '2020-01-31',
    }]
    assert result[employee.login] == expected


def test_api_bi_get_person_income(test_person, client, bi_income_builder, bi_detailed_incomes_builder, bi_vesting_builder, marks_scale_builder):
    helpers.waffle_switch('bi_income_show_long_forecast')

    bi_income_builder(person=test_person)
    bi_detailed_incomes_builder(person=test_person)
    bi_vesting_builder(person=test_person)
    marks_scale = marks_scale_builder()

    result = helpers.get_json(
        client=client,
        login=test_person.login,
        path='/v1/bi-income/',
    )

    expected = {
        'currency': 'UAH',
        'salary': 232340.0,
        'avg_salary': 100500.0,
        'mark_last': {'mark': 'E', 'scale_id': 1},
        'grade_last': 14,
        'mark_current': {'mark': 'good', 'text_value': '+', 'value': 1021, 'scale_id': marks_scale.id},
        'grade_current': 15,
        'grade_main': 15,
        'gold_pay_last': False,
        'gold_pay_curr': False,
        'gold_opt_last': False,
        'gold_opt_curr': True,
        'up_last': 1,
        'up_current': -1,
        'income': INCOME_PARSED_DATA,
    }
    for key in expected:
        assert result[key] == expected[key]


def test_api_bi_person_income_long_forecast_for_finance_viewer(
        client, bi_income_builder, bi_detailed_incomes_builder, bi_vesting_builder,
        hr_builder, person_builder):

    helpers.waffle_switch('bi_income_show_long_forecast')

    person = person_builder()
    bi_income_builder(person=person)
    bi_detailed_incomes_builder(person=person)
    bi_vesting_builder(person=person)

    finance_viewer = person_builder()
    hr_builder(
        cared_person=person,
        hr_person=finance_viewer,
        type=const.ROLE.DEPARTMENT.FINANCE_VIEWER,
    )

    result = helpers.get_json(
        client=client,
        login=finance_viewer.login,
        path='/v1/bi-income/',
        request={'login': person.login},
    )

    for n in range(len(INCOME_PARSED_DATA_LONG_FORECAST['per_calendar_year'])):
        assert result['income']['per_calendar_year'][n] == INCOME_PARSED_DATA_LONG_FORECAST['per_calendar_year'][n]

    for n in range(len(INCOME_PARSED_DATA_LONG_FORECAST['from_current_date'])):
        assert result['income']['from_current_date'][n] == INCOME_PARSED_DATA_LONG_FORECAST['from_current_date'][n]

    for n in range(len(INCOME_PARSED_DATA_LONG_FORECAST['detailed'])):
        assert result['income']['detailed'][n] == INCOME_PARSED_DATA_LONG_FORECAST['detailed'][n]


def test_api_bi_get_person_income_list(department_role_head, person_builder, department_root_builder,
                                       client, bi_income_builder, bi_detailed_incomes_builder,
                                       bi_vesting_builder, marks_scale_builder):
    head = department_role_head.person
    marks_scale_builder()

    employee_one = person_builder(department=department_role_head.department)
    bi_income_builder(person=employee_one)
    bi_detailed_incomes_builder(person=employee_one)
    bi_vesting_builder(person=employee_one)

    employee_two = person_builder(department=department_role_head.department)
    bi_income_builder(person=employee_two)
    bi_detailed_incomes_builder(person=employee_two)
    bi_vesting_builder(person=employee_two)

    employee_from_another_department = person_builder(department=department_root_builder())
    bi_income_builder(person=employee_from_another_department)
    bi_detailed_incomes_builder(person=employee_from_another_department)
    bi_vesting_builder(person=employee_from_another_department)

    with helpers.assert_num_queries(6):
        result = helpers.post_json(
            client=client,
            login=head.login,
            path='/v1/bi-income-list/',
            request={'logins': ','.join([
                employee_one.login,
                employee_two.login,
                employee_from_another_department.login,
                'non_existent_login',
            ])},
        )

    expected = {
        employee_one.login: {
            'salary': 232340.0,
            'currency': 'UAH',
            'income': INCOME_PARSED_DATA,
        },
        employee_two.login: {
            'salary': 232340.0,
            'currency': 'UAH',
            'income': INCOME_PARSED_DATA,
        },
    }

    salary_one = result[employee_one.login]['salary']
    salary_two = result[employee_two.login]['salary']
    assert salary_one == expected[employee_one.login]['salary']
    assert salary_two == expected[employee_two.login]['salary']

    income_one = result[employee_one.login]['income']
    income_two = result[employee_two.login]['income']

    assert income_one['per_calendar_year'] == expected[employee_one.login]['income']['per_calendar_year']
    assert income_one['from_current_date'] == expected[employee_one.login]['income']['from_current_date']
    assert income_one['detailed'] == []  # в списковой ручке не отдаём детализацию

    assert income_two['per_calendar_year'] == expected[employee_two.login]['income']['per_calendar_year']
    assert income_two['from_current_date'] == expected[employee_two.login]['income']['from_current_date']
    assert income_two['detailed'] == []  # в списковой ручке не отдаём детализацию


def test_get_lte_18_grade_persons_ids(person_builder, bi_income_builder):
    from review.api.views.bi import get_lte_18_grade_persons_ids

    employee1 = person_builder()
    bi_income_builder(person=employee1, ext_data={'GRADE_MAIN': '16'})
    employee2 = person_builder()
    bi_income_builder(person=employee2, ext_data={'GRADE_MAIN': '17'})
    employee3 = person_builder()
    bi_income_builder(person=employee3, ext_data={'GRADE_MAIN': '18'})
    employee4 = person_builder()
    bi_income_builder(person=employee4, ext_data={'GRADE_MAIN': '19'})
    employee5 = person_builder()
    bi_income_builder(person=employee5, ext_data={'GRADE_MAIN': '20'})

    persons_ids = get_lte_18_grade_persons_ids([
        employee1.login,
        employee2.login,
        employee3.login,
        employee4.login,
        employee5.login,
    ])

    assert persons_ids == [
        employee1.id,
        employee2.id,
        employee3.id,
    ]


def test_get_allowed_qs_observer_is_finance_viewer(person_builder, hr_builder, bi_income_builder):
    from review.api.views.bi import get_allowed_qs

    observer = person_builder()

    person1 = person_builder()
    bi_income_builder(person=person1)
    hr_builder(cared_person=person1, hr_person=observer, type=const.ROLE.DEPARTMENT.FINANCE_VIEWER)

    person2 = person_builder()
    bi_income_builder(person=person2)

    qs = get_allowed_qs(user=observer, observables_logins=[person1.login, person2.login])

    assert list(qs.values_list('login', flat=True)) == [person1.login]


def test_get_allowed_qs_observer_is_self(department_role_head, person_builder, department_root_builder, bi_income_builder):
    from review.api.views.bi import get_allowed_qs

    observer = person_builder()

    bi_income_builder(person=observer)
    person2 = person_builder()
    bi_income_builder(person=person2)

    qs = get_allowed_qs(user=observer, observables_logins=[observer.login, person2.login])

    assert list(qs.values_list('login', flat=True)) == [observer.login]


def test_get_allowed_qs_observer_is_head(department_role_head, person_builder, department_root_builder, bi_income_builder):
    from review.api.views.bi import get_allowed_qs

    observer = department_role_head.person

    employee = person_builder(department=department_role_head.department)
    bi_income_builder(person=employee)

    employee_from_another_department = person_builder(department=department_root_builder())
    bi_income_builder(person=employee_from_another_department)

    qs = get_allowed_qs(user=observer, observables_logins=[employee.login, employee_from_another_department.login])

    assert list(qs.values_list('login', flat=True)) == [employee.login]


def test_get_allowed_qs_observer_wo_role(person_builder, bi_income_builder):
    from review.api.views.bi import get_allowed_qs

    observer = person_builder()

    person1 = person_builder()
    bi_income_builder(person=person1)
    person2 = person_builder()
    bi_income_builder(person=person2)

    assert not get_allowed_qs(user=observer, observables_logins=[person1.login, person2.login])


def test_get_allowed_qs_observer_as_bi_viewer(person_builder, global_role_builder):
    from review.api.views.bi import get_allowed_qs

    observer = person_builder()
    global_role_builder(person=observer, type=const.ROLE.GLOBAL.BI_VIEWER)

    logins = {person_builder().login for _ in range(3)}

    query = get_allowed_qs(user=observer, observables_logins=logins)
    assert set(query.values_list('login', flat=True)) == logins


def test_get_observable_person(person_builder, bi_income_builder):
    from review.api.views.bi import get_observable_person

    observer = person_builder()
    assert get_observable_person(observer, observer.login) == observer

    with pytest.raises(errors.NotFound):
        assert get_observable_person(observer, 'no_such_login_ever_anywhere')

    person = person_builder()
    with pytest.raises(errors.PermissionDenied):
        assert get_observable_person(observer, person.login)


@pytest.mark.parametrize(
    'ext_income_data,today,expected_report_date',
    [
        ({'ANNOUNCED_FLAG': 'Y'}, '04/30/2022', '05/01/2022'),
        ({'ANNOUNCED_FLAG': 'Y'}, '02/28/2022', '02/28/2022'),
        ({'ANNOUNCED_FLAG': 'Y'}, '09/30/2022', '09/30/2022'),
        ({'ANNOUNCED_FLAG': 'Y'}, '08/30/2022', '08/30/2022'),
        ({'ANNOUNCED_FLAG': 'Y'}, '05/10/2022', '05/01/2022'),
        ({'ANNOUNCED_FLAG': 'Y'}, '11/10/2022', '11/01/2022'),
        ({'ANNOUNCED_FLAG': 'N'}, '10/30/2022', '10/30/2022'),
        ({'ANNOUNCED_FLAG': 'N'}, '04/30/2022', '04/30/2022'),
        ({}, '03/31/2022', '03/31/2022'),
    ]
)
def test_api_report_date_depends_on_announcement_flag(
    test_person,
    client,
    bi_assignment_builder,
    bi_income_builder,
    ext_income_data,
    today,
    expected_report_date,
):
    waffle_switch('enable_review_announcement_report_date')
    bi_income_builder(person=test_person, ext_data=ext_income_data)
    bi_assignment_builder(person=test_person, ext_data={'REPORT_DATE': today})

    with mock.patch('django.utils.timezone.now', return_value=parser.parse(today)):
        result = helpers.get_json(
            client=client,
            login=test_person.login,
            path='/v1/bi-assignments/',
        )

    assert len(result['assignments']) == 1
    assert result['assignments'][0]['report_date'] == expected_report_date
