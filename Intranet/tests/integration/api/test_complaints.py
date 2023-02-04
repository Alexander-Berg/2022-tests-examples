import pytest

from django.urls.base import reverse

from intranet.femida.src.problems.choices import COMPLAINT_KINDS

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_complaint_list(su_client):
    f.ComplaintFactory.create()
    url = reverse('api:complaints:list')
    response = su_client.get(url)
    assert response.status_code == 200


def test_complaint_filter_form(su_client):
    url = reverse('api:complaints:filter-form')
    response = su_client.get(url)
    assert response.status_code == 200


def test_complaint_create(su_client):
    url = reverse('api:complaints:list')
    data = {
        'problem': f.ProblemFactory.create().id,
        'kind': COMPLAINT_KINDS.other,
    }
    response = su_client.post(url, data)
    assert response.status_code == 201


def test_complaint_create_form(su_client):
    url = reverse('api:complaints:create-form')
    response = su_client.get(url)
    assert response.status_code == 200


def test_complaint_reject(su_client):
    complaint = f.ComplaintFactory.create()
    url = reverse('api:complaints:reject', kwargs={'pk': complaint.id})
    response = su_client.post(url)
    assert response.status_code == 200


def test_complaint_resolve(su_client):
    complaint = f.ComplaintFactory.create()
    url = reverse('api:complaints:resolve', kwargs={'pk': complaint.id})
    response = su_client.post(url)
    assert response.status_code == 200
