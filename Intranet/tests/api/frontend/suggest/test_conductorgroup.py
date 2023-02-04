# coding: utf-8


import pytest

from idm.core.models import ConductorGroup
from idm.tests.utils import create_user
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def test_conductor_suggest(client):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/fields/conductor_groups/all')

    client.login(create_user())

    ConductorGroup.objects.create(external_id=1, name='great-dragons-of-the-north')
    ConductorGroup.objects.create(external_id=2, name='durin\'s folk')
    ConductorGroup.objects.create(external_id=255, name='kings of men')

    response = client.json.get(suggest_url)
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [{'id': name, 'name': name} for name in [
            'great-dragons-of-the-north',
            'durin\'s folk',
            'kings of men',
        ]]
    }

    response = client.json.get(suggest_url, {'limit': 1})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 1},
        'data': [
            {'id': 'great-dragons-of-the-north', 'name': 'great-dragons-of-the-north'},
        ]
    }

    for query in ['g', 'G']:
        response = client.json.get(suggest_url, {'q': query})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': [
                {'id': 'great-dragons-of-the-north', 'name': 'great-dragons-of-the-north'},
                {'id': 'kings of men', 'name': 'kings of men'},
            ]
        }

    response = client.json.get(suggest_url, {'id': 'kings of men'})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'id': 'kings of men', 'name': 'kings of men'},
        ]
    }

    response = client.json.get(suggest_url, {'id': 'queens of men'})
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
