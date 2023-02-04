import json

from mock import patch
import pytest

from django.conf import settings
from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.departments.models import DepartmentRoles
from staff.lib.testing import (
    DepartmentFactory,
    DepartmentStaffFactory,
    GroupFactory,
    GroupMembershipFactory,
    OfficeFactory,
    OrganizationFactory,
    StaffFactory,
)
from staff.person.models import Staff, LANG, EMPLOYMENT

from staff.preprofile.models import CITIZENSHIP, FORM_TYPE, Preprofile, PREPROFILE_STATUS, CANDIDATE_TYPE
from staff.preprofile.tests.utils import add_hardware_profile, post_new_form, post_new_form_api, PreprofileFactory
from staff.preprofile.utils import try_create_masshire_preprofiles_by_parsed_data
from staff.preprofile.views import adopt, approve, edit_form, new_form, new_form_api
from staff.preprofile.views.import_views import outstaff_mass_import, preprofile_tuples_to_dict, PreprofileFileCell


@pytest.fixture
def outstaff_dep(company):
    result = DepartmentFactory(
        id=settings.OUTSTAFF_DEPARTMENT_ID,
        parent=company.dep2,
        name='outstaff',
        code='outstaff',
        url='yandex_dep2_outstaff',
    )
    GroupFactory(type=1, name='Outstaff', url='outstaff', department=result)
    return result


@pytest.fixture
def achievery_robot(db):
    achievery_owner_robot = StaffFactory(
        is_robot=True,
        login=settings.ACHIEVERY_ROBOT_PERSON_LOGIN,
    )
    GroupMembershipFactory(
        staff=achievery_owner_robot,
        group__url='achieveryadmin'
    )


@pytest.mark.django_db()
def test_outstaff_form_correct_data_saving(company, rf, tester, base_form):
    outstaff_dep = company.outstaff
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))

    form_data = base_form
    form_data.update({
        'department': outstaff_dep.url,
        'position_staff_text': '1',
        'login': 'testlogin',
        'office': OfficeFactory(name='officename').id,
        'citizenship': CITIZENSHIP.UKRAINIAN,
        'organization': OrganizationFactory().id,
        'lang_ui': LANG.RU,
        'hardware_profile': add_hardware_profile(outstaff_dep).id,
    })

    request = post_new_form(rf, FORM_TYPE.OUTSTAFF, form_data)
    DepartmentStaffFactory(department=outstaff_dep, staff=tester.get_profile(), role_id=DepartmentRoles.CHIEF.value)
    request.user = tester

    result = new_form(request, FORM_TYPE.OUTSTAFF)

    assert result.status_code == 200
    assert json.loads(result.content) == {'id': Preprofile.objects.get().id}

    preprofile = Preprofile.objects.get()
    assert outstaff_dep == preprofile.department
    assert form_data['login'] == preprofile.login
    assert PREPROFILE_STATUS.NEW == preprofile.status
    assert EMPLOYMENT.FULL == preprofile.employment_type


@pytest.fixture
def some_row():
    return [
        1,
        'some_tag',
        '',
        'Энифамилия',
        'Эниимя',
        'Эниотчество',
        'Anyname',
        'Anysurname',
        '',
        'M',
        'a@a.a',
        '+71234567890',
        'nowhere',
        1,
        'https://staff.yandex-team.ru/department/yandex',
        'somepos',
        'Российское',
    ]


@pytest.fixture
def file_to_parse(some_row):
    header_row = [
        '№ п/п',
        'Тег массового найма* ',
        'Анкета в Наниматоре',
        'Фамилия',
        'Имя',
        'Отчество',
        'First Name',
        'Last Name',
        'Login',
        'Пол',
        'E-mail',
        'Телефон *',
        'Адрес',
        'ID юридического лица',
        'Подразделение',
        'Должность',
        'Гражданство',
    ]
    return ([*map(PreprofileFileCell, row)] for row in (header_row, some_row))


