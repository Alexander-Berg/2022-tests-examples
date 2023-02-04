import json

import pytest

from django.core.urlresolvers import reverse

from staff.lib.auth.utils import get_or_create_test_user
from staff.lib.testing import DepartmentStaffFactory

from staff.departments.models import DepartmentRoles
from staff.departments.tests.factories import VacancyFactory
from staff.femida.constants import VACANCY_STATUS
from staff.headcounts.tests.factories import HeadcountPositionFactory
from staff.oebs.constants import PERSON_POSITION_STATUS


@pytest.mark.django_db
def test_load_vacancies(company, mocked_mongo, client):
    DepartmentStaffFactory(
        department=company.yandex,
        staff=get_or_create_test_user().staff,
        role_id=DepartmentRoles.HR_ANALYST_TEMP.value,
    )

    department_url = 'yandex_dep1'
    view_url = reverse('proposal-api:load-vacancies', kwargs={'department_url': department_url})
    response = client.get(view_url)
    assert response.status_code == 200

    content = json.loads(response.content)
    assert set(content.keys()) == {
        'info',
        'is_deep',
        'name',
        'chain',
        'level',
        'url',
        'order_field',
        'descendants',
        'has_descendants',
        'id',
    }
    assert content['is_deep'] is False
    assert content['url'] == department_url


@pytest.mark.django_db
@pytest.mark.parametrize('hc_dep, old_visible, new_visible', (
    ('yandex', False, True),  # Родительский департамент
    ('dep1', True, True),     # Текущий департамент
    ('dep2', False, True),    # Департамент в соседнем дереве
    ('dep11', False, True),   # Дочерний департамент
))
def test_load_vacancies_visibility(company, mocked_mongo, client, hc_dep, old_visible, new_visible):
    hc_dep = company[hc_dep]
    vacancy = VacancyFactory(
        department=company.dep1,
        status=VACANCY_STATUS.IN_PROGRESS,
    )
    HeadcountPositionFactory(
        code=vacancy.headcount_position_code,
        department=hc_dep,
        status=PERSON_POSITION_STATUS.VACANCY_OPEN,
    )
    DepartmentStaffFactory(
        department=company.yandex,
        staff=get_or_create_test_user().staff,
        role_id=DepartmentRoles.HR_ANALYST.value,
    )

    department_url = hc_dep.url
    view_url = reverse('proposal-api:load-vacancies', kwargs={'department_url': department_url})
    response = client.get(view_url)
    assert response.status_code == 200

    # Case 2: вакансия отображается в департаменте связанной БП, вне зависимости от департамента самой вакансии
    content = json.loads(response.content)
    assert ('vacancies' in content['info']) is new_visible
