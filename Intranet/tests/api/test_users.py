import pytest

from django.urls import reverse

from intranet.search.tests.helpers import models_helpers as mh


pytestmark = pytest.mark.django_db(transaction=False)


def test_get_user_meta(api_client):
    user = mh.User(
        frequently_searched_people={1: 100, 2: 50},
        recently_searched_people={3: 10, 4: 1},
    )
    url = reverse('users-meta')
    response = api_client.get(url, {'user': user.username})
    assert response.status_code == 200
    assert response.json() == {
        'frequently_searched_people': {'1': 100, '2': 50},
        'recently_searched_people': {'3': 10, '4': 1},
    }
