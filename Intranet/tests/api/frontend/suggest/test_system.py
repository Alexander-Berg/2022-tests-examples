# coding: utf-8


import pytest

from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def test_system_suggest(client, users_for_test, pt1_system, simple_system):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/systems/all')

    client.login('admin')

    simple_system.description = 'description'
    simple_system.description_text = 'suggest'
    simple_system.save()

    response = client.json.get(suggest_url)
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'slug': 'simple', 'id': 'simple', 'name': 'Simple система', 'description': 'suggest'},
            {'slug': 'test1', 'id': 'test1', 'name': 'Test1 система', 'description': ''},
        ]
    }

    response = client.json.get(suggest_url, {'limit': 1})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 1},
        'data': [
            {'slug': 'simple', 'id': 'simple', 'name': 'Simple система', 'description': 'suggest'},
        ]
    }

    for query in ['tes', 'Tes']:
        response = client.json.get(suggest_url, {'q': query})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': [
                {'slug': 'test1', 'id': 'test1', 'name': 'Test1 система', 'description': ''},
            ]
        }

    for id_query in ['test1', 'Test1']:
        response = client.json.get(suggest_url, {'id': id_query})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': [
                {'slug': 'test1', 'id': 'test1', 'name': 'Test1 система', 'description': ''},
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
