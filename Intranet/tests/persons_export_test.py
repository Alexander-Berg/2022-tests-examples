from mock import patch

from django_mock_queries.query import MockSet, MockModel

from staff.departments.models import DepartmentRoles
from staff.departments.tree.persons_export import ExportForm, PersonsExport


# region TestData

class DepartmentStub(object):
    tree_id = 1
    lft = 1


department_chains = {
    4210: [
        {'id': 1, 'name': 'Yandex'},
        {'id': 4112, 'name': 'Search Portal'},
        {'id': 4210, 'name': 'Machine intelligence and research department'}
    ]
}

people = MockSet(
    MockModel(
        id=1,
        login='alice',
        department=DepartmentStub(),
        department_id=4210,
        is_big_boss=True
    ),
    MockModel(
        id=2,
        login='bob',
        department=DepartmentStub(),
        department_id=4210,
        is_big_boss=False
    ),
)

hr_partners_and_chiefs = {
    4210: [
        {
            'first_name': 'Carol',
            'last_name': 'Last',
            'login': 'carol',
            'middle_name': '',
            'role': DepartmentRoles.HR_PARTNER.value,
        },
        {
            'first_name': 'Chuck',
            'last_name': 'Norris',
            'login': 'chuck',
            'middle_name': '',
            'role': DepartmentRoles.CHIEF.value,
        },
    ]
}

hr_partners_and_chiefs_for_search_portal = {
    4112: [
        {
            'first_name': 'Carol',
            'last_name': 'Last',
            'login': 'carol',
            'middle_name': '',
            'role': DepartmentRoles.HR_PARTNER.value,
        },
        {
            'first_name': 'Chuck',
            'last_name': 'Norris',
            'login': 'chuck',
            'middle_name': '',
            'role': DepartmentRoles.CHIEF.value,
        },
    ]
}

hr_partners_and_chiefs_query_set = MockSet(
    MockModel(
        department_id=4210,
        role=DepartmentRoles.HR_PARTNER.value,
        role_id=DepartmentRoles.HR_PARTNER.value,
        staff=MockModel(
            login='carol',
            first_name='Carol',
            first_name_en='Carol',
            last_name='Last',
            last_name_en='Last',
            middle_name=''
        )
    ),
    MockModel(
        department_id=4210,
        role=DepartmentRoles.CHIEF.value,
        role_id=DepartmentRoles.CHIEF.value,
        staff=MockModel(
            login='chuck',
            first_name='Chuck',
            first_name_en='Chuck',
            last_name='Norris',
            last_name_en='Norris',
            middle_name=''
        )
    ),
)

multiple_hr_partners = {
    4210: [
        {
            'first_name': 'Carol',
            'last_name': 'First',
            'login': 'carol',
            'middle_name': '',
            'role': DepartmentRoles.HR_PARTNER.value,
        },
        {
            'first_name': 'Eve',
            'last_name': 'Last',
            'login': 'eve',
            'middle_name': '',
            'role': DepartmentRoles.HR_PARTNER.value,
        },
    ]
}


# endregion

class FilterContextMock(object):
    filter_id = 1


def test_get_hr_partner():
    form = ExportForm(data={
        'lang': 'en'
    })
    form.is_valid()

    person_export = PersonsExport(None, None, form)
    person_export.department_chains = department_chains
    person_export.hr_partners_and_chiefs = hr_partners_and_chiefs

    for p in people:
        hr_login, hr_name = person_export.get_hr_partner(p)
        assert hr_login == hr_partners_and_chiefs[4210][0]['login']


def test_get_hr_partner_from_ancestors():
    form = ExportForm(data={
        'lang': 'en'
    })
    form.is_valid()

    person_export = PersonsExport(None, None, form)
    person_export.department_chains = department_chains
    person_export.hr_partners_and_chiefs = hr_partners_and_chiefs_for_search_portal

    for p in people:
        hr_login, hr_name = person_export.get_hr_partner(p)
        assert hr_login == hr_partners_and_chiefs_for_search_portal[4112][0]['login']


def test_get_multiple_hr_partner():
    form = ExportForm(data={
        'lang': 'en'
    })
    form.is_valid()

    person_export = PersonsExport(None, None, form)
    person_export.department_chains = department_chains
    person_export.hr_partners_and_chiefs = multiple_hr_partners

    for p in people:
        hr_logins, hr_names = person_export.get_hr_partner(p)
        hr_logins_list = hr_logins.split(',')
        for hr_login in hr_logins_list:
            assert any(h for h in multiple_hr_partners[4210] if h['login'] == hr_login)


def test_get_chief():
    form = ExportForm(data={
        'lang': 'en'
    })
    form.is_valid()

    person_export = PersonsExport(None, None, form)
    person_export.department_chains = department_chains
    person_export.hr_partners_and_chiefs = hr_partners_and_chiefs

    for p in people:
        chief_login, chief_name = person_export.get_chief(p)
        assert chief_login == hr_partners_and_chiefs[4210][1]['login']


@patch('staff.departments.models.DepartmentStaff.objects', hr_partners_and_chiefs_query_set)
def test_hr_partner_extra():
    person_export = PersonsExport(FilterContextMock(), None, None)
    person_export.department_chains = department_chains

    person_export.hr_partner_extra(people)

    assert person_export.hr_partners_and_chiefs == hr_partners_and_chiefs


@patch('staff.departments.models.DepartmentStaff.objects', hr_partners_and_chiefs_query_set)
def test_chief_extra():
    person_export = PersonsExport(FilterContextMock(), None, None)
    person_export.department_chains = department_chains

    person_export.chief_extra(people)

    assert person_export.hr_partners_and_chiefs == hr_partners_and_chiefs
