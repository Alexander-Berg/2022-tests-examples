# -*- coding: utf-8 -*-


import json
import pytest
import requests
from infra.rtc.janitor.test_utils import monkeypatch_function
from infra.rtc.janitor.clients.abc_client import AbcClient
import test_abc_client_data_v4 as data4


# elif url == _url('services/members/') and params['role__scope_in'] == 'administration,development'
def _url(hand):
    return 'https://abc-back.yandex-team.ru/api/v4/{}'.format(hand)


def mocked_requests_get(method, url, params=None, data=None, headers=None):
    class MockResponse:
        def __init__(self, json_data, status_code):
            self.json_data = json_data
            self.status_code = status_code

        def json(self):
            return self.json_data
    if url == _url('services/729/on_duty/'):
        return MockResponse(json.loads(data4.abc_api_v4_services_729_on_duty), 200)
    elif url == _url('services/') and params.get('fields') == 'slug,id,name':
        if params.get('slug') == 'Wall-E':
            return MockResponse(json.loads(data4.abc_api_v4_services_slug2id_Wall_e), 200)
        elif params.get('name') == 'Wall-E: управлятор железом':
            return MockResponse(json.loads(data4.abc_api_v4_services_slug2id_Wall_e), 200)
        elif params.get('id') == 729:
            return MockResponse(json.loads(data4.abc_api_v4_services_slug2id_Wall_e), 200)
        elif params.get('name_en') == 'Wall-E':
            return MockResponse(json.loads(data4.abc_api_v4_services_slug2id_Wall_e), 200)
    elif url == _url('services/members/') and params.get('fields') == 'person' and params.get('service__id') == 729:
        if params.get('role__scope') == 'administration':
            return MockResponse(json.loads(data4.abc_api_v4_srvmembers_slug_Wall_e_adm), 200)
        elif params.get('role__scope') == 'development':
            return MockResponse(json.loads(data4.abc_api_v4_srvmembers_slug_Wall_e_dev_p1), 200)
    elif url == _url('services/members/?cursor=121109-0&fields=person&page_size=5&role__scope=development&service__slug=Wall-E'):
        return MockResponse(json.loads(data4.abc_api_v4_srvmembers_slug_Wall_e_dev_p2), 200)
    return MockResponse(None, 404)


def test_duty(monkeypatch):
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    assert abc_client.get_duty(729) == {
        'login': 'n-malakhov',
        'name_ru': 'Николай Малахов'.decode('utf-8'),
        'name_en': 'Nikolay Malakhov'
    }
    assert abc_client.get_duty(729)['login'] == 'n-malakhov'
    requests.request.assert_called_once()


def test_service_slug_to_id(monkeypatch):
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    assert abc_client.service_slug_to_id('Wall-E') == 729
    assert abc_client.service_slug_to_id('Wall-E') == 729
    requests.request.assert_called_once()


def test_service_name_to_slug(monkeypatch):
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    assert abc_client.service_name_to_slug('Wall-E: управлятор железом') == 'Wall-E'
    assert abc_client.service_name_to_slug('Wall-E: управлятор железом') == 'Wall-E'
    requests.request.assert_called_once()


def test_members(monkeypatch):
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    res = abc_client.get_members(service_slug='Wall-E', roles=['administration', 'development'])
    assert res == {
        u'development': [u'n-malakhov', u'flagist', u'iperfilyev', u'khoden', u'alexsmirnov', u'dkhrutsky', u'staggot', u'ddubrava'],
        u'administration': [u'n-malakhov', u'dldmitry', u'flagist', u'alexsmirnov', u'dkhrutsky', u'staggot']
    }
    requests.request.reset_mock()
    abc_client.get_members(service_slug='Wall-E', roles=['administration'])
    requests.request.assert_not_called()


def test_members_partialcache(monkeypatch):
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    res = abc_client.get_members(service_slug='Wall-E', roles=['development'])
    assert res == {
        u'development': [u'n-malakhov', u'flagist', u'iperfilyev', u'khoden', u'alexsmirnov', u'dkhrutsky', u'staggot', u'ddubrava']
    }
    requests.request.reset_mock()
    res = abc_client.get_members(service_slug='Wall-E', roles=['administration', 'development'])
    assert res == {
        u'development': [u'n-malakhov', u'flagist', u'iperfilyev', u'khoden', u'alexsmirnov', u'dkhrutsky', u'staggot', u'ddubrava'],
        u'administration': [u'n-malakhov', u'dldmitry', u'flagist', u'alexsmirnov', u'dkhrutsky', u'staggot']
    }
    requests.request.assert_called_with(
        'GET',
        'https://abc-back.yandex-team.ru/api/v4/services/members/',
        headers={'Content-Type': 'application/json;charset=UTF-8', 'Authorization': 'OAuth Oauth', 'User-Agent': 'test'},
        params={'service__id': 729, 'role__scope': 'administration', 'fields': 'person'},
        data=None
    )


