# coding: utf-8


import pytest

from idm.utils import reverse

from idm.tests.utils import add_perms_by_role


pytestmark = pytest.mark.django_db


def test_internalrole_suggest_all(client, complex_system, arda_users):
    """Проверка ручки саджеста групп узлов ролей (без фильтрации)"""

    system = complex_system
    frodo = arda_users.frodo
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/internalroles/all')
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
    assert response.json()['data'] == []

    # добавляем фродо одну внутреннюю роль
    add_perms_by_role('roles_manage', frodo, system, '/subs/')

    response = client.json.get(suggest_url, {'system': system.slug})
    assert response.status_code == 200
    assert response.json()['data'] == [
        {'id': 'roles_manage', 'name': 'Менеджер ролей'},
    ]

    # роль на корневой узел не выводится
    add_perms_by_role('users_view', frodo, system, '/')

    response = client.json.get(suggest_url, {'system': system.slug})
    assert response.status_code == 200
    assert response.json()['data'] == [
        {'id': 'roles_manage', 'name': 'Менеджер ролей'},
    ]

    # нет дубликатов при одной роли на несколько узлов
    add_perms_by_role('roles_manage', frodo, system, '/rules/')

    response = client.json.get(suggest_url, {'system': system.slug})
    assert response.status_code == 200
    assert response.json()['data'] == [
        {'id': 'roles_manage', 'name': 'Менеджер ролей'},
    ]

    # несколько ролей на не корневые узлы
    add_perms_by_role('responsible', frodo, system, '/rules/')

    response = client.json.get(suggest_url, {'system': system.slug})
    assert response.status_code == 200
    assert response.json()['data'] == [
        {'id': 'responsible', 'name': 'Ответственный'},
        {'id': 'roles_manage', 'name': 'Менеджер ролей'},
    ]

    # фильтр по id
    response = client.json.get(suggest_url, {'system': system.slug, 'id': 'roles_manage'})
    assert response.json()['data'] == [
        {'id': 'roles_manage', 'name': 'Менеджер ролей'},
    ]

    response = client.json.get(suggest_url, {'system': system.slug, 'id': 'manage'})
    assert response.json()['data'] == []

    # фильтр по названию
    response = client.json.get(suggest_url, {'system': system.slug, 'q': 'manage'})
    assert response.status_code == 200
    assert response.json()['data'] == [
        {'id': 'roles_manage', 'name': 'Менеджер ролей'},
    ]

    # фильтр по названию
    response = client.json.get(suggest_url, {'system': system.slug, 'q': 'ответ'})
    assert response.status_code == 200
    assert response.json()['data'] == [
        {'id': 'responsible', 'name': 'Ответственный'},
    ]

    response = client.json.get(suggest_url, {'system': system.slug, 'q': 'resp'})
    assert response.status_code == 200
    assert response.json()['data'] == [
        {'id': 'responsible', 'name': 'Ответственный'},
    ]


def test_superuser_suggest(client, complex_system, arda_users):
    """Проверим, что флаг is_superuser не протекает. RULES-3070"""

    frodo = arda_users.frodo
    legolas = arda_users.legolas
    add_perms_by_role('roles_manage', legolas, complex_system, '/subs/')
    add_perms_by_role('superuser', frodo)

    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/internalroles/all')
    client.login('frodo')
    response = client.json.get(suggest_url, {'system': complex_system.slug})
    assert response.status_code == 200
    assert response.json()['data'] == []
