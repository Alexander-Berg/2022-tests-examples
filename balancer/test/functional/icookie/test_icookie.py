#!/usr/bin/env python
# -*- coding: utf-8 -*-

from configs import IcookieConfig, IcookieSSLConfig

import balancer.test.plugin.context as mod_ctx

from balancer.test.util.predef.handler.server.http import SimpleConfig
from balancer.test.util.predef import http
from balancer.test.util.process import BalancerStartError
from balancer.test.util.stream.ssl.stream import SSLClientOptions
from balancer.test.util import asserts

import itertools
import pytest
import re
import time
import datetime


ICOOKIE_SETCOOKIE_REGEX = re.compile(
    r"^i=([A-Za-z0-9+/]+=*); Expires=(?P<expires>[^;\n]+); Domain=[^;\n]+; Path=/(?P<secure>(; Secure)?); HttpOnly(?P<samesitenone>(; SameSite=None)?)$")


class Cookie(object):
    def __init__(self, raw, processed, flags=None):
        self.raw = raw                  # like in 'Cookie' header
        self.processed = processed      # validated and/or decrypted
        self.flags = flags or {}

    def __repr__(self):
        return 'Cookie(raw={}, processed={}, flags={})'.format(
            repr(self.raw),
            repr(self.processed),
            repr(self.flags),
        )


class Sample(object):
    def __init__(self, parentuid, parent_ext_uid, icookie, yandexuid, randomuid, yandex_login, parent_login_hash, user_agent, uuid, sae):
        self.parentuid = parentuid      # 'X-Yandex-ICookie' header from parent module
        self.parent_ext_uid = parent_ext_uid
        self.icookie = icookie
        self.yandexuid = yandexuid
        self.randomuid = randomuid
        self.yandex_login = yandex_login
        self.parent_login_hash = parent_login_hash
        self.user_agent = user_agent
        self.uuid = uuid
        self.sae = sae

    def __repr__(self):
        return 'Sample(parentuid={}, parent_ext_uid={}, icookie={}, yandexuid={}, randomuid={}, yandex_login={}, parent_login_hash={}, user_agent={}, uuid={}, sae={})'.format(
            self.parentuid,
            self.parent_ext_uid,
            self.icookie,
            self.yandexuid,
            self.randomuid,
            self.yandex_login,
            self.parent_login_hash,
            self.user_agent,
            self.uuid,
            self.sae,
        )


def get_samples():
    parentuids = [
        None,
        '7394328721468339619',
    ]

    parent_ext_uids = [
        '000291C92C12D9A8578515A3',
    ]

    parent_login_hashes = [
        None,
        '2000',
    ]

    icookies = [
        Cookie(None, None),
        Cookie('bad', None),
        Cookie('QpbgXDuLJDOn06UVFyGteARGz52GTRamlqpNA2hX7H+ROFnloetdSTT+Wgt4yuGa2+1zdSzFYw1NVEgMBEU2aai2geQ=', '4832311560007315655'),
        Cookie('3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg=', '9958730751019942006', flags={'inited_from_uuid': True}),
    ]

    yandexuids = [
        Cookie(None, None),
        Cookie('bad', None),
        Cookie('5829311091478846635', '5829311091478846635'),
    ]

    randomuids = [
        Cookie(None, None),
        Cookie('bad', None),
        Cookie('6149464821414222764', '6149464821414222764'),
    ]

    yandex_logins = [
        Cookie(None, None),
        Cookie("samhan", "3126"),
    ]

    user_agents = [
        Cookie(None, None),
        Cookie(
            'Mozilla/5.0 (Linux; Android 5.0.2; LG-H522 Build/LRX22G; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/63.0.3239.111 Mobile Safari/537.36 YandexSearch/7.21',
            'YandexSearch'
        ),
        Cookie(
            'Mozilla/5.0 (Linux; Android 4.3; GT-I9300I Build/JLS36C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.90 YaBrowser/16.11.0.649.00 Mobile Safari/537.36',
            'YandexBrowser'
        )
    ]

    uuids = [
        Cookie(None, None),
        Cookie('25228bce3cf7e8553b1954b38e3c5b7f', '9958730751019942006'),
    ]

    sae = [
        Cookie(None, None),
        Cookie('0:40d27cf9cef400998a503f6797152322:b:20.4.3.123:a:p:ru:20200423', '6255289990377114319'),
    ]

    for items in itertools.product(
        parentuids,
        parent_ext_uids,
        icookies,
        yandexuids,
        randomuids,
        yandex_logins,
        parent_login_hashes,
        user_agents,
        uuids,
        sae,
    ):
        yield Sample(*items)


class IcookieContext(object):
    def start_icookie_backend(self, config=None):
        return self.start_backend(config or SimpleConfig())

    def start_icookie_balancer(self, **kwargs):
        if 'keys_file_data' in kwargs:
            assert 'keys_file' not in kwargs
            keys_file = self.manager.fs.create_file('keys_file')
            with open(keys_file, 'wb') as f:
                f.write(kwargs['keys_file_data'])
            kwargs['keys_file'] = keys_file
            del kwargs['keys_file_data']

        is_ssl = False

        if 'is_ssl' in kwargs:
            is_ssl = bool(kwargs['is_ssl'])
            del kwargs['is_ssl']

        self.is_ssl = is_ssl

        if is_ssl:
            config = IcookieSSLConfig(backend_port=self.backend.server_config.port,
                                      cert_dir=self.certs.root_dir,
                                      **kwargs)
        else:
            config = IcookieConfig(backend_port=self.backend.server_config.port,
                                   **kwargs)

        return self.start_balancer(config)

    def start_all(self, config=None, **kwargs):
        self.start_icookie_backend(config)
        return self.start_icookie_balancer(**kwargs)

    def create_conn(self):
        if self.is_ssl:
            return self.manager.connection.http.create_ssl(
                self.balancer.config.port,
                SSLClientOptions(ca_file=self.certs.root_ca, quiet=True)
            )
        else:
            return self.create_http_connection()


icookie_ctx = mod_ctx.create_fixture(IcookieContext)


def _extract_setcookie_header(response):
    retval = None

    for header in response.headers.get_all('Set-Cookie'):
        if header.startswith('i='):
            assert retval is None   # multiple set-cookie highly undesired
            retval = header

    return retval


def _extract_icookie(setcookie_header):
    if setcookie_header is None:
        return None

    assert setcookie_header.startswith('i=')

    cookie = setcookie_header[2:].split(';', 1)[0]

    assert re.match(r'^[A-Za-z0-9\+\/]+\=*$', cookie)

    return cookie


def _extract_domain(setcookie_header):
    if setcookie_header is None:
        return None

    assert setcookie_header.startswith('i=')

    for part in setcookie_header.split(';'):
        part = part.strip()
        if part.startswith('Domain='):
            return part[7:]


def _assert_setcookie_header_format(setcookie_header, secure=True, samesitenone=False):
    if setcookie_header is not None:
        m = ICOOKIE_SETCOOKIE_REGEX.match(setcookie_header)

        assert m
        assert secure == bool(m.group('secure'))
        assert samesitenone == bool(m.group('samesitenone'))

        year = datetime.datetime.now().year
        expires_val = m.group('expires')
        assert str(year + 2) in expires_val or str(year + 1) in expires_val


def test_icookie_persistence(icookie_ctx):
    '''
    USEREXP-3593
    Simple check that icookie is set and its decrypted value persists.
    Default configuration is used.
    '''
    icookie_ctx.start_all()
    response = icookie_ctx.create_conn().perform_request(http.request.get(headers={'Host': 'yandex.ru'}))
    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')

    uid = req.headers.get_one('X-Yandex-ICookie')
    cookie = _extract_icookie(_extract_setcookie_header(response))

    assert uid
    assert cookie

    for i in xrange(20):
        headers = {
            'Host': 'yandex.ru',
            'Cookie': 'i={}'.format(cookie),
        }

        response = icookie_ctx.create_conn().perform_request(http.request.get(headers=headers))
        asserts.status(response, 200)
        assert _extract_setcookie_header(response) is None

        req = icookie_ctx.backend.state.get_request()
        asserts.header_value(req, 'X-Yandex-ICookie', uid)


def test_icookie_set_login_hash(icookie_ctx):
    """
    USEREXP-3784, USEREXP-10176
    X-Yandex-LoginHash depends only on yandex_login
    X-Yandex-SetCookie: i=... doesn't depend on yandex_login
    """
    icookie_ctx.start_all()
    cookie = "yandexuid=11111111400000001; i=V7TfPMsJYhSbq6qdFAkDw6WukvfswCth37xhcXb5L4d94wEmheECaHh3ZUJZrHunSexYclELyEaiHSlvdHn0+xU0C2E=; yandex_login=samhan"
    response = icookie_ctx.create_conn().perform_request(http.request.get(headers={'Host': 'yandex.ru', 'Cookie': cookie}))
    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')
    asserts.header_value(req, 'X-Yandex-LoginHash', '3126')

    header = _extract_setcookie_header(response)
    assert header is None

    cookie = "yandexuid=11111111400000001; i=V7TfPMsJYhSbq6qdFAkDw6WukvfswCth37xhcXb5L4d94wEmheECaHh3ZUJZrHunSexYclELyEaiHSlvdHn0+xU0C2E=; yandex_login=rkam"
    response = icookie_ctx.create_conn().perform_request(http.request.get(headers={'Host': 'yandex.ru', 'Cookie': cookie}))
    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')
    asserts.header_value(req, 'X-Yandex-LoginHash', '4428')

    header = _extract_setcookie_header(response)
    assert header is None

    cookie = "yandexuid=11111111400000001; i=V7TfPMsJYhSbq6qdFAkDw6WukvfswCth37xhcXb5L4d94wEmheECaHh3ZUJZrHunSexYclELyEaiHSlvdHn0+xU0C2E="
    response = icookie_ctx.create_conn().perform_request(http.request.get(headers={'Host': 'yandex.ru', 'Cookie': cookie}))
    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')
    asserts.no_header(req, 'X-Yandex-LoginHash')

    header = _extract_setcookie_header(response)
    assert header is None


