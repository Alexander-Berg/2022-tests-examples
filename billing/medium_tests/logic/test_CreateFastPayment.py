# -*- coding: utf-8 -*-
import re
from xmlrpclib import Fault

import httpretty
import pytest
from lxml import etree

from tests import tutils as tut

ERROR_TYPE_PATTERN = "Error: (\w+)\nDescription: (.*)"

SIMPLE_API_URL = 'balance-payments-dev.paysys.yandex.net:8023/simpleapi/xmlrpc'


@pytest.mark.parametrize('status_code', [404, 502])
def test_create_fast_payment(session, medium_xmlrpc, status_code):
    def get_response():
        response = etree.Element('root')
        return response

    httpretty.register_uri(
        httpretty.POST,
        'https://{}'.format(SIMPLE_API_URL),
        get_response(),
        status=status_code)
    with pytest.raises(Fault) as exc_info:
        medium_xmlrpc.CreateFastPayment(0, 143)

    if status_code == 404:
        match = re.match(ERROR_TYPE_PATTERN, exc_info.value.faultString)
        assert match.group(2) == '<ProtocolError for {}: 404 Not Found>'.format(SIMPLE_API_URL)
    else:
        error_text = 'Error in connecting to trust api: <ProtocolError for {}: 502 Bad Gateway>'.format(
            SIMPLE_API_URL)
        assert tut.get_exception_code(exc_info.value, 'code') == 'TRUST_API_CONNECT_EXCEPTION'
        assert tut.get_exception_code(exc_info.value, 'msg') == error_text
