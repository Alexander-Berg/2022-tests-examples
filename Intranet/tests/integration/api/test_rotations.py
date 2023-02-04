import pytest

from django.urls.base import reverse
from unittest.mock import patch

from intranet.femida.src.candidates.choices import ROTATION_REASONS

from intranet.femida.tests import factories as f
from intranet.femida.tests.mock.startrek import EmptyIssue


pytestmark = pytest.mark.django_db


@pytest.fixture
def rotation_data():
    return {
        'reason': ROTATION_REASONS.other,
        'publications': [f.create_publication().id, f.create_publication().id],
        'is_agree': True,
        'is_privacy_needed': True,
    }


def test_rotation_create_form(su_client):
    url = reverse('api:rotations:create-form')
    response = su_client.get(url)
    assert response.status_code == 200


def test_rotation_form_prefill(su_client):
    vac1 = f.create_publication()
    vac2 = f.create_publication()
    data = {
        'publications': [
            vac1.id,
            vac2.id,
        ],
    }
    url = reverse('api:rotations:create-form')
    response = su_client.get(url, data)

    publication_ids_expected = {vac1.id, vac2.id}
    data = response.json()['data']
    publication_ids_received = set(data['publications']['value'])
    assert publication_ids_expected == publication_ids_received


@patch('intranet.femida.src.candidates.startrek.issues.create_issue', lambda *a, **kw: EmptyIssue())
def test_rotation_create(su_client, rotation_data):
    url = reverse('api:rotations:list')
    response = su_client.post(url, rotation_data)
    assert response.status_code == 201


@patch('intranet.femida.src.candidates.startrek.issues.create_issue', lambda *a, **kw: EmptyIssue())
def test_rotation_create_already_exists(client, rotation_data):
    user = f.create_user()
    f.RotationFactory(created_by=user)
    client.login(user.username)
    url = reverse('api:rotations:list')
    response = client.post(url, rotation_data)
    assert response.status_code == 400
