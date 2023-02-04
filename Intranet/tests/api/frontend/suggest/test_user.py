import pytest

from __tests__.utils.common import mock_ids_repo


pytestmark = pytest.mark.django_db


@pytest.fixture(autouse=True)
def rename_users(users):
    helpdesk_user = users['helpdesk_user']
    helpdesk_user.full_name = 'Helpdesk User'
    helpdesk_user.full_name_ru = 'Хелпдеск Пользователь'
    helpdesk_user.save()
    normal_user = users['normal_user']
    normal_user.full_name = 'Normal User'
    normal_user.full_name_ru = 'Обычный Пользователь'
    normal_user.save()
    another_user = users['another_user']
    another_user.full_name = 'Another User'
    another_user.full_name_ru = 'Другой Пользователь'
    another_user.save()


intrasearch_data = [
    {
        'url': 'https://staff.test.yandex-team.ru/helpdesk_user',
        'fields': [],
        'layer': 'people',
        'id': 'helpdesk_user',
        'title': 'Helpdesk User',
    },
    {
        'url': 'https://staff.test.yandex-team.ru/normal_user',
        'fields': [],
        'layer': 'people',
        'id': 'normal_user',
        'title': 'Normal User',
    },
    {
        'url': 'https://staff.test.yandex-team.ru/another_user',
        'fields': [],
        'layer': 'people',
        'id': 'another_user',
        'title': 'Another User',
    },
]

expected = {
    'limit': 10,
    'offset': 0,
    'results': [
        {
            'id': 'helpdesk_user',
            'title': {'en': 'Helpdesk User', 'ru': 'Хелпдеск Пользователь'},
            'type': 'default',
        },
        {
            'id': 'normal_user',
            'title': {'en': 'Normal User', 'ru': 'Обычный Пользователь'},
            'type': 'default',
        },
        {
            'id': 'another_user',
            'title': {'en': 'Another User', 'ru': 'Другой Пользователь'},
            'type': 'default',
        }
    ]
}


def test_user_suggest(crt_client):
    crt_client.login('normal_user')

    with mock_ids_repo('intrasearch', 'people', intrasearch_data):
        response = crt_client.json.get('/api/frontend/suggest/users/')
    assert response.json() == expected


def test_filter_id(crt_client):
    crt_client.login('normal_user')

    with mock_ids_repo('intrasearch', 'people', intrasearch_data):
        response = crt_client.json.get('/api/frontend/suggest/users/', {'id': 'normal_user'})
    new_data = expected.copy()
    new_data['results'] = new_data['results'][1:2]
    assert response.json() == new_data

    with mock_ids_repo('intrasearch', 'people', intrasearch_data):
        response = crt_client.json.get('/api/frontend/suggest/users/', {'id': ['normal_user', 'another_user']})
    users = {user['id'] for user in response.json()['results']}
    expected_users = {'normal_user', 'another_user'}
    assert users == expected_users
