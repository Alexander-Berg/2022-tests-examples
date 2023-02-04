import datetime
from datetime import date, timedelta
import json
import mock

import pytest
from django.conf import settings
from waffle.models import Switch

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.person.models import GENDER, LANG
from staff.departments.tests.factories import ProposalMetadataFactory
from staff.departments.edit.proposal_mongo import MONGO_COLLECTION_NAME
from staff.departments.models.department import Department, DepartmentRoles
from staff.lib.testing import (
    DepartmentFactory,
    DepartmentStaffFactory,
    OfficeFactory,
    OrganizationFactory,
    RoomFactory,
    StaffFactory,
)
from staff.map.models import ROOM_TYPES

from staff.preprofile.login_validation import LOGIN_VALIDATION_ERROR
from staff.preprofile.models import (
    NEED_TABLE,
    EMAIL_DOMAIN,
    FORM_TYPE,
    Preprofile,
    PREPROFILE_STATUS,
    CANDIDATE_TYPE,
)
from staff.preprofile.tests.utils import post_new_form, add_hardware_profile, PreprofileFactory, post_new_form_api
from staff.preprofile.views import (
    approve,
    adopt,
    check_login_availability,
    edit_form,
    new_form,
    new_form_api,
    notification_list,
    create_link,
    get_departments_ids_by_hr_partner,
    get_proposals_rotations,
    get_femida_rotations,
)


@pytest.mark.django_db()
def test_empty_post_returns_400(rf, tester):
    request = rf.post(
        reverse('preprofile:new_form', kwargs={'form_type': FORM_TYPE.EMPLOYEE}),
        '',
        content_type='application/json'
    )
    request.user = tester

    result = new_form(request, FORM_TYPE.EMPLOYEE)

    assert result.status_code == 400


@pytest.mark.django_db()
def test_right_department_has_no_errors(rf, tester, company, base_form):
    form = base_form
    form.update({
        'department': 'yandex',
        'position_staff_text': '1',
    })

    request = post_new_form(rf, FORM_TYPE.EMPLOYEE, form)
    request.user = tester

    result = new_form(request, FORM_TYPE.EMPLOYEE)

    result = json.loads(result.content)
    assert 'errors' not in result or 'department' not in result['errors']


@pytest.mark.django_db()
def test_right_services_has_no_errors(rf, tester, company, abc_services, base_form):
    form = base_form
    form.update({
        'department': 'yandex',
        'abc_services': abc_services,
        'position_staff_text': '1',
    })

    request = post_new_form(rf, FORM_TYPE.EMPLOYEE, form)
    request.user = tester

    result = new_form(request, FORM_TYPE.EMPLOYEE)

    result = json.loads(result.content)
    assert 'errors' not in result or 'abc_services' not in result['errors']


@pytest.mark.django_db()
def test_employee_can_be_edited(rf, tester, company, base_form, abc_services, red_rose_office):
    recruiter = StaffFactory()
    department = company.yandex

    preprofile = PreprofileFactory(
        department_id=department.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=red_rose_office,
        recruiter=recruiter,
    )

    form = base_form
    form.update({
        'department': 'yandex',
        'need_table': NEED_TABLE.ALREADY_HAVE,
        'abc_services': abc_services,
        'hardware_profile': add_hardware_profile(department).id,
    })

    request = rf.post(
        reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}),
        json.dumps(form),
        content_type='application/json',
    )
    request.user = tester
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    result = edit_form(request, preprofile.id)

    result = json.loads(result.content)
    assert 'errors' not in result
    assert Preprofile.objects.get(id=preprofile.id).need_table == NEED_TABLE.ALREADY_HAVE
    assert 'id' in result
    assert result['id'] == preprofile.id


@pytest.mark.django_db()
def test_right_filled_robot_form_can_be_saved(company, rf, tester, base_form, abc_services, robot_staff_user):
    department = company.virtual
    form = base_form
    form.update({
        'department': department.url,
        'position_staff_text': '1',
        'approved_by': company.persons['yandex-chief'].login,
        'abc_services': abc_services,
        'login': 'robot-some',
        'responsible': tester.staff.login,
        'hardware_profile': add_hardware_profile(department).id,
    })

    request = post_new_form(rf, FORM_TYPE.ROBOT, form)
    request.user = tester

    result = new_form(request, FORM_TYPE.ROBOT)

    result = json.loads(result.content)
    assert 'errors' not in result
    assert 'id' in result
    assert Preprofile.objects.count() == 1
    assert Preprofile.objects.first().status == PREPROFILE_STATUS.APPROVED


