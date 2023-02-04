import pytest

from django.urls.base import reverse

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_create_problem(su_client):
    category = f.CategoryFactory()
    url = reverse('api:problems:list')
    data = {
        'summary': 'summary',
        'description': 'description',
        'categories': [category.id],
    }
    response = su_client.post(url, data)
    assert response.status_code == 201


def test_create_problem_form(su_client):
    url = reverse('api:problems:create-form')
    response = su_client.get(url)
    assert response.status_code == 200


def test_search_problems(su_client):
    url = reverse('api:problems:search')
    response = su_client.get(url)
    assert response.status_code == 200


def test_search_problems_form(su_client):
    url = reverse('api:problems:search-form')
    response = su_client.get(url)
    assert response.status_code == 200


def test_detail_problem(su_client):
    problem = f.create_problem()
    url = reverse('api:problems:detail', kwargs={'pk': problem.pk})
    response = su_client.get(url)
    assert response.status_code == 200


def test_update_problem(su_client):
    problem = f.create_problem()
    category = f.CategoryFactory.create()
    url = reverse('api:problems:detail', kwargs={'pk': problem.pk})
    data = {
        'summary': 'updated_summary',
        'description': 'updated_description',
        'categories': [category.id],
    }
    response = su_client.put(url, data)
    assert response.status_code == 200


def test_partial_update_problem(su_client):
    problem = f.create_problem()
    url = reverse('api:problems:detail', kwargs={'pk': problem.pk})
    data = {}
    response = su_client.patch(url, data)
    assert response.status_code == 200


def test_update_problem_form(su_client):
    problem = f.create_problem()

    url = reverse('api:problems:update-form', kwargs={'pk': problem.pk})
    response = su_client.get(url)
    assert response.status_code == 200


def test_merge_problem(su_client):
    problem = f.create_problem()
    original_problem = f.create_problem()

    url = reverse('api:problems:merge', kwargs={'pk': problem.pk})
    data = {
        'original_problem_id': original_problem.id,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


def test_problem_assignments(su_client):
    f.create_interview_with_assignments()

    url = reverse('api:problems:assignments', kwargs={'pk': 1})
    response = su_client.get(url)
    assert response.status_code == 200
