import pytest
import mock
from decimal import Decimal

from review.core import const as core_const
from review.core import models as core_models
from review.lib import datetimes
from review.oebs import const as oebs_const
from review.staff import (
    models,
    const as staff_const,
)
from review.oebs.sync import fake

from tests import helpers


GLOBAL_ROLES = core_const.ROLE.GLOBAL


def test_api_finance_get_as_head(test_person, client, person_builder, finance_builder):
    models.DepartmentRole.objects.create(
        person=test_person,
        department=test_person.department,
        type=core_const.ROLE.DEPARTMENT.HEAD,
    )
    persons = [
        person_builder(department=test_person.department) for _ in range(3)
    ]
    for person in persons:
        finance_builder(
            person=person,
            generate_fields=oebs_const.OEBS_DATA_TYPES,
        )

    request = {
        'persons': [p.login for p in persons],
        'fields': list(oebs_const.OEBS_DATA_TYPES),
    }
    result = helpers.get_json(client, '/v1/finance/', request)

    assert len(result['result']) == len(persons)

    for login, data in result['result'].items():
        for grant in data['options_history']:
            assert grant['grantExercisableUnits'] == 0, login


def test_api_finance_get_as_self(test_person, client, finance_builder):
    fields = [oebs_const.CURRENT_SALARY, oebs_const.OPTION_HISTORY]
    fake_data = fake.generate_data(
        data_types=oebs_const.OEBS_DATA_TYPES,
        logins=[test_person.login],
    )
    finance_builder(person=test_person, **fake_data[test_person.login])

    result = helpers.get_json(
        client=client,
        path='/v1/finance/',
        request={'persons': [test_person.login], 'fields': fields}
    )

    fake_fte = fake_data[test_person.login][oebs_const.CURRENT_SALARY]['factFTE']
    expected = {
        'result': {
            test_person.login: {
                oebs_const.CURRENT_SALARY: {
                    'factFTE': fake_fte,
                },
            },
        },
    }
    helpers.assert_is_substructure(expected, result)

    for login, data in result['result'].items():
        for grant in data['options_history']:
            assert grant['grantExercisableUnits'] == 1, login


@pytest.mark.parametrize(
    'role_type', [
        staff_const.STAFF_ROLE.HR.HR_ANALYST,
        staff_const.STAFF_ROLE.HR.HR_PARTNER,
    ]
)
def test_api_finance_get_as_hr(
    client,
    finance_builder,
    person_builder_bulk,
    test_person,
    role_type,
):
    persons = person_builder_bulk(_count=3)
    models.HR.objects.bulk_create(
        models.HR(
            cared_person=person,
            hr_person=test_person,
            type=role_type,
        )
        for person in persons
    )

    logins = [p.login for p in persons]
    fake_data = fake.generate_data(oebs_const.OEBS_DATA_TYPES, logins)
    for person in persons:
        finance_builder(person=person, **fake_data[person.login])

    request = {
        'persons': [p.login for p in persons],
        'fields': list(oebs_const.OEBS_DATA_TYPES),
    }
    top_analysts_module_path = 'review.finance.permissions.TOP_ANALYSTS'
    with mock.patch(top_analysts_module_path, {test_person.login}):
        result = helpers.get_json(client, '/v1/finance/', request)

    assert len(result['result']) == len(persons)

    for login, data in result['result'].items():
        for grant in data['options_history']:
            assert grant['grantExercisableUnits'] == 0, login


def test_api_finance_wo_top_analyst_role(
    client,
    finance_builder,
    person_builder_bulk,
    test_person,
):
    persons = person_builder_bulk(_count=3)
    models.HR.objects.bulk_create(
        models.HR(
            cared_person=person,
            hr_person=test_person,
            type=staff_const.STAFF_ROLE.HR.HR_ANALYST,
        )
        for person in persons
    )

    logins = [p.login for p in persons]
    fake_data = fake.generate_data(oebs_const.OEBS_DATA_TYPES, logins)
    for person in persons:
        finance_builder(person=person, **fake_data[person.login])

    request = {
        'persons': [p.login for p in persons],
        'fields': list(oebs_const.OEBS_DATA_TYPES),
    }
    result = helpers.get_json(client, '/v1/finance/', request)

    assert len(result['result']) == len(persons)

    for login, data in result['result'].items():
        for grant in data['options_history']:
            assert data['options_history'] == core_const.NO_ACCESS, data


