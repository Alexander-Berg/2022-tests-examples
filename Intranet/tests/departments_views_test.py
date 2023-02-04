import json
from functools import partial

import mock
import pytest
from django.core.urlresolvers import reverse
from django.conf import settings

from staff.femida.constants import VACANCY_STATUS

from staff.departments.models import DepartmentRoles, Department
from staff.departments.tests.factories import VacancyFactory, StaffFactory
from staff.departments.tree.views import tree, tree_expand, tree_info, value_streams

from staff.lib.testing import DepartmentStaffFactory


@pytest.fixture
def femida_user(db, company):
    robot_department = Department.objects.get(id=settings.ROBOT_DEPARTMENT_ID)
    return StaffFactory(login='robot-femida', department=robot_department).user


@pytest.mark.django_db()
def test_departments_tree(company, rf):
    vacancy_factory = partial(VacancyFactory, is_published=True)
    vacancy_factory(department=company.yandex, status=VACANCY_STATUS.IN_PROGRESS)
    vacancy_factory(department=company.dep1, status=VACANCY_STATUS.OFFER_PROCESSING)
    vacancy_factory(department=company.dep111, status=VACANCY_STATUS.SUSPENDED)
    vacancy_factory(department=company.dep2, status=VACANCY_STATUS.DRAFT)
    vacancy_factory(department=company.dep12, status=VACANCY_STATUS.CLOSED)

    request = rf.get(reverse('departments-api:tree-url', kwargs={'url': company.yandex.url}))
    request.user = company.persons['yandex-chief'].user

    response = tree(request, company.yandex.url)
    result = json.loads(response.content)

    yandex_dep_info = result['tree'][0]
    assert yandex_dep_info['has_descendants']
    assert yandex_dep_info['level'] == 0
    assert yandex_dep_info['persons_qty'] == 17
    assert yandex_dep_info['vacancies_count'] == 3

    assert 'is_expanded' in yandex_dep_info
    assert 'is_hidden' in yandex_dep_info


@pytest.mark.django_db()
def test_departments_tree_info_curators(mocked_mongo, company, rf):
    request = rf.get(reverse('departments-api:tree-info', kwargs={'url': company.dep1.url}))
    request.user = company.persons['yandex-chief'].user

    DepartmentStaffFactory(
        department=company.dep1,
        staff=company.persons['dep2-chief'],
        role_id=DepartmentRoles.CURATOR_EXPERIMENT.value,
    )
    DepartmentStaffFactory(
        department=company.dep1,
        staff=company.persons['dep2-chief'],
        role_id=DepartmentRoles.CURATOR_BU.value,
    )

    response = tree_info(request, company.dep1.url)
    dep1_info = json.loads(response.content)

    assert dep1_info['info']['curator_bu'][0]['login'] == company.persons['dep2-chief'].login
    assert dep1_info['info']['curator_experiment'][0]['login'] == company.persons['dep2-chief'].login
    assert dep1_info['is_bu']
    assert dep1_info['is_experiment']

    assert 'is_expanded' in dep1_info
    assert 'is_hidden' in dep1_info


@pytest.mark.django_db()
def test_departments_tree_curators(mocked_mongo, company, rf):
    request = rf.get(reverse('departments-api:tree-url', kwargs={'url': company.dep1.url}))
    request.user = company.persons['yandex-chief'].user

    DepartmentStaffFactory(
        department=company.dep1,
        staff=company.persons['dep2-chief'],
        role_id=DepartmentRoles.CURATOR_EXPERIMENT.value,
    )
    DepartmentStaffFactory(
        department=company.dep1,
        staff=company.persons['dep2-chief'],
        role_id=DepartmentRoles.CURATOR_BU.value,
    )

    response = tree(request, company.dep1.url)
    result = json.loads(response.content)
    dep1_info = result['tree'][0]['descendants'][0]

    assert dep1_info['is_bu']
    assert dep1_info['is_experiment']

    assert 'is_expanded' in dep1_info
    assert 'is_hidden' in dep1_info


@pytest.mark.django_db()
def test_departments_tree_expand_curators(mocked_mongo, company, rf):
    request = rf.get(reverse('departments-api:tree-expand', kwargs={'url': company.dep1.url}))
    request.user = company.persons['yandex-chief'].user

    DepartmentStaffFactory(
        department=company.dep1,
        staff=company.persons['dep2-chief'],
        role_id=DepartmentRoles.CURATOR_EXPERIMENT.value,
    )
    DepartmentStaffFactory(
        department=company.dep1,
        staff=company.persons['dep2-chief'],
        role_id=DepartmentRoles.CURATOR_BU.value,
    )
    with mock.patch('staff.departments.tree.views.TreeExpander._target_is_deep', mock.MagicMock(return_value=True)):
        response = tree_expand(request, company.yandex.url)
    dep1_info = json.loads(response.content)

    assert dep1_info['descendants'][0]['is_bu']
    assert dep1_info['descendants'][0]['is_experiment']

    assert 'is_expanded' in dep1_info['descendants'][0]
    assert 'is_hidden' in dep1_info['descendants'][0]


@mock.patch('staff.lib.decorators._check_service_id', lambda *a, **b: True)
def test_value_streams_view(rf, femida_user):
    request = rf.get(reverse('departments-api:value-streams'))

    request.user = femida_user
    request.yauser = None

    response = value_streams(request)

    assert response.status_code == 200, response.content

    data = json.loads(response.content)['result']
    assert len(data) == Department.valuestreams.count()