@pytest.mark.parametrize('trust_parent', [False, True], ids=['distrust_parent', 'trust_parent'])
@pytest.mark.parametrize('trust_children', [False, True], ids=['distrust_children', 'trust_children'])
@pytest.mark.parametrize('enable_set_cookie', [False, True], ids=['setcookie_off', 'setcookie_on'])
@pytest.mark.parametrize('enable_decrypting', [False, True], ids=['decrypt_off', 'decrypt_on'])
@pytest.mark.parametrize('child_active', [False, True], ids=['child_passive', 'child_active'])
@pytest.mark.parametrize('force_equal_to_yandexuid', [False, True], ids=['normal', 'force_equal_to_yandexuid'])
@pytest.mark.parametrize('force_generate_from_searchapp_uuid', [False, True], ids=['normal', 'force_generate_from_searchapp_uuid'])
@pytest.mark.parametrize('force_generate_from_yandex_browser_uuid', [False, True], ids=['normal', 'force_generate_from_yandex_browser_uuid'])
def test_icookie_options_combinations(icookie_ctx, trust_parent, trust_children, enable_set_cookie, enable_decrypting, child_active,
                                      force_equal_to_yandexuid, force_generate_from_searchapp_uuid, force_generate_from_yandex_browser_uuid):
    '''
    USEREXP-3593
    Test icookie on many combinations of options and input data
    '''

    child_headers = [
        ('Set-Cookie', 'yandexuid=1001777331453206209'),        # should never be affected
    ]

    if child_active:
        child_headers.append(('Set-Cookie', 'i=SOMEVALUE'))

    opts = dict(
        trust_parent=trust_parent,
        trust_children=trust_children,
        enable_set_cookie=enable_set_cookie,
        enable_decrypting=enable_decrypting,
        child_active=child_active,
        force_equal_to_yandexuid=force_equal_to_yandexuid,
        force_generate_from_searchapp_uuid=force_generate_from_searchapp_uuid,
        force_generate_from_yandex_browser_uuid=force_generate_from_yandex_browser_uuid,
    )

    icookie_ctx.start_all(
        config=SimpleConfig(http.response.ok(headers=child_headers)),
        trust_parent=trust_parent,
        trust_children=trust_children,
        enable_set_cookie=enable_set_cookie,
        enable_decrypting=enable_decrypting,
        force_equal_to_yandexuid=force_equal_to_yandexuid,
        force_generate_from_searchapp_uuid=force_generate_from_searchapp_uuid,
        force_generate_from_yandex_browser_uuid=force_generate_from_yandex_browser_uuid,
        enable_guess_searchapp=True,
    )

    for sample in get_samples():
        parent_cookies = [
            ('yandexuid', sample.yandexuid.raw),
            ('i', sample.icookie.raw),
            ('sae', sample.sae.raw),
        ]
        if sample.yandex_login.raw:
            parent_cookies.append(("yandex_login", sample.yandex_login.raw))

        parent_cookies = '; '.join('{}={}'.format(k, v) for k, v in parent_cookies if v is not None)

        host = 'yandex.ru'

        parent_headers = [
            ('Host', host),
        ]

        if parent_cookies:
            parent_headers.append(('Cookie', parent_cookies))

        if sample.randomuid.raw is not None:
            parent_headers.append(('X-Yandex-RandomUID', sample.randomuid.raw))

        if sample.parentuid is not None:
            parent_headers.append(('X-Yandex-ICookie', sample.parentuid))

        if sample.parent_ext_uid is not None:
            parent_headers.append(('X-Yandex-ICookie-Ext', sample.parent_ext_uid))

        if sample.parent_login_hash is not None:
            parent_headers.append(('X-Yandex-LoginHash', sample.parent_login_hash))

        if sample.user_agent.raw is not None:
            parent_headers.append(('User-Agent', sample.user_agent.raw))

        if sample.uuid.raw is not None:
            path = '/?uuid={}'.format(sample.uuid.raw)
        else:
            path = '/'

        resp = icookie_ctx.create_conn().perform_request(http.request.get(path=path, headers=parent_headers))
        req = icookie_ctx.backend.state.get_request()

        asserts.status(resp, 200)
        asserts.one_header_value(resp, 'Set-Cookie', 'yandexuid=1001777331453206209')

        asserts.header_value(req, 'Host', host)

        if parent_cookies:
            asserts.header_value(req, 'Cookie', parent_cookies)
        else:
            asserts.no_header(req, 'Cookie')

        _check_decrypted_uid(opts, sample, req)
        _check_setcookie_header(opts, sample, resp)
        _check_decrypted_login_hash(opts, sample, req)
        _check_misc_headers(opts, sample, req)


def _check_misc_headers(opts, sample, req):
    asserts.no_header(req, 'X-Yandex-ICookie-Error')

    if (opts['trust_parent'] and sample.parentuid) or not opts['enable_decrypting']:
        asserts.no_header(req, 'X-Yandex-ICookie-Info')
        return
    elif opts['force_generate_from_searchapp_uuid'] and sample.user_agent.processed == 'YandexSearch':
        if sample.uuid.processed is not None or sample.sae.processed is not None:
            asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=uuid')
            return
    elif opts['force_generate_from_yandex_browser_uuid'] and sample.user_agent.processed == 'YandexBrowser':
        if sample.sae.processed is not None:
            asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=uuid')
            return
    elif sample.icookie.flags.get('inited_from_uuid'):
        pass
    elif (opts['force_equal_to_yandexuid'] and (sample.yandexuid.raw is not None) and
          (sample.yandexuid.processed is not None) and (sample.yandexuid.processed != sample.icookie.processed)):
        asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=yandexuid')
        return

    if sample.icookie.flags.get('inited_from_uuid'):
        asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=uuid')
    elif sample.icookie.processed is not None:
        asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=unknown')
    elif sample.yandexuid.processed is not None:
        asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=yandexuid')
    else:
        asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=random')


def _check_decrypted_login_hash(opts, sample, req):
    if sample.parent_login_hash is not None and opts['trust_parent']:
        asserts.header_value(req, 'X-Yandex-LoginHash', sample.parent_login_hash)
    elif sample.yandex_login.processed is None:
        asserts.no_header(req, 'X-Yandex-LoginHash')
    elif opts['enable_decrypting'] and sample.yandex_login.processed:
        asserts.header_value(req, 'X-Yandex-LoginHash', sample.yandex_login.processed)
    else:
        asserts.no_header(req, 'X-Yandex-LoginHash')


def _check_decrypted_uid(opts, sample, req):
    uid = req.headers.get_one('X-Yandex-ICookie')

    if opts['trust_parent'] and sample.parentuid is not None:
        # this case should not be affected by 'enable_decrypting'
        assert uid == sample.parentuid
        return

    if not opts['enable_decrypting']:
        assert uid is None
        return

    assert uid is not None

    if opts['force_generate_from_searchapp_uuid'] and sample.user_agent.processed == 'YandexSearch':
        candidates = [
            sample.sae.processed,
            sample.uuid.processed,
            sample.icookie.processed,
            sample.yandexuid.processed,
            sample.randomuid.processed,
        ]
    elif opts['force_generate_from_yandex_browser_uuid'] and sample.user_agent.processed == 'YandexBrowser':
        candidates = [
            sample.sae.processed,
            sample.icookie.processed,
            sample.yandexuid.processed,
            sample.randomuid.processed,
        ]
    elif sample.icookie.flags.get('inited_from_uuid'):
        assert sample.icookie.processed is not None

        candidates = [
            sample.icookie.processed,
        ]
    elif opts['force_equal_to_yandexuid']:
        candidates = []

        if sample.yandexuid.raw:
            candidates.append(sample.yandexuid.processed)
        elif not sample.icookie.processed:
            candidates.append(sample.randomuid.processed)

        candidates.append(sample.icookie.processed)
    else:
        candidates = [
            sample.icookie.processed,
            sample.yandexuid.processed,
            sample.randomuid.processed,
        ]

    for cookie in candidates:
        if cookie is not None:
            assert uid == cookie
            return


def _check_setcookie_header(opts, sample, resp):
    header = _extract_setcookie_header(resp)
    value = _extract_icookie(header)

    if opts['trust_children'] and opts['child_active']:
        # this case should not be affected by 'enable_set_cookie'
        assert value == 'SOMEVALUE'
        return

    _assert_setcookie_header_format(header)

    if not opts['enable_set_cookie']:
        assert header is None
        return

    if opts['trust_parent'] and sample.parentuid is not None and sample.parent_login_hash is not None and sample.parent_ext_uid is not None:
        assert header is None
        return

    setcookie_needed = True

    if sample.icookie.raw is not None and sample.icookie.processed is not None:
        if opts['force_generate_from_searchapp_uuid'] and sample.user_agent.processed == 'YandexSearch':
            if sample.sae.processed is not None:
                setcookie_needed = (sample.sae.processed != sample.icookie.processed)
            elif sample.uuid.processed is not None:
                setcookie_needed = (sample.uuid.processed != sample.icookie.processed)
            else:
                setcookie_needed = False
        elif opts['force_generate_from_yandex_browser_uuid'] and sample.user_agent.processed == 'YandexBrowser':
            if sample.sae.processed is not None:
                setcookie_needed = (sample.sae.processed != sample.icookie.processed)
            else:
                setcookie_needed = False
        elif sample.icookie.flags.get('inited_from_uuid'):
            setcookie_needed = False
        elif opts['force_equal_to_yandexuid']:
            if sample.yandexuid.raw is not None:
                setcookie_needed = (sample.yandexuid.processed is not None) and (sample.yandexuid.processed != sample.icookie.processed)
            elif sample.randomuid.raw is not None and not sample.icookie.processed:
                setcookie_needed = (sample.randomuid.processed is not None) and (sample.randomuid.processed != sample.icookie.processed)
            else:
                setcookie_needed = False
        else:
            setcookie_needed = False

    if setcookie_needed:
        assert header is not None
        assert value is not None
    else:
        assert header is None


def test_icookie_domains(icookie_ctx):
    '''
    USEREXP-3593
    Simple check on domains processing
    Default configuration is used.
    '''

    data = [
        # good
        ('yandex.ru', '.yandex.ru'),
        ('www.yandex.ru', '.yandex.ru'),
        ('yandex.com.tr', '.yandex.com.tr'),
        ('www.yandex.com.tr', '.yandex.com.tr'),

        # not specified in options
        ('yandex.ua', None),
        ('www.yandex.ua', None),
        ('yandex.by', None),
        ('www.yandex.by', None),

        # bad
        ('fakeyandex.ru', None),
        ('andex.ru', None),
        ('yandex', None),
        ('yandex.ru.ru', None),
        ('..', None),
        ('.', None),
        ('', None),
        (None, None),
    ]

    icookie_ctx.start_all()

    for host, domain in data:
        headers = []

        if host is not None:
            headers.append(('Host', host))

        resp = icookie_ctx.create_conn().perform_request(http.request.get(headers=headers))
        asserts.status(resp, 200)

        req = icookie_ctx.backend.state.get_request()
        asserts.single_header(req, 'X-Yandex-ICookie')
        setcookie_header = _extract_setcookie_header(resp)

        if domain is None:
            assert setcookie_header is None
        else:
            assert setcookie_header is not None

        assert domain == _extract_domain(setcookie_header)


