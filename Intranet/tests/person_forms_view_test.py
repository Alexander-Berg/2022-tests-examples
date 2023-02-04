import json
from datetime import date, timedelta
from typing import Any

import pytest
from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.lib.testing import StaffFactory
from staff.lib.tests.pytest_fixtures import AttrDict
from staff.lib.utils.ordered_choices import OrderedChoices
from staff.users.models import User
from staff.preprofile.models import PREPROFILE_STATUS, FORM_TYPE
from staff.preprofile.tests.utils import PreprofileFactory, make_adopter
from staff.preprofile.views import person_forms


def create_preprofiles_in_company(company, red_rose_office, tester):

    date_in_2_days = date.today() + timedelta(days=2)
    date_in_5_days = date.today() + timedelta(days=5)

    model1 = PreprofileFactory(
        department_id=company.yandex.id,
        first_name='Koluychka',
        last_name='Vonyuchka',
        office=red_rose_office,
        recruiter=tester.staff,
        status=PREPROFILE_STATUS.NEW,
        join_at=date.today(),
        date_completion_internship=date_in_2_days,
    )

    model2 = PreprofileFactory(
        department_id=company.dep1.id,
        recruiter=StaffFactory(),
        status=PREPROFILE_STATUS.READY,
        join_at=date_in_2_days,
        date_completion_internship=date_in_5_days,
    )

    model3 = PreprofileFactory(
        department_id=company.dep2.id,
        recruiter=tester.staff,
        status=PREPROFILE_STATUS.CANCELLED,
        join_at=date_in_5_days,
    )

    return [model1, model2, model3]


@pytest.mark.django_db
def test_that_several_saved_preprofiles_can_be_retrieved(company, rf, tester, red_rose_office):
    m1, m2, m3 = create_preprofiles_in_company(company, red_rose_office, tester)

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.get(reverse('preprofile:person_forms'))
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert m1.id in ids
    assert m2.id in ids

    for preprofile in result['result']:
        assert 'first_name' in preprofile
        assert 'last_name' in preprofile
        assert 'middle_name' in preprofile
        assert 'created_at' in preprofile
        assert 'modified_at' in preprofile
        assert 'id' in preprofile
        assert 'login' in preprofile
        assert 'department_name' in preprofile
        assert 'department_url' in preprofile
        assert 'office_name' in preprofile
        assert 'organization_name' in preprofile
        assert 'position' in preprofile
        assert 'status' in preprofile
        assert 'city_name' in preprofile
        assert 'join_at' in preprofile

    [preprofile_for_model1] = [preprofile for preprofile in result['result'] if preprofile['id'] == m1.id]

    assert 'Koluychka' == preprofile_for_model1['first_name']
    assert 'Vonyuchka' == preprofile_for_model1['last_name']
    assert 'Яндекс' == preprofile_for_model1['department_name']
    assert 'RedRose' == preprofile_for_model1['office_name']
    assert 'Moscow' == preprofile_for_model1['city_name']
    assert 'Аркадий' == preprofile_for_model1['chief_first_name']
    assert 'yandex-chief' == preprofile_for_model1['chief_login']


@pytest.mark.django_db
def test_that_several_saved_preprofiles_filtered_by_office_can_be_retrieved(company, rf, tester, red_rose_office):
    m1, m2, m3 = create_preprofiles_in_company(company, red_rose_office, tester)

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.get(reverse('preprofile:person_forms'), {'office': red_rose_office.id})
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert 1 == len(ids)
    assert m1.id in ids

    for preprofile in result['result']:
        assert 'first_name' in preprofile
        assert 'last_name' in preprofile
        assert 'middle_name' in preprofile
        assert 'created_at' in preprofile
        assert 'modified_at' in preprofile
        assert 'id' in preprofile
        assert 'login' in preprofile
        assert 'department_name' in preprofile
        assert 'department_url' in preprofile
        assert 'office_name' in preprofile
        assert 'organization_name' in preprofile
        assert 'position' in preprofile
        assert 'status' in preprofile
        assert 'city_name' in preprofile
        assert 'join_at' in preprofile

    [preprofile_for_model1] = [preprofile for preprofile in result['result'] if preprofile['id'] == m1.id]

    assert 'Koluychka' == preprofile_for_model1['first_name']
    assert 'Vonyuchka' == preprofile_for_model1['last_name']
    assert 'Яндекс' == preprofile_for_model1['department_name']
    assert 'RedRose' == preprofile_for_model1['office_name']
    assert 'Moscow' == preprofile_for_model1['city_name']
    assert 'Аркадий' == preprofile_for_model1['chief_first_name']
    assert 'yandex-chief' == preprofile_for_model1['chief_login']


