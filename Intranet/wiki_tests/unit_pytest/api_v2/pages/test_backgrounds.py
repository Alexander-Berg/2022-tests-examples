import pytest

from django.test import override_settings

from intranet.wiki.tests.wiki_tests.common.assert_helpers import assert_json

pytestmark = [pytest.mark.django_db]


def test_get_backgrounds(client, wiki_users):
    client.login(wiki_users.thasonic)

    backgrounds = [
        {'id': 8, 'url': 'https://yandex.net/8.jpg', 'preview': 'https://yandex.net/8_small.jpg', 'type': 'image'},
        {'id': 0, 'gradient': 'linear-gradient(blue, pink)', 'type': 'gradient'},
        {'id': 5, 'url': 'https://yandex.net/5.jpg', 'preview': 'https://yandex.net/5_small.jpg', 'type': 'image'},
        {'id': 15, 'color': 'rgba(73, 160, 246, 0.3)', 'type': 'color'},
    ]

    with override_settings(PAGE_BACKGROUNDS=backgrounds):
        response = client.get('/api/v2/public/pages/backgrounds')

    assert response.status_code == 200, response.json()
    assert_json(response.json(), {'results': sorted(backgrounds, key=lambda x: x['id'])})


def test_get_backgrounds__empty(client, wiki_users):
    client.login(wiki_users.thasonic)

    with override_settings(PAGE_BACKGROUNDS=[]):
        response = client.get('/api/v2/public/pages/backgrounds')

    assert response.status_code == 200
    assert len(response.json()['results']) == 0
