import pytest
from bcl.api.expections import ArgumentError
from bcl.api.views.base import ApiView, Argument, arg_date_parse
from bcl.core.models import Service
from django.test import Client


class BogusApiView(ApiView):

    alias = 'bogus'
    title = 'meh'

    def get(self, request):
        raise ValueError('Something unexpected')


def test_unhandled_exception(api_client, rf):

    response = BogusApiView.as_view()(rf.get('/api/bogus/')).content.decode()
    assert '"errors": [{"msg": "Unhandled exception' in response
    assert '"event_id": "' in response
    assert '"description": "' in response


def test_arg_date_parse():

    with pytest.raises(ArgumentError):
        arg_date_parse(44)

    with pytest.raises(ArgumentError):
        arg_date_parse('not-a-date')

    assert arg_date_parse(None) is None

    assert arg_date_parse('2019-12-17').year == 2019


def test_argument():
    assert str(Argument('name', hint='some')) == 'name'


def test_overview():
    response = Client().get('/api/overview/').content.decode()
    assert 'api/statements/' in response


def test_access_check(api_client_bare, monkeypatch):
    api_client_bare.monkeypatch = monkeypatch
    response = api_client_bare.get('/api/')
    assert response.status_code == 401
    response = response.json

    # Попытка доступа без билета.
    assert 'built' in response['meta']
    assert response['errors'] == [{'msg': 'Unauthorized: Unable to authenticate your service using TVM'}]

    # Попытка доступа от неизвестного сервиса.
    api_client_bare.auth('dummy')

    response = api_client_bare.get('/api/')
    assert response.status_code == 403

    response = response.json
    assert response['errors'] == [{'msg': "Forbidden: Client 'dummy' is not allowed"}]

    # Попытка доступа от неизвестного сервиса.
    monkeypatch.setattr('bcl.api.views.base.view.Service.tvm_ids', {'fakemarket': Service.MARKET})
    api_client_bare.auth('fakemarket')
    response = api_client_bare.post('/api/proxy/paypal/getuserinfo/')
    assert response.status_code == 403
    assert response.json['errors'] == [{'msg': "Forbidden: Client 'fakemarket' is not allowed to access 'proxy_paypal_getuserinfo'"}]


def test_post_invalid(api_client):
    response = api_client.post('/api/proxy/paypal/getuserinfo/', data='xxxxxx', content_type='some/bogus')
    assert response.json['errors'] == [
        {'arg': 'token', 'failure': [{'msg': 'Invalid JSON supplied.'}], 'msg': 'argument failure'}
    ]


def test_parse_date(api_client):
    response = api_client.get('/api/statements/?accounts=["10"]&on_date=xxxx')
    assert response.json['errors'] == [
        {'arg': 'on_date', 'failure': [{'msg': 'Not a valid date.', 'value': 'xxxx'}], 'msg': 'argument failure'}
    ]


def test_required_args(api_client):
    response = api_client.get('/api/statements/')
    assert response.json['errors'] == [
        {'arg': 'accounts', 'failure': [{'msg': 'Required but missing.'}], 'msg': 'argument failure'},
        {'arg': 'on_date', 'failure': [{'msg': 'Required but missing.'}], 'msg': 'argument failure'}
    ]


def test_ping(api_client):
    response = api_client.get('/api/ping/')
    assert response.ok
    response = response.json
    assert not response['errors']


def test_introspection(api_client):
    response = api_client.get('/api/')
    response = response.json
    assert not response['errors']

    realms = {realm['alias']: realm for realm in response['data']['items']}
    assert realms['introspection']['url'] == 'api/'
    assert realms['introspection']['methods']['GET']['args'] == {}

    args = realms['refs_accounts']['methods']['GET']['args']
    assert 'accounts' in args
