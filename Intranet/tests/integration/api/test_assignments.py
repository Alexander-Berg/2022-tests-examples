import pytest

from django.urls.base import reverse

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_create_assignment(su_client):
    interview = f.create_interview()
    problem = f.create_problem()
    url = reverse('api:assignments:list', kwargs={'interview_id': interview.id})
    data = {
        'problem': problem.id,
    }
    response = su_client.post(url, data)
    assert response.status_code == 201


def test_create_assignment_form(su_client):
    interview = f.create_interview()
    url = reverse('api:assignments:create-form', kwargs={'interview_id': interview.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_update_assignment(su_client):
    assignment = f.AssignmentFactory.create()
    url = reverse('api:assignments-new:detail', kwargs={'pk': assignment.id})
    data = {
        'grade': 3,
        'comment': 'comment',
    }
    response = su_client.put(url, data)
    assert response.status_code == 200


def test_partial_update_assignment(su_client):
    assignment = f.AssignmentFactory.create()
    url = reverse('api:assignments-new:detail', kwargs={'pk': assignment.id})
    data = {'grade': 3}
    response = su_client.patch(url, data)
    assert response.status_code == 200


def test_update_assignment_form(su_client):
    assignment = f.AssignmentFactory.create()
    url = reverse('api:assignments-new:update-form', kwargs={'pk': assignment.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_delete_assignment(su_client):
    assignment = f.AssignmentFactory.create()
    url = reverse('api:assignments-new:detail', kwargs={'pk': assignment.id})
    response = su_client.delete(url)
    assert response.status_code == 204