@pytest.mark.django_db()
@pytest.mark.parametrize(
    'form_type, perm',
    [
        (FORM_TYPE.ROBOT, 'can_create_robot_by_api'),
        (FORM_TYPE.ZOMBIE, 'can_create_zombie_by_api'),
    ]
)
def test_robots_form_can_be_saved_from_api_with_perms(company, rf, tester, base_form, abc_services, form_type, perm):
    StaffFactory(login=settings.ROBOT_STAFF_LOGIN)
    tester.user_permissions.add(Permission.objects.get(codename=perm))

    department = {FORM_TYPE.ROBOT: company.virtual, FORM_TYPE.ZOMBIE: company.virtual_robot}[form_type]
    login = {FORM_TYPE.ROBOT: 'robot-some', FORM_TYPE.ZOMBIE: 'zomb-some'}[form_type]

    form = base_form
    form.update({
        'department': department.url,
        'position_staff_text': '1',
        'approved_by': company.persons['yandex-chief'].login,
        'abc_services': abc_services,
        'login': login,
        'responsible': tester.staff.login,
    })
    if form_type == FORM_TYPE.ROBOT:
        form['hardware_profile'] = add_hardware_profile(department).id
    elif form_type == FORM_TYPE.ZOMBIE:
        form['zombie_hw'] = 'Pentium MMX'

    request = post_new_form_api(rf, form_type, form)
    request.user = tester

    result = new_form_api(request, form_type)
    assert result.status_code == 200
    assert 'id' in json.loads(result.content)


@pytest.mark.django_db()
@pytest.mark.parametrize('form_type', (FORM_TYPE.ROBOT, FORM_TYPE.ZOMBIE))
def test_robots_form_can_be_saved_from_api_without_permissions(rf, tester, base_form, form_type):
    request = post_new_form_api(rf, form_type, base_form)
    request.user = tester

    result = new_form_api(request, form_type)
    assert result.status_code == 403
    assert json.loads(result.content) == {'errors': [{'message': 'access_denied'}]}


@pytest.mark.django_db()
def test_right_filled_zombie_form_can_be_saved(company, rf, tester, base_form, abc_services, robot_staff_user):
    form = base_form
    form.update({
        'department': company.virtual_robot.url,
        'position_staff_text': '1',
        'approved_by': company.persons['yandex-chief'].login,
        'abc_services': abc_services,
        'login': 'zomb-some',
        'responsible': tester.staff.login,
        'zombie_hw': 'Pentium MMX',
    })

    request = post_new_form(rf, FORM_TYPE.ZOMBIE, form)
    request.user = tester

    result = new_form(request, FORM_TYPE.ZOMBIE)

    result = json.loads(result.content)
    assert 'errors' not in result
    assert 'id' in result
    assert Preprofile.objects.count() == 1
    assert Preprofile.objects.first().status == PREPROFILE_STATUS.APPROVED


@pytest.mark.django_db()
def test_that_multiple_services_suggest_field_works(rf, tester, company):
    request = rf.get(reverse('preprofile:new_form', kwargs={'form_type': FORM_TYPE.ROBOT}))
    request.user = tester

    result = new_form(request, FORM_TYPE.ROBOT)
    assert result.status_code == 200
    result = json.loads(result.content)
    assert result['structure']['abc_services']['type'] == 'multiplesuggest'
    assert 'services' in result['structure']['abc_services']['types']


@pytest.mark.django_db()
@pytest.mark.parametrize(
    'form_type,permission', [
        [FORM_TYPE.OUTSTAFF, 'can_outstaff'],
        [FORM_TYPE.EXTERNAL, None],
    ]
)
def test_default_lang(rf, tester, company, form_type, permission):
    request = rf.get(reverse('preprofile:new_form', kwargs={'form_type': form_type}))
    request.user = tester

    if permission:
        tester.user_permissions.add(Permission.objects.get(codename=permission))
    result = new_form(request, form_type)
    lang_ui_field = json.loads(result.content)['structure']['lang_ui']
    assert lang_ui_field['value'] == 'ru'


# STAFF-15775
# @pytest.mark.django_db()
# def test_table_choice_disabled_for_interns(rf, tester, company, red_rose_office):
#     recruiter = StaffFactory()
#     department = company.yandex
#     preprofile = PreprofileFactory(
#         department_id=department.id,
#         first_name='Petya',
#         last_name='Vasechkin',
#         office=red_rose_office,
#         recruiter=recruiter,
#
#         # Intern mark
#         date_completion_internship=datetime.datetime.now(),
#     )
#
#     request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}))
#     request.user = tester
#     tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
#     result = edit_form(request, preprofile.id)
#     result = json.loads(result.content)
#     assert 'errors' not in result
#
#     assert result['structure']['need_table']['readonly']
#
#
# @pytest.mark.django_db()
# def test_table_choice_ignored_for_disabled_cases(rf, tester, company, base_form, red_rose_office):
#     recruiter = StaffFactory()
#     department = company.yandex
#     preprofile = PreprofileFactory(
#         department_id=department.id,
#         first_name='Petya',
#         last_name='Vasechkin',
#         office=red_rose_office,
#         recruiter=recruiter,
#
#         # Intern mark
#         date_completion_internship=datetime.datetime.now(),
#     )
#
#     form = base_form
#     form.update({
#         'department': 'yandex',
#         'need_table': NEED_TABLE.YES,
#     })
#
#     request = rf.post(
#         reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}),
#         json.dumps(form),
#         content_type='application/json',
#     )
#     request.user = tester
#     tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
#     result = edit_form(request, preprofile.id)
#     result = json.loads(result.content)
#     assert 'errors' not in result
#
#     preprofile = Preprofile.objects.get(id=preprofile.id)
#     assert preprofile.need_table != NEED_TABLE.YES


