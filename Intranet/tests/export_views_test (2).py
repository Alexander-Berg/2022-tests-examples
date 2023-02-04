from datetime import timedelta, datetime, date
from itertools import chain, count, product
import json
import pytz

import pytest

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.departments.models import DepartmentStaff, DepartmentRoles
from staff.person.models import GENDER

from staff.preprofile.models import PREPROFILE_STATUS, FORM_TYPE, CANDIDATE_TYPE, CITIZENSHIP
from staff.preprofile.tests.utils import PreprofileFactory, add_hardware_profile
from staff.preprofile.views import (
    cab_export,
    certificator_export,
    femida_export,
    idm_export,
    puncher_export,
    masshire_export,
    preprofile_export,
)


@pytest.mark.django_db()
def test_that_certificator_export_returns_valid_results(rf, tester, company):
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    preprofile1 = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.READY,
        login='offer1',
    )

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.CANCELLED,
        login='offer2',
    )

    preprofile3 = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.APPROVED,
        login='offer3',
    )

    request = rf.get(reverse('preprofile:certificator_export'))
    request.user = tester

    result = certificator_export(request)

    assert result.status_code == 200
    result = json.loads(result.content)

    assert len(result) == 2
    result.sort(key=lambda user: user['username'])
    assert result[0]['username'] == preprofile1.login
    assert result[0]['office'] == preprofile1.office_id
    assert result[0]['department_url'] == preprofile1.department.url
    assert result[1]['username'] == preprofile3.login
    assert result[1]['office'] == preprofile3.office_id
    assert result[1]['department_url'] == preprofile3.department.url


@pytest.mark.django_db()
def test_that_certificator_export_returns_closed_preprofiles_not_older_than_one_week(
    rf, tester, company, disable_preprofile_modified_auto_now,
):
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    now = datetime.now()

    expected_preprofile = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.CLOSED,
        login='offer2',
        modified_at=now - timedelta(days=1),
    )

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.CLOSED,
        login='offer3',
        modified_at=now - timedelta(days=8),
    )

    request = rf.get(reverse('preprofile:certificator_export'))
    request.user = tester

    result = certificator_export(request)

    assert result.status_code == 200
    result = json.loads(result.content)

    assert len(result) == 1
    assert result[0]['username'] == expected_preprofile.login
    assert result[0]['office'] == expected_preprofile.office_id


@pytest.mark.django_db()
def test_cab_export(rf, company):
    login_gen = ('login_{}'.format(i) for i in count())
    child_dep_ruled = company.dep11
    DepartmentStaff.objects.filter(
        department=child_dep_ruled,
        role_id=DepartmentRoles.CHIEF.value,
    ).delete()

    ruled = [
        company.dep1,
        child_dep_ruled,
    ]
    not_ruled = [
        company.dep111,
        company.yandex,
        company.dep2,
    ]

    right_kwargs = dict(status=PREPROFILE_STATUS.NEW, form_type=FORM_TYPE.EMPLOYEE)
    not_right_kwargs_list = [
        dict(status=PREPROFILE_STATUS.PREPARED, form_type=FORM_TYPE.EMPLOYEE),
        dict(status=PREPROFILE_STATUS.NEW, form_type=FORM_TYPE.MONEY),
    ]
    non_expected = chain(
        product(ruled, not_right_kwargs_list),
        product(not_ruled, [right_kwargs]),
    )
    for (dep, kwargs), login in zip(non_expected, login_gen):
        PreprofileFactory(department_id=dep.id, login=login, last_name=login, **kwargs)

    expected_preprofiles = [
        PreprofileFactory(department_id=dep.id, login=login, last_name=login, **right_kwargs)
        for dep, login in zip(ruled, login_gen)
    ]
    expected_result = [
        dict(id=it.id, last_name=it.last_name, first_name='')
        for it in expected_preprofiles
    ]

    request = rf.get(reverse('preprofile:puncher_export'))
    request.user = company.persons['dep1-chief'].user

    result = json.loads(cab_export(request).content)
    preprofiles = result['preprofiles']

    assert (
        len(preprofiles) == len(expected_result) and
        all(it in preprofiles for it in expected_result)
    )


