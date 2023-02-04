import pytest
from intranet.crt import VERSION

pytestmark = pytest.mark.django_db


def test_userdata_401(crt_client, users, settings):
    settings.YAUTH_TEST_USER = False
    response = crt_client.json.get('/api/frontend/userdata/')
    assert response.status_code == 401
    assert response.json() == {
        'detail': 'Authentication credentials were not provided.',
        'passport-url': 'https://passport.yandex-team.ru/auth?retpath=',
    }


def test_userdata(crt_client, users):
    user = users['helpdesk_user']
    user.first_name = 'Helpdesk'
    user.last_name = 'User'
    user.first_name_ru = 'Хелпдеск'
    user.last_name_ru = 'Пользователь'
    user.save()

    crt_client.login('helpdesk_user')

    response = crt_client.json.get('/api/frontend/userdata/')
    assert response.status_code == 200
    data = response.json()
    assert data == {
        'username': 'helpdesk_user',
        'email': 'helpdesk_user@yandex-team.ru',
        'lang_ui': 'ru',
        'first_name': {
            'ru': 'Хелпдеск',
            'en': 'Helpdesk',
        },
        'last_name': {
            'ru': 'Пользователь',
            'en': 'User',
        }
    }


def test_info_headers(crt_client, users):
    crt_client.login('normal_user')
    response = crt_client.json.get('/api/frontend/userdata/')
    assert response['X-CRT-READONLY'] == 'false'
    assert response['X-CRT-VERSION'] == VERSION
