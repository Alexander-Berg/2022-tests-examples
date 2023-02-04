from itertools import groupby
from typing import AnyStr

import pytest

from django.contrib.auth.models import Permission
from django.db.models import QuerySet

from staff.departments.vacancy_permissions import VacancyPermissions
from staff.femida.constants import VACANCY_STATUS
from staff.lib.models.mptt import filter_by_heirarchy
from staff.lib.testing import DepartmentFactory
from staff.person_filter.filter_context import FilterContext

from staff.departments.tests.factories import VacancyFactory
from staff.departments.tree.persons_entity_info import PersonsEntityInfo
from staff.departments.tree.vacancies_entity_info import VacanciesEntityInfo
from staff.departments.tree.vacancies_filter_context import VacanciesFilterContext
from staff.departments.tree_lib import TreeBuilder
from staff.departments.models import Vacancy, Department


def department_filter_qs(filter_context, url):
    return filter_by_heirarchy(
        Department.objects.filter(intranet_status=1),
        mptt_objects=[filter_context.get_base_dep_qs().get(url=url)],
        by_children=True,
        include_self=True,
    )


def test_department_filler_persons_qty(company):
    filter_context = FilterContext()
    qs = (
        department_filter_qs(filter_context, company.yandex.url)
        .values(*FilterContext.dep_fields)
        .order_by('tree_id', 'level', 'lft')
    )

    result = TreeBuilder(PersonsEntityInfo(filter_context)).get_as_short_list(qs)

    assert len(result) == 6
    assert result[0]['persons_qty'] == 17  # yandex
    assert result[1]['persons_qty'] == 12  # yandex_dep1
    assert result[2]['persons_qty'] == 2  # yandex_dep2

    assert result[3]['persons_qty'] == 5  # yandex_dep1_dep11
    assert result[4]['persons_qty'] == 3  # yandex_dep1_dep12
    assert result[5]['persons_qty'] == 2  # yandex_dep1_dep111


def test_department_filler_vacancies_count(company, vacancies):
    url = company.yandex.url
    filter_context = FilterContext()
    qs = department_filter_qs(filter_context, url).values(*FilterContext.dep_fields)

    result = TreeBuilder(PersonsEntityInfo(filter_context)).get_as_short_list(qs)

    assert len(result) == 6
    assert result[0]['vacancies_count'] == 8  # yandex
    assert result[1]['vacancies_count'] == 4  # yandex_dep1
    assert result[2]['vacancies_count'] == 2  # yandex_dep2
    assert result[3]['vacancies_count'] == 2  # yandex_dep1_dep11
    assert result[4]['vacancies_count'] == 2  # yandex_dep1_dep12
    assert result[5]['vacancies_count'] == 2  # yandex_dep1_dep111


def test_persons_entity_info_returns_headcounts_availability_flag_if_needed(company):
    viewer_person = company.persons['dep11-chief']
    viewer_person.user.user_permissions.add(Permission.objects.get(codename='can_view_headcounts'))
    filter_context = FilterContext()
    persons_entity_info = PersonsEntityInfo(filter_context, viewer_person)
    expected_ids = {company.dep11.id, company.dep111.id}

    departments_qs = filter_context.get_base_dep_qs()
    departments = list(departments_qs)
    departments_ids = departments_qs.values_list('id', flat=True)
    persons_entity_info.fill_counters(departments_ids, departments)

    headcounts_available = {d['id'] for d in departments if d['headcounts_available']}
    assert headcounts_available == expected_ids


@pytest.fixture
def vacancies(company):
    VacancyFactory(department=company.yandex, is_published=True, status=VACANCY_STATUS.IN_PROGRESS)
    VacancyFactory(department=company.yandex, is_published=True, status=VACANCY_STATUS.IN_PROGRESS)
    VacancyFactory(department=company.dep2, is_published=True, status=VACANCY_STATUS.IN_PROGRESS)
    VacancyFactory(department=company.dep2, is_published=True, status=VACANCY_STATUS.IN_PROGRESS)
    VacancyFactory(department=company.dep2, is_published=True, status=VACANCY_STATUS.CLOSED)  # closed vacancy
    VacancyFactory(department=company.dep12, is_published=True, status=VACANCY_STATUS.IN_PROGRESS)
    VacancyFactory(department=company.dep12, is_published=True, status=VACANCY_STATUS.IN_PROGRESS)
    VacancyFactory(department=company.dep111, is_published=True, status=VACANCY_STATUS.OFFER_PROCESSING)
    VacancyFactory(department=company.dep111, is_published=True, status=VACANCY_STATUS.OFFER_PROCESSING)

    VacancyFactory(department=company.dep111, is_published=True, status=VACANCY_STATUS.ON_APPROVAL)
    VacancyFactory(department=company.dep111, is_published=True, status=VACANCY_STATUS.OFFER_ACCEPTED)
    VacancyFactory(department=company.dep111, is_published=False, status=VACANCY_STATUS.OFFER_PROCESSING)

    VacancyFactory(department=None, is_published=False, status=VACANCY_STATUS.OFFER_PROCESSING)
    VacancyFactory(department=None, is_published=False, status=VACANCY_STATUS.CLOSED)

    return Vacancy.objects.all()