def test__get_service_info_from_abc(monkeypatch):
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    assert abc_client._get_service_info_from_abc('slug', 'Wall-E') == {
        u'slug': u'Wall-E',
        u'id': 729,
        u'name': 'Wall-E: управлятор железом',
        u'name_en': u'Wall-E'
    }
    assert abc_client._get_service_info_from_abc('id', 729) == {
        u'slug': u'Wall-E',
        u'id': 729,
        u'name': 'Wall-E: управлятор железом',
        u'name_en': u'Wall-E'
    }
    assert abc_client._get_service_info_from_abc('name', 'Wall-E: управлятор железом') == {
        u'slug': u'Wall-E',
        u'id': 729,
        u'name': 'Wall-E: управлятор железом',
        u'name_en': u'Wall-E'
    }
    assert abc_client._get_service_info_from_abc('name_en', 'Wall-E') == {
        u'slug': u'Wall-E',
        u'id': 729,
        u'name': 'Wall-E: управлятор железом',
        u'name_en': u'Wall-E'
    }
    assert abc_client._AbcClient__cache_services_info == {
        'name_en': {
            u'Wall-E': {
                'name_en': u'Wall-E',
                u'slug': u'Wall-E',
                u'id': 729,
                u'name': 'Wall-E: управлятор железом'
                }
            },
        'slug': {
            u'Wall-E': {
                'name_en': u'Wall-E',
                u'slug': u'Wall-E',
                u'id': 729,
                u'name': 'Wall-E: управлятор железом'
                }
            },
        'name': {
            'Wall-E: управлятор железом': {
                'name_en': u'Wall-E',
                u'slug': u'Wall-E',
                u'id': 729,
                u'name': 'Wall-E: управлятор железом'
                }
            },
        'id': {
            729: {
                'name_en': u'Wall-E',
                u'slug': u'Wall-E',
                u'id': 729,
                u'name': 'Wall-E: управлятор железом'
                }
            }
        }


def test_get_filtered_param():
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    assert abc_client._get_filtered_param({'slug': 'test'}) == ('slug', 'test')
    assert abc_client._get_filtered_param({'slug': 'test', 'foo': 'buzz'}) == ('slug', 'test')
    assert abc_client._get_filtered_param({'id': 10}) == ('id', 10)
    assert abc_client._get_filtered_param({'name': 'Тест'}) == ('name', 'Тест')
    assert abc_client._get_filtered_param({'name_en': 'Test'}) == ('name_en', 'Test')
    with pytest.raises(ValueError):
        assert abc_client._get_filtered_param({'name_en': 'Test', 'name': 'Тест'})
        assert abc_client._get_filtered_param({'name_en': 'Test', 'name': 'Тест', 'id': 10})


def test_get_service_info(monkeypatch):
    res = {
        'slug': 'Wall-E',
        'id': 729,
        'name': 'Wall-E: управлятор железом',
        'name_en': 'Wall-E'
    }
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    assert abc_client.get_service_info(slug='Wall-E') == res
    assert abc_client.get_service_info(slug='Wall-E') == res
    requests.request.assert_called_once()


def test_get_members_filtered_param():
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    assert abc_client._get_members_filtered_param({'service_slug': 'test'}) == {'slug': 'test'}
    assert abc_client._get_members_filtered_param({'service_slug': 'test', 'foo': 'buzz'}) == {'slug': 'test'}
    assert abc_client._get_members_filtered_param({'service_id': 10}) == {'id': 10}
    assert abc_client._get_members_filtered_param({'service_name': 'Тест'}) == {'name': 'Тест'}
    assert abc_client._get_members_filtered_param({'service_name_en': 'Test'}) == {'name_en': 'Test'}
    with pytest.raises(ValueError):
        assert abc_client._get_members_filtered_param({'service_name_en': 'Test', 'service_name': 'Тест'})
        assert abc_client._get_members_filtered_param({'service_name_en': 'Test', 'service_name': 'Тест', 'service__id': 10})


def test__get_members_remove_prefix():
    abc_client = AbcClient(useragent='test', oauth_token='Oauth')
    assert abc_client._get_members_remove_prefix('service_slug') == 'slug'
    assert abc_client._get_members_remove_prefix('service_name') == 'name'