@pytest.mark.django_db()
def test_that_login_in_form_is_readonly_if_guid_or_uid_is_present(rf, tester, company):
    preprofile_with_uid = PreprofileFactory(
        department_id=company.yandex.id,
        recruiter=tester.staff,
        uid='111',
    )

    preprofile_with_guid = PreprofileFactory(
        department_id=company.yandex.id,
        recruiter=tester.staff,
        uid='112',
    )

    preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        recruiter=tester.staff
    )

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile_with_uid.id}))
    request.user = tester
    result = edit_form(request, preprofile_with_uid.id)
    result = json.loads(result.content)
    assert result['data']['login']['readonly']

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile_with_guid.id}))
    request.user = tester
    result = edit_form(request, preprofile_with_guid.id)
    result = json.loads(result.content)
    assert result['data']['login']['readonly']

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}))
    request.user = tester
    result = edit_form(request, preprofile.id)
    result = json.loads(result.content)
    assert 'readonly' not in result['data']['login']


@pytest.mark.django_db()
def test_that_saved_preprofile_can_be_retrieved(rf, tester, company):
    preprofile = PreprofileFactory(department_id=company.yandex.id, recruiter=tester.staff)

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}))
    request.user = tester

    result = edit_form(request, preprofile.id)
    assert 200 == result.status_code
    result = json.loads(result.content)
    assert 'employee' == result['form_type']
    assert preprofile.department.url == result['data']['department']['value']
    assert '' == result['data']['adopted_by']['value']


@pytest.mark.django_db()
def test_that_closed_preprofile_has_adopter_and_approver(rf, tester, company):
    adopter = StaffFactory()
    approver = StaffFactory()

    preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        recruiter=tester.staff,
        adopted_by=adopter,
        approved_by=approver,
        status=PREPROFILE_STATUS.CLOSED
    )

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}))
    request.user = tester

    result = edit_form(request, preprofile.id)
    assert 200 == result.status_code
    result = json.loads(result.content)
    assert 'employee' == result['form_type']
    assert preprofile.department.url == result['data']['department']['value']
    assert preprofile.adopted_by.login == result['data']['adopted_by']['value']
    assert preprofile.approved_by.login == result['data']['approved_by']['value']


@pytest.mark.django_db()
def test_that_preprofile_can_be_adopted_after_join_date(rf, tester, company, robot_staff_user, achievements):
    today = date.today()

    preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
        login='imperator',
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.FEMALE,
        join_at=today - timedelta(days=1),
        recruiter=tester.staff,
    )

    tester.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))
    request = rf.post(reverse('preprofile:adopt', kwargs={'preprofile_id': preprofile.id}))
    request.user = tester

    result = adopt(request, preprofile.id)
    assert result.status_code == 200


@pytest.mark.django_db()
def test_that_preprofile_cannot_be_adopted_before_join_date(rf, tester, company, robot_staff_user):
    today = date.today()

    preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
        login='imperator',
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.FEMALE,
        join_at=today + timedelta(days=1)
    )

    tester.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))
    request = rf.post(reverse('preprofile:adopt', kwargs={'preprofile_id': preprofile.id}))
    request.user = tester

    result = adopt(request, preprofile.id)
    assert result.status_code == 400


@pytest.mark.django_db()
def test_that_adopter_can_edit_join_at_in_ready_state(rf, tester, company):
    tester.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))

    preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
    )

    request = rf.post(
        reverse('preprofile:check_login_availability'),
        content_type='application/json'
    )
    request.user = tester

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}))
    request.user = tester

    result = edit_form(request, preprofile.id)
    assert 200 == result.status_code
    result = json.loads(result.content)
    assert 'employee' == result['form_type']
    assert 'readonly' not in result['structure']['join_at']
    assert result['actions']['save']


EDITABLE_STATUSES = {PREPROFILE_STATUS.NEW, PREPROFILE_STATUS.APPROVED, PREPROFILE_STATUS.READY}


