# -*- coding: utf-8 -*-


import json
import pytest
import mock
import requests
from infra.rtc.janitor.test_utils import monkeypatch_function
from infra.rtc.janitor.clients.bot_client import BotClient, BotClientError, BotClientHostNotFound
import test_bot_client_data as testdata


def _url(hand):
    return 'https://bot.yandex-team.ru/api/{}'.format(hand)


def mocked_requests_get(method, url, params=None, data=None, headers=None):
    class MockResponse:
        def __init__(self, json_data, status_code, url):
            self.json_data = json_data
            self.status_code = status_code
            self.ok = status_code == 200
            self.url = url

        def json(self):
            return self.json_data
    if url == _url('osinfo.php'):
        if params.get('inv') == 8383:
            return MockResponse(json.loads(testdata.bot_inv_notfound), 200, url)
        elif params.get('inv') == 9292:
            return MockResponse(json.loads(testdata.bot_inv_msg), 200, url)
        elif params.get('inv') == 100415867:
            return MockResponse(json.loads(testdata.bot_inv_100415867), 200, url)
        elif params.get('inv') == 100415867 and params.get('output') == "XXCSI_FQDN|GROUP_OWNED|loc_object|instance_number":
            return MockResponse(json.loads(testdata.bot_inv_fields), 200, url)
    elif url == _url('v1/hwr/pre-orders/8570'):
        return MockResponse(json.loads(testdata.bot_preorder), 200, url)
    return MockResponse(None, 404, url)


def test_parse_resp_ok():
    client = BotClient(useragent='test', oauth_token='Oauth')
    resp = mock.Mock()
    resp.json.return_value = json.loads(testdata.bot_inv_100415867)
    assert client._parse_resp(resp=resp, params={'inv': 100415867}, data=None) == {
        "res": 1,
        "os": [
            {
                "instance_number": "100415867",
                "item_segment3": "NODES",
                "status_name": "OPERATION",
                "item_segment2": "XEONE5-2650V2",
                "item_segment1": "AIC/1SHV2608/DPSB0406/8xT3.5/1U/N"
            }
        ]
    }


def test_parse_resp_inv():
    client = BotClient(useragent='test', oauth_token='Oauth')
    resp = mock.Mock()
    with pytest.raises(BotClientError):
        resp.json.return_value = json.loads(testdata.bot_inv_msg)
        assert client._parse_resp(resp=resp, params={'inv': 100415867}, data=None)


def test_parse_resp_notfnd():
    client = BotClient(useragent='test', oauth_token='Oauth')
    resp = mock.Mock()
    with pytest.raises(BotClientHostNotFound):
        resp.json.return_value = json.loads(testdata.bot_inv_notfound)
        assert client._parse_resp(resp=resp, params={'inv': 100415867}, data=None)


def test_call_ok(monkeypatch):
    client = BotClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    assert client.call(method='GET', hand='osinfo.php', params={'inv': 100415867}, data=None) == {
        "res": 1,
        "os": [
            {
                "instance_number": "100415867",
                "item_segment3": "NODES",
                "status_name": "OPERATION",
                "item_segment2": "XEONE5-2650V2",
                "item_segment1": "AIC/1SHV2608/DPSB0406/8xT3.5/1U/N"
            }
        ]
    }


def test_call_err_msg(monkeypatch):
    client = BotClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    with pytest.raises(BotClientError):
        assert client.call(method='GET', hand='osinfo.php', params={'inv': 9292}, data=None)


def test_call_err_notfnd(monkeypatch):
    client = BotClient(useragent='test', oauth_token='Oauth')
    monkeypatch_function(monkeypatch, requests.request, module=requests, side_effect=mocked_requests_get)
    with pytest.raises(BotClientError):
        assert client.call(method='GET', hand='osinfo.php', params={'inv': 8383}, data=None)


def test_get_host_info(monkeypatch):
    with mock.patch.object(BotClient, 'call', return_value=json.loads(testdata.bot_inv_fields)) as mock_call:
        client = BotClient(useragent='test', oauth_token='Oauth')
        assert client.get_host_info(inv=100415867) == {
            'inv':  '100415867',
            'fqdn': 'sas4-6617.search.yandex.net',
            'abc_prj': "YT в RTC".decode('utf-8'),
            'dc': 'sas',
            'rack': '4A1'
        }
        mock_call.assert_called()
        mock_call.assert_called_with(
            'GET',
            'osinfo.php',
            params={
                "output": "XXCSI_FQDN|GROUP_OWNED|loc_object|instance_number|loc_segment5",
                "inv": 100415867
            }
        )


def test_get_preorder_info(monkeypatch):
    with mock.patch.object(BotClient, 'call', return_value=json.loads(testdata.bot_preorder)):
        client = BotClient(useragent='test', oauth_token='Oauth')
        assert client.get_preorder_info(id=8570) == {
            "status_id": 3,
            "status_code": "approved",
            "status_display": "В закупке".decode('utf-8'),
            "ticket_id": "DISPENSERREQ-3358",
        }
