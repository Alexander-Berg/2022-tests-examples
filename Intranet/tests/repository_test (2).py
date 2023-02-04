import pytest

from django.contrib.auth.models import Permission

from staff.departments.models import DepartmentRoles
from staff.lib.testing import StaffFactory, DepartmentFactory, DepartmentStaffFactory

from staff.preprofile.models import FORM_TYPE
from staff.preprofile.repository import Repository, NoRightsRepositoryError
from staff.preprofile.tests.utils import PreprofileFactory, make_adopter


@pytest.mark.django_db()
def test_preprofiles_returns_only_recruiter_preprofiles(company):
    staff = StaffFactory(login='denis-p')

    preprofiles = [
        PreprofileFactory(department=company.yandex, recruiter=staff),
        PreprofileFactory(department=company.yandex, recruiter=staff),
        PreprofileFactory(department=company.yandex),
    ]

    result = list(Repository(staff).preprofiles_qs())

    assert result == preprofiles[:2]


@pytest.mark.django_db
def test_repository_raises_exception_if_recruiter_has_no_rights_for_preprofile(company):
    recruiter1 = StaffFactory()
    recruiter2 = StaffFactory()

    p = PreprofileFactory(department=company.yandex, recruiter=recruiter1)

    with pytest.raises(NoRightsRepositoryError):
        Repository(recruiter2).existing(p.id)


@pytest.mark.django_db
def test_preprofiles_returns_all_preprofiles_on_permission(company):
    staff = StaffFactory(login='denis-p')
    staff.user.user_permissions.add(Permission.objects.get(codename='can_view_all_preprofiles'))

    preprofiles = [
        PreprofileFactory(department=company.yandex, recruiter=staff),
        PreprofileFactory(department=company.yandex, recruiter=staff),
        PreprofileFactory(department=company.yandex),
    ]

    result = list(Repository(staff).preprofiles_qs())

    assert result == preprofiles


@pytest.mark.django_db
def test_repository_not_raises_exception_for_robozombie(company):
    staff = StaffFactory()

    p1 = PreprofileFactory(department=company.yandex, form_type=FORM_TYPE.ROBOT)
    p2 = PreprofileFactory(department=company.yandex, form_type=FORM_TYPE.ZOMBIE)

    Repository(staff).existing(p1.id)
    Repository(staff).existing(p2.id)


@pytest.mark.django_db
def test_preprofiles_returns_preprofiles_for_chiefs_only_for_chief_departments(company):
    chief = company.persons['dep1-chief']

    preprofiles = [
        PreprofileFactory(department=company.dep1),
        PreprofileFactory(department=company.dep11),
        PreprofileFactory(department=company.dep2),
    ]

    result = list(Repository(chief).preprofiles_qs())

    assert result == preprofiles[:2]


@pytest.mark.django_db
def test_repository_raises_exception_if_staff_is_not_chief_for_department(company):
    chief = company.persons['dep1-chief']

    PreprofileFactory(department=company.dep1)
    PreprofileFactory(department=company.dep11)
    p3 = PreprofileFactory(department=company.dep2)

    with pytest.raises(NoRightsRepositoryError):
        Repository(chief).existing(p3.id)


@pytest.mark.django_db
def test_repository_not_raises_exception_if_staff_is_chief_for_department(company):
    chief = company.persons['dep1-chief']

    p1 = PreprofileFactory(department=company.dep1)
    p2 = PreprofileFactory(department=company.dep11)
    PreprofileFactory(department=company.dep2)

    Repository(chief).existing(p1.id)
    Repository(chief).existing(p2.id)


@pytest.mark.django_db
def test_preprofiles_returns_preprofiles_for_yamoney_if_has_permission(company):
    staff = StaffFactory(login='denis-p')
    staff.user.user_permissions.add(Permission.objects.get(codename='can_create_yamoney'))

    yamoney_department = company.yamoney
    child_yamoney_department = DepartmentFactory(parent=yamoney_department)

    preprofiles = [
        PreprofileFactory(department=yamoney_department, form_type=FORM_TYPE.MONEY),
        PreprofileFactory(department=child_yamoney_department, form_type=FORM_TYPE.MONEY),
        PreprofileFactory(department=company.dep2),
    ]

    result = list(Repository(staff).preprofiles_qs())

    assert result == preprofiles[:2]


