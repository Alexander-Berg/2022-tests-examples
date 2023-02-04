import uuid

import pytest

from django.urls.base import reverse

from intranet.femida.src.candidates.choices import CHALLENGE_STATUSES
from intranet.femida.src.certifications.models import Certification
from intranet.femida.src.interviews.choices import INTERVIEW_STATES
from intranet.femida.tests import factories as f

pytestmark = pytest.mark.django_db


@pytest.mark.parametrize('is_published, can_publish', (
    pytest.param(True, False, id='published'),
    pytest.param(False, True, id='unpublished'),
))
def test_certifications_actions(tvm_certification_client, is_published, can_publish):
    cert = f.CertificationFactory(is_published=is_published)
    f.create_interview(consideration=cert.consideration, state=INTERVIEW_STATES.finished)
    f.ChallengeFactory(consideration=cert.consideration, status=CHALLENGE_STATUSES.finished)

    url = reverse(
        'private-api:certifications:detail',
        kwargs={'private_uuid': cert.private_uuid},
    )
    response = tvm_certification_client.get(url)

    assert response.status_code == 200
    actions = response.json().get('actions', {})
    assert actions.get('publish') == can_publish and actions.get('unpublish') != can_publish
    assert len(response.json()['interviews']) == 1
    assert len(response.json()['challenges']) == 1


def test_alive_interviews(tvm_certification_client):
    cert = f.CertificationFactory(is_published=True)
    for state in INTERVIEW_STATES:
        f.create_interview(
            consideration=cert.consideration,
            state=state[0],
        )
    for status in CHALLENGE_STATUSES:
        f.ChallengeFactory(
            consideration=cert.consideration,
            status=status[0],
        )

    url = reverse(
        'private-api:certifications:detail',
        kwargs={'private_uuid': cert.private_uuid},
    )
    response = tvm_certification_client.get(url)

    assert response.status_code == 200
    assert len(response.json()['interviews']) == 1
    assert len(response.json()['challenges']) == 1


def test_wrong_uuid(tvm_certification_client):
    url = reverse(
        'private-api:certifications:detail',
        kwargs={'private_uuid': str(uuid.uuid4())},
    )
    response = tvm_certification_client.get(url)
    assert response.status_code == 404


def test_publish_cert(tvm_certification_client):
    cert = f.CertificationFactory(is_published=False)
    url = reverse(
        'private-api:certifications:publish',
        kwargs={'private_uuid': cert.private_uuid},
    )
    response = tvm_certification_client.post(url)

    assert response.status_code == 200
    response_data = response.json()
    assert response_data['public_uuid'] == cert.public_uuid
    assert response_data.get('candidate')
    assert response_data.get('consideration')
    assert Certification.objects.filter(id=cert.id).first().is_published


def test_unpublish_cert(tvm_certification_client):
    cert = f.CertificationFactory(is_published=True)
    url = reverse(
        'private-api:certifications:unpublish',
        kwargs={'private_uuid': cert.private_uuid},
    )
    response = tvm_certification_client.post(url)
    assert response.status_code == 200
    assert response.json()['public_uuid'] is None
    assert not Certification.objects.filter(id=cert.id).first().is_published


def test_public_cert_view_published(tvm_certification_client):
    cert = f.CertificationFactory(is_published=True)
    url = reverse(
        'private-api:certifications:public',
        kwargs={'public_uuid': cert.public_uuid},
    )
    response = tvm_certification_client.get(url)
    assert response.status_code == 200
    data = response.json()
    assert set(data['consideration']) == {'started', 'finished'}
    assert set(data['candidate']) == {'first_name', 'last_name'}


def test_public_cert_view_unpublished(tvm_certification_client):
    cert = f.CertificationFactory(is_published=False)
    url = reverse(
        'private-api:certifications:public',
        kwargs={'public_uuid': cert.public_uuid},
    )
    response = tvm_certification_client.get(url)
    assert response.status_code == 200
    data = response.json()
    assert data == {'is_published': False}


def test_public_cert_view_not_found(tvm_certification_client):
    url = reverse(
        'private-api:certifications:public',
        kwargs={'public_uuid': str(uuid.uuid4())},
    )
    response = tvm_certification_client.get(url)
    assert response.status_code == 404