@pytest.mark.django_db()
@pytest.mark.parametrize('status', EDITABLE_STATUSES)
def test_recruiter_can_edit_join_at_in_rotation(rf, tester, company, status):
    preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        status=status,
        form_type=FORM_TYPE.ROTATION,
        candidate_type=CANDIDATE_TYPE.CURRENT_EMPLOYEE,
        recruiter=tester.staff,
    )

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}))
    request.user = tester

    result = edit_form(request, preprofile.id)
    assert 200 == result.status_code
    result = json.loads(result.content)
    assert not result['structure']['join_at'].get('readonly')
    assert result['actions']['save']


READONLY_STATUSES = {PREPROFILE_STATUS.CLOSED, PREPROFILE_STATUS.CANCELLED}


@pytest.mark.django_db()
@pytest.mark.parametrize('status', READONLY_STATUSES)
def test_recruiter_cant_edit_join_at_in_closed_rotation(rf, tester, company, status):
    preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        status=status,
        form_type=FORM_TYPE.ROTATION,
        candidate_type=CANDIDATE_TYPE.CURRENT_EMPLOYEE,
        recruiter=tester.staff,
    )

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': preprofile.id}))
    request.user = tester

    result = edit_form(request, preprofile.id)
    assert 200 == result.status_code
    result = json.loads(result.content)
    assert result['structure']['join_at'].get('readonly')


@pytest.mark.django_db()
@mock.patch('staff.preprofile.login_validation.validate_for_dns')
@mock.patch('staff.preprofile.login_validation.validate_in_ldap')
def test_login_validation_view_simple_success(validate_for_dns, validate_in_ldap, rf, tester):
    form = {
        'is_robot': False,
        'login': 'denis-p'
    }

    request = rf.post(
        reverse('preprofile:check_login_availability'),
        json.dumps(form),
        content_type='application/json'
    )
    request.user = tester

    result = check_login_availability(request)
    assert result.status_code == 200


@pytest.mark.django_db()
def test_login_validation_returns_errors(rf, tester):
    StaffFactory(login='denis-p')

    form = {
        'is_robot': False,
        'login': 'denis-p'
    }

    request = rf.post(
        reverse('preprofile:check_login_availability'),
        json.dumps(form),
        content_type='application/json'
    )
    request.user = tester

    result = check_login_availability(request)
    assert result.status_code == 400
    result = json.loads(result.content)
    assert result['errors']['login'][0]['code'] == LOGIN_VALIDATION_ERROR.LOGIN_NOT_UNIQUE


@pytest.mark.django_db()
def test_approve_view(rf, tester, company, robot_staff_user):
    model = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.NEW,
        need_table=NEED_TABLE.YES,
    )

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    request = rf.post(reverse('preprofile:approve', kwargs={'preprofile_id': model.id}))
    request.user = tester

    result = approve(request, model.id)

    assert result.status_code == 200
    model = Preprofile.objects.get(id=model.id)
    assert model.status == PREPROFILE_STATUS.APPROVED


@pytest.mark.django_db()
def test_yamoney_form_correct_data_saving(company, rf, tester, base_form, settings, robot_staff_user):
    yamoney_organization = OrganizationFactory(name='Yandex Money inc.', id=settings.YAMONEY_ORGANIZATION_ID)
    money_dep = company.yamoney
    tester.user_permissions.add(Permission.objects.get(codename='can_create_yamoney'))

    form_data = base_form
    form_data.update({
        'department': money_dep.url,
        'position_staff_text': '1',
        'login': 'testlogin',
        # 'recruiter': company.persons['dep2-chief'].login,
        'office': OfficeFactory(name='officename').id,
    })
    del form_data['employment_type']
    del form_data['phone']

    request = post_new_form(rf, FORM_TYPE.MONEY, form_data)
    request.user = tester

    result = new_form(request, FORM_TYPE.MONEY)

    assert result.status_code == 200
    preprofile = Preprofile.objects.get()
    assert preprofile.department == money_dep
    assert preprofile.login == form_data['login']
    assert preprofile.status == PREPROFILE_STATUS.APPROVED
    assert preprofile.organization == yamoney_organization
    assert preprofile.email_domain == EMAIL_DOMAIN.YAMONEY_RU
    assert preprofile.recruiter


@pytest.mark.django_db()
def test_yamoney_form_incorrect_department_error(company, rf, tester, base_form):
    not_money_dep = company.dep11
    tester.user_permissions.add(Permission.objects.get(codename='can_create_yamoney'))

    form_data = base_form
    form_data.update({
        'department': not_money_dep.url,
        'position_staff_text': '1',
        'login': 'testlogin',
        'office': OfficeFactory(name='officename').id,
    })
    del form_data['employment_type']
    del form_data['phone']

    request = post_new_form(rf, FORM_TYPE.MONEY, form_data)
    request.user = tester

    result = new_form(request, FORM_TYPE.MONEY)

    assert result.status_code == 400
    assert json.loads(result.content) == {'errors': {'department': [{'code': 'invalid_choice'}]}}