@pytest.mark.django_db
def test_that_deputy_has_right_for_outstaff_preprofile(company):
    deputy = StaffFactory(login='denis-p')
    department = company.dep1
    DepartmentStaffFactory(staff=deputy, department=department, role_id=DepartmentRoles.DEPUTY.value)
    p = PreprofileFactory(department=company.dep11, form_type=FORM_TYPE.OUTSTAFF)

    Repository(deputy).existing(p.id)


@pytest.mark.django_db
def test_preprofiles_returns_preprofiles_for_deputy_for_external_and_outstaff(company):
    deputy = StaffFactory(login='denis-p')
    department = company.dep1
    DepartmentStaffFactory(staff=deputy, department=department, role_id=DepartmentRoles.DEPUTY.value)

    preprofiles = [
        PreprofileFactory(department=company.dep11, form_type=FORM_TYPE.OUTSTAFF),
        PreprofileFactory(department=company.dep11, form_type=FORM_TYPE.EXTERNAL),
        PreprofileFactory(department=company.dep11),
    ]

    result = list(Repository(deputy).preprofiles_qs())

    assert preprofiles[:2] == result


@pytest.mark.django_db
def test_preprofiles_returns_preprofiles_for_deputy_for_external_and_outstaff_employee_form(company):
    deputy = StaffFactory(login='denis-an')
    DepartmentStaffFactory(staff=deputy, department=company.out1, role_id=DepartmentRoles.DEPUTY.value)
    DepartmentStaffFactory(staff=deputy, department=company.ext1, role_id=DepartmentRoles.DEPUTY.value)

    preprofiles = [
        PreprofileFactory(department=company.out11, form_type=FORM_TYPE.EMPLOYEE),
        PreprofileFactory(department=company.ext11, form_type=FORM_TYPE.EMPLOYEE),
        PreprofileFactory(department=company.dep11, form_type=FORM_TYPE.EMPLOYEE),
    ]

    result = list(Repository(deputy).preprofiles_qs())

    assert preprofiles[:2] == result


@pytest.mark.django_db
def test_that_deputy_has_right_for_external_preprofile(company):
    deputy = StaffFactory(login='denis-p')
    department = company.dep1
    DepartmentStaffFactory(staff=deputy, department=department, role_id=DepartmentRoles.DEPUTY.value)
    p = PreprofileFactory(department=company.dep11, form_type=FORM_TYPE.EXTERNAL)

    Repository(deputy).existing(p.id)


@pytest.mark.django_db()
def test_that_user_having_yamoney_right_can_access_not_his_own_yamoney_preporfile(company, settings):
    recruiter = StaffFactory()
    recruiter.user.user_permissions.add(Permission.objects.get(codename='can_create_yamoney'))

    recruiter2 = StaffFactory()
    recruiter2.user.user_permissions.add(Permission.objects.get(codename='can_create_yamoney'))

    child_yamoney_department = DepartmentFactory(parent=company.yamoney)

    p = PreprofileFactory(department=child_yamoney_department, recruiter=recruiter, form_type=FORM_TYPE.MONEY)

    Repository(recruiter2).existing(p.id)


@pytest.mark.django_db
def test_that_preprofiles_qs_excludes_rotation_from_result(company):
    adopter = company.persons['yandex-person']
    make_adopter(adopter.user)

    PreprofileFactory(form_type=FORM_TYPE.ROTATION, department=company.yandex)
    employee_form1 = PreprofileFactory(form_type=FORM_TYPE.EMPLOYEE, department=company.yandex)
    employee_form2 = PreprofileFactory(form_type=FORM_TYPE.EMPLOYEE, department=company.yandex)

    result = set(Repository(adopter).preprofiles_qs().values_list('id', flat=True))

    assert result == {employee_form1.id, employee_form2.id}