@pytest.fixture
def parsed_data(some_row):
    asdict = dict(zip(
        (
            'masshire_tag',
            'id',
            'last_name',
            'first_name',
            'middle_name',
            'first_name_en',
            'last_name_en',
            'login',
            'gender',
            'email',
            'phone',
            'address',
            'organization_id',
            'department',
            'position',
            'citizenship',
        ), some_row[1:]
    ))
    asdict['department'] = 'yandex'
    return [asdict]


def test_converting_tuples(file_to_parse, parsed_data):
    assert preprofile_tuples_to_dict(file_to_parse) == parsed_data


@pytest.mark.django_db
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_mass_create_preprofile_data(dns_mock, validate_in_ldap, parsed_data, company):
    recruiter = StaffFactory()
    created_data = try_create_masshire_preprofiles_by_parsed_data(parsed_data, recruiter)
    created_id = created_data[0].get('id')
    assert created_id
    preprofile = Preprofile.objects.get(id=created_id)
    replacing = {
        'id': created_id,
        'citizenship': CITIZENSHIP.RUSSIAN,
        'department': company.yandex,
    }
    for field, value in created_data[0].items():
        if field in replacing:
            value = replacing[field]
        assert getattr(preprofile, field) == value
    assert preprofile.office_id == settings.HOMIE_OFFICE_ID
    assert preprofile.candidate_type == CANDIDATE_TYPE.NEW_EMPLOYEE
    assert preprofile.form_type == FORM_TYPE.MASS_HIRE
    assert preprofile.employment_type == EMPLOYMENT.FULL
    assert preprofile.recruiter == recruiter


@pytest.mark.django_db
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_mass_update_preprofile_data(dns_mock, validate_in_ldap, parsed_data, company):
    recruiter = StaffFactory()
    created_data = try_create_masshire_preprofiles_by_parsed_data(parsed_data, recruiter)
    new_pos = 'shiny new position'
    created_data[0]['position'] = new_pos
    try_create_masshire_preprofiles_by_parsed_data(created_data, recruiter)
    created_id = created_data[0].get('id')
    assert created_id
    assert Preprofile.objects.get(id=created_id).position == new_pos


@pytest.mark.django_db
@patch('staff.preprofile.login_validation.validate_for_dns')
@patch('staff.preprofile.login_validation.validate_in_ldap')
def test_mass_update_preprofile_data_with_wrong_login(dns_mock, validate_in_ldap, parsed_data, company):
    recruiter = StaffFactory()
    created_data = try_create_masshire_preprofiles_by_parsed_data(parsed_data, recruiter)
    old_login = created_data[0]['login']
    created_data[0]['login'] = 'any'
    try_create_masshire_preprofiles_by_parsed_data(created_data, recruiter)
    created_id = created_data[0].get('id')
    assert created_id
    assert Preprofile.objects.get(id=created_id).login == old_login


@pytest.mark.django_db
def test_outstaff_form_incorrect_data_saving_attempt(company, rf, tester, base_form):
    outstaff_dep = company.outstaff
    form_data = base_form
    form_data.update({
        'department': outstaff_dep.url,
        'position_staff_text': '1',
        'login': 'testlogin',
        'office': OfficeFactory(name='officename').id,
        'citizenship': CITIZENSHIP.UKRAINIAN,
        'organization': OrganizationFactory().id,
        'lang_ui': LANG.RU,
        'hardware_profile': add_hardware_profile(outstaff_dep).id,
    })

    request = post_new_form(rf, FORM_TYPE.OUTSTAFF, form_data)
    request.user = tester

    # 1-st attempt
    result = new_form(request, FORM_TYPE.OUTSTAFF)

    assert result.status_code == 403
    assert json.loads(result.content) == {'errors': [{'message': 'forbidden'}]}

    # 2-nd attempt
    del tester._perm_cache
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))

    result = new_form(request, FORM_TYPE.OUTSTAFF)
    assert result.status_code == 400
    assert json.loads(result.content) == {
        'errors': {
            'department': [{'code': 'invalid_choice'}]
        }
    }

    # 3-rd attempt
    DepartmentStaffFactory(department=outstaff_dep, staff=tester.get_profile(), role_id=DepartmentRoles.CHIEF.value)

    result = new_form(request, FORM_TYPE.OUTSTAFF)
    assert 200 == result.status_code
    assert {'id': Preprofile.objects.get().id} == json.loads(result.content)