@pytest.mark.django_db()
def test_right_filled_ext_employee_form_can_be_saved(company, rf, tester, base_form, robot_staff_user):
    dep = company.ext
    DepartmentStaffFactory(department=dep, staff=tester.staff, role_id=DepartmentRoles.CHIEF.value)

    form = base_form
    form.update({
        'department': dep.url,
        'login': 'some',
        'lang_ui': LANG.RU,
        'hardware_profile': add_hardware_profile(dep).id,
    })

    request = post_new_form(rf, FORM_TYPE.EXTERNAL, form)
    request.user = tester

    result = new_form(request, FORM_TYPE.EXTERNAL)

    result = json.loads(result.content)
    assert 'errors' not in result
    assert 'id' in result
    assert Preprofile.objects.count() == 1
    p = Preprofile.objects.first()
    assert p.status == PREPROFILE_STATUS.APPROVED
    assert p.organization_id == settings.ROBOTS_ORGANIZATION_ID
    assert p.position_staff_text == 'Внешний консультант'
    assert p.recruiter


@pytest.mark.django_db()
def test_ext_employee_form_can_be_saved_from_api_with_permissions(company, rf, tester, base_form, robot_staff_user):
    dep = company.ext
    DepartmentStaffFactory(department=dep, staff=tester.staff, role_id=DepartmentRoles.CHIEF.value)
    tester.user_permissions.add(Permission.objects.get(codename='can_create_external_by_api'))

    form = base_form
    form.update({
        'department': dep.url,
        'login': 'some',
        'lang_ui': LANG.RU,
        'hardware_profile': add_hardware_profile(dep).id,
    })

    request = post_new_form_api(rf, FORM_TYPE.EXTERNAL, form)
    request.user = tester

    result = new_form_api(request, FORM_TYPE.EXTERNAL)
    assert result.status_code == 200

    result = json.loads(result.content)
    assert 'errors' not in result
    assert 'id' in result

    p = Preprofile.objects.get()
    assert p.status == PREPROFILE_STATUS.APPROVED
    assert p.position_staff_text == 'Внешний консультант'
    assert p.recruiter


@pytest.mark.django_db()
def test_ext_employee_form_can_be_saved_from_api_without_permissions(company, rf, tester, base_form, robot_staff_user):
    dep = company.ext
    DepartmentStaffFactory(department=dep, staff=tester.staff, role_id=DepartmentRoles.CHIEF.value)
    request = post_new_form_api(rf, FORM_TYPE.EXTERNAL, base_form)
    request.user = tester

    result = new_form_api(request, FORM_TYPE.EXTERNAL)
    assert result.status_code == 403
    assert json.loads(result.content) == {'errors': [{'message': 'access_denied'}]}


PREPROFILE_STATUS_TO_SHOWING_TO_RKN = (
    (PREPROFILE_STATUS.NEW, True),
    (PREPROFILE_STATUS.PREPARED, True),
    (PREPROFILE_STATUS.APPROVED, True),
    (PREPROFILE_STATUS.READY, True),
    (PREPROFILE_STATUS.CLOSED, False),
    (PREPROFILE_STATUS.CANCELLED, False),
)


@pytest.mark.django_db
@pytest.mark.parametrize('status,is_showing', PREPROFILE_STATUS_TO_SHOWING_TO_RKN)
def test_showing_dismissed_if_rkn(rf, tester, company, status, is_showing):
    Switch(name='rkn_mode', active=True).save()
    existing_person = StaffFactory(is_dismissed=True)
    p = PreprofileFactory(
        login=existing_person.login,
        department_id=company.yandex.id,
        recruiter=tester.staff,
        status=status,
        form_type=FORM_TYPE.EXTERNAL,
    )

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': p.id}))
    request.user = tester
    result = edit_form(request, p.id)

    assert result.status_code == (200 if is_showing else 404)


@pytest.mark.django_db()
def test_that_ext_form_is_readonly_in_approved_state(rf, tester, company):
    p = PreprofileFactory(
        department_id=company.yandex.id,
        recruiter=tester.staff,
        status=PREPROFILE_STATUS.APPROVED,
        form_type=FORM_TYPE.EXTERNAL,
    )

    request = rf.get(reverse('preprofile:edit_form', kwargs={'preprofile_id': p.id}))
    request.user = tester
    result = edit_form(request, p.id)
    result = json.loads(result.content)

    for field, value in result['data'].items():
        assert value['readonly']


