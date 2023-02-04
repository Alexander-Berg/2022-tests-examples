import json
import pytest

from django.core.urlresolvers import reverse

from staff.departments.tests.factories import VacancyFactory


@pytest.mark.django_db
def test_load_vacancies(company, client):
    vacancy = VacancyFactory(department=company.dep1)
    vacancy_id = str(vacancy.id)
    view_url = reverse('proposal-api:vacancy-data')
    response = client.get(
        '{url}?vacancy_id={vacancy_id}'.format(
            vacancy_id=vacancy_id,
            url=view_url,
        )
    )
    assert response.status_code == 200
    content = json.loads(response.content)
    assert vacancy_id in content
    assert set(content[vacancy_id]) == {'meta', 'value'}
