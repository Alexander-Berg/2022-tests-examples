# -*- coding: utf-8 -*-

import pytest

import six
import urlpy


def punycode_url(url):
    parsed_url = six.moves.urllib.parse.urlparse(url)
    encoded_hostname = parsed_url.hostname.decode('utf8').encode('idna')

    encoded_netloc = encoded_hostname

    if parsed_url.port:
        encoded_netloc += ':' + str(parsed_url.port)

    if parsed_url.username:
        userinfo = parsed_url.username
        if parsed_url.password:
            userinfo += ':' + parsed_url.password
        encoded_netloc = userinfo + '@' + encoded_netloc

    parsed_url = parsed_url._replace(netloc=encoded_netloc)
    return parsed_url.geturl()


def test_punycode():
    original_url = 'http://привет:привет@привет.привет/привет/привет?привет=привет'
    parsed_url = urlpy.parse(six.ensure_text(original_url))
    punycoded_url = parsed_url.punycode()
    assert six.ensure_str(punycoded_url.unicode) == 'http://привет:привет@xn--b1agh1afp.xn--b1agh1afp/привет/привет?привет=привет'


@pytest.mark.xfail(six.PY3, reason='punycode_url not working under PY3')
@pytest.mark.parametrize('original_url', [
    'http://логин:пароль@привет.привет/привет/привет?привет=привет',
    'http://логин@привет.привет/привет/привет?привет=привет',
    'http://login@привет.привет/привет/привет?привет=привет',
    'http://login@example.com/привет/привет?привет=привет',
    'http://логин@example.com/привет/привет?привет=привет',
    'http://логин@example.com/example/привет?привет=привет',
    'http://логин@example.com/example/example?привет=привет',
    'http://логин@example.com/example/example?example=привет',
    'http://логин@example.com/example/example?example=example',
])
def test_punycode_equal(original_url):
    parsed_url = urlpy.parse(six.ensure_text(original_url))
    punycoded_url = parsed_url.punycode().unicode
    punycoded_url_old = punycode_url(original_url)
    assert isinstance(punycoded_url_old, str)
    assert isinstance(punycoded_url, six.text_type)
    assert six.ensure_str(punycoded_url) == punycoded_url_old


@pytest.mark.xfail(six.PY3, reason='punycode_url not working under PY3')
def test_punycode_old():
    original_url = 'http://привет:привет@привет.привет/привет/привет?привет=привет'
    punycoded_url = punycode_url(original_url)
    assert six.ensure_str(punycoded_url) == 'http://привет:привет@xn--b1agh1afp.xn--b1agh1afp/привет/привет?привет=привет'


@pytest.mark.xfail(six.PY3, reason='punycode_url not working under PY3')
def test_punycode_old_text():
    original_url = 'http://привет:привет@привет.привет/привет/привет?привет=привет'
    text_url = six.ensure_text(original_url)
    with pytest.raises(UnicodeEncodeError):
        punycode_url(text_url)