def test_api_finance_no_access(client, finance_builder, person_builder):
    person = person_builder()
    finance_builder(
        person=person,
        generate_fields=oebs_const.OEBS_DATA_TYPES,
    )

    request = {
        'persons': [person.login],
        'fields': list(oebs_const.OEBS_DATA_TYPES),
    }
    result = helpers.get_json(client, '/v1/finance/', request)['result']

    assert len(result) == 1
    result = result[person.login]
    oebs_fields = result.keys() & oebs_const.OEBS_DATA_TYPES
    assert len(oebs_fields) == len(oebs_const.OEBS_DATA_TYPES)
    for field in oebs_fields:
        assert result[field] == core_const.NO_ACCESS, field


TO_RUB = Decimal('0.03')
TO_USD = Decimal('32')

RSU_AMOUNT = 20
RSU_CURRENCY = 'USD'
RSU_CUR_PRICE = 80
RSU_PRICE_FOR_USER = 0

SMT_AMOUNT = 100500
SMT_CURRRENCY = 'RUB'
SMT_CUR_PRICE = 110
SMT_PRICE_FOR_USER = 100


@pytest.fixture
def create_vesting_scenario(
    bi_assignment_builder,
    bi_currency_coversion_builder,
    bi_income_builder,
    finance_builder,
):
    def builder(person, grade):
        today = datetimes.today()
        rsu_per_vest = RSU_AMOUNT / 4
        smt_per_vest = SMT_AMOUNT / 2

        rsu_vest = [
            fake.build_vesting(rsu_per_vest, datetimes.shifted(today, days=1)),
            fake.build_vesting(rsu_per_vest, datetimes.shifted(today, days=100)),
            fake.build_vesting(9999, today),
            fake.build_vesting(999999, datetimes.shifted(today, days=-100)),
        ]

        smt_vest = [
            fake.build_vesting(smt_per_vest, datetimes.shifted(today, days=1)),
            fake.build_vesting(smt_per_vest, datetimes.shifted(today, days=100)),
            fake.build_vesting(9999, today),
            fake.build_vesting(999999, datetimes.shifted(today, days=-100)),
        ]
        options_history = [
            fake.build_grant(
                vest,
                type_='RSU',
                cur_price=RSU_CUR_PRICE,
                price_for_user=RSU_PRICE_FOR_USER,
                currency=RSU_CURRENCY,
            )
            for vest in [rsu_vest] * 2
        ] + [
            fake.build_grant(
                vest,
                type_='SMT',
                cur_price=SMT_CUR_PRICE,
                price_for_user=SMT_PRICE_FOR_USER,
                currency=SMT_CURRRENCY,
            )
            for vest in [smt_vest]
        ]
        bi_income_builder(person=person, ext_data={'GRADE_MAIN': str(grade)})

        finance_builder(
            person=person,
            generate_fields=oebs_const.OEBS_DATA_TYPES - {oebs_const.OPTION_HISTORY},
            options_history=options_history,
        )

        org_id = 666
        bi_assignment_builder(person=person, organization_id=org_id)
        rates = [
            ['USD', 'RUB', TO_RUB],
            ['RUB', 'USD', TO_USD]
        ]
        for from_, to, rate in rates:
            bi_currency_coversion_builder(
                organization_id=org_id,
                from_currency=from_,
                to_currency=to,
                conversion_date=today,
                rate=rate,
            )

    return builder


@pytest.fixture
def loan_viewer_case(global_role_builder, person_builder, test_person):
    global_role_builder(
        type=core_const.ROLE.GLOBAL.LOAN_VIEWER,
        person=test_person,
    )
    return person_builder()


