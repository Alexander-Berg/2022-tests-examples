# -*- coding: utf-8 -*-
from __future__ import absolute_import
from __future__ import division

from future import standard_library

standard_library.install_aliases()
import httpretty
import pytest
from requests import ConnectionError
from yb_snout_api.tests_unit.base import TestCaseApiAppBase
from lxml import etree
from contextlib import contextmanager
from yb_snout_api.resources.muzzle.bridge.utils.files import XMLParser
from yb_snout_api.tests_unit.resources.muzzle_bridge.helpers import get_file_loader_mock


@contextmanager
def httpretty_enable():
    httpretty.enable()

    yield

    httpretty.disable()
    httpretty.reset()


content = """<?xml version="1.0" encoding="utf-8"?>
<?xml-stylesheet type="text/xsl" href="xsl/process-wm-pm.xsl"?>
<page xmlns:xi="http://www.w3.org/2001/XInclude">
    <xi:include href="xml/std-xscript.xml"/>
    <xi:include href="../balance-common/xml/include/add-content-security-policy.xml"/>
    <block timeout="350000">
        <nameref>PaysysServantRef</nameref>
        <method>handle_webmoney_pm_callback</method>
        <param type="Request"/>
    </block>
</page>
"""


class TestMuzzle(TestCaseApiAppBase):
    BASE_API = '/muzzle/bridge/no-xslt/balance-user/test_file.xml'

    def test_connection_error(self):
        def exceptionCallback(*args, **kwargs):
            raise ConnectionError()

        httpretty.register_uri(httpretty.POST,
                               "https://balance-payments-test.paysys.yandex.net:8023/proxy-old-paysys/corba",
                               body=exceptionCallback)

        with httpretty_enable(), get_file_loader_mock(content=etree.fromstring(content, XMLParser())):
            response = self.test_client.get(self.BASE_API, {'passport_id': 555}, is_admin=False)
        assert response.json['is_trust_error'] is True

    @pytest.mark.parametrize('status_code', [404, 502])
    def test_http_error(self, status_code):
        def get_response(*args, **kwargs):
            response = etree.Element('root')
            message = etree.Element('message-sent')
            message.attrib['id'] = '232'
            response.append(message)
            return etree.tostring(response)

        httpretty.register_uri(httpretty.POST,
                               "https://balance-payments-test.paysys.yandex.net:8023/proxy-old-paysys/corba",
                               get_response(),
                               status=status_code)
        with httpretty_enable(), get_file_loader_mock(content=etree.fromstring(content, XMLParser())):
            response = self.test_client.get(self.BASE_API, {'passport_id': 555}, is_admin=False)

        if status_code < 500:
            assert 'is_trust_error' not in response.json
        else:
            assert response.json['is_trust_error'] is True

    def test_lxml_error(self):
        def get_response(*args, **kwargs):
            return '<'

        httpretty.register_uri(httpretty.POST,
                               "https://balance-payments-test.paysys.yandex.net:8023/proxy-old-paysys/corba",
                               get_response())
        with httpretty_enable(), get_file_loader_mock(content=etree.fromstring(content, XMLParser())):
            response = self.test_client.get(self.BASE_API, {'passport_id': 555}, is_admin=False)

        assert response.json['is_trust_error'] is True
