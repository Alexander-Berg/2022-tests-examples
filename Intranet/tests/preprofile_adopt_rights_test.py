from datetime import date

import pytest

from django.contrib.auth.models import Permission
from django.core.urlresolvers import reverse

from staff.departments.models import DepartmentRoles
from staff.lib.testing import StaffFactory, DepartmentStaffFactory
from staff.person.models import GENDER

from staff.preprofile.models import FORM_TYPE, PREPROFILE_STATUS
from staff.preprofile.tests.utils import PreprofileFactory
from staff.preprofile.views import adopt


@pytest.mark.django_db()
def test_that_preprofile_cannot_be_adopted_without_adoption_right(rf, tester, company, robot_staff_user):
    today = date.today()

    model = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
        login='imperator',
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.FEMALE,
        join_at=today,
        recruiter=tester.staff,
    )

    request = rf.post(reverse('preprofile:adopt', kwargs={'preprofile_id': model.id}))
    request.user = tester

    result = adopt(request, model.id)
    assert result.status_code == 403


@pytest.mark.django_db()
def test_that_preprofile_can_be_adopted_with_adoption_right(rf, tester, company, robot_staff_user, achievements):
    tester.user_permissions.add(Permission.objects.get(codename='add_personadoptapplication'))

    today = date.today()

    model = PreprofileFactory(
        department_id=company.yandex.id,
        femida_offer_id=1,
        login='imperator',
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.FEMALE,
        join_at=today,
        recruiter=tester.staff,
    )

    request = rf.post(reverse('preprofile:adopt', kwargs={'preprofile_id': model.id}))
    request.user = tester

    result = adopt(request, model.id)
    assert result.status_code == 200


@pytest.mark.django_db()
def test_that_employee_preprofile_for_ext_dep_can_be_adopted_by_chief(rf, company, robot_staff_user, tester, settings):
    today = date.today()

    model = PreprofileFactory(
        department_id=settings.EXT_DEPARTMENT_ID,
        femida_offer_id=1,
        login='imperator',
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.FEMALE,
        join_at=today,
        recruiter=tester.staff,
    )

    request = rf.post(reverse('preprofile:adopt', kwargs={'preprofile_id': model.id}))
    chief = StaffFactory()
    DepartmentStaffFactory(
        staff=chief,
        department_id=settings.EXT_DEPARTMENT_ID,
        role_id=DepartmentRoles.CHIEF.value,
    )

    request.user = chief.user

    result = adopt(request, model.id)
    assert result.status_code == 200


@pytest.mark.django_db()
@pytest.mark.parametrize('role', (
    DepartmentRoles.CHIEF.value,
    DepartmentRoles.DEPUTY.value,
))
def test_that_employee_preprofile_for_outstaff_dep_can_be_adopted_by_chief_or_deputy(
    role, rf, company, robot_staff_user, tester, settings,
):
    today = date.today()

    model = PreprofileFactory(
        department_id=settings.OUTSTAFF_DEPARTMENT_ID,
        femida_offer_id=1,
        login='imperator',
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.FEMALE,
        join_at=today,
        recruiter=tester.staff,
    )

    request = rf.post(reverse('preprofile:adopt', kwargs={'preprofile_id': model.id}))
    staff = StaffFactory()
    DepartmentStaffFactory(
        staff=staff,
        department_id=settings.OUTSTAFF_DEPARTMENT_ID,
        role_id=role,
    )

    request.user = staff.user

    result = adopt(request, model.id)
    assert result.status_code == 200


@pytest.mark.django_db()
def test_that_outstaff_preprofile_can_be_adopted_without_adoption_right_by_chief(
        rf, company, robot_staff_user, tester, achievements
        ):
    today = date.today()

    model = PreprofileFactory(
        department_id=company.dep111.id,
        femida_offer_id=1,
        login='imperator',
        form_type=FORM_TYPE.OUTSTAFF,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.FEMALE,
        join_at=today,
        recruiter=tester.staff,
    )

    request = rf.post(reverse('preprofile:adopt', kwargs={'preprofile_id': model.id}))
    chief = company.persons['dep11-chief']
    chief.departmentstaff_set.clear()
    chief.departmentstaff_set.create(
        role_id=DepartmentRoles.DEPUTY.value,
        department=company.dep11,
    )
    request.user = company.persons['dep11-chief'].user

    result = adopt(request, model.id)
    assert result.status_code == 200


@pytest.mark.django_db()
def test_that_employee_preprofile_cannot_be_adopted_without_special_adoption_right_by_chief(
        company, rf, robot_staff_user, tester
        ):
    today = date.today()

    model = PreprofileFactory(
        department_id=company.dep111.id,
        femida_offer_id=1,
        login='imperator',
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.FEMALE,
        join_at=today,
        recruiter=tester.staff,
    )

    request = rf.post(reverse('preprofile:adopt', kwargs={'preprofile_id': model.id}))
    chief = company.persons['dep11-chief']
    chief.departmentstaff_set.clear()
    chief.departmentstaff_set.create(
        role_id=DepartmentRoles.CHIEF.value,
        department=company.dep11,
    )
    request.user = company.persons['dep11-chief'].user

    result = adopt(request, model.id)
    assert result.status_code == 403


@pytest.mark.django_db
def test_that_employee_preprofile_can_be_adopted_with_special_adoption_right_by_chief(
    company, rf, tester, robot_staff_user, achievements, settings,
):
    today = date.today()
    chief = company.persons['dep111-chief']

    model = PreprofileFactory(
        department_id=company.dep111.id,
        femida_offer_id=1,
        login='imperator',
        form_type=FORM_TYPE.EMPLOYEE,
        status=PREPROFILE_STATUS.READY,
        guid='guid',
        uid='112',
        gender=GENDER.FEMALE,
        join_at=today,
        recruiter=tester.staff,
        approved_by=chief,
    )

    request = rf.post(reverse('preprofile:adopt', kwargs={'preprofile_id': model.id}))

    chief.user.user_permissions.add(Permission.objects.get(codename='chief_that_can_adopt'))
    request.user = chief.user

    result = adopt(request, model.id)
    assert result.status_code == 200
