# coding: utf-8

import json
from unittest.mock import patch

import pytest
import responses

from .inn import inn_checksum
from .request import get_current_url, is_internal
from .staff import get_phone

from procu.utils.test import mock_phone

pytestmark = [pytest.mark.django_db]


# ------------------------------------------------------------------------------


def test_inn_checksum_10_digit():
    # Generate 10-digit INNs with the same prefix
    # and the last digit ranging from 0 to 9.
    # Check that only a single INN is valid.

    prefix = '606752414'
    inns = (prefix + str(i) for i in range(10))
    assert sum(inn_checksum(x) for x in inns) == 1, 'Single valid INN expected'


def test_inn_checksum_12_digit():
    # Generate 12-digit INNs with the same prefix
    # and the last two digits ranging from 00 to 99.
    # Check that only a single INN is valid.

    prefix = '6067524145'
    inns = (prefix + str(i).zfill(2) for i in range(100))
    assert sum(inn_checksum(x) for x in inns) == 1, 'Single valid INN expected'


# ------------------------------------------------------------------------------


@pytest.mark.parametrize(
    'env,expectation', (('internal', True), ('external', False))
)
def test_is_internal(clients, env, expectation):

    request = clients[env].get('/api/monitoring/state').wsgi_request
    assert is_internal(request) is expectation


# ------------------------------------------------------------------------------


def test_get_current_url(clients):
    request = clients['internal'].get('/api/monitoring/state').wsgi_request
    assert get_current_url(request) == 'http://procu.int/api/monitoring/state'


def test_get_current_url_add_params(clients):
    request = clients['internal'].get('/api/monitoring/state').wsgi_request

    url = get_current_url(request, add_params={'foo': 'bar'})
    assert url == 'http://procu.int/api/monitoring/state?foo=bar'


def test_get_current_url_del_params(clients):

    client = clients['internal']
    request = client.get('/api/monitoring/state?foo=bar&aaa=bbb').wsgi_request

    url = get_current_url(request, del_params=['foo'])
    assert url == 'http://procu.int/api/monitoring/state?aaa=bbb'


# ------------------------------------------------------------------------------

STAFF_MOCK = {
    'links': {},
    'page': 1,
    'limit': 50,
    'result': [{'phones': [{'number': '+79000000001'}]}],
    'total': 1,
    'pages': 1,
}

STAFF_MOCK_NO_PHONE = STAFF_MOCK.copy()
STAFF_MOCK_NO_PHONE['result'] = [{'phones': []}]


@patch('procu.api.utils.common.get_service_ticket', lambda x: '***')
def test_get_phone_ok():
    with mock_phone('robot-procu', number='+79000000001'):
        assert get_phone('robot-procu') == '+79000000001'


@patch('procu.api.utils.common.get_service_ticket', lambda x: '***')
def test_get_phone_error():

    with responses.RequestsMock(), pytest.raises(ValueError):
        get_phone('robot-procu')

    with mock_phone('robot-procu'), pytest.raises(ValueError):
        get_phone('robot-procu')