def test_outstaff_form_adopting(company, rf, achievery_robot):
    outstaff_dep = company.outstaff
    recruiter = company.persons['dep2-chief']
    preprofile = PreprofileFactory(
        department_id=outstaff_dep.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=recruiter,
        login='testlogin123',
        form_type=FORM_TYPE.OUTSTAFF,
        status=PREPROFILE_STATUS.READY,
        uid='3784671538462',
        guid='2983746723543',
    )
    recruiter.user.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    recruiter.user.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))

    request = rf.post(
        reverse(
            'preprofile:adopt',
            kwargs={'preprofile_id': preprofile.id}
        )
    )
    request.user = recruiter.user
    response = adopt(request, preprofile_id=preprofile.id)
    assert 200 == response.status_code
    assert Staff.objects.filter(login='testlogin123').exists()


@pytest.mark.django_db
@pytest.mark.parametrize('candidate_type, can_be_saved', (
    (CANDIDATE_TYPE.NEW_EMPLOYEE, True),
    (CANDIDATE_TYPE.FORMER_EMPLOYEE, True),
))
def test_outstaff_form_saving_without_login(company, rf, tester, base_form, candidate_type, can_be_saved):
    outstaff_dep = company.outstaff
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))

    form_data = base_form
    form_data.update({
        'department': outstaff_dep.url,
        'position_staff_text': '1',
        'office': OfficeFactory(name='officename').id,
        'citizenship': CITIZENSHIP.UKRAINIAN,
        'organization': OrganizationFactory().id,
        'lang_ui': LANG.RU,
        'hardware_profile': add_hardware_profile(outstaff_dep).id,
        'candidate_type': candidate_type,
    })

    del form_data['login']

    request = post_new_form(rf, FORM_TYPE.OUTSTAFF, form_data)
    DepartmentStaffFactory(department=outstaff_dep, staff=tester.get_profile(), role_id=DepartmentRoles.CHIEF.value)
    request.user = tester

    result = new_form(request, FORM_TYPE.OUTSTAFF)
    if can_be_saved:
        assert result.status_code == 200
        assert json.loads(result.content) == {'id': Preprofile.objects.get().id}

        preprofile = Preprofile.objects.get()
        assert outstaff_dep == preprofile.department
        assert preprofile.login is None
        assert PREPROFILE_STATUS.NEW == preprofile.status
        assert EMPLOYMENT.FULL == preprofile.employment_type
    else:
        assert result.status_code == 400
        assert json.loads(result.content) == {
            'errors': {
                'login': [{'code': 'required'}]
            }
        }


@pytest.mark.django_db
def test_outstaff_form_saving_from_api_with_permission(company, rf, tester, base_form):
    outstaff_dep = company.outstaff
    tester.user_permissions.add(Permission.objects.get(codename='can_create_outstaff_by_api'))

    form_data = base_form
    form_data.update({
        'department': outstaff_dep.url,
        'position_staff_text': '1',
        'office': OfficeFactory(name='officename').id,
        'citizenship': CITIZENSHIP.UKRAINIAN,
        'organization': OrganizationFactory().id,
        'hardware_profile': add_hardware_profile(outstaff_dep).id,
        'candidate_type': CANDIDATE_TYPE.NEW_EMPLOYEE,
    })

    request = post_new_form_api(rf, FORM_TYPE.OUTSTAFF, form_data)
    DepartmentStaffFactory(department=outstaff_dep, staff=tester.get_profile(), role_id=DepartmentRoles.CHIEF.value)
    request.user = tester

    result = new_form_api(request, FORM_TYPE.OUTSTAFF)

    assert result.status_code == 200
    preprofile = Preprofile.objects.get()
    assert json.loads(result.content) == {'id': preprofile.id}

    assert preprofile.department == outstaff_dep
    assert preprofile.login == form_data['login']
    assert preprofile.status == PREPROFILE_STATUS.NEW
    assert preprofile.employment_type == EMPLOYMENT.FULL


