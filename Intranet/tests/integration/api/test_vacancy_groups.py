import pytest

from django.urls.base import reverse

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def get_vacancy_group_form_data():
    return {
        'name': 'Name',
        'is_active': True,
        'recruiters': [f.create_recruiter().username],
        'vacancies': [f.create_active_vacancy().id],
    }


def test_vacancy_group_list(su_client):
    url = reverse('api:vacancy-groups:list')
    response = su_client.get(url)
    assert response.status_code == 200


def test_vacancy_group_create(su_client):
    url = reverse('api:vacancy-groups:list')
    data = get_vacancy_group_form_data()
    response = su_client.post(url, data)
    assert response.status_code == 201


def test_vacancy_group_detail(su_client):
    group = f.VacancyGroupFactory.create()
    url = reverse('api:vacancy-groups:detail', kwargs={'pk': group.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_vacancy_group_update(su_client):
    group = f.VacancyGroupFactory.create()
    url = reverse('api:vacancy-groups:detail', kwargs={'pk': group.id})
    data = get_vacancy_group_form_data()
    response = su_client.put(url, data)
    assert response.status_code == 200


def test_vacancy_group_create_form(su_client):
    url = reverse('api:vacancy-groups:create-form')
    response = su_client.get(url)
    assert response.status_code == 200


def test_vacancy_group_update_form(su_client):
    group = f.VacancyGroupFactory.create()
    url = reverse('api:vacancy-groups:update-form', kwargs={'pk': group.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_vacancy_group_activate(su_client):
    group = f.VacancyGroupFactory.create(is_active=False)
    url = reverse('api:vacancy-groups:activate', kwargs={'pk': group.id})
    response = su_client.post(url)
    assert response.status_code == 200


def test_vacancy_group_deactive(su_client):
    group = f.VacancyGroupFactory.create()
    url = reverse('api:vacancy-groups:deactivate', kwargs={'pk': group.id})
    response = su_client.post(url)
    assert response.status_code == 200
