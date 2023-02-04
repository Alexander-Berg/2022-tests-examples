# coding: utf-8

from unittest.mock import patch

import pytest

from procu.api import models
from procu.utils.test import assert_status, mock_find_tickets, prepare_user

pytestmark = [pytest.mark.django_db]


LINKS = {
    'results': [
        {
            'id': 1,
            'object': {
                'type': {'key': 'procu', 'name': 'Заявка'},
                'data': {
                    'key': 'YP2',
                    'title': 'Автомобили',
                    'assignee': {
                        'username': 'robot-procu',
                        'full_name': 'Робот Закупок',
                        'is_staff': True,
                        'is_deleted': False,
                    },
                    'priority': {'key': 'normal', 'name': 'Средний'},
                    'status': 'Черновик',
                    'deadline_at': None,
                },
                'url': 'https://procu.test.yandex-team.ru/YP2',
                'icon': 'https://procu.test.yandex-team.ru/favicon.ico',
            },
            'key': 'YP2',
            'can_delete': True,
        },
        {
            'id': 2,
            'object': {
                'type': {'key': 'tracker', 'name': 'Тикет в Трекере'},
                'data': {
                    'key': 'TESTLOGIC-142',
                    'title': 'Оплата корп. картой: Кофе',
                    'status': 'Open',
                    'assignee': {
                        'is_staff': True,
                        'username': 'robot-procu',
                        'full_name': 'Робот Закупок',
                        'is_deleted': False,
                    },
                    'priority': {'key': 'normal', 'name': 'Normal'},
                    'status_key': 'open',
                    'deadline_at': '2017-12-24',
                },
                'url': 'https://st.test.yandex-team.ru/TESTLOGIC-142',
                'icon': 'https://st.test.yandex-team.ru/favicon__testing.ico',
            },
            'key': 'TESTLOGIC-142',
            'can_delete': True,
        },
    ]
}


@mock_find_tickets()
@patch('procu.api.utils.tracker.get_service_ticket', lambda x: '***')
@patch('procu.api.utils.tracker.get_user_ticket', lambda x: '***')
def test_enquiry_links_list(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    resp = client.get('/api/enquiries/1/links')
    assert_status(resp, 200)

    assert resp.json() == LINKS


def test_enquiry_links_create(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    data = {
        'links': [
            {'id': 3, 'type': 'procu'},
            {'id': 'TESTSCS-1', 'type': 'tracker'},
        ]
    }

    with patch('procu.api.tasks.sync_remotes') as task:
        resp = client.post('/api/enquiries/1/links', data=data)
        task.assert_called()

    assert_status(resp, 200)

    qs = models.Link.objects.filter(enquiry_id=1)

    assert qs.filter(key='TESTSCS-1').exists()
    assert qs.filter(key='YP3').exists()


def test_enquiry_links_destroy(clients):

    client = clients['internal']
    prepare_user(client, username='robot-procu', roles=['admin'])

    qs = models.Link.objects.filter(enquiry_id=1)
    link_id = qs.values_list('id', flat=True).get(key='TESTLOGIC-142')

    with patch('procu.api.tasks.sync_remotes') as task:
        resp = client.delete(f'/api/enquiries/1/links/{link_id}')
        task.assert_called()

    assert_status(resp, 204)

    assert not qs.filter(key='TESTLOGIC-142').exists()
