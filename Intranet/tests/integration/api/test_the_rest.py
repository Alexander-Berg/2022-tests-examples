import pytest

from unittest.mock import patch

from django.urls.base import reverse

from intranet.femida.tests.utils import get_mocked_event


pytestmark = pytest.mark.django_db


def test_meta(su_client):
    url = reverse('api:meta')
    response = su_client.get(url)
    assert response.status_code == 200


@patch('intranet.femida.src.api.interviews.views.get_event', get_mocked_event)
@pytest.mark.parametrize('query_params', [
    {},
    {'instanceStartTs': "2022-07-03T05:00:00"},
])
def test_interview_event(su_client, query_params):
    url = reverse('api:calendar-event', kwargs={'event_id': 1})
    response = su_client.get(url, query_params)
    assert response.status_code == 200
