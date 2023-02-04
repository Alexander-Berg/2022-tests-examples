# coding: utf-8


import pytest

from idm.core.constants.action import ACTION
from idm.core.constants.role import ROLE_STATE
from idm.utils import reverse

pytestmark = pytest.mark.django_db


def test_rolestate_suggest(client, users_for_test):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/raw/rolestate')

    client.login('admin')
    response = client.json.get(suggest_url)
    assert response.json()['data'][0] == {
        'id': ROLE_STATE.DEPRIVING_VALIDATION, 'slug': ROLE_STATE.DEPRIVING_VALIDATION, 'name': 'Валидация отзыва'
    }

    response = client.json.get(suggest_url, {'q': 'Вы'})
    assert response.json()['data'][0] == {'id': ROLE_STATE.GRANTED, 'slug': ROLE_STATE.GRANTED, 'name': 'Выдана'}

    response = client.json.get(suggest_url, {'q': 'вы'})
    assert response.json()['data'][0] == {'id': ROLE_STATE.GRANTED, 'slug': ROLE_STATE.GRANTED, 'name': 'Выдана'}

    response = client.json.get(suggest_url, {'q': 'ыда'})
    assert response.json()['data'][0] == {'id': ROLE_STATE.GRANTED, 'slug': ROLE_STATE.GRANTED, 'name': 'Выдана'}

    response = client.json.get(suggest_url, {'q': 'gra'})
    assert response.json()['data'][0] == {'id': ROLE_STATE.GRANTED, 'slug': ROLE_STATE.GRANTED, 'name': 'Выдана'}

    response = client.json.get(suggest_url, {'q': 'anted'})
    assert response.json()['data'][0] == {'id': ROLE_STATE.GRANTED, 'slug': ROLE_STATE.GRANTED, 'name': 'Выдана'}

    response = client.json.get(suggest_url, {'id': ROLE_STATE.GRANTED})
    assert response.json()['data'][0] == {'id': ROLE_STATE.GRANTED, 'slug': ROLE_STATE.GRANTED, 'name': 'Выдана'}

    response = client.json.get(suggest_url, {'id': 'grant'})
    assert response.json()['data'] == []

    response = client.json.get(suggest_url, {'q': 'ANT'})
    assert response.json()['data'][0] == {'id': ROLE_STATE.GRANTED, 'slug': ROLE_STATE.GRANTED, 'name': 'Выдана'}


def test_actiontype_suggest(client, users_for_test):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/raw/actiontype')

    client.login('admin')
    response = client.json.get(suggest_url, {'limit': len(ACTION.ACTIONS)})
    data = response.json()['data']

    assert {'id': 'user_ad_enable', 'slug': 'user_ad_enable', 'name': 'активирован аккаунт в AD'} in data


def test_grouptype_suggest(client, users_for_test):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/raw/grouptype')

    client.login('admin')
    response = client.json.get(suggest_url)
    data = response.json()['data']

    assert {'id': 'wiki', 'slug': 'wiki', 'name': 'Вики-группа'} in data


def test_pluginhandle_suggest(client, users_for_test):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/raw/pluginhandle')

    client.login('admin')
    response = client.json.get(suggest_url)
    data = response.json()['data']

    assert {'id': 'info', 'slug': 'info', 'name': 'info'} in data
    assert [item['slug'] for item in data] == ['info', 'get-all-roles', 'get-roles']


def test_authfactor_suggest(client, users_for_test, simple_system):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/raw/authfactor')

    client.login('admin')
    response = client.json.get(suggest_url, data={'system': simple_system.slug})
    data = response.json()['data']

    expected_data = [{'id': 'no', 'name': 'Без авторизации', 'slug': 'no'},
                     {'id': 'cert', 'name': 'Сертификат', 'slug': 'cert'},
                     {'id': 'tvm', 'name': 'TVM', 'slug': 'tvm', 'default': True}]
    assert data == expected_data


def test_pluginlibrary_suggest(client, users_for_test):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/raw/pluginlibrary')

    client.login('admin')
    response = client.json.get(suggest_url)
    data = response.json()['data']

    assert {'id': 'default', 'slug': 'default', 'name': 'Прописанная в системе'} in data
    assert [item['slug'] for item in data] == ['default', 'requests', 'curl']


def test_inconsistency_state(client, users_for_test):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/raw/inconsistencystate')

    client.login('admin')
    response = client.json.get(suggest_url)
    data = response.json()['data']

    assert {'id': 'active', 'name': 'Активна', 'slug': 'active'} in data


def test_inconsistency_type(client, users_for_test):
    suggest_url = reverse('api_dispatch_list', api_name='frontend', resource_name='suggest/raw/inconsistencytype')

    client.login('admin')
    response = client.json.get(suggest_url)
    data = response.json()['data']

    we_have_system_dont = {
        'id': 'we_have_system_dont',
        'slug': 'we_have_system_dont',
        'name': 'У нас есть роль, в системе – нет'
    }
    assert we_have_system_dont in data
