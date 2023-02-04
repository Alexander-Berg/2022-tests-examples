# coding: utf-8


import pytest

from idm.tests.utils import assert_response, mock_ids_repo, use_proxied_suggest, create_user
from idm.utils import reverse


pytestmark = pytest.mark.django_db


def test_group_suggest(client, arda_users, department_structure):

    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/groups/all')

    client.login('frodo')

    objects = [
        {
            'url': 'https://staff.test.yandex-team.ru/groups/the-shire/',
            'fields': [
                {
                    'type': 'slug',
                    'value': 'the-shire'
                },
                {
                    'type': 'state',
                    'value': 'active'
                },
                {
                    'type': 'type',
                    'value': 'department'
                }
            ],
            'layer': 'idm_groups',
            'id': '104',
            'title': 'the-shire'
        }
    ]

    with use_proxied_suggest(should_use=False):
        response = client.json.get(suggest_url)
        assert response.status_code == 200
        data = response.json()
        expected_keys = {'is_group', 'id', 'full_name', 'department__name', 'group_type'}
        assert set(data['data'][0].keys()) == expected_keys

        data = response.json()
        assert data['meta'] == {'offset': 0, 'limit': 20}
        ids = {item['id'] for item in data['data']}

        # проверка на подмножество, так как фикстуры могут расширяться, а точное совпадение сделает тест хрупким
        assert ids.issuperset({101, 102, 103, 104, 105, 999, 7})

        response = client.json.get(suggest_url, {'limit': 1})
        assert_response(response, meta={'offset': 0, 'limit': 1}, id={7})  # Братство

        response = client.json.get(suggest_url, {'limit': 1, 'offset': 1})  # Братство
        assert_response(response, meta={'offset': 1, 'limit': 1}, id={105})

        response = client.json.get(suggest_url, {'offset': 30})
        assert_response(response, meta={'offset': 30, 'limit': 20}, id=set())

        response = client.json.get(suggest_url, {'q': 'Associa'})
        assert_response(response, id={103})

        response = client.json.get(suggest_url, {'q': 'Fello'})
        assert_response(response, id={105})

        response = client.json.get(suggest_url, {'id': 102})
        assert_response(response, id={102})

        response = client.json.get(suggest_url, {'id': 'text'})
        assert response.status_code == 400
        assert response.json()['message'] == "{'external_id': 'Введите целое число.'}"

    with use_proxied_suggest(should_use=True), mock_ids_repo('intrasearch', 'idm_users', objects):
        response = client.json.get(suggest_url, {'q': 'hello'})
        assert response.json() == {
            'meta': {
                'limit': 20,
                'offset': 0
            },
            'data': [
                {
                    'id': 104,
                    'is_group': True,
                    'department__name': 'Земли',
                    'full_name': 'the-shire',
                    'group_type': 'department'
                }
            ]
        }

        response = client.json.get(suggest_url, {'id': 102})
        assert_response(response, id={102})

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


def test_suggest_id__in_on_group(client, department_structure):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/subjects/all')
    client.login('frodo')
    for kw in (
            {'offset': 0, 'limit': 20, 'q': 'fellowship'},
            {'offset': 0, 'limit': 20, 'q': '', 'id__in': 'fellowship'},
    ):
        data = client.json.get(suggest_url, data=kw).json()
        assert len(data['data']) == 1
        assert data['data'][0]['login'] == 'fellowship-of-the-ring'


def test_suggest_id__in_on_group_uses_intrasearch(client):
    objects = [
        {
            'url': 'https://staff.test.yandex-team.ru/groups/fellowship-of-the-ring/',
            'fields': [
                {
                    'type': 'slug',
                    'value': 'fellowship-of-the-ring'
                },
                {
                    'type': 'state',
                    'value': 'active'
                },
                {
                    'type': 'type',
                    'value': 'wiki'
                },
                {
                    'type': 'object_type',
                    'value': 'groups'
                }
            ],
            'layer': 'idm_groups',
            'id': '26986',
            'title': 'fellowship-of-the-ring'
        }
    ]
    with \
            use_proxied_suggest(should_use=True), \
            mock_ids_repo('intrasearch', 'idm_subjects', objects) as repo:
        suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/subjects/all')
        client.login(create_user())
        repo.get.reset_mock()
        data = client.json.get(suggest_url, data={'offset': 0, 'limit': 19, 'q': '', 'id__in': 'fellowship'}).json()
        assert len(data['data']) == 1
        assert data['data'][0]['login'] == 'fellowship-of-the-ring'
        repo.get.assert_called_once_with({
            'layers': 'idm_subjects',
            'allow_empty': True,
            'idm_subjects.page': 0,
            'idm_subjects.per_page': 19,
            'language': 'ru',
            'text': 'fellowship'
        })