@pytest.mark.django_db
def test_outstaff_form_saving_from_api_without_permission(company, rf, tester, base_form):
    outstaff_dep = company.outstaff
    form_data = base_form

    request = post_new_form_api(rf, FORM_TYPE.OUTSTAFF, form_data)
    DepartmentStaffFactory(department=outstaff_dep, staff=tester.get_profile(), role_id=DepartmentRoles.CHIEF.value)
    request.user = tester

    result = new_form_api(request, FORM_TYPE.OUTSTAFF)

    assert result.status_code == 403
    assert json.loads(result.content) == {'errors': [{'message': 'access_denied'}]}


@pytest.mark.django_db()
def test_outstaff_form_can_be_saved_without_candidate_type(company, rf, tester, base_form):
    outstaff_dep = company.outstaff
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))

    form_data = base_form
    form_data.update({
        'department': outstaff_dep.url,
        'position_staff_text': '1',
        'office': OfficeFactory(name='officename').id,
        'citizenship': CITIZENSHIP.UKRAINIAN,
        'organization': OrganizationFactory().id,
        'lang_ui': LANG.RU,
        'hardware_profile': add_hardware_profile(outstaff_dep).id,
    })

    del form_data['candidate_type']

    request = post_new_form(rf, FORM_TYPE.OUTSTAFF, form_data)
    DepartmentStaffFactory(department=outstaff_dep, staff=tester.get_profile(), role_id=DepartmentRoles.CHIEF.value)
    request.user = tester

    result = new_form(request, FORM_TYPE.OUTSTAFF)

    assert 400 == result.status_code
    assert {'errors': {'candidate_type': [{'code': 'required'}]}} == json.loads(result.content)


@pytest.mark.django_db()
def test_outstaff_approve_action_is_not_allowed_without_login(company, rf):
    outstaff_dep = company.outstaff
    recruiter = company.persons['dep2-chief']
    preprofile = PreprofileFactory(
        department_id=outstaff_dep.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=recruiter,
        login=None,
        form_type=FORM_TYPE.OUTSTAFF,
        status=PREPROFILE_STATUS.NEW,
        uid='3784671538462',
        guid='2983746723543',
    )
    recruiter.user.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}))
    request.user = recruiter.user
    response = edit_form(request, preprofile_id=preprofile.id)
    assert 200 == response.status_code

    content = json.loads(response.content)

    assert not content['actions']['approve']


@pytest.mark.django_db()
def test_outstaff_approve_not_working_without_login(rf, company):
    outstaff_dep = company.outstaff
    recruiter = company.persons['dep2-chief']
    preprofile = PreprofileFactory(
        department_id=outstaff_dep.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=recruiter,
        login=None,
        form_type=FORM_TYPE.OUTSTAFF,
        status=PREPROFILE_STATUS.NEW,
        uid='3784671538462',
        guid='2983746723543',
    )
    recruiter.user.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    recruiter.user.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))

    request = rf.post(reverse('preprofile:approve', kwargs={'preprofile_id': preprofile.id}))
    request.user = recruiter.user
    response = approve(request, preprofile_id=preprofile.id)
    assert 400 == response.status_code
    assert {'errors': [{'message': 'not_applicable'}]} == json.loads(response.content)


@pytest.mark.django_db()
def test_sip_fields_is_false_by_default(rf, tester):
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))
    request = rf.get(reverse('preprofile:new_form', kwargs={'form_type': FORM_TYPE.OUTSTAFF}))
    request.user = tester

    result = new_form(request, FORM_TYPE.OUTSTAFF)
    assert 200 == result.status_code

    result = json.loads(result.content)
    assert not result['structure']['need_internal_phone']['value']
    assert not result['structure']['need_sip_redirect']['value']


@pytest.mark.django_db
def test_outstaff_mass_import_without_permission(rf, tester):
    request = rf.post(reverse('preprofile:outstaff_mass_import'))
    request.user = tester

    response = outstaff_mass_import(request)
    assert response.status_code == 302


@pytest.mark.django_db
def test_outstaff_mass_import_with_permission(rf, tester):
    request = rf.post(reverse('preprofile:outstaff_mass_import'))
    request.user = tester
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))

    response = outstaff_mass_import(request)
    assert response.status_code == 400