@pytest.mark.django_db()
def test_that_puncher_export_returns_valid_results(rf, tester, company):
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    model = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.READY,
        login='offer1',
        join_at=date.today(),
    )

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.CLOSED,
        login='offer2',
        join_at=date.today() - timedelta(days=1),
    )

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.READY,
        login='offer3',
        join_at=date.today() - timedelta(days=4),
    )

    request = rf.get(reverse('preprofile:puncher_export'))
    request.user = tester

    result = puncher_export(request)

    assert 200 == result.status_code
    result = json.loads(result.content)

    assert 1 == len(result)
    assert model.login == result[0]['username']
    assert 40 == result[0]['state']


@pytest.mark.django_db()
def test_that_idm_export_returns_valid_results(rf, tester, company):
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    model = PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.READY,
        login='offer1',
        candidate_type=CANDIDATE_TYPE.FORMER_EMPLOYEE,
    )

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.CLOSED,
        login='offer2',
    )

    request = rf.get(reverse('preprofile:idm_export'))
    request.user = tester

    result = idm_export(request)

    assert 200 == result.status_code
    result = json.loads(result.content)

    assert len(result) == 1
    assert model.login == result[0]['username']
    assert 40 == result[0]['state']


@pytest.mark.django_db()
def test_that_femida_export_returns_valid_results(rf, tester, company, disable_preprofile_modified_auto_now):
    now = datetime.now()
    now_tz = datetime.now(pytz.timezone('Europe/Moscow'))

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.READY,
        login='offer1',
        candidate_type=CANDIDATE_TYPE.FORMER_EMPLOYEE,
        modified_at=now - timedelta(seconds=3),
        femida_offer_id=1,
        recruiter=tester.get_profile(),
    )

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.READY,
        login='offer2',
        modified_at=now - timedelta(seconds=1),
        femida_offer_id=2,
        recruiter=tester.get_profile(),
    )

    PreprofileFactory(
        department_id=company.yandex.id,
        status=PREPROFILE_STATUS.READY,
        login='offer3',
        modified_at=now - timedelta(seconds=1),
        femida_offer_id=None,
        ext_form_link='http://ok-pod.ru',
        recruiter=tester.get_profile(),
    )

    date_from = now_tz - timedelta(seconds=2)
    request = rf.get(reverse('preprofile:femida_export'), data={'date_from': date_from.isoformat()})
    request.user = tester

    result = femida_export(request)

    assert result.status_code == 200
    result = json.loads(result.content)

    assert len(result) == 2
    assert result[0]['femida_offer_id'] == 2


@pytest.mark.django_db()
def test_that_masshire_export_works(tester_request, tester, company):
    tester.user_permissions.add(Permission.objects.get(codename='can_outstaff'))
    PreprofileFactory(
        masshire_tag='tag1',
        department=company.yandex,
        email='test@yandex.ru',
        form_type=FORM_TYPE.MASS_HIRE,
        gender=GENDER.MALE,
        citizenship=CITIZENSHIP.RUSSIAN,
    )
    PreprofileFactory(
        masshire_tag='tag2',
        department=company.yandex,
        email='test2@yandex.ru',
        form_type=FORM_TYPE.MASS_HIRE,
        gender=GENDER.FEMALE,
        citizenship=CITIZENSHIP.KAZAKHSTAN,
    )
    result = masshire_export(tester_request('preprofile:masshire_export'))
    assert result.status_code == 200
    assert result.content
    expected_header = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=utf-8'
    assert dict(result.items())['Content-Type'] == expected_header


@pytest.mark.django_db()
def test_that_preprofile_export_works(tester_request, tester, company):
    tester.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))
    PreprofileFactory(
        department=company.yandex,
        email='test@yandex.ru',
        form_type=FORM_TYPE.EMPLOYEE,
        gender=GENDER.MALE,
        citizenship=CITIZENSHIP.RUSSIAN,
    )
    PreprofileFactory(
        department=company.yandex,
        email='test2@yandex.ru',
        form_type=FORM_TYPE.EMPLOYEE,
        gender=GENDER.FEMALE,
        citizenship=CITIZENSHIP.KAZAKHSTAN,
        hardware_profile=add_hardware_profile(company.yandex),
    )
    result = preprofile_export(tester_request('preprofile:preprofile_export'))
    assert result.status_code == 200
    assert result.content
    expected_header = 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet; charset=utf-8'
    assert dict(result.items())['Content-Type'] == expected_header
