# coding: utf-8


import pytest

from idm.services.models import Service
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def test_service_suggest(client, arda_users):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/services/all')

    client.login('frodo')

    root = Service.objects.get_root()
    Service.objects.create(name='Сервис 1', slug='service1', external_id=1, parent=root)
    Service.objects.create(name='Сервис 2', slug='service2', external_id=2, parent=root)

    response = client.json.get(suggest_url)

    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'slug': 'service1', 'id': 1, 'name': 'Сервис 1'},
            {'slug': 'service2', 'id': 2, 'name': 'Сервис 2'},
        ]
    }

    response = client.json.get(suggest_url, {'limit': 1})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 1},
        'data': [
            {'slug': 'service1', 'id': 1, 'name': 'Сервис 1'},
        ]
    }

    response = client.json.get(suggest_url, {'q': 'ис 1'})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'slug': 'service1', 'id': 1, 'name': 'Сервис 1'},
        ]
    }

    response = client.json.get(suggest_url, {'id': 1})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'slug': 'service1', 'id': 1, 'name': 'Сервис 1'},
        ]
    }

    response = client.json.get(suggest_url, {'id': 999})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': []
    }

    response = client.json.get(suggest_url, {'offset': 'a'})
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'offset': ['Ожидается число']
        }
    }


def test_order_by_name_length(client, arda_users):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/services/all')

    client.login('frodo')

    root = Service.objects.get_root()

    # При лексикографической сортировке был бы первым в выдаче ручки, но при сортировке по длине должен стать вторым
    Service.objects.create(name='00000000 Сервис 1', slug='service1', external_id=1, parent=root)
    Service.objects.create(name='Сервис 2', slug='service2', external_id=2, parent=root)

    response = client.json.get(suggest_url, {'q': 'сервис'})
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'slug': 'service2', 'id': 2, 'name': 'Сервис 2'},
            {'slug': 'service1', 'id': 1, 'name': '00000000 Сервис 1'},
        ]
    }
