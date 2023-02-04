# -*- coding: utf-8 -*-
import re
from xmlrpclib import Fault

import httpretty
import pytest
import json
from lxml import etree
from requests.exceptions import ConnectionError

from tests import tutils as tut

ERROR_TYPE_PATTERN = "Error: (\w+)\nDescription: (.*)"
TRUST_API_URL = 'https://trust-payments-dev.paysys.yandex.net:8028/trust-payments/v2/bindings'


@pytest.mark.parametrize('status_code', [404, 502])
def test_check_binding_http_error(session, medium_xmlrpc, status_code):
    def get_response():
        response = etree.Element('root')
        return response

    httpretty.register_uri(
        httpretty.GET,
        '{}/143'.format(TRUST_API_URL),
        get_response(),
        status=status_code)
    with pytest.raises(Fault) as exc_info:
        medium_xmlrpc.CheckBinding(0, {'PurchaseToken': 143, 'ServiceID': 23})

    if status_code == 404:
        match = re.match(ERROR_TYPE_PATTERN, exc_info.value.faultString)
        assert match.group(2) == '404 Client Error: Not Found for url: {}/143'.format(TRUST_API_URL)
    else:
        error_text = 'Error in connecting to trust api: 502 Server Error: Bad Gateway for url: {}/143'.format(
            TRUST_API_URL)
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_CONNECT_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == error_text


def test_check_binding_connection_error(session, medium_xmlrpc):
    def exceptionCallback(*args, **kwargs):
        raise ConnectionError()

    httpretty.register_uri(
        httpretty.GET,
        '{}/143'.format(TRUST_API_URL),
        body=exceptionCallback,
        status=200)

    with pytest.raises(Fault) as exc_info:
        medium_xmlrpc.CheckBinding(0, {'PurchaseToken': 143, 'ServiceID': 23})

    assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_CONNECT_EXCEPTION'


@pytest.mark.parametrize('trust_response', [
    {"status": "error",
     "status_code": 'technical_error',
     "status_desc": "2 items returned",
     "method": "Trust.DoBinding"},

    {"status": "error",
     "status_code": 'non_technical_error',
     "status_desc": "2 items returned",
     "method": "Trust.DoBinding"},

    {"status": "error",
     "status_code": "unknown_error",
     "method": "yandex_balance_simple.post_bindings",
     "status_desc": "RuntimeError: HttpError: 429 No Reason"}
])
def test_technical_error(session, medium_xmlrpc, trust_response):
    httpretty.register_uri(
        httpretty.GET,
        '{}/143'.format(TRUST_API_URL),
        json.dumps(trust_response),
        status=500)

    with pytest.raises(Fault) as exc_info:
        medium_xmlrpc.CheckBinding(0, {'PurchaseToken': 143, 'ServiceID': 23})

    if trust_response['status_code'] == 'technical_error':
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_CONNECT_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == 'Error in connecting to trust api: 2 items returned'
    elif trust_response['status_code'] == 'unknown_error':
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_RATE_LIMITER_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == 'Error in connecting to trust api: rate limiter exception'
    else:
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == 'Error in trust api call: 2 items returned'
