# encoding: utf-8
from __future__ import unicode_literals

import pytest
from tornado import gen

from intranet.webauth.lib.step import CertificateStep
from intranet.webauth.tests.utils import MockResponse, MockRequest


# TODO: check internal/external blackbox instances


@pytest.fixture(autouse=True)
def patch_AsyncHTTPClient(monkeypatch):
    class MockHTTPClient(object):
        @gen.coroutine
        def fetch(self, url):
            response = '{"id": "testuid"}'
            raise gen.Return(MockResponse(response))

    monkeypatch.setattr('tornado.httpclient.AsyncHTTPClient', MockHTTPClient)


@pytest.mark.gen_test
def test_no_cert():
    headers = {'X-Qloud-Ssl-Verified': 'NONE'}
    status, info = yield CertificateStep(MockRequest(headers), None).check()
    assert status is None
    assert info == 'No client certificate provided'


@pytest.mark.gen_test
def test_unverified_cert():
    headers = {'X-Qloud-Ssl-Verified': 'FAILED'}
    status, info = yield CertificateStep(MockRequest(headers), None).check()
    assert status is False
    assert info == 'Cannot validate client certificate'


@pytest.mark.parametrize('issuer', ['/DC=ru/DC=yandex/DC=ld',
                                    '/DC=ru/DC=yandex/DC=ld/',
                                    '/DC=ru/DC=yandex/DC=ld/CN'])
@pytest.mark.gen_test
def test_badly_formatted_issuer(issuer):
    headers = {'X-Qloud-Ssl-Verified': 'SUCCESS',
               'X-Qloud-Ssl-Issuer': issuer}
    status, info = yield CertificateStep(MockRequest(headers), None).check()
    assert status is False
    assert info == 'Incorrect SSL issuer format'


@pytest.mark.parametrize('issuer', ['', 'abc', 'FakeYandexCA'])
@pytest.mark.gen_test
def test_bad_issuer(issuer):
    headers = {'X-Qloud-Ssl-Verified': 'SUCCESS',
               'X-Qloud-Ssl-Issuer': '/DC=ru/DC=yandex/DC=ld/CN=%s' % issuer}
    status, info = yield CertificateStep(MockRequest(headers), None).check()
    assert status is False
    assert info == 'Incorrect SSL issuer'


@pytest.mark.parametrize('subject', ['/C=RU/ST=Russia/L=Moscow/O=Yandex/OU=MAC/emailAddress=testlogin@yandex-team.ru',
                                     '/C=RU/ST=Russia/L=Moscow/O=Yandex/OU=MAC/CN=/emailAddress=testlogin@yandex-team.ru',
                                     '/C=RU/ST=Russia/L=Moscow/O=Yandex/OU=MAC/CN=testlogin/emailAddress=testlogin@yandex-team.ru'])
@pytest.mark.gen_test
def test_good_issuer_badly_formatted_subject(subject):
    headers = {'X-Qloud-Ssl-Verified': 'SUCCESS',
               'X-Qloud-Ssl-Issuer': '/DC=ru/DC=yandex/DC=ld/CN=YandexInternalCA',
               'X-Qloud-Ssl-Subject': subject}
    status, info = yield CertificateStep(MockRequest(headers), None).check()
    assert status is False
    assert info == 'Incorrect SSL subject format'


@pytest.mark.gen_test
def test_good_issuer_bad_domain():
    headers = {'X-Qloud-Ssl-Verified': 'SUCCESS',
               'X-Qloud-Ssl-Issuer': '/DC=ru/DC=yandex/DC=ld/CN=YandexInternalCA',
               'X-Qloud-Ssl-Subject': '/C=RU/ST=Russia/L=Moscow/O=Yandex/OU=MAC/CN=login@baddomain.ru/emailAddress=testlogin'}
    status, info = yield CertificateStep(MockRequest(headers), None).check()
    assert status is False
    assert info == 'Incorrect SSL domain'


@pytest.mark.gen_test
def test_good_cert():
    headers = {'X-Qloud-Ssl-Verified': 'SUCCESS',
               'X-Qloud-Ssl-Issuer': '/DC=ru/DC=yandex/DC=ld/CN=YandexInternalCA',
               'X-Qloud-Ssl-Subject': '/C=RU/ST=Russia/L=Moscow/O=Yandex/OU=MAC/CN=somelogin@ld.yandex.ru/emailAddress=testlogin'}
    status, user_info = yield CertificateStep(MockRequest(headers), None).check()
    assert status is True
    assert user_info == ('somelogin', 'testuid')