def test_icookie_bad_domains(icookie_ctx):
    '''
    USEREXP-3593
    Test that module fails if domains are empty or improperly configured
    '''

    samples = [
        None,
        '',
        '\n',
        '\nyandex.ru',
        ';yandex.ru',
        ' yandex.ru',
        'yandex.ru\n',
        'yandex.ru;',
        'yandex.ru ',
        'yandex .ru',
        'yandex.ru, yandex.com.tr',
    ]

    icookie_ctx.start_icookie_backend()

    for sample in samples:
        with pytest.raises(BalancerStartError):
            icookie_ctx.start_icookie_balancer(domains=sample)


@pytest.mark.parametrize('take_randomuid_from', [None, 'X-Yandex-RandomUID', 'X-Yandex-SomeOther'], ids=['None', 'default', 'custom'])
def test_icookie_randomuid_header(icookie_ctx, take_randomuid_from):
    '''
    USEREXP-3593
    Test that reading of randomuid can be configured.
    '''

    icookie_ctx.start_all(
        config=None,
        take_randomuid_from=take_randomuid_from,
    )

    headers = {
        'X-Yandex-RandomUID': '10000001230000000',
        'X-Yandex-SomeOther': '20000001234500000',
        'Host': 'yandex.ru',
    }

    resp = icookie_ctx.create_conn().perform_request(http.request.get(headers=headers))

    asserts.status(resp, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')

    uid = req.headers.get_one('X-Yandex-ICookie')
    cookie = _extract_icookie(_extract_setcookie_header(resp))

    assert uid
    assert cookie

    for header in ('X-Yandex-RandomUID', 'X-Yandex-SomeOther'):
        if header == take_randomuid_from:
            assert uid == headers[header]
        else:
            assert not uid == headers[header]


@pytest.mark.parametrize('decrypt_custom', [False, True], ids=['decrypt_default', 'decrypt_custom'])
@pytest.mark.parametrize('error_custom', [False, True], ids=['error_default', 'error_custom'])
@pytest.mark.parametrize('info_custom', [False, True], ids=['info_default', 'info_custom'])
@pytest.mark.parametrize('trust_parent', [False, True], ids=['distrust_parent', 'trust_parent'])
def test_icookie_headers_change(icookie_ctx, decrypt_custom, error_custom, info_custom, trust_parent):
    '''
    USEREXP-3593
    Test that it is possible to change 'X-Yandex-ICookie' and 'X-Yandex-ICookie-Error' headers
    And also that in 'trust_parent' mode correct header is being read
    '''

    decrypt_current = 'X-Yandex-ICookie'
    decrypt_other = 'X-Yandex-Smth'
    error_current = 'X-Yandex-ICookie-Error'
    error_other = 'X-Yandex-Smth-Error'
    info_current = 'X-Yandex-ICookie-Info'
    info_other = 'X-Yandex-Smth-Info'

    if decrypt_custom:
        decrypt_current, decrypt_other = decrypt_other, decrypt_current
    if error_custom:
        error_current, error_other = error_other, error_current
    if info_custom:
        info_current, info_other = info_other, info_current

    icookie_ctx.start_all(
        config=None,
        decrypted_uid_header=decrypt_current,
        error_header=error_current,
        info_header=info_current,
        trust_parent=trust_parent,
    )

    headers = {
        'Host': 'yandex.ru',

        decrypt_current: 'current',  # parent claims it has already processed icookie
        decrypt_other: 'other',      # and with other header too

        info_current: 'current-info',
        info_other: 'other-info',

    }

    resp = icookie_ctx.create_conn().perform_request(http.request.get(headers=headers))
    req = icookie_ctx.backend.state.get_request()

    asserts.status(resp, 200)

    asserts.single_header(req, decrypt_current)
    asserts.single_header(req, decrypt_other)

    asserts.header_value(req, decrypt_other, 'other')
    asserts.header_value(req, info_other, 'other-info')

    if trust_parent:
        asserts.header_value(req, decrypt_current, 'current')
        asserts.header_value(req, info_current, 'current-info')
    else:
        asserts.no_header_value(req, decrypt_current, 'current')
        asserts.no_header_value(req, info_current, 'current-info')

    asserts.no_header(req, error_current)    # don't know how to reproduce error
    asserts.no_header(req, error_other)


@pytest.mark.parametrize('use_custom_keys', [False, True], ids=['default', 'custom'])
def test_icookie_keys_sources(icookie_ctx, use_custom_keys):
    '''
    USEREXP-3593
    Test that it is possible to use custom keys
    '''

    kwargs = {}

    if use_custom_keys:
        kwargs['use_default_keys'] = False
        kwargs['keys_file_data'] = ('2\t1\n' +
                                    '73819369BBF07549CF22A33CB9E9CEDC\t9DCB4BF2CF958101DA6251CF9B6E2590\n' +
                                    'D406CFFEF25D92F3F2C248E77DCF1293\tA3D3C5D80ED0889DFFAD6156B0CE7A9E')
    else:
        kwargs['use_default_keys'] = True

    icookie_ctx.start_all(config=None, **kwargs)

    samples = [
        # bad
        (None, False, None, True),
        ('bad', False, None, True),

        # default
        ('ykFdLUx0eDR9g0CVAJ7QffeHx4E5Gbdzif+a/fwxXJTs9FXzX+ZObPTm6cO22KR2+Z1m+2F/4vUnMEBGbbfJS1zkEzo=', False, '7128964061480464153', False),
        ('gInxg9OcZlIpCE6B44V1NnFpTjbWeux3j7HZDlRT3a5TJnMcDfwzR6gSxL7ZixgR9KvHfSSMLcLYrB97q8ZV5smxnIs=', False, '2271308501480464153', False),

        # custom old keyno
        ('06lcmyschsjAraaJilk5wv1KMyEcysDRTXu+axr2Efy1bW2vbhW1Q1VOiKqkwQPXIKLPFaYNcebOct3fSrZXld+47YQ=', True, '1495879651480464153', True),
        # custom current keyno
        ('yR+Iwq9NCJx1VxKbAsKcRlNHNmokTgE9M+wWp53gJ3bdJ3IJmZ/R/XLjc13yA+aR0hJVGMqqQI8Zhe8+m6Y6o1wW6m8=', True, '3831459661480464153', False),
    ]

    for cookie, is_custom, uid, setcookie in samples:
        headers = {'Host': 'yandex.ru'}
        if cookie:
            headers['Cookie'] = 'i={}'.format(cookie)

        resp = icookie_ctx.create_conn().perform_request(http.request.get(headers=headers))
        asserts.status(resp, 200)

        req = icookie_ctx.backend.state.get_request()
        asserts.single_header(req, 'X-Yandex-ICookie')

        req_uid = req.headers.get_one('X-Yandex-ICookie')
        resp_cookie = _extract_icookie(_extract_setcookie_header(resp))

        if uid is None:
            assert resp_cookie is not None
        else:
            if is_custom == use_custom_keys:
                if setcookie:
                    assert resp_cookie is not None
                else:
                    assert resp_cookie is None
                assert req_uid == uid
            else:
                assert resp_cookie is not None
                assert not req_uid == uid


def test_icookie_bad_headers(icookie_ctx):
    '''
    USEREXP-3593
    Test that balancer fails when given bad headers names
    '''

    values = [
        ' ',
        '\n',
        'bad header name',
    ]

    options = [
        'take_randomuid_from',
        'decrypted_uid_header',
        'error_header',
    ]

    icookie_ctx.start_icookie_backend()

    for option in options:
        for value in values:
            with pytest.raises(BalancerStartError):
                icookie_ctx.start_icookie_balancer(**{option: value})


@pytest.mark.parametrize('trust_parent', [False, True], ids=['distrust_parent', 'trust_parent'])
@pytest.mark.parametrize('trust_children', [False, True], ids=['distrust_children', 'trust_children'])
def test_icookie_file_switch(icookie_ctx, trust_parent, trust_children):
    '''
    USEREXP-3593
    Test that when file_switch is on balancer stops handling icookie, but continues cleaning headers correctly
    '''

    child_headers = {
        'Set-Cookie': 'i=SOMEVALUE',
    }

    headers = {
        'Host': 'yandex.ru',
        'X-Yandex-ICookie': 'SOMEHEADER',
        'X-Yandex-ICookie-Ext': 'SOMEHEADER',
        'X-Yandex-LoginHash': 'SOMEHEADER',
        'X-Yandex-ICookie-Error': 'SOMEERROR',
    }

    file_switch = icookie_ctx.manager.fs.create_file('file_switch')

    icookie_ctx.start_all(
        config=SimpleConfig(http.response.ok(headers=child_headers)),
        file_switch=file_switch,
        trust_parent=trust_parent,
        trust_children=trust_children,
    )
    time.sleep(1.5)

    resp = icookie_ctx.create_conn().perform_request(http.request.get(headers=headers))
    asserts.status(resp, 200)

    if trust_children:
        asserts.single_header(resp, 'Set-Cookie')
    else:
        asserts.no_header(resp, 'Set-Cookie')

    req = icookie_ctx.backend.state.get_request()

    if trust_parent:
        asserts.single_header(req, 'X-Yandex-ICookie-Error')
        asserts.single_header(req, 'X-Yandex-ICookie')
    else:
        asserts.no_header(req, 'X-Yandex-ICookie-Error')
        asserts.no_header(req, 'X-Yandex-ICookie')

    icookie_ctx.manager.fs.remove(file_switch)
    time.sleep(1.5)

    resp = icookie_ctx.create_conn().perform_request(http.request.get(headers=headers))
    asserts.status(resp, 200)

    if trust_children:
        asserts.single_header(resp, 'Set-Cookie')
        asserts.header_value(resp, 'Set-Cookie', 'i=SOMEVALUE')
    elif trust_parent:
        asserts.no_header(resp, 'Set-Cookie')
    else:
        asserts.single_header(resp, 'Set-Cookie')
        asserts.no_header_value(resp, 'Set-Cookie', 'i=SOMEVALUE')

    req = icookie_ctx.backend.state.get_request()

    if trust_parent:
        asserts.single_header(req, 'X-Yandex-ICookie-Error')
        asserts.single_header(req, 'X-Yandex-ICookie')
        asserts.header_value(req, 'X-Yandex-ICookie-Error', 'SOMEERROR')
        asserts.header_value(req, 'X-Yandex-ICookie', 'SOMEHEADER')
    else:
        asserts.no_header(req, 'X-Yandex-ICookie-Error')
        asserts.single_header(req, 'X-Yandex-ICookie')
        asserts.no_header_value(req, 'X-Yandex-ICookie', 'SOMEHEADER')


def test_icookie_nossl_no_setcookie_by_default(icookie_ctx):
    '''
    USEREXP-4244
    Test that there is no Set-Cookie over unsecure connection by default
    '''
    icookie_ctx.start_all(
        is_ssl=False,
        scheme_bitmask=None,
    )

    response = icookie_ctx.create_conn().perform_request(http.request.get(headers={'Host': 'yandex.ru'}))
    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')

    assert _extract_setcookie_header(response) is None


@pytest.mark.parametrize('is_ssl', [False, True], ids=['nossl', 'ssl'])
@pytest.mark.parametrize('flag_secure', [False, True], ids=['nosecure', 'secure'])
@pytest.mark.parametrize('scheme_bitmask', [0, 1, 2, 3], ids=['0', '1', '2', '3'])
def test_icookie_ssl_and_flag_secure(icookie_ctx, is_ssl, flag_secure, scheme_bitmask):
    '''
    USEREXP-4244
    Test 'flag_secure' and 'scheme_bitmask' over nossl and ssl connections
    '''
    icookie_ctx.start_all(
        is_ssl=is_ssl,
        flag_secure=flag_secure,
        scheme_bitmask=scheme_bitmask,
    )

    response = icookie_ctx.create_conn().perform_request(http.request.get(headers={'Host': 'yandex.ru'}))
    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')

    setcookie_header = _extract_setcookie_header(response)

    scheme = 2 if is_ssl else 1

    if scheme & scheme_bitmask:
        assert setcookie_header is not None
        _assert_setcookie_header_format(setcookie_header, secure=flag_secure)
    else:
        assert setcookie_header is None


_USER_AGENTS = [
    'Mozilla/5.0 (Android 6.0; Mobile; rv:68.0) Gecko/68.0 Firefox/68.0',
    'Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/76.0.3729.169 Safari/537.36'
]


@pytest.mark.parametrize('flag_secure', [False, True], ids=['nosecure', 'secure'])
@pytest.mark.parametrize('bro', [False, True], ids=['nobro', 'bro'])
def test_icookie_samesite_none(icookie_ctx, flag_secure, bro):
    '''
    BALANCER-3030, USEREXP-8154, USEREXP-7324
    '''
    icookie_ctx.start_all(
        flag_secure=flag_secure,
    )

    response = icookie_ctx.create_conn().perform_request(http.request.get(headers={
        'Host': 'yandex.ru',
        'user-agent': _USER_AGENTS[bro]
    }))
    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')

    setcookie_header = _extract_setcookie_header(response)

    assert setcookie_header is not None
    _assert_setcookie_header_format(setcookie_header, secure=flag_secure or bro, samesitenone=bro)


@pytest.mark.parametrize('force_generate_from_searchapp_uuid', [False, True], ids=['normal', 'force_generate_from_searchapp_uuid'])
def test_searchapp_user_agent_parsing(icookie_ctx, force_generate_from_searchapp_uuid):
    '''
    USEREXP-5030
    Test that searhapp is correctly detected from User-Agent header
    '''
    user_agents_searchapp = [
        # YandexSearch
        'Dalvik/2.1.0 (Linux; U; Android 5.0.2; D5503 Build/14.5.A.0.242) YandexSearch/7.50',
        'Dalvik/2.1.0 (Linux; U; Android 5.0.2; HTC_E9pw Build/LRX22G) YandexSearch/7.50 YandexSearchWebView/7.50',
        'Mozilla/5.0 (Linux; Android 4.4.2; ALCATEL ONE TOUCH P310X Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Safari/537.36 YandexSearch/5.40/apad',
        'Mozilla/5.0 (Linux; Android 4.4.2; 4024D Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36 YandexSearch/6.10',
        'Dalvik/2.1.0 (Linux; U; Android 7.0; SM-J330F Build/NRD90M) YandexSearch/7.45 YandexSearchBrowser/7.45',
        'Dalvik/2.1.0 (Linux; U; Android 6.0; Lenovo A2016a40 Build/MRA58K) YandexSearch/7.45',

        # YaApp_iOS
        'Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) YaBrowser/16.3.3.4 YaApp_iOS/3.90 YaApp_iOS_Browser/3.90',
        'ozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.3.8 (KHTML, like Gecko) YaBrowser/16.3.3.4 YaApp_iOS/3.32',
        'Mozilla/5.0 (iPhone; CPU iPhone OS 10_1_1 like Mac OS X) AppleWebKit/602.2.14 (KHTML, like Gecko) Mobile/14B100 YaBrowser/16.3.3.4 YaApp_iOS/2.10',

        # YaAppFenerbahce_iOS
        'Mozilla/5.0 (iPhone; CPU iPhone OS 11_2 like Mac OS X) AppleWebKit/604.4.7 (KHTML, like Gecko) YaAppFenerbahce_iOS/100',

        # YaAppNext_iOS
        'Mozilla/5.0 (iPhone; CPU iPhone OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) YaAppNext_iOS/1.1',

        # Yandex Search Plugin Android
        'Yandex Search Plugin Android/312',
    ]

    user_agents_other = [
        # Chrome
        'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1145.0 Safari/537.1',
        # Firefox
        'Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:60.0) Gecko/20100101 Firefox/60.0',
        # YandexBrowser
        'Mozilla/5.0 (Linux; Android 4.3; GT-I9300I Build/JLS36C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.90 YaBrowser/16.11.0.649.00 Mobile Safari/537.36',
        # Opera
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.170 Safari/537.36 OPR/53.0.2907.68 (Edition Rambler)',
        # MSIE
        'Mozilla/5.0 (Windows NT 8.0; Trident/7.0; rv:9.0) like Gecko',
        # AndroidBrowser
        'Mozilla/5.0 (Linux; Android 5.1.1; Lenovo A6020a40 Build/LMY47V; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/63.0.3239.111 Mobile Safari/537.36',
        # YandexBrowserLite
        'Mozilla/5.0 (Linux; U; Android 4.1.2; ru-ru; s4502 Build/JZO54K) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 Mobile Safari/534.30 YaBrowser/18.3.1.78 (lite)',
        # UCBrowser
        'Mozilla/5.0 (Linux; U; Android 4.2.2; en-US; NT-3603P Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/11.4.5.1005 U3/0.8.0 Mobile Safari/534.30',
        # OperaMobile
        'Mozilla/5.0 (Linux; Android 7.0; PMT3101_4G Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.111 Safari/537.36 OPR/46.3.2246.127744',
        # ChromeMobile
        ('Mozilla/5.0 (Linux; Android 7.0; SM-G930S Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Crosswalk/20.50.533.51 Mobile Safari/537.36'
         ' NAVER(inapp; search; 590; 8.7.3)'),
        # OperaMini
        'Mozilla/5.0 (Linux; U; Android 8.0.0; SM-G955N Build/R16NW; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 OPR/35.0.2254.127755',
        # Unknown
        '',

        # Trash
        'YandexSearch',
        'YandexSearchFake/1.0',
        'andexSearch/1.0',
    ]

    data = [
        (True, user_agents_searchapp),
        (False, user_agents_other),
    ]

    icookie_ctx.start_all(
        force_generate_from_searchapp_uuid=force_generate_from_searchapp_uuid,
    )

    path = '/?uuid=588ed30474e6b358016b86124470df14'

    headers = {
        'Host': 'yandex.ru',
        'Cookie': 'i=l1DPiO5z0UCW94LKlqtExSjZpvl8R8XsNPGTgF6JTP7fhdMdJjqWhXcqVmsNHUnse4c3QBK1lb7Oak4+9NUg3GfdRmw=',
    }

    for is_searchapp, user_agents in data:
        for user_agent in user_agents:
            headers['User-Agent'] = user_agent

            response = icookie_ctx.create_conn().perform_request(http.request.get(path=path, headers=headers))
            asserts.status(response, 200)

            req = icookie_ctx.backend.state.get_request()
            asserts.single_header(req, 'X-Yandex-ICookie')

            if force_generate_from_searchapp_uuid and is_searchapp:
                asserts.header_value(req, 'X-Yandex-ICookie', '3379313980872344960')
            else:
                asserts.header_value(req, 'X-Yandex-ICookie', '9793170961527790053')


@pytest.mark.parametrize('force_generate_from_yandex_browser_uuid', [False, True], ids=['normal', 'force_generate_from_yandex_browser_uuid'])
def test_yandex_browser_user_agent_parsing(icookie_ctx, force_generate_from_yandex_browser_uuid):
    '''
    USEREXP-5030
    Test that yandex_browser is correctly detected from User-Agent header
    '''
    user_agents_yandex_browser = [
        # YandexBrowser
        'Mozilla/5.0 (Linux; Android 4.3; GT-I9300I Build/JLS36C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.90 YaBrowser/16.11.0.649.00 Mobile Safari/537.36',
    ]

    user_agents_other = [
        # YandexSearch
        'Dalvik/2.1.0 (Linux; U; Android 5.0.2; D5503 Build/14.5.A.0.242) YandexSearch/7.50',
        'Dalvik/2.1.0 (Linux; U; Android 5.0.2; HTC_E9pw Build/LRX22G) YandexSearch/7.50 YandexSearchWebView/7.50',
        'Mozilla/5.0 (Linux; Android 4.4.2; ALCATEL ONE TOUCH P310X Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Safari/537.36 YandexSearch/5.40/apad',
        'Mozilla/5.0 (Linux; Android 4.4.2; 4024D Build/KOT49H) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Mobile Safari/537.36 YandexSearch/6.10',
        'Dalvik/2.1.0 (Linux; U; Android 7.0; SM-J330F Build/NRD90M) YandexSearch/7.45 YandexSearchBrowser/7.45',
        'Dalvik/2.1.0 (Linux; U; Android 6.0; Lenovo A2016a40 Build/MRA58K) YandexSearch/7.45',
        # YaApp_iOS
        'Mozilla/5.0 (iPhone; CPU iPhone OS 11_0 like Mac OS X) AppleWebKit/604.1.38 (KHTML, like Gecko) YaBrowser/16.3.3.4 YaApp_iOS/3.90 YaApp_iOS_Browser/3.90',
        'ozilla/5.0 (iPhone; CPU iPhone OS 10_3_3 like Mac OS X) AppleWebKit/603.3.8 (KHTML, like Gecko) YaBrowser/16.3.3.4 YaApp_iOS/3.32',
        'Mozilla/5.0 (iPhone; CPU iPhone OS 10_1_1 like Mac OS X) AppleWebKit/602.2.14 (KHTML, like Gecko) Mobile/14B100 YaBrowser/16.3.3.4 YaApp_iOS/2.10',
        # YaAppFenerbahce_iOS
        'Mozilla/5.0 (iPhone; CPU iPhone OS 11_2 like Mac OS X) AppleWebKit/604.4.7 (KHTML, like Gecko) YaAppFenerbahce_iOS/100',
        # YaAppNext_iOS
        'Mozilla/5.0 (iPhone; CPU iPhone OS 8_1_2 like Mac OS X) AppleWebKit/600.1.4 (KHTML, like Gecko) YaAppNext_iOS/1.1',
        # Yandex Search Plugin Android
        'Yandex Search Plugin Android/312',
        # Chrome
        'Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.1 (KHTML, like Gecko) Chrome/21.0.1145.0 Safari/537.1',
        # Firefox
        'Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:60.0) Gecko/20100101 Firefox/60.0',
        # Opera
        'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/66.0.3359.170 Safari/537.36 OPR/53.0.2907.68 (Edition Rambler)',
        # MSIE
        'Mozilla/5.0 (Windows NT 8.0; Trident/7.0; rv:9.0) like Gecko',
        # AndroidBrowser
        'Mozilla/5.0 (Linux; Android 5.1.1; Lenovo A6020a40 Build/LMY47V; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/63.0.3239.111 Mobile Safari/537.36',
        # UCBrowser
        'Mozilla/5.0 (Linux; U; Android 4.2.2; en-US; NT-3603P Build/JDQ39) AppleWebKit/534.30 (KHTML, like Gecko) Version/4.0 UCBrowser/11.4.5.1005 U3/0.8.0 Mobile Safari/534.30',
        # OperaMobile
        'Mozilla/5.0 (Linux; Android 7.0; PMT3101_4G Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.111 Safari/537.36 OPR/46.3.2246.127744',
        # ChromeMobile
        ('Mozilla/5.0 (Linux; Android 7.0; SM-G930S Build/NRD90M) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/50.0.2661.102 Crosswalk/20.50.533.51 Mobile Safari/537.36'
         ' NAVER(inapp; search; 590; 8.7.3)'),
        # OperaMini
        'Mozilla/5.0 (Linux; U; Android 8.0.0; SM-G955N Build/R16NW; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/64.0.3282.137 Mobile Safari/537.36 OPR/35.0.2254.127755',
        # Unknown
        '',

        # Trash
        'YandexSearch',
        'YandexSearchFake/1.0',
        'andexSearch/1.0',
    ]

    data = [
        (True, user_agents_yandex_browser),
        (False, user_agents_other),
    ]

    icookie_ctx.start_all(
        force_generate_from_yandex_browser_uuid=force_generate_from_yandex_browser_uuid,
    )

    path = '/'

    headers = {
        'Host': 'yandex.ru',
        'Cookie': 'i=l1DPiO5z0UCW94LKlqtExSjZpvl8R8XsNPGTgF6JTP7fhdMdJjqWhXcqVmsNHUnse4c3QBK1lb7Oak4+9NUg3GfdRmw=; sae=0:588ed30474e6b358016b86124470df14:b:20.4.3.123:a:p:ru:20200423',
    }

    for is_yandex_browser, user_agents in data:
        for user_agent in user_agents:
            headers['User-Agent'] = user_agent

            response = icookie_ctx.create_conn().perform_request(http.request.get(path=path, headers=headers))
            asserts.status(response, 200)

            req = icookie_ctx.backend.state.get_request()
            asserts.single_header(req, 'X-Yandex-ICookie')

            if force_generate_from_yandex_browser_uuid and is_yandex_browser:
                asserts.header_value(req, 'X-Yandex-ICookie', '3379313980872344960')
                asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=uuid')
            else:
                asserts.header_value(req, 'X-Yandex-ICookie', '9793170961527790053')
                asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=unknown')


@pytest.mark.parametrize('enable_parse_searchapp_uuid', [False, True], ids=['no_parse_uuid', 'normal'])
def test_searchapp_uuid_parsing(icookie_ctx, enable_parse_searchapp_uuid):
    '''
    USEREXP-5030
    Test that searhapp uuid is correctly parsed from CGI
    '''

    data = [
        # good
        ('7659861380437818494', '/assistant?api_key=45de325a-08de-435d-bcc3-1ebf6e0ae41b&apiv=2&app_build_number=49403&app_id=ru.yand'
                                'ex.searchplugin&app_platform=android&app_req_id=1527692795560833-102258-vla1-1656-AST&app_version=70'
                                '30500&app_version_name=7.35&cellid=250%2C02%2C41981%2C3640%2C0&did=1b7c400c0d4f92db28622da5caeffbdd&'
                                'lang=ru-RU&manufacturer=T591&model=KIICAA+POWER&os_version=7.0&rn=7&search_token=793ce6b09d2ded8a431'
                                '476f82122e2a7%3Aiwtzqsmokbinpiguywq%3A1527682887&uuid=d447b1646bc548f1bb09967bde6eb1b3&wifi=98%3Ade%'
                                '3Ad0%3A92%3A8e%3Aad%2C-74'),
        ('1943448490213014489', '/assistant?api_key=45de325a-08de-435d-bcc3-1ebf6e0ae41b&apiv=2&app_build_number=49480&app_id=ru.yand'
                                'ex.searchplugin&app_platform=android&app_req_id=1527694114258307-193898-sas1-5678-AST&app_version=70'
                                '50003&app_version_name=7.50&cellid=250%2C01%2C506113%2C17760%2C0&did=3c8abb1400d2f44c6248a3c39718cc8'
                                '0&lang=ru-RU&manufacturer=samsung&model=SM-J530FM&os_version=7.0&rn=7&search_token=0a77ad1e6672a2901'
                                'e7a58d77eb8bba6%3Akhpooxzbtxuozbgbfm%3A1527683954&uuid=21083baf1ddcb2d7441cabae39b5f87f&wifi=10%3A47'
                                '%3A80%3A5d%3A01%3Ab3%2C-43;98%3Ade%3Ad0%3Ab3%3Acb%3Ac6%2C-61;cc%3Acc%3A81%3A80%3A20%3A3c%2C-64;ac%3A'
                                '22%3A0b%3A91%3A07%3A10%2C-65;10%3A7b%3A44%3A79%3Abc%3A88%2C-73;cc%3Acc%3A81%3A7a%3A29%3A36%2C-74;34%'
                                '3Ace%3A00%3A42%3Aec%3A01%2C-74;cc%3Acc%3A81%3A7b%3A17%3Af6%2C-80;44%3A94%3Afc%3Af4%3Aef%3A8e%2C-85;d'
                                '4%3A60%3Ae3%3A20%3Aa9%3A0a%2C-83;cc%3Acc%3A81%3A7b%3A3d%3A12%2C-86;b4%3Aa5%3Aef%3Aa2%3A36%3Af1%2C-88'
                                ';cc%3Acc%3A81%3A7a%3A27%3A42%2C-89'),
        ('9863116160414176827', '/search/touch/?banner_ua=Mozilla/5.0+%28Linux;+Android+6.0;+CAM-L21+Build/HUAWEICAM-L21;+wv%29+Apple'
                                'WebKit/537.36+%28KHTML%2C+like+Gecko%29+Version/4.0+Chrome/63.0.3239.111+Mobile+Safari/537.36+Yandex'
                                'Search/7.50&lr=11082&noreask=1&promo=nomooa&rearr=scheme_Local/Facts/Create/EntityAsFactFlag%3D1&rea'
                                'rr=scheme_Local/Assistant/ClientId%3Dru.yandex.searchplugin/7.50%20%28HUAWEI%20CAM-L21%7C%20android%'
                                '206.0%29&service=assistant.yandex&text=%D0%BC%D0%B8%D1%80+%D1%8E%D1%80%D1%81%D0%BA%D0%BE%D0%B3%D0%BE'
                                '+%D0%BF%D0%B5%D1%80%D0%B8%D0%BE%D0%B4%D0%B0+1+%D1%87%D0%B0%D1%81%D1%82%D1%8C+%D1%84%D0%B8%D0%BB%D1%8'
                                'C%D0%BC+%D0%B2+hd+%D0%BA%D0%B0%D1%87%D0%B5%D1%81%D1%82%D0%B2%D0%B5&ui=mobapp&uuid=728e6f908da8579512'
                                '75e367bb679057'),
        ('8175299620413678583', '/images/touch/search?images_touch_download_target_self=1&service=images.yandex&ui=webmobileapp.yande'
                                'x&appsearch_header=1&uuid=3711a6aeb3fb0aa0db6fc89fcd929c0d&app_id=ru.yandex.searchplugin&text=%D0%BD'
                                '%D0%BE%D0%B2%D1%8B%D0%B5%20%D0%BF%D0%B5%D1%81%D0%BD%D0%B8%20%D0%BC%D0%B8%D1%85%D0%B0%D0%BB%D1%8C%D1%'
                                '87%D0%B8%D0%BA%202018&clid=2218567&myreqid=1527693680557-7-fd250513-e34f-4d7d-8d78-be1b82894660-LMET'
                                'A&app_req_id=1527693680557-7-fd250513-e34f-4d7d-8d78-be1b82894660-LMETA'),

        ('3379313980872344960', '/?a=b&uuid=588ed30474e6b358016b86124470df14&c=d'),
        ('3379313980872344960', '/?a=b&uuid=588ed30474e6b358016b86124470df14'),
        ('3379313980872344960', '/?uuid=588ed30474e6b358016b86124470df14&c=d'),
        ('3379313980872344960', '/?uuid=588ed30474e6b358016b86124470df14'),

        # bad
        (None, '/?fakeuuid=588ed30474e6b358016b86124470df14adkgh'),
        (None, '/?a=1&fakeuuid=588ed30474e6b358016b86124470df14adkgh'),
        (None, '/?a=b&fakeuuid=588ed30474e6b358016b86124470df14adkgh&c=d'),
        (None, '/?uuid=588ed30474e6b358016b86124470df1'),
        (None, '/?uuid=588ed30474e6b358016b86124470df14b'),
        (None, '/?uuid=588ed30474e6b358016b86124470df1x'),
        (None, '/?uuid=AAB88ed30474e6b358016b86124470df'),
        (None, '/?uuid=1'),
        (None, ('/suggest?api_key=45de325a-08de-435d-bcc3-1ebf6e0ae41b&app_build_number=41060&app_id=ru.yandex.search'
                'plugin&app_platform=apad&app_req_id=1527692418466984-187595-vla1-1364-SGST&app_version=6040501&app_v'
                'ersion_name=6.45&apps_flyer_uid=1507545739423-2715446666707827036&cellid=250%2C01%2C44853751%2C3007%'
                '2C0&clid=2155511&lang=ru&manufacturer=samsung&model=SM-T561&os_version=4.4.4&part=%D0%BF%D0%BE%D1%81'
                '%D0%BC%D0%BE%D1%82%D1%80%D0%B5%D1%82%D1%8C+%D0%B1%D0%B5%D1%81%D0%BF%D0%BB%D0%B0%D1%82%D0%BD%D0%BE+%D'
                '0%BA%D0%B0%D0%BA+%D0%BF%D1%80%D0%B0%D0%B2%D0%B8%D0%BB%D1%8C%D0%BD%D0%BE+%D0%BF%D0%BE%D1%81%D1%82%D0%'
                'B0%D0%B2%D0%B8%D1%82%D1%8C+%D1%82%D0%B5%D0%BC%D0%BF%D0%B5%D1%80%D0%B0%D1%82%D1%83%D1%80%D1%83+%D0%B2'
                '+%D0%B8%D0%BD%D0%BA%D1%83%D0%B1%D0%B0%D1%82%D0%BE%D1%80%D0%B5+%D0%BD%D0%B5%D1%81%D1%83%D1%88%D0%BA%D'
                '0%B0+%D0%B1%D0%B81+%D0%B2+%D0%B4%D0%BE%D0%BC%D0%B0%D1%88%D0%BD%D0%B8%D1%85&pid2=2%3A5764613554455396'
                '663&search_token=3bb534534f7a3ec5e66d1fabcdadc150%3Afnsrpxdcmlvuxawwc%3A1521439004&version=1')),
    ]

    icookie_ctx.start_all(
        force_generate_from_searchapp_uuid=True,
        enable_parse_searchapp_uuid=enable_parse_searchapp_uuid,
    )

    headers = {
        'Host': 'yandex.ru',
        'Cookie': 'i=l1DPiO5z0UCW94LKlqtExSjZpvl8R8XsNPGTgF6JTP7fhdMdJjqWhXcqVmsNHUnse4c3QBK1lb7Oak4+9NUg3GfdRmw=',
        'User-Agent': ('Mozilla/5.0 (Linux; Android 4.4.2; ALCATEL ONE TOUCH P310X Build/KOT49H) AppleWebKit/537.36 '
                       '(KHTML, like Gecko) Version/4.0 Chrome/30.0.0.0 Safari/537.36 YandexSearch/5.40/apad'),
    }

    for expected_icookie, path in data:
        response = icookie_ctx.create_conn().perform_request(http.request.get(path=path, headers=headers))
        asserts.status(response, 200)

        req = icookie_ctx.backend.state.get_request()
        asserts.single_header(req, 'X-Yandex-ICookie')

        if enable_parse_searchapp_uuid and expected_icookie is not None:
            asserts.header_value(req, 'X-Yandex-ICookie', expected_icookie)
        else:
            asserts.header_value(req, 'X-Yandex-ICookie', '9793170961527790053')


@pytest.mark.parametrize('enable_parse_searchapp_uuid', [False, True], ids=['no_parse_uuid', 'normal'])
def test_searchapp_uuid_from_sae_parsing(icookie_ctx, enable_parse_searchapp_uuid):
    '''
    BROWSER-138806
    Test that searhapp uuid is correctly parsed from SAE
    '''

    icookie_ctx.start_all(
        force_generate_from_searchapp_uuid=True,
        enable_parse_searchapp_uuid=enable_parse_searchapp_uuid,
    )
    sae_icookies = [
        ('0:40d27cf9cef400998a503f6797152322:b:20.4.3.123:a:p:ru:20200423', '6255289990377114319'),
        ('0:B08F4911-BD28-4337-B6F4-77BFD17A7249:p:20.4.3.123:i:p:ru:20200423', '5943359070901873267'),
        ('0:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:p:20.4.3.123:i:p:ru:20200423', '9104058940743849002'),
        ('asdf', None),
    ]
    icookie_paths = [
        # good
        ('7659861380437818494', '/assistant?api_key=45de325a-08de-435d-bcc3-1ebf6e0ae41b&apiv=2&app_build_number=49403&app_id=ru.yand'
                                'ex.searchplugin&app_platform=android&app_req_id=1527692795560833-102258-vla1-1656-AST&app_version=70'
                                '30500&app_version_name=7.35&cellid=250%2C02%2C41981%2C3640%2C0&did=1b7c400c0d4f92db28622da5caeffbdd&'
                                'lang=ru-RU&manufacturer=T591&model=KIICAA+POWER&os_version=7.0&rn=7&search_token=793ce6b09d2ded8a431'
                                '476f82122e2a7%3Aiwtzqsmokbinpiguywq%3A1527682887&uuid=d447b1646bc548f1bb09967bde6eb1b3&wifi=98%3Ade%'
                                '3Ad0%3A92%3A8e%3Aad%2C-74'),
        ('8175299620413678583', '/images/touch/search?images_touch_download_target_self=1&service=images.yandex&ui=webmobileapp.yande'
                                'x&appsearch_header=1&uuid=3711a6aeb3fb0aa0db6fc89fcd929c0d&app_id=ru.yandex.searchplugin&text=%D0%BD'
                                '%D0%BE%D0%B2%D1%8B%D0%B5%20%D0%BF%D0%B5%D1%81%D0%BD%D0%B8%20%D0%BC%D0%B8%D1%85%D0%B0%D0%BB%D1%8C%D1%'
                                '87%D0%B8%D0%BA%202018&clid=2218567&myreqid=1527693680557-7-fd250513-e34f-4d7d-8d78-be1b82894660-LMET'
                                'A&app_req_id=1527693680557-7-fd250513-e34f-4d7d-8d78-be1b82894660-LMETA'),

        ('3379313980872344960', '/?a=b&uuid=588ed30474e6b358016b86124470df14&c=d'),
        ('3379313980872344960', '/?uuid=588ed30474e6b358016b86124470df14'),

        # bad
        (None, '/?fakeuuid=588ed30474e6b358016b86124470df14adkgh'),
        (None, '/?uuid=1'),
        (None, ('/suggest?api_key=45de325a-08de-435d-bcc3-1ebf6e0ae41b&app_build_number=41060&app_id=ru.yandex.search'
                'plugin&app_platform=apad&app_req_id=1527692418466984-187595-vla1-1364-SGST&app_version=6040501&app_v'
                'ersion_name=6.45&apps_flyer_uid=1507545739423-2715446666707827036&cellid=250%2C01%2C44853751%2C3007%'
                '2C0&clid=2155511&lang=ru&manufacturer=samsung&model=SM-T561&os_version=4.4.4&part=%D0%BF%D0%BE%D1%81'
                '%D0%BC%D0%BE%D1%82%D1%80%D0%B5%D1%82%D1%8C+%D0%B1%D0%B5%D1%81%D0%BF%D0%BB%D0%B0%D1%82%D0%BD%D0%BE+%D'
                '0%BA%D0%B0%D0%BA+%D0%BF%D1%80%D0%B0%D0%B2%D0%B8%D0%BB%D1%8C%D0%BD%D0%BE+%D0%BF%D0%BE%D1%81%D1%82%D0%'
                'B0%D0%B2%D0%B8%D1%82%D1%8C+%D1%82%D0%B5%D0%BC%D0%BF%D0%B5%D1%80%D0%B0%D1%82%D1%83%D1%80%D1%83+%D0%B2'
                '+%D0%B8%D0%BD%D0%BA%D1%83%D0%B1%D0%B0%D1%82%D0%BE%D1%80%D0%B5+%D0%BD%D0%B5%D1%81%D1%83%D1%88%D0%BA%D'
                '0%B0+%D0%B1%D0%B81+%D0%B2+%D0%B4%D0%BE%D0%BC%D0%B0%D1%88%D0%BD%D0%B8%D1%85&pid2=2%3A5764613554455396'
                '663&search_token=3bb534534f7a3ec5e66d1fabcdadc150%3Afnsrpxdcmlvuxawwc%3A1521439004&version=1')),
    ]

    cookie = 'sae={}; yandexuid=11111111400000001; i=Tgd+O/Ugk1Z2HlwnQAHvD2RjqIJguO6Am5BSYLk5nN8gUpRN1bx3OlXDFsVK/6abhashMd6EC3iWxCFWmuf4u2QCW+A='

    for sae, sae_icookie in sae_icookies:
        for cgi_icookie, path in icookie_paths:
            response = icookie_ctx.create_conn().perform_request(
                http.request.get(
                    path=path,
                    headers={
                        'Host': 'yandex.ru',
                        'User-Agent': 'Mozilla/5.0 (Linux; arm; Android 11; MA-A3) AppleWebKit/537.36 (KHTML, like Gecko) '
                            'Chrome/93.0.4577.82 YaApp_Android/21.90.1 YaSearchBrowser/21.90.1 BroPP/1.0 SA/3 Mobile Safari/537.36 TA/7.1',
                        'Cookie': cookie.format(sae)
                    }
                )
            )
            asserts.status(response, 200)
            asserts.no_header(response, 'Y-Balancer-Experiments')

            req = icookie_ctx.backend.state.get_request()
            asserts.single_header(req, 'X-Yandex-ICookie')

            expected_icookie = None
            if enable_parse_searchapp_uuid:
                expected_icookie = (sae_icookie or cgi_icookie)

            if expected_icookie is None:
                asserts.header_value(req, 'X-Yandex-ICookie', '11111111400000001')
                asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=yandexuid')
            else:
                asserts.header_value(req, 'X-Yandex-ICookie', expected_icookie)


@pytest.mark.parametrize('enable_guess_searchapp', [None, False, True], ids=['default', 'disable', 'enable'])
def test_enable_guess_searchapp(icookie_ctx, enable_guess_searchapp):
    '''
    USEREXP-6946
    Test that option enable_guess_searchapp works
    '''
    icookie_ctx.start_all(
        force_equal_to_yandexuid=True,
        enable_guess_searchapp=enable_guess_searchapp,
    )

    cookie = 'yandexuid=11111111400000001; i=3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg='
    response = icookie_ctx.create_conn().perform_request(http.request.get(headers={'Host': 'yandex.ru', 'Cookie': cookie}))
    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')

    setcookie_header = _extract_setcookie_header(response)
    setcookie_value = _extract_icookie(setcookie_header)

    if enable_guess_searchapp or enable_guess_searchapp is None:
        assert setcookie_header is None
        assert setcookie_value is None

        asserts.header_value(req, 'X-Yandex-ICookie', '9958730751019942006')
        asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=uuid')
    else:
        assert setcookie_header is not None
        assert setcookie_value is not None

        asserts.header_value(req, 'X-Yandex-ICookie', '11111111400000001')
        asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=yandexuid')


@pytest.mark.parametrize('force_generate_from_yandex_browser_uuid', [False, True], ids=['normal', 'force_generate_from_yandex_browser_uuid'])
def test_sae_cookie_experiment(icookie_ctx, force_generate_from_yandex_browser_uuid):
    exp_A_testid='1'
    exp_B_testid='2'
    icookie_ctx.start_all(
        force_equal_to_yandexuid=True,
        enable_guess_searchapp=False,
        force_generate_from_yandex_browser_uuid=force_generate_from_yandex_browser_uuid,
        exp_A_testid=exp_A_testid,
        exp_B_testid=exp_B_testid,
        exp_salt='abc',
        exp_A_slots='37,38,39,40,41,42,43,44,45,46,69',
        exp_B_slots='73,88,89,90,91,92,93,94,95,96,97',
    )
    data = [
        (
            '0:40d27cf9cef400998a503f6797152322:b:20.4.3.123:a:p:ru:20200423',
            '1', 91,
            '6255289990377114319',
        ),
        (
            '0:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa:b:20.4.3.123:a:p:ru:20200423',
            '2', 28,
            '5906247140874454993',
        ),
        (
            '0:bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb:b:20.4.3.123:a:p:ru:20200423',
            None, None,
            '1866059590179891651',
        ),
        (
            # USEREXP-12931 -parse uuid only for android
            '0:B08F4911-BD28-4337-B6F4-77BFD17A7249:p:20.4.3.123:i:p:ru:20200423',
            None, None,
            None,
        ),
        (
            '0:XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX:p:20.4.3.123:a:p:ru:20200423',
            None, None,
            None,
        ),
        (
            'asdf',
            None, None,
            None,
        ),
    ]
    user_agents = [
        (True, 'Mozilla/5.0 (Linux; Android 4.3; GT-I9300I Build/JLS36C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.90 YaBrowser/16.11.0.649.00 Mobile Safari/537.36'),
        (False, 'Some invalid user-agent')
    ]

    cookie = 'sae={}; yandexuid=11111111400000001; i=Tgd+O/Ugk1Z2HlwnQAHvD2RjqIJguO6Am5BSYLk5nN8gUpRN1bx3OlXDFsVK/6abhashMd6EC3iWxCFWmuf4u2QCW+A='

    for sae, testid, bucket, ick in data:
        for is_yandex_browser, user_agent in user_agents:
            response = icookie_ctx.create_conn().perform_request(
                http.request.get(
                    headers={
                        'Host': 'yandex.ru',
                        'User-Agent': user_agent,
                        'Cookie': cookie.format(sae)
                    }
                )
            )
            asserts.status(response, 200)
            asserts.no_header(response, 'Y-Balancer-Experiments')

            req = icookie_ctx.backend.state.get_request()
            asserts.single_header(req, 'X-Yandex-ICookie')

            if not is_yandex_browser or ick is None:
                asserts.no_header(req, 'Y-Balancer-Experiments')
                asserts.header_value(req, 'X-Yandex-ICookie', '11111111400000001')
                asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=yandexuid')
            elif force_generate_from_yandex_browser_uuid:
                asserts.header_value(req, 'X-Yandex-ICookie', ick)
                asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=uuid')
                asserts.no_header(req, 'Y-Balancer-Experiments')
            else:
                if testid is not None:
                    asserts.header_value(req, 'Y-Balancer-Experiments', '{},0,{}'.format(testid, bucket))
                if testid == exp_B_testid:
                    asserts.header_value(req, 'X-Yandex-ICookie', ick)
                    asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=uuid')
                else:
                    asserts.header_value(req, 'X-Yandex-ICookie', '11111111400000001')
                    asserts.header_value(req, 'X-Yandex-ICookie-Info', 'source=yandexuid')


@pytest.mark.parametrize('encrypted_header', [None, 'X-Yandex-ICookie-Encrypted'], ids=['disable', 'enable'])
@pytest.mark.parametrize('trust_parent', [False, True], ids=['distrust_parent', 'trust_parent'])
@pytest.mark.parametrize('encrypted_parent_header', [False, True], ids=['encrypted_parent_header_not_exists', 'encrypted_parent_header_exists'])
def test_icookie_encrypted_header(icookie_ctx, encrypted_header, trust_parent, encrypted_parent_header):
    '''
    USEREXP-10631
    Test that option encrypted_header works
    '''
    icookie_ctx.start_all(
        encrypted_header=encrypted_header,
        trust_parent=trust_parent,
    )

    cookie = 'i=3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg='
    headers={
        'Host': 'yandex.ru',
        'Cookie': cookie,
        'X-Yandex-ICookie': '9958730751019942006',
        'X-Yandex-LoginHash': '3132',
        'X-Yandex-ICookie-Ext': '400059A03B5BD1333CCB1475'
    }

    if encrypted_parent_header:
        headers['X-Yandex-ICookie-Encrypted'] = 'test'

    response = icookie_ctx.create_conn().perform_request(http.request.get(headers=headers))
    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()
    asserts.single_header(req, 'X-Yandex-ICookie')

    if encrypted_header and not trust_parent:
        asserts.single_header(req, 'X-Yandex-ICookie-Encrypted')
        asserts.no_header_value(req, 'X-Yandex-ICookie-Encrypted', 'test')

    if encrypted_header and trust_parent and encrypted_parent_header:
        asserts.one_header_value(req, 'X-Yandex-ICookie-Encrypted', 'test')

    if not encrypted_header and not encrypted_parent_header:
        asserts.no_header(req, 'X-Yandex-ICookie-Encrypted')

    if trust_parent:
        asserts.one_header_value(req, 'X-Yandex-ICookie-Ext', '400059A03B5BD1333CCB1475')
    else:
        asserts.one_header_value(req, 'X-Yandex-ICookie-Ext', '400059A03B5BD1333CCB1476')


@pytest.mark.parametrize('src_icookie_header', [None, 'X-Yandex-Some-Other-Header'], ids=['default', 'custom'])
def test_src_icookie_header(icookie_ctx, src_icookie_header):
    '''
    USEREXP-11161
    Test that option src_icookie_header works
    '''
    icookie_ctx.start_all(
        src_icookie_header=src_icookie_header,
    )

    if src_icookie_header is None:
        src_icookie_header = 'X-Yandex-Src-ICookie'

    data = [
        (
            '/path?icookie=kRJaRgLFkFi7LKc6y6aUbUXlHmQS/xmn2qovSo+SE/LssCBrgbsrWgj0Zs73b2pRGDfi5Cd4YPXkEAEqhMXL6uniaQo=',
            '9958730751019942006',
        ),
        (
            '/path?icookie=3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg=',
            '9958730751019942006',
        ),
        (
            '/path?icookie=3g43bRg9iBL9RD%2FJ4tYGE5pDCTsaetYZDnRLK5WtTaU%2BeE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg%3D',
            '9958730751019942006',
        ),
        (
            '/path?a=1&icookie=3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg=',
            '9958730751019942006',
        ),
        (
            '/path?a=1&icookie=3g43bRg9iBL9RD%2FJ4tYGE5pDCTsaetYZDnRLK5WtTaU%2BeE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg%3D',
            '9958730751019942006',
        ),
        (
            '/path?icookie=3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg=&b=2',
            '9958730751019942006',
        ),
        (
            '/path?icookie=3g43bRg9iBL9RD%2FJ4tYGE5pDCTsaetYZDnRLK5WtTaU%2BeE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg%3D&b=2',
            '9958730751019942006',
        ),
        (
            '/path?a=1&icookie=3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg=&b=2',
            '9958730751019942006',
        ),
        (
            '/path?a=1&icookie=3g43bRg9iBL9RD%2FJ4tYGE5pDCTsaetYZDnRLK5WtTaU%2BeE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg%3D&b=2',
            '9958730751019942006',
        ),
        (
            '/path?a=1&icookie=3g43bRg9iBL9RD%2FJ4tYGE5pDCTsaetYZDnRLK5WtTaU%2BeE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg%3&b=2',
            None,
        ),
        (
            '/path?icookie=3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDn',
            None,
        ),
        (
            '/path?icookie=%',
            None,
        ),
        (
            '/',
            None,
        ),
    ]

    for path, src_icookie in data:
        response = icookie_ctx.create_conn().perform_request(http.request.get(
            path=path,
            headers={
                'Host': 'yandex.ru',
            },
        ))

        asserts.status(response, 200)

        req = icookie_ctx.backend.state.get_request()

        if src_icookie is None:
            asserts.no_header(req, src_icookie_header)
        else:
            asserts.header_value(req, src_icookie_header, src_icookie)


@pytest.mark.parametrize('max_transport_age', [None, 1, 10000])
def test_transport_lifetime(icookie_ctx, max_transport_age):
    '''
    USEREXP-13992
    Test transport lifetime
    '''
    icookie_ctx.start_all(
        encrypted_header='X-Yandex-ICookie-Encrypted',
        max_transport_age=max_transport_age,
    )

    icookie_encrypted = '3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg='
    icookie_decrypted = '9958730751019942006'

    response = icookie_ctx.create_conn().perform_request(http.request.get(
        path='/',
        headers={
            'Host': 'yandex.ru',
            'Cookie': 'i={}'.format(icookie_encrypted),
        },
    ))

    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()

    asserts.header_value(req, 'X-Yandex-ICookie', icookie_decrypted)
    asserts.single_header(req, 'X-Yandex-ICookie-Encrypted')

    transport = req.headers.get_one('X-Yandex-ICookie-Encrypted')

    time.sleep(2)

    response = icookie_ctx.create_conn().perform_request(http.request.get(
        path='/?icookie={}'.format(transport),   # good
        headers={
            'Host': 'yandex.ru',
        },
    ))

    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()

    if max_transport_age != 1:
        asserts.header_value(req, 'X-Yandex-Src-ICookie', icookie_decrypted)
    else:
        asserts.no_header(req, 'X-Yandex-Src-ICookie')


def test_transport_lifecycle(icookie_ctx):
    '''
    USEREXP-11760
    Test transport lifecycle
    '''
    icookie_ctx.start_all(
        encrypted_header='X-Yandex-ICookie-Encrypted',
    )

    icookie_encrypted = '3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg='
    icookie_decrypted = '9958730751019942006'

    response = icookie_ctx.create_conn().perform_request(http.request.get(
        path='/',
        headers={
            'Host': 'yandex.ru',
            'Cookie': 'i={}'.format(icookie_encrypted),
        },
    ))

    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()

    asserts.header_value(req, 'X-Yandex-ICookie', icookie_decrypted)
    asserts.single_header(req, 'X-Yandex-ICookie-Encrypted')

    transport = req.headers.get_one('X-Yandex-ICookie-Encrypted')

    # good usage of transport
    response = icookie_ctx.create_conn().perform_request(http.request.get(
        path='/?icookie={}'.format(transport),   # good
        headers={
            'Host': 'yandex.ru',
            'Cookie': 'i=BxGXJqsWjed8t/6+NRjV9LtgmWzMOtNqVf2sn6tbj6RdCz63RucPAG8aSM/cYwvAbHjmAEskGMm39iag1UpFMxnIXu4=',
        },
    ))

    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()

    asserts.header_value(req, 'X-Yandex-Src-ICookie', icookie_decrypted)
    asserts.header_value(req, 'X-Yandex-ICookie', '2088893681629155690')

    # bad usage of transport
    response = icookie_ctx.create_conn().perform_request(http.request.get(
        path='/',
        headers={
            'Host': 'yandex.ru',
            'Cookie': 'i={}'.format(transport),  # bad
            'X-Yandex-RandomUID': '9949609191629156126',
        },
    ))

    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()

    asserts.no_header(req, 'X-Yandex-Src-ICookie')
    asserts.header_value(req, 'X-Yandex-ICookie', '9949609191629156126')


@pytest.mark.parametrize('force_generate_from_transport', [False, True], ids=['normal', 'force'])
@pytest.mark.parametrize('exp_type', [None, 1, 2], ids=['normal', 'other', 'experiment'])
@pytest.mark.parametrize('fast', [False, True], ids=['long', 'fast'])
def test_init_from_transport(icookie_ctx, force_generate_from_transport, exp_type, fast):
    '''
    USEREXP-11685
    Test transport initialization
    '''

    if fast:
        max_transport_age = 10
    else:
        max_transport_age = 4000000000  # 126 years

    exp_A_testid='1'
    exp_B_testid='2'

    opts = dict(
        force_equal_to_yandexuid=True,
        force_generate_from_transport=force_generate_from_transport,
        max_transport_age=max_transport_age,
    )

    if exp_type is not None:
        opts.update(dict(
            exp_type=exp_type,
            exp_A_testid=exp_A_testid,
            exp_B_testid=exp_B_testid,
            exp_salt='abc',
            exp_A_slots='37,38,39,40,41,42,43,44,45,96',
            exp_B_slots='86,87,88,89,91,92,93,94,95,69',
        ))

    icookie_ctx.start_all(**opts)

    user_agents = [
        (True, 'Beru/378 (iPhone; iOS 14.6; Scale/2.00)'),
        (False, 'Mozilla/5.0 (Linux; Android 4.3; GT-I9300I Build/JLS36C) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.90 YaBrowser/16.11.0.649.00 Mobile Safari/537.36'),
        (False, 'Some invalid user-agent'),
    ]

    transports = [
        ('asdf', None, False, None, None),
        ('3g43bRg9iBL9RD/J4tYGE5pDCTsaetYZDnRLK5WtTaU+eE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg=',
         '9958730751019942006', False, '2', '60'),
        ('3g43bRg9iBL9RD%2FJ4tYGE5pDCTsaetYZDnRLK5WtTaU%2BeE0eLTUD0xvw4ZfxDRZNcTXeDqMBvMmqUpmePVHxRxQtLtg%3D',
         '9958730751019942006', False, '2', '60'),
        ('MmR77/BBnZnUoISPTrcy+H2UW6R6CVMozh3bXEKrE2AFTcYowsBY7ZDkPqmTwRCkMvttqmrACVlLdyaeH6V2Sn9RTo0=',
         '9958730751019942006', True, '2', '60'),
        ('MmR77%2FBBnZnUoISPTrcy%2BH2UW6R6CVMozh3bXEKrE2AFTcYowsBY7ZDkPqmTwRCkMvttqmrACVlLdyaeH6V2Sn9RTo0%3D',
         '9958730751019942006', True, '2', '60'),
        ('qyO8eEK+1Gp/P/63B3/75p0K2Bg5yTelVSlpr56AoGMvcTE/OJPup3h1mRihzn67ZbolYM0F52qJP9mObrXHk9BnOO4=',
         '6869222881629313553', False, '1', '33'),
        ('qyO8eEK%2B1Gp%2FP%2F63B3%2F75p0K2Bg5yTelVSlpr56AoGMvcTE%2FOJPup3h1mRihzn67ZbolYM0F52qJP9mObrXHk9BnOO4%3D',
         '6869222881629313553', False, '1', '33'),
        ('HWycDwvHlIL4IwOfLLVETePZ8lHTcIlrZusLvvxoZBV/M03KpKYNM+m91DXFWIp6pcFGP/GfElpK9IyK+kcw7FAJpJk=',
         '6869222881629313553', True, '1', '33'),
        ('HWycDwvHlIL4IwOfLLVETePZ8lHTcIlrZusLvvxoZBV%2FM03KpKYNM%2Bm91DXFWIp6pcFGP%2FGfElpK9IyK%2Bkcw7FAJpJk%3D',
         '6869222881629313553', True, '1', '33'),
        ('7MN9nhyhxDgma57I/LMw2PGRLZi7+U4OrJGfUeXvP3uEEyE42L7xjQPK864M4rPlo9D9RrjBeRsexK/e2bpAWX11618=',
         '5171764961629313905', False, None, None),
        ('7MN9nhyhxDgma57I%2FLMw2PGRLZi7%2BU4OrJGfUeXvP3uEEyE42L7xjQPK864M4rPlo9D9RrjBeRsexK%2Fe2bpAWX11618%3D',
         '5171764961629313905', False, None, None),
        ('dm+DiK5m+jaSyP04c9KjI3SZ7GQHaHwonIYp5f5Y8d7gh7ve/Oggr71paDGFcuRNMtl60YK2f4/BuxDhI51X8foIr3Y=',
         '5171764961629313905', True, None, None),
        ('dm%2BDiK5m%2BjaSyP04c9KjI3SZ7GQHaHwonIYp5f5Y8d7gh7ve%2FOggr71paDGFcuRNMtl60YK2f4%2FBuxDhI51X8foIr3Y%3D',
         '5171764961629313905', True, None, None),
    ]

    cookie = 'yandexuid=5571226851629311388; i=A6KpO+sUhWkB8ZbPIJMJpEiqz/dniyLrguZRCnO9LkMo2cbqMOXqTQqryYVh6QNmx5ij6lOZjaatg7bVRg6DrpOB5Fc='

    for is_beru, user_agent in user_agents:
        for transport_encrypted, transport_decrypted, is_transport, testid, bucket in transports:
            resp = icookie_ctx.create_conn().perform_request(
                http.request.get(
                    '/?icookie={}'.format(transport_encrypted),
                    headers={
                        'Host': 'yandex.ru',
                        'User-Agent': user_agent,
                        'Cookie': cookie,
                    }
                )
            )
            asserts.status(resp, 200)
            asserts.no_header(resp, 'Y-Balancer-Experiments')

            req = icookie_ctx.backend.state.get_request()
            asserts.single_header(req, 'X-Yandex-ICookie')

            if not fast and not force_generate_from_transport and exp_type == 2 and testid is not None:
                asserts.header_value(req, 'Y-Balancer-Experiments', '{},0,{}'.format(testid, bucket))
            else:
                asserts.no_header(req, 'Y-Balancer-Experiments')

            if not fast and transport_decrypted:
                asserts.header_value(req, 'X-Yandex-Src-ICookie', transport_decrypted)
            else:
                asserts.no_header(req, 'X-Yandex-Src-ICookie')

            if ((force_generate_from_transport or (exp_type == 2 and testid == '2')) and
                    is_beru and is_transport and not fast):
                assert _extract_icookie(_extract_setcookie_header(resp))
                asserts.header_value(req, 'X-Yandex-ICookie', transport_decrypted)
            else:
                asserts.no_header(resp, 'Set-Cookie')
                asserts.header_value(req, 'X-Yandex-ICookie', '5571226851629311388')


def test_blacklist(icookie_ctx):
    '''
    USEREXP-13802
    Test that blacklisted cookies get removed
    '''
    icookie_ctx.start_all()

    response = icookie_ctx.create_conn().perform_request(http.request.get(
        path='/',
        headers={
            'Host': 'yandex.ru',
            'Cookie': 'i=iVsBuRgsbclR7kdFmSGHPynQPih4x3ba/aHaM5cqbpLAf68COtT2AzvwnS8x/iD8O1XWeJt8eyd/O5fcsmd4kEfSaHo=; yandexuid=5876612831652825242; other=must_not_be_deleted; noname',
            'X-Yandex-RandomUID': '4200287661652825207',
        },
    ))

    asserts.status(response, 200)

    req = icookie_ctx.backend.state.get_request()

    asserts.header_value(req, 'X-Yandex-ICookie', '4200287661652825207')
    asserts.header_value(req, 'Cookie', 'i=iVsBuRgsbclR7kdFmSGHPynQPih4x3ba/aHaM5cqbpLAf68COtT2AzvwnS8x/iD8O1XWeJt8eyd/O5fcsmd4kEfSaHo=; other=must_not_be_deleted; noname')

    assert _extract_icookie(_extract_setcookie_header(response)) is not None