@pytest.fixture
def chief_case(department_role_builder, person_builder, test_person):
    target_person = person_builder()
    department_role_builder(
        person=test_person,
        type=staff_const.STAFF_ROLE.DEPARTMENT.HEAD,
        department=target_person.department,
    )
    return target_person


@pytest.fixture
def hr_partner_case(department_role_builder, person_builder, test_person):
    target_person = person_builder()
    department_role_builder(
        person=test_person,
        type=staff_const.STAFF_ROLE.HR.HR_PARTNER,
        department=target_person.department,
    )
    return target_person


@pytest.fixture
def self_case(test_person):
    return test_person


def _get_finances(client, create_vesting_scenario, person, grade=18):
    today = datetimes.today()
    with mock.patch('review.lib.datetimes.today', return_value=today):
        create_vesting_scenario(person, grade)
        return helpers.get_json(
            client,
            '/v1/finance/',
            {'persons': [person.login]}
        )['result']


@pytest.mark.parametrize('person_to_show_fixture', [
    'loan_viewer_case',
    'chief_case',
    'hr_partner_case',
    'self_case',
])
def test_get_not_vested(
    client,
    create_vesting_scenario,
    request,
    person_to_show_fixture,
):
    person_to_show = request.getfixturevalue(person_to_show_fixture)

    finances = _get_finances(
        client,
        create_vesting_scenario,
        person_to_show,
    )

    assert list(finances) == [person_to_show.login]
    person_data = finances[person_to_show.login]
    assert person_data['has_not_vested']
    rsu_usd = RSU_AMOUNT * (RSU_CUR_PRICE - RSU_PRICE_FOR_USER)
    rsu_rub = rsu_usd * TO_RUB
    smt_rub = SMT_AMOUNT * (SMT_CUR_PRICE - SMT_PRICE_FOR_USER)
    smt_usd = smt_rub * TO_USD
    assert len(person_data['not_vested']) == 2
    for it in person_data['not_vested']:
        if it['type'] == 'RSU':
            assert it['amount'] == RSU_AMOUNT
            assert float(it['in_usd']) == float(rsu_usd)
            assert float(it['in_rub']) == float(rsu_rub)
        elif it['type'] == 'SMT':
            assert it['amount'] == SMT_AMOUNT
            assert float(it['in_usd']) == float(smt_usd)
            assert float(it['in_rub']) == float(smt_rub)


@pytest.mark.parametrize('grade', [
    19,
    0,
    -1,
    'abrakadabra',
])
def test_get_not_vested_no_access(
    client,
    create_vesting_scenario,
    loan_viewer_case,
    grade,
):
    person_to_show = loan_viewer_case
    finances = _get_finances(
        client,
        create_vesting_scenario,
        person_to_show,
        grade=grade,
    )

    assert list(finances) == [person_to_show.login]
    person_data = finances[person_to_show.login]
    assert person_data['has_not_vested']
    assert not person_data['not_vested']


@pytest.mark.parametrize('field', [
    'salary_history',
    'grade_history',
    'bonus_history',
    'options_history',
    'current_salary',
    'current_loans',
    'social_package',
])
def test_get_loan_viewer_forbidden_fields(
    client,
    create_vesting_scenario,
    loan_viewer_case,
    field
):
    person_to_show = loan_viewer_case
    finances = _get_finances(
        client,
        create_vesting_scenario,
        person_to_show,
    )

    assert list(finances) == [person_to_show.login]
    person_data = finances[person_to_show.login]
    assert person_data[field] == core_const.NO_ACCESS


@pytest.mark.parametrize('field', [
    'has_not_vested',
    'not_vested',
])
def test_hr_analyst_not_see_vested(
    client,
    create_vesting_scenario,
    department_role_builder,
    person_builder,
    test_person,
    field,
):
    target_person = person_builder()
    department_role_builder(
        person=test_person,
        type=staff_const.STAFF_ROLE.HR.HR_ANALYST,
        department=target_person.department,
    )
    finances = _get_finances(
        client,
        create_vesting_scenario,
        target_person,
    )

    assert list(finances) == [target_person.login]
    person_data = finances[target_person.login]
    assert person_data[field] is None