@pytest.mark.django_db
def test_creating_external_link_in_femida(rf, company, settings):
    outstaff_dep = company.outstaff
    recruiter = company.persons['dep2-chief']
    preprofile = PreprofileFactory(
        department_id=outstaff_dep.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=recruiter,
        login='testlogin123',
        form_type=FORM_TYPE.OUTSTAFF,
        status=PREPROFILE_STATUS.NEW,
        uid='3784671538462',
        guid='2983746723543',
    )
    recruiter.user.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.post(
        reverse(
            'preprofile:create_link',
            kwargs={'preprofile_id': preprofile.id}
        )
    )
    request.user = recruiter.user

    mocked_url = 'https://vacancies-test.qloud.yandex.ru/jobs/offer/52b54002f3ab4bd9a7d5585b3d1f0b83'
    settings.ROBOT_STAFF_OAUTH_TOKEN == 'mocked_token'

    return_value = mock.Mock(**{'json.return_value': {'url': mocked_url}})
    path = 'staff.preprofile.controllers.controller.requests.post'
    with mock.patch(path, return_value=return_value) as patched_requests:
        response = create_link(request, preprofile_id=preprofile.id)

    patched_requests.assert_called_once_with(
        'https://femida.test.yandex-team.ru/_api/newhire/preprofiles/',
        json={'id': preprofile.id},
        timeout=(0.5, 1, 3),
        headers={'Authorization': 'OAuth {}'.format(settings.ROBOT_STAFF_OAUTH_TOKEN)}
    )
    assert response.status_code == 200
    assert Preprofile.objects.get().ext_form_link == mocked_url

    assert json.loads(response.content) == {
        'link': mocked_url
    }


@pytest.mark.django_db
def test_creating_external_link_in_femida_while_femida_is_out(rf, company, stub_requests, settings):
    outstaff_dep = company.outstaff
    recruiter = company.persons['outstaff-chief']
    preprofile = PreprofileFactory(
        department_id=outstaff_dep.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=recruiter,
        login='testlogin123',
        form_type=FORM_TYPE.OUTSTAFF,
        status=PREPROFILE_STATUS.NEW,
        uid='3784671538462',
        guid='2983746723543',
    )
    recruiter.user.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.post(
        reverse(
            'preprofile:create_link',
            kwargs={'preprofile_id': preprofile.id}
        )
    )
    request.user = recruiter.user
    response = create_link(request, preprofile_id=preprofile.id)
    assert response.status_code == 400
    assert json.loads(response.content) == {
        'errors': [
            {'message': 'femida_error'}
        ]
    }


preprofile_correct_statuses = {
    PREPROFILE_STATUS.NEW: True,
    PREPROFILE_STATUS.PREPARED: True,
    PREPROFILE_STATUS.APPROVED: False,
    PREPROFILE_STATUS.READY: False,
    PREPROFILE_STATUS.CLOSED: False,
    PREPROFILE_STATUS.CANCELLED: False,
}


@pytest.mark.django_db
@pytest.mark.parametrize('preprofile_status, is_correct', list(preprofile_correct_statuses.items()))
def test_creating_external_link_in_femida_while_preprofile_is_in_different_statuses(rf, company,
                                                                                    stub_requests, settings,
                                                                                    preprofile_status, is_correct):
    outstaff_dep = company.outstaff
    recruiter = company.persons['outstaff-chief']
    preprofile = PreprofileFactory(
        department_id=outstaff_dep.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        recruiter=recruiter,
        login='testlogin123',
        form_type=FORM_TYPE.OUTSTAFF,
        status=preprofile_status,
        uid='3784671538462',
        guid='2983746723543',
    )
    recruiter.user.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.post(
        reverse(
            'preprofile:create_link',
            kwargs={'preprofile_id': preprofile.id}
        )
    )
    request.user = recruiter.user
    response = create_link(request, preprofile_id=preprofile.id)
    if is_correct:
        # remida request stubbed with timeout
        assert response.status_code == 400
    else:
        assert response.status_code == 400
        assert json.loads(response.content) == {
            'errors': [
                {'message': 'not_applicable'}
            ]
        }


@pytest.mark.django_db()
def test_notifications_list(rf, tester, company):
    p = PreprofileFactory(
        department_id=company.yandex.id,
        recruiter=tester.staff,
        status=PREPROFILE_STATUS.APPROVED,
        form_type=FORM_TYPE.EXTERNAL,
    )

    request = rf.get(reverse('preprofile:notification_list', kwargs={'id': p.id}))
    request.user = tester
    result = notification_list(request, p.id)
    assert result.status_code == 200


