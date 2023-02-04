# coding: utf-8


import pytest

from idm.core.models import NetworkMacro
from idm.tests.utils import create_user
from idm.utils import reverse

# разрешаем использование базы в тестах
pytestmark = pytest.mark.django_db


def test_macro_suggest(client):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/fields/macros/all')

    client.login(create_user())

    for macro in ['_SHIRE_TEST_NETS_', '_ROHAN_PROD_NETS_']:
        NetworkMacro.objects.create(slug=macro)

    response = client.json.get(suggest_url)
    assert response.status_code == 200
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'slug': '_ROHAN_PROD_NETS_', 'id': '_ROHAN_PROD_NETS_', 'name': '_ROHAN_PROD_NETS_'},
            {'slug': '_SHIRE_TEST_NETS_', 'id': '_SHIRE_TEST_NETS_', 'name': '_SHIRE_TEST_NETS_'},
        ]
    }

    response = client.json.get(suggest_url, {'limit': 1})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 1},
        'data': [
            {'slug': '_ROHAN_PROD_NETS_', 'id': '_ROHAN_PROD_NETS_', 'name': '_ROHAN_PROD_NETS_'},
        ]
    }

    for query in ['tes', 'TES']:
        response = client.json.get(suggest_url, {'q': query})
        assert response.json() == {
            'meta': {'offset': 0, 'limit': 20},
            'data': [
                {'slug': '_SHIRE_TEST_NETS_', 'id': '_SHIRE_TEST_NETS_', 'name': '_SHIRE_TEST_NETS_'},
            ]
        }

    response = client.json.get(suggest_url, {'id': '_ROHAN_PROD_NETS_'})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
            {'slug': '_ROHAN_PROD_NETS_', 'id': '_ROHAN_PROD_NETS_', 'name': '_ROHAN_PROD_NETS_'},
        ]
    }

    response = client.json.get(suggest_url, {'id': '_VALINOR_DEV_NETS_'})
    assert response.json() == {
        'meta': {'offset': 0, 'limit': 20},
        'data': [
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
