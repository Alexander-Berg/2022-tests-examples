# coding: utf-8


import pytest

from idm.tests.utils import mock_ids_repo, use_proxied_suggest
from idm.utils import reverse


pytestmark = [pytest.mark.django_db]


def _leave_only_logins(answer):
    answer['data'] = [user['login'] for user in answer['data']]
    return answer


@pytest.mark.robotless
def test_user_suggest(client, users_for_test):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/users/all')

    client.login('admin')

    objects = [
        {
            'url': 'https://staff.test.yandex-team.ru/frodo',
            'fields': [
                {
                    'type': 'is_active',
                    'value': True
                },
                {
                    'type': 'department_name',
                    'value': 'Some dept'
                },
                {
                    'type': 'first_name',
                    'value': 'Фродо'
                },
                {
                    'type': 'last_name',
                    'value': 'Беггинс'
                },
                {
                    'type': 'email',
                    'value': 'frodo@yandex-team.ru'
                }
            ],
            'layer': 'idm_subjects',
            'id': 'frodo',
            'title': 'Фродо Беггинс'
        }
    ]

    with use_proxied_suggest(should_use=False):
        response = client.json.get(suggest_url)

        assert response.status_code == 200
        expected = {
            'meta': {'offset': 0, 'limit': 20},
            'data': [
                {
                    'id': 'admin',
                    'is_group': False,
                    'department__name': 'Арда',
                    'first_name': '',
                    'full_name': 'admin',
                    'last_name': '',
                    'login': 'admin',
                    'work_email': 'admin@example.yandex.ru',
                },
                {
                    'id': 'terran',
                    'is_group': False,
                    'department__name': 'Арда',
                    'first_name': 'Легат',
                    'full_name': 'Легат Аврелий',
                    'last_name': 'Аврелий',
                    'login': 'terran',
                    'work_email': 'terran@example.yandex.ru',
                },
                {
                    'id': 'fantom',
                    'is_group': False,
                    'department__name': 'Арда',
                    'first_name': 'Легионер',
                    'full_name': 'Легионер Тит',
                    'last_name': 'Тит',
                    'login': 'fantom',
                    'work_email': 'fantom@example.yandex.ru',
                },
                {
                    'id': 'art',
                    'is_group': False,
                    'department__name': 'Арда',
                    'first_name': 'Центурион',
                    'full_name': 'Центурион Марк',
                    'last_name': 'Марк',
                    'login': 'art',
                    'work_email': 'art@example.yandex.ru',
                },
            ]
        }
        assert response.json() == expected

        response = client.json.get(suggest_url, {'id__in': 'art,fantom'})
        assert _leave_only_logins(response.json()) == {
            'meta': {'offset': 0, 'limit': 20},
            'data': ['fantom', 'art']
        }

        response = client.json.get(suggest_url, {'limit': 1})
        assert _leave_only_logins(response.json()) == {
            'meta': {'offset': 0, 'limit': 1},
            'data': ['admin'],
        }

        response = client.json.get(suggest_url, {'limit': 1, 'offset': 1})
        assert _leave_only_logins(response.json()) == {
            'meta': {'offset': 1, 'limit': 1},
            'data': ['terran'],
        }

        response = client.json.get(suggest_url, {'offset': 10})
        assert _leave_only_logins(response.json()) == {
            'meta': {'offset': 10, 'limit': 20},
            'data': [],
        }

        response = client.json.get(suggest_url, {'q': 'te'})
        assert _leave_only_logins(response.json()) == {
            'meta': {'offset': 0, 'limit': 20},
            'data': ['terran'],
        }

        response = client.json.get(suggest_url, {'q': 'ОНЕР'})
        assert _leave_only_logins(response.json()) == {
            'meta': {'offset': 0, 'limit': 20},
            'data': ['fantom'],
        }

        response = client.json.get(suggest_url, {'id': 'fantom'})
        assert _leave_only_logins(response.json()) == {
            'meta': {'offset': 0, 'limit': 20},
            'data': ['fantom'],
        }

        response = client.json.get(suggest_url, {'id': 'xxx'})
        assert _leave_only_logins(response.json()) == {
            'meta': {'offset': 0, 'limit': 20},
            'data': [],
        }

    with use_proxied_suggest(should_use=True), mock_ids_repo('intrasearch', 'idm_users', objects):
        response = client.json.get(suggest_url, {'q': 'hello'})
        assert response.json() == {
            'meta': {
                'limit': 20,
                'offset': 0
            },
            'data': [
                {
                    'first_name': 'Фродо',
                    'last_name': 'Беггинс',
                    'is_group': False,
                    'id': 'frodo',
                    'full_name': 'Фродо Беггинс',
                    'login': 'frodo',
                    'department__name': 'Some dept'
                }
            ]
        }

        response = client.json.get(suggest_url, {'id': 'fantom'})
        assert _leave_only_logins(response.json()) == {
            'meta': {'offset': 0, 'limit': 20},
            'data': ['fantom'],
        }

        response = client.json.get(suggest_url, {'limit': 'a', 'offset': '-2'})
        assert response.status_code == 400
        assert response.json() == {
            'error_code': 'BAD_REQUEST',
            'message': 'Invalid data sent',
            'errors': {
                'limit': ['Ожидается число'],
                'offset': ['Значение должно быть неотрицательным'],
            }
        }


def test_user_with_hyphen(client, simple_system, arda_users):
    """
    GET /testapi/users/
    https://st.yandex-team.ru/RULES-2145
    Проверяем, что пользователь с дефисом в username находится через саджест
    """
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/users/all')
    client.login('frodo')
    response = client.json.get(suggest_url, {'system': simple_system.slug, 'q': 'ch-'})
    data = response.json()['data']
    assert len(data) == 1
    assert data[0]['id'] == 'witch-king-of-angmar'
    response = client.json.get(suggest_url, {'system': simple_system.slug, 'q': 'witch'})
    data = response.json()['data']
    assert len(data) == 1
    assert data[0]['id'] == 'witch-king-of-angmar'


def test_search_user_by_en_name_and_username(client, simple_system, arda_users):
    """
    GET /testapi/users/
    Проверяем, что пользователя можно найти по его английскому fullname и/или логину
    """
    frodo = arda_users.frodo
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/users/all')
    client.login('frodo')
    response = client.json.get(suggest_url, {'system': simple_system.slug, 'q': 'bag'})
    data = response.json()['data']
    assert len(data) == 1
    assert data[0]['id'] == 'frodo'
    response = client.json.get(suggest_url, {'system': simple_system.slug, 'q': 'bag frodo'})
    data = response.json()['data']
    assert len(data) == 1
    assert data[0]['id'] == 'frodo'
    frodo.first_name_en = 'Throdo'
    frodo.save()
    response = client.json.get(suggest_url, {'system': simple_system.slug, 'q': 'bag th'})
    assert response.json()['data'][0]['id'] == 'frodo'
    response = client.json.get(suggest_url, {'system': simple_system.slug, 'q': 'bag th fr'})
    assert response.json()['data'][0]['id'] == 'frodo'