@pytest.mark.django_db
def test_get_departments_ids_by_hr_partner(company):
    """  `company` fixture provides this:
                   yandex
                /         \
   [hrbp1] -> dep1      dep2  <- [hrbp2]
              |    \
           dep11    dep12  <- [hrbp2]
              |        |
           dep111     removed1
    """

    def get_sorted_department_urls_by_id(department_ids):
        urls = Department.objects.filter(id__in=department_ids).values_list('url', flat=True)
        assert len(urls) == len(department_ids)
        return sorted(urls)

    hrbp1 = company.persons['dep1-hr-partner']
    hrbp2 = company.persons['dep2-hr-partner']

    hrbp1_departments = get_departments_ids_by_hr_partner(hrbp1)
    hrbp2_departments = get_departments_ids_by_hr_partner(hrbp2)

    deps_controlled_by_hrbp1 = ['yandex_dep1', 'yandex_dep1_dep11', 'yandex_dep1_dep11_dep111']
    deps_controlled_by_hrbp2 = ['yandex_dep1_dep12', 'yandex_dep2']

    assert get_sorted_department_urls_by_id(hrbp1_departments) == deps_controlled_by_hrbp1
    assert get_sorted_department_urls_by_id(hrbp2_departments) == deps_controlled_by_hrbp2


@pytest.mark.django_db()
def test_table_validation(rf, tester, map_test_data, company, base_form):
    department = company.yandex

    preprofile = PreprofileFactory(
        department_id=department.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=map_test_data.redrose,
    )
    copyroom = RoomFactory(
        name='Copy',
        floor=map_test_data.first,
        intranet_status=1,
        room_type=ROOM_TYPES.OFFICE,
        num=123
    )

    form = base_form
    form.update({
        'need_table': NEED_TABLE.YES,
        'room': copyroom.id,
        'table': map_test_data.tbl_1.id,
    })

    request = rf.post(
        path=reverse(
            'preprofile:edit_form',
            kwargs={
                'preprofile_id': preprofile.id
            }
        ),
        data=json.dumps(form),
        content_type='application/json',
    )
    request.user = tester
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    result = edit_form(request, preprofile.id)

    result = json.loads(result.content)
    assert 'errors' not in result
    assert Preprofile.objects.get(id=preprofile.id).room.id == copyroom.id
    assert Preprofile.objects.get(id=preprofile.id).table.id == map_test_data.tbl_1.id
    assert 'id' in result
    assert result['id'] == preprofile.id


@pytest.mark.django_db()
def test_invalid_table(rf, tester, map_test_data, company, base_form):
    department = company.yandex

    preprofile = PreprofileFactory(
        department_id=department.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=map_test_data.redrose,
    )
    copyroom = RoomFactory(
        name='Copy',
        floor=map_test_data.first,
        intranet_status=1,
        room_type=ROOM_TYPES.OFFICE,
        num=123
    )

    form = base_form
    form.update({
        'need_table': NEED_TABLE.YES,
        'room': copyroom.id,
        'table': map_test_data.tbl_2.id,
    })

    request = rf.post(
        path=reverse(
            'preprofile:edit_form',
            kwargs={
                'preprofile_id': preprofile.id
            }
        ),
        data=json.dumps(form),
        content_type='application/json',
    )
    request.user = tester
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    result = edit_form(request, preprofile.id)

    result = json.loads(result.content)

    assert result == {
        'errors': {
            'table': [
                {'code': 'invalid_table_for_office'},
            ],
        },
    }


@pytest.mark.django_db()
def test_invalid_room(rf, tester, map_test_data, company, base_form):
    department = company.yandex

    preprofile = PreprofileFactory(
        department_id=department.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=map_test_data.redrose,
    )
    copyroom = RoomFactory(
        name='Copy',
        floor=map_test_data.second,
        intranet_status=1,
        room_type=ROOM_TYPES.OFFICE,
        num=123
    )

    form = base_form
    form.update({
        'need_table': NEED_TABLE.YES,
        'room': copyroom.id,
        'table': map_test_data.tbl_1.id,
    })

    request = rf.post(
        path=reverse(
            'preprofile:edit_form',
            kwargs={
                'preprofile_id': preprofile.id
            }
        ),
        data=json.dumps(form),
        content_type='application/json',
    )
    request.user = tester
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    result = edit_form(request, preprofile.id)

    result = json.loads(result.content)

    assert result == {
        'errors': {
            'room': [
                {'code': 'invalid_room_for_office'},
            ],
        },
    }


@pytest.mark.django_db()
def test_invalid_table_and_room(rf, tester, map_test_data, company, base_form):
    department = company.yandex

    preprofile = PreprofileFactory(
        department_id=department.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=map_test_data.redrose,
    )
    copyroom = RoomFactory(
        name='Copy',
        floor=map_test_data.second,
        intranet_status=1,
        room_type=ROOM_TYPES.OFFICE,
        num=123
    )

    form = base_form
    form.update({
        'need_table': NEED_TABLE.YES,
        'room': copyroom.id,
        'table': map_test_data.tbl_2.id,
    })

    request = rf.post(
        path=reverse(
            'preprofile:edit_form',
            kwargs={
                'preprofile_id': preprofile.id
            }
        ),
        data=json.dumps(form),
        content_type='application/json',
    )
    request.user = tester
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    result = edit_form(request, preprofile.id)

    result = json.loads(result.content)

    assert result == {
        'errors': {
            'room': [
                {'code': 'invalid_room_for_office'},
            ],
            'table': [
                {'code': 'invalid_table_for_office'},
            ],
        },
    }


