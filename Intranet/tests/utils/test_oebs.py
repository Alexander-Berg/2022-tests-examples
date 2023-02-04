# coding: utf-8
from __future__ import unicode_literals

import json
from collections import namedtuple

import pytest
import requests

from cab.utils.oebs import get_stock_options, push_plan, get_plan, YANDEX_NV_COMPANY_ID

test_plan_data = {
    'planId': 0,  # id плана. null если план только что добавили на фронте
    'grants': [
        {
            'grantId': 0,
            'groups': [
                {
                    'groupId': 0,  # id группы. null если группу только что добавили на фронте
                    'vestingAmount': 100,
                    'vestingDate': '2019-01-02',  # YYYY-MM-DD
                    'rules': [
                        {
                            'ruleId': 1,  # id условий. null если условие только что добавили на фронте
                            'dateFrom': '2020-01-01',  # YYYY-MM-DD
                            'dateTo': '2020-02-02',  # YYYY-MM-DD
                            'percent': 88.06,
                            'value': 12345.987,
                            'ruleType': 'threshold',  # "threshold",
                            'threshold': 'fixed',  # "closing-each"
                        }
                    ]
                }
            ]
        }
    ]
}

@pytest.mark.parametrize('exception', [requests.Timeout, requests.ConnectTimeout])
def test_get_options_oebs_timeout(exception, monkeypatch):
    def mocked(*args, **kwargs):
        raise exception
    monkeypatch.setattr(requests, 'post', mocked)

    response = get_stock_options('asd', False)
    assert response == dict(error='OEBS is unavailable')


def test_get_options_wrong_json(monkeypatch):
    def mocked(*args, **kwargs):
        Response = namedtuple('Response', 'json')
        return Response(lambda: json.loads('{'))
    monkeypatch.setattr(requests, 'post', mocked)

    response = get_stock_options('asd', False)
    assert response == dict(error='OEBS returned invalid json')


def test_get_options_empty(monkeypatch):
    def mocked(*args, **kwargs):
        Response = namedtuple('Response', 'json')
        return Response(lambda: dict(holders=[]))
    monkeypatch.setattr(requests, 'post', mocked)

    response = get_stock_options('asd', False)
    assert response == dict(grants=[])


def test_get_options_of_yandex_nv(monkeypatch):
    def mocked(*args, **kwargs):
        assert kwargs['json']['showGrants']['company'] == [YANDEX_NV_COMPANY_ID]
        Response = namedtuple('Response', 'json')
        return Response(lambda: dict(holders=[]))

    monkeypatch.setattr(requests, 'post', mocked)
    response = get_stock_options('asd', False)


def test_get_options_non_empty(monkeypatch):
    def mocked(*args, **kwargs):
        Response = namedtuple('Response', 'json')
        return Response(lambda: {
            'holders': [
                {
                    'grants': [{'classCode': 'N1R'}]
                }
            ]
        })
    monkeypatch.setattr(requests, 'post', mocked)

    response = get_stock_options('asd', False)
    assert response == dict(grants=[dict(classCode='RSU')])


def test_push_plan(monkeypatch):
    def mocked(*args, **kwargs):
        Response = namedtuple('Response', 'json')
        return Response(lambda: {
            '__args__': args,
            '__kwargs__': kwargs
        })

    login = 'login'

    expected_plan_data = test_plan_data.copy()
    monkeypatch.setattr(requests, 'post', mocked)
    result = push_plan(login, test_plan_data)

    expected_plan_data['login'] = login

    assert result['__args__'] == tuple()
    assert result['__kwargs__']['json'] == expected_plan_data


oebs_plans_response_mock = {
    "login": "",
    "plan":
    [
        {
            "planId": 0,
            "versionId": 0,
            "adoptionDate": "2019-11-11",
            "modificationDate": "2019-11-11",
            "cancellationDate": "2019-11-11",
            "status": "",
            "criterion": [
                {
                    "grantId": 0,
                    "dateFrom": "2019-11-11",
                    "dateTo": "2019-11-11",
                    "amount": 0,
                    "ruleType": "",
                    "threshold": "",
                    "value": 0,
                    "percent": 0,
                },
                {
                    "grantId": 0,
                    "dateFrom": "2019-11-11",
                    "dateTo": "2019-11-11",
                    "amount": 0,
                    "ruleType": "",
                    "threshold": "",
                    "value": 0,
                    "percent": 0,
                }
            ]
        }
    ]
}


def test_get_plans(monkeypatch):
    login = 'login'

    def mocked(*args, **kwargs):
        assert kwargs['json'] == {'login': login}
        Response = namedtuple('Response', 'json')
        return Response(lambda: oebs_plans_response_mock)

    monkeypatch.setattr(requests, 'post', mocked)
    result = get_plan(login)

    assert result == oebs_plans_response_mock
