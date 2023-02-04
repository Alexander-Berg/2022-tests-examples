# coding: utf-8


import pytest

from idm.utils import reverse


pytestmark = pytest.mark.django_db


def test_rolenodeset_suggest_all(client, complex_system_with_nodesets, arda_users):
    """Проверка ручки саджеста групп узлов ролей (без фильтрации)"""

    system = complex_system_with_nodesets
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/nodesets/all')
    client.login('frodo')

    response = client.json.get(suggest_url)
    assert response.status_code == 400
    assert response.json() == {
        'error_code': 'BAD_REQUEST',
        'message': 'Invalid data sent',
        'errors': {
            'system': ['Обязательное поле.']
        }
    }

    response = client.json.get(suggest_url, {'system': system.slug})
    assert response.status_code == 200
    assert response.json()['data'] == [
        {'id': 'developer_id', 'name': 'Разработчик'},
        {'id': 'invisic_id', 'name': 'невидимка'},
        {'id': 'manager_id', 'name': 'Менеджер'},
    ]

    response = client.json.get(suggest_url, {'system': system.slug, 'id': 'developer_id'})
    assert response.status_code == 200
    assert response.json()['data'] == [{
        'id': 'developer_id', 'name': 'Разработчик'
    }]

    response = client.json.get(suggest_url, {'system': system.slug, 'q': 'man'})
    assert response.status_code == 200
    assert response.json()['data'] == [{
        'id': 'manager_id', 'name': 'Менеджер'
    }]

    response = client.json.get(suggest_url, {'system': system.slug, 'q': 'работ'})
    assert response.status_code == 200
    assert response.json()['data'] == [{
        'id': 'developer_id', 'name': 'Разработчик'
    }]

    response = client.json.get(suggest_url, {'system': system.slug, 'id': 'developer_id'})
    assert response.status_code == 200
    assert response.json()['data'] == [{
        'id': 'developer_id', 'name': 'Разработчик'
    }]

    response = client.json.get(suggest_url, {'system': system.slug, 'id': 'developer'})
    assert response.status_code == 200
    assert response.json()['data'] == []
