# coding: utf-8

import pytest

from procu.utils.test import assert_status, prepare_user

pytestmark = [pytest.mark.django_db]

# ------------------------------------------------------------------------------

STATUSES_INT = [
    {'id': 'draft', 'name': 'Черновик', 'is_active': False},
    {'id': 'bidding', 'name': 'Сбор предложений', 'is_active': True},
    {'id': 'review', 'name': 'Выбор поставщика', 'is_active': True},
    {'id': 'checkout', 'name': 'Согласование документов', 'is_active': True},
    {'id': 'shipped', 'name': 'Поставка', 'is_active': True},
    {'id': 'closed', 'name': 'Закрыто', 'is_active': False},
]

STATUSES_EXT = [x for x in STATUSES_INT if x['id'] != 'draft']


@pytest.mark.parametrize(
    'env,user,ref',
    [
        ('internal', 'robot-procu', STATUSES_INT),
        ('external', 's001@procu.ru', STATUSES_EXT),
    ],
)
def test_statuses_all(clients, env, user, ref):

    client = clients[env]
    prepare_user(client, username=user)

    resp = client.get(f'/api/suggests/statuses', data={'scope': env})
    assert_status(resp, 200)
    assert resp.json()['results'] == ref


# ------------------------------------------------------------------------------


LOG_GROUPS = [
    {"id": "enquiry", "title": "Заявки", "type": "default"},
    {"id": "quote", "title": "Предложения", "type": "default"},
    {"id": "quote_product", "title": "Предложенные товары", "type": "default"},
    {"id": "enquiry_product", "title": "Запрошенные товары", "type": "default"},
    {
        "id": "enquiry_comment",
        "title": "Комментарии к заявкам",
        "type": "default",
    },
    {
        "id": "quote_comment",
        "title": "Комментарии к предложениям",
        "type": "default",
    },
    {"id": "enquiry_link", "title": "Связи", "type": "default"},
    {"id": "invoice", "title": "Счета", "type": "default"},
    {"id": "request", "title": "Запросы", "type": "default"},
]


def test_log_groups_all(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu')

    resp = client.get('/api/suggests/log_groups')
    assert_status(resp, 200)
    assert resp.json()['results'] == LOG_GROUPS


def test_log_groups_include_by_id(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu')

    resp = client.get('/api/suggests/log_groups', data={'id': 'quote'})
    assert_status(resp, 200)

    ids = [x['id'] for x in resp.json()['results']]

    assert ids == ['quote']
