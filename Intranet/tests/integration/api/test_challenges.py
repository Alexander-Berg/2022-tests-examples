import pytest

from django.urls.base import reverse

from intranet.femida.src.candidates import choices

from intranet.femida.tests import factories as f


pytestmark = pytest.mark.django_db


def test_challenge_estimate(su_client):
    challenge = f.ChallengeFactory.create(
        type=choices.CHALLENGE_TYPES.quiz,
        status=choices.CHALLENGE_STATUSES.pending_review,
    )
    url = reverse('api:challenges:estimate', kwargs={'pk': challenge.id})
    data = {
        'comment': 'comment',
        'resolution': choices.CHALLENGE_RESOLUTIONS.hire,
    }
    response = su_client.post(url, data)
    assert response.status_code == 200


def test_challenge_estimate_form(su_client):
    challenge = f.ChallengeFactory.create()
    url = reverse('api:challenges:estimate-form', kwargs={'pk': challenge.id})
    response = su_client.get(url)
    assert response.status_code == 200


def test_challenge_cancel(su_client):
    challenge = f.ChallengeFactory.create(
        type=choices.CHALLENGE_TYPES.contest,
        status=choices.CHALLENGE_STATUSES.assigned,
    )
    url = reverse('api:challenges:cancel', kwargs={'pk': challenge.id})
    data = {
        'cancel_reason': 'Nothing',
    }
    response = su_client.post(url, data)
    assert response.status_code == 200
