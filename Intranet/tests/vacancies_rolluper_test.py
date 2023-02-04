import pytest

from waffle.models import Switch

from staff.departments.models import Vacancy, VacancyMember, DepartmentRoles
from staff.departments.tests.factories import VacancyFactory, VacancyMemberFactory
from staff.lib.testing import DepartmentFactory, StaffFactory, DepartmentStaffFactory
from staff.preprofile.tests.utils import PreprofileFactory

from staff.femida.models import FemidaVacancy
from staff.femida.constants import VACANCY_STATUS
from staff.femida.tests.utils import FemidaVacancyFactory
from staff.femida.vacancies_rolluper import VacanciesRolluper


@pytest.mark.django_db
def test_vacancy_creation_on_rollup():
    preprofile = PreprofileFactory(department=DepartmentFactory())

    femida_vacancy = FemidaVacancyFactory(
        name='To be created',
        startrek_key='JOB-100500',
        offer_id=100500,
        candidate_id=500100,
        preprofile_id=preprofile.id,
        candidate_first_name='firstname',
        candidate_last_name='lastname',
        candidate_login='login',
        status=VACANCY_STATUS.IN_PROGRESS,
        department_url=DepartmentFactory().url,
    )

    VacanciesRolluper(create_absent=True).run_rollup()

    vacancy = Vacancy.objects.get(id=femida_vacancy.id)  # type: Vacancy
    femida_vacancy = FemidaVacancy.objects.first()  # type: FemidaVacancy

    assert vacancy.name == femida_vacancy.name
    assert vacancy.id == femida_vacancy.id
    assert vacancy.id == femida_vacancy.staff_vacancy.id
    assert vacancy.ticket == femida_vacancy.startrek_key
    assert vacancy.offer_id == femida_vacancy.offer_id
    assert vacancy.candidate_id == femida_vacancy.candidate_id
    assert vacancy.candidate_first_name == femida_vacancy.candidate_first_name
    assert vacancy.candidate_last_name == femida_vacancy.candidate_last_name
    assert vacancy.candidate_login == femida_vacancy.candidate_login
    assert vacancy.preprofile_id == femida_vacancy.preprofile_id
    assert vacancy.status == femida_vacancy.status
    assert vacancy.headcount_position_code == femida_vacancy.headcount_position_id
    assert vacancy.department.url == femida_vacancy.department_url
    assert vacancy.is_active


@pytest.mark.django_db
def test_vacancy_department_update_on_rollup():
    vacancy_to_be_changed = VacancyFactory(
        department=DepartmentFactory(),
    )

    femida_vacancy = FemidaVacancyFactory(
        id=vacancy_to_be_changed.id,
        department_url=DepartmentFactory().url,
        staff_vacancy=vacancy_to_be_changed,
    )

    VacanciesRolluper(create_absent=True).run_rollup()
    vacancy = Vacancy.objects.get(id=vacancy_to_be_changed.id)  # type: Vacancy

    assert vacancy.department.url == femida_vacancy.department_url


@pytest.mark.django_db
def test_vacancy_member_rollup():
    Switch.objects.get_or_create(name='enable_proposal_vacancies_for_everyone', active=True)

    person1, person2, person3 = StaffFactory(), StaffFactory(), StaffFactory()
    vacancy = VacancyFactory()
    FemidaVacancyFactory(id=vacancy.id, members=[person1.login, person2.login])
    VacancyMemberFactory(vacancy=vacancy, person=person2)
    VacancyMemberFactory(vacancy=vacancy, person=person3)

    VacanciesRolluper(create_absent=True).run_rollup()

    members = VacancyMember.objects.values_list('person_id', flat=True)
    assert set(members) == {person1.id, person2.id}


@pytest.mark.django_db
def test_vacancy_member_hr_analyst_rollup():
    Switch.objects.get_or_create(name='enable_proposal_vacancies_for_everyone', active=False)

    person1, person2 = StaffFactory(), StaffFactory()
    DepartmentStaffFactory(staff=person1, role_id=DepartmentRoles.HR_ANALYST.value)
    FemidaVacancyFactory(members=[person1.login, person2.login])

    VacanciesRolluper(create_absent=True).run_rollup()

    members = VacancyMember.objects.values_list('person_id', flat=True)
    assert set(members) == {person1.id}
