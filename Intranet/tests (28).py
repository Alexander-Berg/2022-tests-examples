# coding: utf-8
import pytest

from procu.utils.test import assert_status, prepare_user

pytestmark = [pytest.mark.django_db]


# ------------------------------------------------------------------------------


REFERENCE = [
    {
        'id': 1,
        'key': 'YP1',
        'title': 'Мультиварки',
        'username': 'robot-procu',
        'status': {'key': 'review', 'name': 'Выбор поставщика'},
    },
    {
        'id': 3,
        'key': 'YP3',
        'title': 'Дрели',
        'username': None,
        'status': {'key': 'draft', 'name': 'Черновик'},
    },
    {
        'id': 3,
        'key': 'YP3',
        'title': 'Дрели',
        'username': None,
        'status': {'key': 'draft', 'name': 'Черновик'},
    },
    {'id': 30, 'key': 'YP30'},
    {},
    {},
]


def test_magiclink(clients):

    client = clients['internal']
    # FIXME: use oauth
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {
        'urls': [
            'https://procu.yandex-team.ru/YP1',
            'https://procu.yandex-team.ru/YP3',
            'https://procu.test.yandex-team.ru/YP3',
            'https://procu.yandex-team.ru/YP30',
            'https://procu.yandex-team.ru',
            'https://ya.ru',
        ]
    }

    resp = client.post('/api/wiki/magiclink', data=data)

    assert_status(resp, 200)
    assert resp.json() == REFERENCE