@pytest.fixture
def proposal_moving_2_persons(db, mocked_mongo):
    collection = mocked_mongo.db.get_collection(MONGO_COLLECTION_NAME)
    person1 = StaffFactory(login='login1')
    person2 = StaffFactory(login='login2')
    dep = DepartmentFactory(url='dep1_url')
    ins = collection.insert_one(
        {
            'author': person1.id,
            'apply_at': datetime.datetime(2020, 9, 10, 0, 0),
            'apply_at_hr': '2018-05-02',
            'description': 'Пример заявки',
            'created_at': '2020-04-18T15:54:12.685',
            'pushed_to_oebs': None,
            'root_departments': [],
            'tickets': {
                'department_ticket': '',
                'department_linked_ticket': '',
                'persons': {},
                'restructurisation': '',
                'deleted_persons': {},
                'deleted_restructurisation': '',
            },
            'actions': [],
            'persons': {
                'actions': [
                    {
                        'action_id': 'act_30878',
                        'sections': ['department'],
                        'department': {
                            'with_budget': True,
                            'from_maternity_leave': False,
                            'vacancy_url': '',
                            'department': dep.url,
                            'fake_department': '',
                            'service_groups': ['svc_devoops'],
                            'changing_duties': True,
                        },
                        'comment': 'Obosnovanie',
                        'login': person1.login,
                        '__department_chain__': ['yandex']
                    },
                    {
                        'action_id': 'act_30879',
                        'sections': ['department'],
                        'department': {
                            'with_budget': False,
                            'from_maternity_leave': False,
                            'vacancy_url': '',
                            'department': dep.url,
                            'fake_department': '',
                            'service_groups': ['svc_devoops'],
                            'changing_duties': False
                        },
                        'comment': 'Obosnovanie',
                        'login': person2.login,
                        '__department_chain__': ['yandex']
                    },
                ],
            },
            'vacancies': {'actions': []},
            'updated_at': '2020-04-18T15:54:12.685',
            '_prev_actions_state': {},
        }
    )
    proposal_id = str(ins.inserted_id)
    applied_at = datetime.datetime(year=2020, month=4, day=1, hour=12, minute=34, second=56)
    meta = ProposalMetadataFactory(proposal_id=proposal_id, applied_at=applied_at.isoformat())
    meta.save()
    return meta, person1, person2, dep


def test_get_proposals_rotations(proposal_moving_2_persons):
    meta, person_with_changing_duties, person_without_changing_duties, dep = proposal_moving_2_persons
    date_from = datetime.date(year=1990, month=1, day=18)
    date_to = datetime.date(year=2090, month=1, day=18)
    rotations1 = get_proposals_rotations(person_with_changing_duties.login, from_date=date_from, to_date=date_to)
    rotations2 = get_proposals_rotations(person_without_changing_duties.login, from_date=date_from, to_date=date_to)

    assert rotations1 == [{'applied_at': '2020-04-01T12:34:56', 'new_department': 'dep1_url'}]
    assert rotations2 == []


@pytest.mark.django_db
def test_get_femida_rotations(company):
    department = company.dep12
    person = company.persons['dep12-person']

    rotation_preprofile = PreprofileFactory(
        first_name=person.first_name,
        last_name=person.last_name,
        status=PREPROFILE_STATUS.CLOSED,
        join_at=datetime.date.today(),
        login=person.login,
        form_type=FORM_TYPE.ROTATION,
        department_id=department.id,
    )
    # Other preprofies
    PreprofileFactory(
        status=PREPROFILE_STATUS.APPROVED,
        join_at=datetime.date.today(),
        login=person.login,
        form_type=FORM_TYPE.ROTATION,
        department_id=department.id,
    )
    PreprofileFactory(
        status=PREPROFILE_STATUS.CLOSED,
        join_at=datetime.date.today(),
        login=person.login,
        form_type=FORM_TYPE.EMPLOYEE,
        department_id=department.id,
    )
    PreprofileFactory(
        status=PREPROFILE_STATUS.CLOSED,
        join_at=datetime.date.today(),
        login=company.persons['yandex-chief'].login,
        form_type=FORM_TYPE.ROTATION,
        department_id=department.id,
    )

    person.preprofile_id = rotation_preprofile.id
    person.save()

    from_date = datetime.date(year=1996, month=1, day=1)
    to_date = datetime.date(year=2028, month=1, day=1)
    rotations = get_femida_rotations(person.login, from_date=from_date, to_date=to_date)

    assert rotations == [
        {
            'applied_at': rotation_preprofile.join_at.isoformat(),
            'new_department': rotation_preprofile.department.url,
        }
    ]