def get_positions(departments, positions_filter_context):
    departments_ids = [dep['id'] for dep in departments]
    positions_qs = list(
        positions_filter_context
        .positions_qs()
        .filter(department_id__in=departments_ids)
        .order_by('department')
    )

    return {
        dep_id: list(positions)
        for dep_id, positions in groupby(positions_qs, lambda p: p['department_id'])
    }


def get_vacancies(departments, vacancies_filter_context):
    departments_ids = [dep['id'] for dep in departments]
    vacancies_qs = list(
        vacancies_filter_context
        .vacancies_qs()
        .filter(department_id__in=departments_ids)
        .order_by('department')
    )

    return {
        dep_id: list(vacancies)
        for dep_id, vacancies in groupby(vacancies_qs, lambda p: p['department_id'])
    }


def test_vacancies_filter_context(company, vacancies, settings):
    permissons = VacancyPermissions(company.persons['general'])
    former_employees_dep = DepartmentFactory(url='former',)
    settings.INTRANET_DISMISSED_STAFF_DEPARTMENT_ID = former_employees_dep.id
    VacancyFactory(department=former_employees_dep, is_published=True, status=VACANCY_STATUS.IN_PROGRESS)

    vfc = VacanciesFilterContext(permissons)
    vfc_vacancies = list(vfc.vacancies_objects_qs())
    for vacancy in vacancies:
        if vacancy.department is None or vacancy.department == former_employees_dep:
            assert vacancy not in vfc_vacancies
            continue

        if vacancy.status in vfc.VALID_STATUSES:
            assert vacancy in vfc_vacancies
        else:
            assert vacancy not in vfc_vacancies

    valid_statuses = [VACANCY_STATUS.IN_PROGRESS, VACANCY_STATUS.ON_APPROVAL]
    vfc = VacanciesFilterContext(permissions=permissons, status__in=valid_statuses, is_published=True)
    vfc_vacancies = list(vfc.vacancies_objects_qs())
    for vacancy in vacancies:
        if vacancy.department is None or vacancy.department == former_employees_dep:
            assert vacancy not in vfc_vacancies
            continue

        if vacancy.status in valid_statuses and vacancy.is_published:
            assert vacancy in vfc_vacancies
        else:
            assert vacancy not in vfc_vacancies


def vacancies_department_filter_qs(filter_context, url):
    # type: (VacanciesFilterContext, AnyStr) -> QuerySet
    yandex_dep = filter_context.departments_qs().get(url=url)

    return filter_by_heirarchy(
        Department.objects.all(),
        mptt_objects=[yandex_dep],
        by_children=True,
        include_self=True,
    )


def test_vacancy_department_info_provider(company, vacancies, settings):
    permissions = VacancyPermissions(company.persons['general'])
    former_employees_dep = DepartmentFactory(url='former',)
    settings.INTRANET_DISMISSED_STAFF_DEPARTMENT_ID = former_employees_dep.id
    VacancyFactory(department=former_employees_dep, is_published=True, status=VACANCY_STATUS.IN_PROGRESS)

    filter_context = VacanciesFilterContext(permissions=permissions, is_published=True)
    VacanciesEntityInfo(filter_context)

    department = company.dep1

    filter_context.departments_qs().get(url=department.url)
    departments = list(
        vacancies_department_filter_qs(filter_context, department.url)
        .values(*VacanciesFilterContext.dep_fields)
        .order_by('tree_id', 'level', 'lft')
    )
    vacancies = get_vacancies(departments, filter_context)

    result = TreeBuilder(
        VacanciesEntityInfo(filter_context)
    ).get_for_info_list(departments, vacancies)

    assert len(result) == 5

    assert result[0]['url'] == department.url
    assert result[0]['info'] == {}  # no published vacancies in yandex_dep1

    assert result[2]['url'] == 'yandex_dep1_dep12'
    assert len(result[2]['info']['vacancies']) == 2  # 2 published vacancies in yandex_dep1_dep12

    assert result[3]['url'] == 'yandex_dep1_dep11_dep111'
    assert len(result[3]['info']['vacancies']) == 4  # 4 published vacancies in yandex_dep1_dep11_dep111 (1 unpublished)