@pytest.mark.django_db
def test_that_several_saved_preprofiles_filtered_by_status_can_be_retrieved(company, rf, tester, red_rose_office):
    m1, m2, m3 = create_preprofiles_in_company(company, red_rose_office, tester)

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.get(reverse('preprofile:person_forms'), {'status': 'ready'})
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert 1 == len(ids)
    assert m2.id in ids

    for preprofile in result['result']:
        assert 'first_name' in preprofile
        assert 'last_name' in preprofile
        assert 'middle_name' in preprofile
        assert 'created_at' in preprofile
        assert 'modified_at' in preprofile
        assert 'id' in preprofile
        assert 'login' in preprofile
        assert 'department_name' in preprofile
        assert 'department_url' in preprofile
        assert 'office_name' in preprofile
        assert 'organization_name' in preprofile
        assert 'position' in preprofile
        assert 'status' in preprofile
        assert 'city_name' in preprofile
        assert 'join_at' in preprofile


@pytest.mark.django_db
def test_that_several_saved_preprofiles_filtered_by_department_can_be_retrieved(company, rf, tester, red_rose_office):
    m1, m2, m3 = create_preprofiles_in_company(company, red_rose_office, tester)

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.get(reverse('preprofile:person_forms'), {'department': company.dep2.url})
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert 1 == len(ids)
    assert m3.id in ids


@pytest.mark.django_db
def test_that_several_saved_preprofiles_filtered_by_recruiter_can_be_retrieved(company, rf, tester, red_rose_office):
    m1, m2, m3 = create_preprofiles_in_company(company, red_rose_office, tester)

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.get(reverse('preprofile:person_forms'), {'recruiter': tester.staff.login})
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert 2 == len(ids)
    assert m1.id in ids
    assert m3.id in ids


@pytest.mark.django_db
def test_that_several_saved_preprofiles_filtered_by_date_from_can_be_retrieved(company, rf, tester, red_rose_office):
    m1, m2, m3 = create_preprofiles_in_company(company, red_rose_office, tester)

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.get(reverse('preprofile:person_forms'), {'date_from': date.today() + timedelta(days=1)})
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert 2 == len(ids)
    assert m2.id in ids
    assert m3.id in ids


@pytest.mark.django_db
def test_that_several_saved_preprofiles_filtered_by_date_to_can_be_retrieved(company, rf, tester, red_rose_office):
    m1, m2, m3 = create_preprofiles_in_company(company, red_rose_office, tester)

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.get(reverse('preprofile:person_forms'), {'date_to': date.today() + timedelta(days=2)})
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert ids == {m1.id, m2.id}


@pytest.mark.django_db
def test_that_several_saved_preprofiles_filtered_by_internship_can_be_retrieved(company, rf, tester, red_rose_office):
    m1, m2, m3 = create_preprofiles_in_company(company, red_rose_office, tester)

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    request = rf.get(reverse('preprofile:person_forms'), {'internship_fulltime': 'internship'})
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert ids == {m1.id, m2.id}


@pytest.mark.django_db
def test_that_several_saved_preprofiles_filtered_by_root_department_can_be_retrieved(
    settings,
    company,
    rf,
    tester,
    red_rose_office,
):

    create_preprofiles_in_company(company, red_rose_office, tester)

    m4 = PreprofileFactory(
        department_id=company.out1.id,
        recruiter=StaffFactory(),
        status=PREPROFILE_STATUS.READY,
        join_at=date.today(),
    )
    m5 = PreprofileFactory(
        department_id=company.ext1.id,
        recruiter=StaffFactory(),
        status=PREPROFILE_STATUS.READY,
        join_at=date.today(),
    )

    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    from staff.preprofile.forms.person_forms_filter_form import PersonFormsFilterForm
    PersonFormsFilterForm.base_fields['root_department'].choices = OrderedChoices(
        ('YANDEX', settings.YANDEX_DEPARTMENT_ID, 'yandex'),
        ('EXTERNAL', settings.EXT_DEPARTMENT_ID, 'external'),
        ('OUTSTAFF', settings.OUTSTAFF_DEPARTMENT_ID, 'outstaff'),
    )

    request = rf.get(reverse('preprofile:person_forms'), {'root_department': [company.outstaff.id, company.ext.id]})
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert ids == {m4.id, m5.id}


@pytest.mark.django_db
def test_that_adopter_doesnt_see_rotation_preprofiles(company: AttrDict, rf: Any, tester: User):
    PreprofileFactory(form_type=FORM_TYPE.ROTATION, department=company.yandex)
    employee_form1 = PreprofileFactory(form_type=FORM_TYPE.EMPLOYEE, department=company.yandex)
    employee_form2 = PreprofileFactory(form_type=FORM_TYPE.EMPLOYEE, department=company.yandex)

    make_adopter(tester)

    request = rf.get(reverse('preprofile:person_forms'))
    request.user = tester

    result = person_forms(request)
    assert result.status_code == 200
    result = json.loads(result.content)

    ids = {form['id'] for form in result['result']}
    assert ids == {employee_form1.id, employee_form2.id}
