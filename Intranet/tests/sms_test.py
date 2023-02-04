import pytest
from staff.lib import requests

from staff.lib import sms


XML_WITH_ERR = '''<?xml version="1.0" encoding="windows-1251"?>
<doc>
    <error>User does not have an active phone to recieve messages</error>
    <errorcode>{}</errorcode>
</doc>
'''
CORRECT_PHONE = '+79996660022'


def test_bad_phone():
    assert sms.send('sad', 'text') == sms.RESPONSES.BADPHONE


def test_ok(monkeypatch):
    xml = '''<?xml version="1.0" encoding="windows-1251"?>
    <doc>
        <message-sent id="127000000003456" />
    </doc>
    '''
    monkeypatch.setattr(sms, '_send_request', lambda *a, **kw: xml)
    assert sms.send(CORRECT_PHONE, 'text') == sms.RESPONSES.OK


def test_wrong_xml(monkeypatch):
    monkeypatch.setattr(sms, '_send_request', lambda *a, **kw: 'not xml')
    assert sms.send(CORRECT_PHONE, 'text') == sms.RESPONSES.INCORRECT_YASMS_ANSWER


def test_interror(monkeypatch):
    monkeypatch.setattr(
        sms,
        '_send_request',
        lambda *a, **kw: XML_WITH_ERR.format(sms.RESPONSES.INTERROR),
    )
    assert sms.send(CORRECT_PHONE, 'text') == sms.RESPONSES.INTERROR


def test_some_yasms_error(monkeypatch):
    err = 'SOME_ERR'
    monkeypatch.setattr(
        sms,
        '_send_request',
        lambda *a, **kw: XML_WITH_ERR.format(err),
    )
    assert sms.send(CORRECT_PHONE, 'text') == err.lower()


@pytest.mark.parametrize(
    'request_exception', [requests.ConnectionError, requests.ConnectTimeout]
)
def test_requests_exception(monkeypatch, request_exception):
    def mocked(*args, **kwargs):
        raise request_exception
    monkeypatch.setattr(sms, '_send_request', mocked)
    assert sms.send(CORRECT_PHONE, 'text') == sms.RESPONSES.CONNECTION_ERROR


def test_unkown_error(monkeypatch):
    def mocked(*args, **kwargs):
        raise Exception
    monkeypatch.setattr(sms, '_send_request', mocked)
    assert sms.send(CORRECT_PHONE, 'text') == sms.RESPONSES.UNKNOWN_ERROR
