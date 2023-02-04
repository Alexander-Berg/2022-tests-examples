# -*- coding: utf-8 -*-
import httpretty
import pytest
from lxml import etree
from mock import patch

from balance.corba_buffers import RequestBuffer
from butils.passport import PassportCfg


def get_response():
    response = etree.Element('root')
    message = etree.Element('message-sent')
    message.attrib['id'] = '232'
    response.append(message)
    return response


@pytest.mark.usefixtures('httpretty_enabled_fixture')
def test_headers(session, muzzle_logic):
    httpretty.register_uri(
        httpretty.GET,
        'https://phone-passport-test.yandex.ru/sendsms',
        etree.tostring(get_response()),
        status=200)
    session.config.__dict__['PASSPORT_SMS_API_TEST_ALLOWED'] = {'uids': [session.oper_id]}
    request_obj = RequestBuffer(params=([],
                                        [('X-Real-Ip', '123'),
                                         ('user-agent', u'питон-requests/2.23.0'),
                                        ('wrong-header', 'python-requests/2.23.0')],
                                        []
                                        ))
    with patch.object(PassportCfg, 'get_suitable_passport',
                      return_value={'SmsApiURL': 'https://phone-passport-test.yandex.ru',
                                    'name': 'test_name',
                                    'BlackBoxTimeout': 1}):
        muzzle_logic.send_overdraft_verification_sms(session, '343', '23233223', request_obj)
    request = httpretty.last_request()
    assert request.headers.dict['ya-client-user-agent'] == 'питон-requests/2.23.0'
    assert request.headers.dict['ya-consumer-client-ip'] == '123'
