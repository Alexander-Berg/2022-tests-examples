import json
import re
import xmlrpclib

import hamcrest as hm
import httpretty
import pytest

from tests import tutils as tut

TRUST_API_URL = 'https://trust-payments-dev.paysys.yandex.net:8028/trust-payments/v2/bindings'
ERROR_TYPE_PATTERN = "Error: (\w+)\nDescription: (.*)"


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_check_exc_text(xmlrpcserver, session):
    httpretty.register_uri(
        httpretty.POST,
        TRUST_API_URL,
        json.dumps({"status": "error", "status_code": "too_many_cards",
                    "status_desc": "Can't bind more than 5 cards to one user"}),
        status=200)
    with pytest.raises(xmlrpclib.Fault) as e:
        xmlrpcserver.GetCardBindingURL(session.oper_id, {'service_id': 23, 'Currency': 23})
    hm.assert_that(e.value.faultString,
                   hm.contains_string("Error in trust api call: Can't bind more than 5 cards to one user"))


@pytest.mark.parametrize('status_code', [404, 502])
def test_error_502_bindings(medium_xmlrpc, session, status_code):
    httpretty.register_uri(
        httpretty.POST,
        TRUST_API_URL,
        json.dumps({"status": "success", "status_code": "too_many_cards",
                    "status_desc": "Can't bind more than 5 cards to one user"}),
        status=status_code)

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        medium_xmlrpc.GetCardBindingURL(session.oper_id, {'service_id': 23, 'Currency': 23})

    if status_code == 404:
        match = re.match(ERROR_TYPE_PATTERN, exc_info.value.faultString)
        assert match.group(2) == '404 Client Error: Not Found for url: {}'.format(TRUST_API_URL)
    else:
        error_text = 'Error in connecting to trust api: 502 Server Error: Bad Gateway for url: {}'.format(TRUST_API_URL)
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_CONNECT_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == error_text


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
def test_technical_error(medium_xmlrpc, session, trust_response):
    httpretty.register_uri(
        httpretty.POST,
        TRUST_API_URL,
        json.dumps(trust_response),
        status=500)

    with pytest.raises(xmlrpclib.Fault) as exc_info:
        medium_xmlrpc.GetCardBindingURL(session.oper_id, {'service_id': 23, 'Currency': 23})
    if trust_response['status_code'] == 'technical_error':
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_CONNECT_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == 'Error in connecting to trust api: 2 items returned'
    elif trust_response['status_code'] == 'unknown_error':
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_RATE_LIMITER_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == 'Error in connecting to trust api: rate limiter exception'
    else:
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == 'Error in trust api call: 2 items returned'
