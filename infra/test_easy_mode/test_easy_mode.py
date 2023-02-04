# coding: utf-8
import contextlib
import logging
import os
import pytest
import requests
import six
from infra.swatlib.logutil import rndstr

from flaky import flaky
from requests.exceptions import SSLError
from six.moves import http_client as httplib
from six.moves.urllib import parse as urlparse

from awacs.lib import context
from awtest import t
from awtest.api import create_namespace
from awtest.balancer import Balancer
from awtest.https_adapter import modify_url, get_https_session
from awtest.mocks.ports import MAGIC_HTTPS_PORT
from awtest import wait_until
from infra.awacs.proto import model_pb2


NON_EC_CIPHERS = u'kRSA+AESGCM+AES128'
FIXTURE_DIR = t(u'fixtures/easy_mode_v2/')
ROOT_CA_PEM = t(u'awtest/balancer/config/certs/rootCA.pem')

pytestmark = [pytest.mark.usefixtures('httpbin'), pytest.mark.slow]


@pytest.fixture
def balancer(ctx, worker_id, balancer_executable_path, zk_storage, cache, sd_stub):
    ns_id = u'easy-mode-v2.test.yandex.net'
    create_namespace(zk_storage, cache, ns_id)
    tag = rndstr(n=10) + worker_id
    ctx = context.BackgroundCtx().with_op(op_id=tag, log=logging.getLogger('awacs-tests'))
    return Balancer(ctx, tag, balancer_executable_path, ns_id, FIXTURE_DIR, sd_port=sd_stub.port)


MAX_RUNS = 3


@flaky(max_runs=MAX_RUNS, min_passes=1)
@pytest.mark.parametrize(u'uem_version', [
    u'0.0.1',
    u'0.1.0',
])
@pytest.mark.parametrize(u'scheme', [
    u'flat',
    u'by_dc',
])
def test_easy_mode_http_response_retries(uem_version, scheme, balancer, worker_id):
    def get_balancer_pb(config_pb_):
        l7_upstream_macro_pb = config_pb_.l7_upstream_macro
        if scheme == u'flat':
            return l7_upstream_macro_pb.flat_scheme.balancer
        elif scheme == u'by_dc':
            return l7_upstream_macro_pb.by_dc_scheme.balancer
        else:
            raise AssertionError

    def get_on_error_pb(config_pb_):
        l7_upstream_macro_pb = config_pb_.l7_upstream_macro
        if scheme == u'flat':
            return l7_upstream_macro_pb.flat_scheme.on_error
        elif scheme == u'by_dc':
            return l7_upstream_macro_pb.by_dc_scheme.on_error
        else:
            raise AssertionError

    with balancer.update_upstream_config(scheme) as config_pb:
        config_pb.l7_upstream_macro.version = uem_version
        balancer_pb = get_balancer_pb(config_pb)
        balancer_pb.backend_timeout = u'100ms'
        balancer_pb.connect_timeout = u'30ms'
        balancer_pb.attempts = 3
        balancer_pb.do_not_retry_http_responses = True
        balancer_pb.max_reattempts_share = 0.2
        balancer_pb.max_pessimized_endpoints_share = 0.2
        on_error_pb = get_on_error_pb(config_pb)
        on_error_pb.rst = True

    h = {
        b'Host': '{}.easy-mode.yandex.net'.format(scheme).encode('ascii'),
    }
    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/status/200'.format(url), headers=h)
        assert resp.status_code == 200

        resp = requests.get(u'{}/status/502'.format(url), headers=h)
        assert resp.status_code == 502

        with pytest.raises(requests.ConnectionError) as e:
            requests.get(u'{}/delay/0.2'.format(url), headers=h)
        assert u'Connection reset by peer' in six.text_type(e)

    balancer_pb.do_not_retry_http_responses = True
    on_error_pb.rst = False
    on_error_pb.static.status = 504
    on_error_pb.static.content = u'Uh-oh'
    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/status/200'.format(url), headers=h)
        assert resp.status_code == 200

        resp = requests.get(u'{}/status/502'.format(url), headers=h)
        assert resp.status_code == 502

        resp = requests.get(u'{}/delay/0.2'.format(url), headers=h)
        assert resp.status_code == 504
        assert resp.text == u'Uh-oh'

    balancer_pb.retry_http_responses.codes.append(u'5xx')
    balancer_pb.retry_http_responses.exceptions.append(u'555')
    balancer_pb.retry_http_responses.on_last_failed_retry = balancer_pb.retry_http_responses.GO_TO_ON_ERROR
    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/status/200'.format(url), headers=h)
        assert resp.status_code == 200

        resp = requests.get(u'{}/status/555'.format(url), headers=h)
        assert resp.status_code == 555

        resp = requests.get(u'{}/status/502'.format(url), headers=h)
        assert resp.status_code == 504 and resp.text == u'Uh-oh'

        resp = requests.get(u'{}/delay/0.2'.format(url), headers=h)
        assert resp.status_code == 504 and resp.text == u'Uh-oh'

    balancer_pb.retry_http_responses.on_last_failed_retry = balancer_pb.retry_http_responses.PROXY_RESPONSE_AS_IS
    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/status/200'.format(url), headers=h)
        assert resp.status_code == 200

        resp = requests.get(u'{}/status/555'.format(url), headers=h)
        assert resp.status_code == 555

        resp = requests.get(u'{}/status/502'.format(url), headers=h)
        assert resp.status_code == 502

        resp = requests.get(u'{}/delay/0.2'.format(url), headers=h)
        assert resp.status_code == 504 and resp.text == u'Uh-oh'


@flaky(max_runs=MAX_RUNS, min_passes=1)
@pytest.mark.parametrize(u'part', [
    u'l7_macro',
    u'l7_upstream_macro',
])
def test_headers_rewrite(part, balancer, worker_id):
    @contextlib.contextmanager
    def update_headers():
        if part == u'l7_macro':
            with balancer.update_l7_macro() as l7_macro_pb:
                yield l7_macro_pb.headers
            pass
        elif part == u'l7_upstream_macro':
            with balancer.update_upstream_config(u'flat') as config_pb:
                yield config_pb.l7_upstream_macro.headers
        else:
            raise AssertionError()

    h = {
        b'Host': '{}.easy-mode.yandex.net'.format('flat').encode('ascii'),
        b'X-Yandex': 'bububu',
        b'Y-Google': 'lalala',
    }
    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/headers'.format(url), headers=h)
        headers = resp.json()[u'headers']
        assert headers['Host'] == 'flat.easy-mode.yandex.net'
        assert headers['X-Yandex'] == 'bububu'
        assert headers['Y-Google'] == 'lalala'

    with update_headers() as headers_pb:
        del headers_pb[:]
        rewrite_action_pb = headers_pb.add().rewrite
        rewrite_action_pb.target = u'X-Yandex'
        rewrite_action_pb.pattern.re = u'b.'
        rewrite_action_pb.replacement = u'la'

        create_action_pb = headers_pb.add().create
        create_action_pb.target = u'X-Host'
        create_action_pb.value = u'xxx'

        rewrite_action_pb = headers_pb.add().rewrite
        rewrite_action_pb.target = u'X-Host'
        rewrite_action_pb.pattern.re = u'.*'
        rewrite_action_pb.replacement = u'%{host}'

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/headers?arg=1'.format(url), headers=h)
        headers = resp.json()[u'headers']
        assert headers['Host'] == 'flat.easy-mode.yandex.net'
        assert headers['X-Yandex'] == 'labubu'
        assert headers['Y-Google'] == 'lalala'
        assert headers['X-Host'] == 'flat.easy-mode.yandex.net'

    del headers_pb[:]
    rewrite_action_pb = headers_pb.add().rewrite
    rewrite_action_pb.target = u'X-Yandex'
    rewrite_action_pb.pattern.re = u'bu'
    getattr(rewrite_action_pb.pattern, u'global').value = True
    rewrite_action_pb.pattern.case_sensitive.value = True
    rewrite_action_pb.pattern.literal.value = True
    rewrite_action_pb.replacement = u'la'

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/headers'.format(url), headers=h)
        headers = resp.json()[u'headers']
        assert headers['Host'] == 'flat.easy-mode.yandex.net'
        assert headers['X-Yandex'] == 'lalala'


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_copy_headers_from_request(balancer, worker_id):
    def f1(config_pb):
        act_pb = config_pb.l7_upstream_macro.headers.add()
        act_pb.create.target = u'X-Test1'
        act_pb.create.value = u'1'

        act_pb = config_pb.l7_upstream_macro.headers.add()
        act_pb.copy.source = u'X-Test1'
        act_pb.copy.target = u'X-Test2'

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.copy_from_request.source = u'X-Test1'
        act_pb.copy_from_request.target = u'X-Test1-copy'
        act_pb.copy_from_request.keep_existing = False

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.copy_from_request.source = u'X-Test2'
        act_pb.copy_from_request.target = u'X-Test2-copy'
        act_pb.copy_from_request.keep_existing = False

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.delete.target_re = u'X-Test4-copy'

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.copy_from_request.source = u'X-Test3'
        act_pb.copy_from_request.target = u'X-Test3-copy'
        act_pb.copy_from_request.keep_existing = False

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.copy.source = u'X-Test3-copy'
        act_pb.copy.target = u'X-Test3-copy-2'

    def f2(config_pb):
        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.create.target = u'X-Test4-copy'
        act_pb.create.value = u'init'

        act_pb = config_pb.l7_upstream_macro.headers.add()
        act_pb.create.target = u'X-Test4'
        act_pb.create.value = u'new'

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.copy_from_request.source = u'X-Test4'
        act_pb.copy_from_request.target = u'X-Test4-copy'
        act_pb.copy_from_request.keep_existing = False

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.create.target = u'X-Test5-copy'
        act_pb.create.value = u'init'

        act_pb = config_pb.l7_upstream_macro.headers.add()
        act_pb.create.target = u'X-Test5'
        act_pb.create.value = u'new'

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.copy_from_request.source = u'X-Test5'
        act_pb.copy_from_request.target = u'X-Test5-copy'
        act_pb.copy_from_request.keep_existing = True

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.copy_from_request.source = u'X-Test5'
        act_pb.copy_from_request.target = u'X-Test5-copy-2'
        act_pb.copy_from_request.keep_existing = False

        act_pb = config_pb.l7_upstream_macro.response_headers.add()
        act_pb.delete.target_re = u'X-Test5-copy-2'

    h = {
        b'Host': b'flat.easy-mode.yandex.net',
        b'X-Test3': '1',
    }

    def get_headers(url):
        resp = requests.get(u'{}/headers'.format(url), headers=h)
        assert resp.status_code == 200
        headers = resp.headers
        return headers

    with balancer.update_upstream_config(u'flat') as config_pb:
        f1(config_pb)

    with balancer.start_pginx(worker_id=worker_id) as url:
        headers = get_headers(url)
        assert 'X-Test1-copy' in headers
        assert 'X-Test2-copy' in headers
        assert 'X-Test3-copy' in headers
        assert 'X-Test3-copy-2' in headers

    with balancer.update_upstream_config(u'flat') as config_pb:
        config_pb.l7_upstream_macro.ClearField('headers')
        config_pb.l7_upstream_macro.ClearField('response_headers')
        f2(config_pb)

    with balancer.start_pginx(worker_id=worker_id) as url:
        headers = get_headers(url)
        assert headers['X-Test4-copy'] == 'new'
        assert headers['X-Test5-copy'] == 'init'
        assert 'X-Test5-copy-2' not in headers


@flaky(max_runs=3, min_passes=1)
def test_headers_xffy(balancer, worker_id):
    h = {
        b'Host': '{}.easy-mode.yandex.net'.format('flat').encode('ascii'),
        b'X-Forwarded-For-Y': 'hello_dude'
    }
    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/headers'.format(url), headers=h)
        headers = resp.json()[u'headers']
        assert headers['X-Forwarded-For-Y'] == 'hello_dude'

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = '0.3.0'

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/headers'.format(url), headers=h)
        headers = resp.json()[u'headers']
        assert headers['X-Forwarded-For-Y'] != 'hello_dude'

        resp = requests.get(u'{}/headers'.format(url), headers={b'Host': h[b'Host']})
        headers = resp.json()[u'headers']
        assert 'X-Forwarded-For-Y' in headers

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.core.trust_x_forwarded_for_y = True

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/headers'.format(url), headers=h)
        headers = resp.json()[u'headers']
        assert headers['X-Forwarded-For-Y'] == 'hello_dude'


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_headers_ja_x(balancer, worker_id):
    h = {
        b'Host': '{}.easy-mode.yandex.net'.format('flat').encode('ascii'),
        b'X-Yandex-Ja3': 'ja3',
        b'X-Yandex-Ja4': 'ja4',
        b'Z-I-Swear-I-Am-Not-A-Robot': 'just_kidding',
    }
    balancer.add_antirobot_backends()

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = '0.3.3'

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp_headers = requests.get(u'{}/headers'.format(url), headers=h).json()['headers']
        assert 'X-Yandex-Ja3' in resp_headers
        assert 'ja3' == resp_headers['X-Yandex-Ja3']
        assert 'X-Yandex-Ja4' in resp_headers
        assert 'ja4' == resp_headers['X-Yandex-Ja4']

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.antirobot.SetInParent()
        l7_macro_pb.announce_check_reply.SetInParent()
        l7_macro_pb.announce_check_reply.url_re = 'something'
        l7_macro_pb.health_check_reply.SetInParent()

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp_headers = requests.get(u'{}/headers'.format(url), headers=h).json()['headers']
        assert 'X-Yandex-Ja3' in resp_headers
        assert '0,,,,' == resp_headers['X-Yandex-Ja3']
        assert 'X-Yandex-Ja4' in resp_headers
        assert ',,,,,' == resp_headers['X-Yandex-Ja4']


@flaky(max_runs=MAX_RUNS, min_passes=1)
@pytest.mark.parametrize(u'part', [
    u'l7_upstream_macro',
])
def test_rewrite(part, balancer, worker_id):
    @contextlib.contextmanager
    def update_rewrite():
        if part == u'l7_macro':
            with balancer.update_l7_macro() as l7_macro_pb_:
                yield l7_macro_pb_.rewrite
        elif part == u'l7_upstream_macro':
            with balancer.update_upstream_config(u'flat') as config_pb:
                yield config_pb.l7_upstream_macro.rewrite
        else:
            raise AssertionError()

    h = {
        b'Host': 'flat.easy-mode.yandex.net',
    }
    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/status/200'.format(url), headers=h)
        assert resp.status_code == 200

    with update_rewrite() as rewrite_pb:
        del rewrite_pb[:]
        action_pb = rewrite_pb.add()
        action_pb.target = action_pb.URL
        action_pb.pattern.re = u'2(\\d{2})'
        action_pb.replacement = u'4(%1)'

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/status/400'.format(url), headers=h)
        assert resp.status_code == 400

    del rewrite_pb[:]
    action_pb = rewrite_pb.add()
    action_pb.target = action_pb.PATH
    action_pb.pattern.re = u'201'
    action_pb.pattern.literal.value = True
    action_pb.replacement = u'202'

    with balancer.start_pginx(worker_id=worker_id) as url:
        # path is rewritten
        resp = requests.get(u'{}/status/201'.format(url), headers=h)
        assert resp.status_code == 202

        # and args are not
        resp = requests.get(u'{}/get?x=201'.format(url), headers=h)
        assert resp.json()[u'args'][u'x'] == u'201'

    del rewrite_pb[:]
    action_pb = rewrite_pb.add()
    action_pb.target = action_pb.CGI
    action_pb.pattern.re = u'\\?x=y'
    action_pb.replacement = u'?x=%{url}'

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/get?x=y'.format(url), headers=h)
        assert resp.json()[u'args'][u'x'] == u'/get?x=y'

        resp = requests.get(u'{}/status////204'.format(url), headers=h)
        assert resp.status_code == 404

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.core.merge_slashes = True

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/status////204'.format(url), headers=h)
        assert resp.status_code == 204


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_deflation(balancer, worker_id):
    with balancer.update_upstream_config('flat') as config_pb:
        l7_upstream_macro_pb = config_pb.l7_upstream_macro
        l7_upstream_macro_pb.version = '0.1.1'
        l7_upstream_macro_pb.compression.codecs.extend(['br'])

        on_error_pb = l7_upstream_macro_pb.flat_scheme.on_error
        on_error_pb.static.status = 504
        on_error_pb.static.content = u'Uh-oh'

    h = {
        b'Host': 'flat.easy-mode.yandex.net',
        b'Accept-Encoding': '*',
    }

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_deflation', u'1.lua')) as url:
        resp = requests.get(u'{}/delay/0.2'.format(url), headers=h)
        assert resp.status_code == 504
        assert resp.headers.get('Content-Encoding') == 'br'

    with balancer.update_upstream_config('flat') as config_pb:
        l7_upstream_macro_pb = config_pb.l7_upstream_macro
        l7_upstream_macro_pb.version = '0.0.1'
        l7_upstream_macro_pb.ClearField('compression')
        l7_upstream_macro_pb.compression.SetInParent()

        static_response_pb = l7_upstream_macro_pb.static_response
        static_response_pb.status = 200
        static_response_pb.content = u'Okay'

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_deflation', u'2.lua')) as url:
        resp = requests.get(u'{}/pumpurum'.format(url), headers=h)
        assert resp.status_code == 200
        assert resp.text == u'Okay'
        assert resp.headers.get('Content-Encoding') == 'gzip'


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_headers_icookie(balancer, worker_id):
    h = {
        b'Host': '{}.easy-mode.yandex.net'.format('flat').encode('ascii'),
        b'Cookie': 'i=6n7j4pt0IRbexbKa/xIbIn6o91ZjIoduB8uBrfKWRL2VQrXg1prQZ2dBF4ETUyN5DMvwDyLahN3+c8KghjNiUCPoWOU=',
        b'X-Yandex-Icookie': 'hello_dude',
    }
    balancer.add_antirobot_backends()

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = '0.3.2'
        l7_macro_pb.core.trust_icookie = True
        l7_macro_pb.headers.add().decrypt_icookie.SetInParent()

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp_headers = requests.get(u'{}/headers'.format(url), headers=h).json()['headers']
        assert 'X-Yandex-Icookie' in resp_headers
        assert 'hello_dude' == resp_headers['X-Yandex-Icookie']

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.core.trust_icookie = False

    with balancer.start_pginx(worker_id=worker_id) as url:
        resp_headers = requests.get(u'{}/headers'.format(url), headers=h).json()['headers']
        assert 'X-Yandex-Icookie' in resp_headers
        assert '5566428151582038194' == resp_headers['X-Yandex-Icookie']


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_yandex_tld(balancer, worker_id):
    domain_pb = balancer.add_domain([u'yandex-tld.yandex.net'], domain_type=model_pb2.DomainSpec.Config.YANDEX_TLD)
    domain_pb.yandex_balancer.config.cert.id = ''  # test possible edge case

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_l7_tld_domain', u'tld.lua')) as url:
        resp = requests.get(u'{}/get'.format(url), headers={})
        assert resp.status_code == 404

        h = {
            'X-Forwarded-For-Y': 'xxx'
        }
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == 200


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_wildcard_domain_and_tld(balancer, worker_id):
    fqdn = u'flat.easy-mode.yandex.net'
    balancer.add_cert(fqdn)
    balancer.add_domain([], domain_type=model_pb2.DomainSpec.Config.WILDCARD, cert_id=u'flat.easy-mode.yandex.net',
                        protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_l7_wildcard_domain', u'wildcard_domain.lua')) as url:
        resp = requests.get(u'{}/get'.format(url), headers={})
        assert resp.status_code == 200

        s, https_url = get_https_session(balancer, url, fqdn)
        resp = s.get(u'{}/get'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

    balancer.add_domain([u'yandex-tld.yandex.net'], domain_type=model_pb2.DomainSpec.Config.YANDEX_TLD)

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_l7_tld_wildcard_domains', u'tld_wildcard_domains.lua')) as url:
        h = {
            'X-Forwarded-For-Y': 'xxx'
        }
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == 200


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_l7_macro_report(balancer, worker_id):
    balancer.add_domain([u'test.yandex.net'])

    def get_stats(unistat_url):
        unistat_url = u'{}/unistat'.format(modify_url(unistat_url, port_offset=2))
        resp_ = requests.get(unistat_url, timeout=5)
        stats_ = {}
        for k, v in resp_.json():
            stats_[k] = v
        return stats_

    h = {
        b'Host': 'test.yandex.net'
    }
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.monitoring.enable_total_signals = True

    with balancer.update_upstream_config(u'flat') as config_pb:
        config_pb.l7_upstream_macro.monitoring.uuid = u'flat'
        config_pb.l7_upstream_macro.monitoring.response_codes.extend([u'500', u'503'])

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_l7_macro_report', u'enable_total_signals=true.lua')) as url:
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == 200

        stats = get_stats(url)
        assert stats[u'report-flat-requests_summ'] == 1
        prev_service_total_reqs = stats[u'report-service_total-requests_summ']

        resp = requests.get(u'{}/get?everybodybecoolthisis=molly'.format(url), headers=h)
        assert resp.status_code == 200

        def check_1():
            stats = get_stats(url)
            assert stats[u'report-flat-requests_summ'] == 2
            assert stats[u'report-service_total-requests_summ'] == prev_service_total_reqs + 1
            assert stats[u'report-service_total_molly-requests_summ'] == 1

        wait_until(check_1, timeout=0.5)

        prev_service_total_reqs = stats[u'report-service_total-requests_summ']

        resp = requests.get(u'{}/xxx?everybodybecoolthisis=crasher'.format(url))
        assert resp.status_code == 404

        resp = requests.get(u'{}/ping'.format(url))
        assert resp.status_code == 200

        def check_2():
            stats = get_stats(url)
            assert u'report-announce_check-requests_summ' not in stats

            assert stats[u'report-flat-requests_summ'] == 2
            assert stats[u'report-service_total-requests_summ'] >= prev_service_total_reqs + 2
            assert stats[u'report-service_total_molly-requests_summ'] == 2

            assert sum(v for _, v in stats[u'report-service_total-input_size_hgram']) >= 5
            assert u'report-http-input_size_hgram' not in stats

        wait_until(check_2, timeout=0.5)

        prev_service_total_reqs = stats[u'report-service_total-requests_summ']

        resp = requests.get(u'{}/status/500'.format(url), headers=h)
        assert resp.status_code == 500
        resp = requests.get(u'{}/status/503'.format(url), headers=h)
        assert resp.status_code == 503

        def check_3():
            stats = get_stats(url)
            assert stats[u'report-service_total-requests_summ'] >= prev_service_total_reqs + 2
            assert stats[u'report-flat-requests_summ'] == 4
            assert stats[u'report-flat-outgoing_500_summ'] == 1
            assert stats[u'report-flat-outgoing_503_summ'] == 1
            assert u'report-flat-outgoing_501_summ' not in stats  # just in case

        wait_until(check_3, timeout=0.5)

    l7_macro_pb.monitoring.enable_total_signals = False
    l7_macro_pb.monitoring.enable_announce_check_signals = True

    with balancer.start_pginx(
        worker_id=worker_id, path_to_expected_lua=os.path.join(u'_l7_macro_report',
                                                               u'enable_total_signals=false,enable_announce_check_signals=true.lua')) as url:
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == 200

        resp = requests.get(u'{}/ping'.format(url))
        assert resp.status_code == 200

        def check_4():
            stats = get_stats(url)
            assert stats[u'report-announce_check-requests_summ'] == 1
            assert sum(v for _, v in stats[u'report-service_total-input_size_hgram']) >= 2
            assert u'report-http-input_size_hgram' not in stats

        wait_until(check_4, timeout=0.5)

    l7_macro_pb.ClearField('monitoring')
    with balancer.start_pginx(worker_id=worker_id,
                              path_to_expected_lua=os.path.join(u'_l7_macro_report', u'no-monitoring-section.lua')) as url:
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == 200

        stats = get_stats(url)

        def check_5():
            assert sum(v for _, v in stats[u'report-service_total-input_size_hgram']) >= 1
            assert u'report-http-input_size_hgram' not in stats

        wait_until(check_5, timeout=0.5)


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_shadow_fqdns(balancer, worker_id):
    domain_spec_pb = balancer.add_domain(['real.yandex.net'])
    domain_spec_pb.yandex_balancer.config.shadow_fqdns.append(u'shadow.yandex.net')

    h = {
        b'Host': 'real.yandex.net'
    }
    with balancer.start_pginx(worker_id=worker_id) as url:
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == 200
        h = {b'Host': 'shadow.yandex.net'}
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == 200


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_redirect_http_to_https(balancer, worker_id):
    h = {b'Host': 'flat.easy-mode.yandex.net'}
    balancer.add_cert(u'flat.easy-mode.yandex.net')
    balancer.add_domain([u'flat2.easy-mode.yandex.net'],
                        cert_id=u'flat.easy-mode.yandex.net',
                        protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)
    balancer.remove_domain(u'by_dc.easy-mode.yandex.net')
    balancer.remove_domain(u'flat.easy-mode.yandex.net')
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)
        l7_macro_pb.https.enable_tlsv1_3 = True
        l7_macro_pb.http.redirect_to_https.SetInParent()

    with balancer.start_pginx(worker_id=worker_id) as url:
        get_url = '{}/get?x=1&y=2'.format(url)
        https_get_url = u'https://{}/get?x=1&y=2'.format(h[b'Host'])
        post_url = '{}/post'.format(url)
        https_post_url = u'https://{}/post'.format(h[b'Host'])

        resp = requests.get(get_url, headers=h, allow_redirects=False)
        assert resp.headers['Location'] == https_get_url
        assert resp.status_code == 302
        resp = requests.post(post_url, headers=h, allow_redirects=False)
        assert resp.headers['Location'] == https_post_url
        assert resp.status_code == 307

    l7_macro_pb.http.redirect_to_https.permanent = True
    with balancer.start_pginx(worker_id=worker_id) as url:
        get_url = '{}/get?x=1&y=2'.format(url)
        https_get_url = u'https://{}/get?x=1&y=2'.format(h[b'Host'])
        post_url = '{}/post'.format(url)
        https_post_url = u'https://{}/post'.format(h[b'Host'])

        resp = requests.get(get_url, headers=h, allow_redirects=False)
        assert resp.headers['Location'] == https_get_url
        assert resp.status_code == 301
        resp = requests.post(post_url, headers=h, allow_redirects=False)
        assert resp.headers['Location'] == https_post_url
        assert resp.status_code == 307

    l7_macro_pb.http.ClearField('redirect_to_https')
    domain_spec_pb = balancer.add_domain([u'redirect.easy-mode.yandex.net'],
                                         cert_id=u'flat.easy-mode.yandex.net',
                                         protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)
    domain_spec_pb.yandex_balancer.config.redirect_to_https.SetInParent()

    redirect_h = {b'Host': 'redirect.easy-mode.yandex.net'}
    https_get_url = u'https://{}/get?x=1&y=2'.format(redirect_h[b'Host'])
    https_post_url = u'https://{}/post'.format(redirect_h[b'Host'])

    with balancer.start_pginx(worker_id=worker_id) as url:
        get_url = '{}/get?x=1&y=2'.format(url)
        post_url = '{}/post'.format(url)
        resp = requests.get(get_url, headers=redirect_h, allow_redirects=False)
        assert resp.headers['Location'] == https_get_url
        assert resp.status_code == 302
        resp = requests.post(post_url, headers=redirect_h, allow_redirects=False)
        assert resp.headers['Location'] == https_post_url
        assert resp.status_code == 307

    domain_spec_pb.yandex_balancer.config.redirect_to_https.permanent = True
    with balancer.start_pginx(worker_id=worker_id) as url:
        get_url = '{}/get?x=1&y=2'.format(url)
        post_url = '{}/post'.format(url)
        resp = requests.get(get_url, headers=redirect_h, allow_redirects=False)
        assert resp.headers['Location'] == https_get_url
        assert resp.status_code == 301
        resp = requests.post(post_url, headers=redirect_h, allow_redirects=False)
        assert resp.headers['Location'] == https_post_url
        assert resp.status_code == 307


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_verify_client_cert(balancer, worker_id):
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)
        l7_macro_pb.https.enable_tlsv1_3 = True

        for name, func in [(u'X-SSL-Client-CN', u'ssl_client_cert_cn'),
                           (u'X-SSL-Client-Subject', u'ssl_client_cert_subject'),
                           (u'X-SSL-Client-Verify', u'ssl_client_cert_verify_result')]:
            action_pb = l7_macro_pb.headers.add()
            action_pb.create.target = name
            action_pb.create.func = func

    balancer.add_cert(u'verify-me.yandex.net')
    balancer.add_domain([u'verify-me.yandex.net'],
                        cert_id=u'verify-me.yandex.net',
                        protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)

    certs_dir = os.path.dirname(ROOT_CA_PEM)
    with balancer.start_pginx(worker_id=worker_id) as url:
        s, https_url = get_https_session(balancer, url, hostname=u'verify-me.yandex.net')
        resp = s.get(u'{}/get'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

        headers = resp.json()[u'headers']
        assert headers[u'X-Ssl-Client-Cn'] == u'undefined'
        assert headers[u'X-Ssl-Client-Verify'] == u'undefined'
        assert headers[u'X-Ssl-Client-Subject'] == u'undefined'

    l7_macro_pb.https.verify_client_cert.SetInParent()
    with balancer.start_pginx(worker_id=worker_id) as url:
        s, https_url = get_https_session(balancer, url, hostname=u'verify-me.yandex.net')

        resp = s.get(u'{}/get'.format(https_url),
                     verify=ROOT_CA_PEM,
                     cert=(os.path.join(certs_dir, u'allCAs-verify-me.yandex.net.pem'),
                           os.path.join(certs_dir, u'verify-me.yandex.net.pem')))
        assert resp.status_code == 200

        headers = resp.json()[u'headers']
        assert headers[u'X-Ssl-Client-Cn'] == u'verify-me.yandex.net'
        assert headers[u'X-Ssl-Client-Verify'] == u'0'
        assert headers[u'X-Ssl-Client-Subject'] == (u'/C=RU/ST=Russian Federation/O=Yandex/CN=verify-me.yandex.net/'
                                                    u'emailAddress=vagrant@yandex-team.ru')


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_http2(balancer, worker_id):
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.http2.fraction.value = 0.99
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)
        l7_macro_pb.https.enable_http2 = True

    fqdn = u'test.yandex.ru'
    balancer.add_cert(fqdn)
    balancer.add_domain([fqdn],
                        cert_id=fqdn,
                        protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_http2', u'http2.lua')) as url:
        s, https_url = get_https_session(balancer, url, hostname=fqdn)
        resp = s.get(u'{}/get'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = '0.3.11'

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_http2', u'http2-0.3.11.lua')) as url:
        s, https_url = get_https_session(balancer, url, hostname=fqdn)
        resp = s.get(u'{}/get'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.http.enable_http2 = True

        l7_macro_pb.http2.ClearField('fraction')
        l7_macro_pb.https.enable_http2 = True

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_http2', u'http2-0.3.11-http.lua')) as url:
        resp = s.get(u'{}/get'.format(url))
        assert resp.status_code == 200

        s, https_url = get_https_session(balancer, url, hostname=fqdn)
        resp = s.get(u'{}/get'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

    with balancer.update_upstream_config('by_dc') as config_pb:
        l7_upstream_macro_pb = config_pb.l7_upstream_macro
        l7_upstream_macro_pb.by_dc_scheme.balancer.protocol = l7_upstream_macro_pb.by_dc_scheme.balancer.HTTP2

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_http2', u'http2-backend.lua')) as url:
        s, https_url = get_https_session(balancer, url, hostname=fqdn)
        resp = s.get(u'{}/get'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200


@pytest.mark.skip(reason="XXX romanovich@: due to py3-related issues with awtest/https_adapter.py")
@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_verify_cyrillic_certs(balancer, worker_id):
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)
        l7_macro_pb.https.enable_tlsv1_3 = True

    balancer.add_cert(u'test.yandex.ru')
    balancer.add_domain([u'test.yandex.ru', u'тест.яндекс.рф'],
                        cert_id=u'test.yandex.ru',
                        protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)

    with balancer.start_pginx(worker_id=worker_id) as url:
        if six.PY3:
            fqdns = (u'test.yandex.ru', u'тест.яндекс.рф')
        else:
            fqdns = (u'test.yandex.ru', u'тест.яндекс.рф'.encode(u'idna'))
        for fqdn in fqdns:
            s, https_url = get_https_session(balancer, url, fqdn)
            resp = s.get(u'{}/get'.format(https_url), verify=ROOT_CA_PEM)
            assert resp.status_code == 200


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_check_replies(balancer, worker_id):
    fqdn = u'test.yandex.ru'
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)
        l7_macro_pb.https.enable_tlsv1_3 = True

    balancer.add_cert(fqdn)
    balancer.add_domain([fqdn], cert_id=fqdn, protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)

    with balancer.start_pginx(worker_id=worker_id) as url:
        s, https_url = get_https_session(balancer, url, fqdn)

        resp = s.get(u'{}/get'.format(url))
        assert resp.status_code == 200

        resp = s.get(u'{}/get'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

        resp = s.get(u'{}/ping'.format(url))
        assert resp.status_code == 200
        resp = s.get(u'{}/ping'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

        resp = s.get(u'{}/awacs-balancer-health-check'.format(url))
        assert resp.status_code == 200
        resp = s.get(u'{}/awacs-balancer-health-check'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.2.1'

    with balancer.start_pginx(worker_id=worker_id) as url:
        s, https_url = get_https_session(balancer, url, fqdn)

        resp = s.get(u'{}/get'.format(url))
        assert resp.status_code == 200
        resp = s.get(u'{}/get'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

        resp = s.get(u'{}/ping'.format(url))
        assert resp.status_code == 200
        resp = s.get(u'{}/ping'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

        resp = s.get(u'{}/awacs-balancer-health-check'.format(url))
        assert resp.status_code == 200
        resp = s.get(u'{}/awacs-balancer-health-check'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.antirobot.captcha_reply.SetInParent()
    balancer.add_antirobot_backends()

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_check_replies', u'config.lua')) as url:
        resp = s.get(u'{}/showcaptcha?IAMROBOT=1'.format(url))
        assert resp.text == u'CAPTCHA'
        assert resp.status_code == 200
        for not_expected_header in ('x-yandex-ja3', 'x-yandex-ja4', 'x-yandex-icookie',
                                    'x-forwarded-for-y', 'x-antirobot-service-y'):
            assert not_expected_header not in resp.headers

        resp = s.get(u'{}/showcaptcha'.format(url))
        assert resp.text == u'NONCAPTCHA'
        assert resp.status_code == 200

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.3.8'
        l7_macro_pb.antirobot.service = u'pumpurum'
        l7_macro_pb.antirobot.req_group = u'murupmup'

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_check_replies', u'config-0.3.8.lua')) as url:
        resp = s.get(u'{}/showcaptcha?IAMROBOT=1'.format(url))
        assert resp.text == u'CAPTCHA'
        assert resp.status_code == 200
        for expected_header in ('x-yandex-ja3', 'x-yandex-ja4', 'x-yandex-icookie',
                                'x-forwarded-for-y', 'x-antirobot-service-y', 'x-antirobot-req-group'):
            assert expected_header in resp.headers

        resp = s.get(u'{}/showcaptcha'.format(url))
        assert resp.text == u'NONCAPTCHA'
        assert resp.status_code == 200


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_domain_matchers(balancer, worker_id):
    fqdn = u'test.yandex.ru'
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.2.0'
    balancer.add_domain([fqdn])

    h = {
        b'Host': 'bad.fqdn',
    }
    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_domain_matchers', u'0_2_0.lua')) as url:
        with pytest.raises(requests.ConnectionError, match=u'Connection reset by peer'):
            requests.get(u'{}/get'.format(url), headers=h)

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.2.2'
    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_domain_matchers', u'0_2_2.lua')) as url:
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == 404

    balancer.add_domain([u'yandex-tld.yandex.net'], domain_type=model_pb2.DomainSpec.Config.YANDEX_TLD)
    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_domain_matchers', u'0_2_2_tld.lua')) as url:
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == 404

    with balancer.update_domain_config(u'flat.easy-mode.yandex.net') as domain_config_pb:
        domain_config_pb.fqdns.append(u'pumpurum.easy-mode.yandex.net')
        domain_config_pb.fqdns.append(u'*.flat.easy-mode.yandex.net')

    # Regression test for https://st.yandex-team.ru/AWACS-602
    hosts = [
        'flat.easy-mode.yandex.net',
        'flat.easy-mode.yandex.net:{}',
        'pumpurum.easy-mode.yandex.net',
        'pumpurum.easy-mode.yandex.net:{}',
        '123.flat.easy-mode.yandex.net',
        '123.flat.easy-mode.yandex.net:{}',
    ]
    with balancer.start_pginx(worker_id=worker_id) as url:
        port = urlparse.urlparse(url).port

        resp_codes = set()
        for host in hosts:
            h = {b'Host': host.format(port).encode('ascii')}
            resp = requests.get(u'{}/get'.format(url), headers=h)
            resp_codes.add(resp.status_code)
        assert resp_codes == {200, 404}

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.2.5'

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_domain_matchers', u'0_2_5.lua')) as url:
        port = urlparse.urlparse(url).port
        resp_codes = set()
        for host in hosts:
            h = {b'Host': host.format(port).encode('ascii')}
            resp = requests.get(u'{}/get'.format(url), headers=h)
            resp_codes.add(resp.status_code)
        assert resp_codes == {200}


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_secondary_cert(balancer, worker_id):
    fqdn = u'ec.yandex.net'
    primary_cert_id = u'ec.yandex.net_ec'
    secondary_cert_id = u'ec.yandex.net_rsa'

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)
        l7_macro_pb.ClearField('include_domains')
        l7_macro_cert_pb = l7_macro_pb.https.certs.add(id=primary_cert_id)

    balancer.add_cert(primary_cert_id)

    with balancer.start_pginx(worker_id=worker_id) as url:
        s, https_url = get_https_session(balancer, url, fqdn, ciphers=NON_EC_CIPHERS)
        resp = s.get(u'{}/get'.format(url))
        assert resp.status_code == 200
        with pytest.raises(SSLError):
            s.get(u'{}/ping'.format(https_url), verify=ROOT_CA_PEM)

    l7_macro_cert_pb.secondary_id = secondary_cert_id
    balancer.add_cert(secondary_cert_id, is_ecc=True)
    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_l7_macro_secondary_cert', 'without_domain.lua')) as url:
        s, https_url = get_https_session(balancer, url, fqdn, ciphers=NON_EC_CIPHERS)
        resp = s.get(u'{}/get'.format(url))
        assert resp.status_code == 200
        resp = s.get(u'{}/ping'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_secondary_cert_in_domain(balancer, worker_id):
    fqdn = u'ec.yandex.net'
    primary_cert_id = u'ec.yandex.net_ec'
    secondary_cert_id = u'ec.yandex.net_rsa'

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)

    balancer.add_cert(primary_cert_id)
    domain_spec_pb = balancer.add_domain([fqdn],
                                         cert_id=primary_cert_id,
                                         protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)

    with balancer.start_pginx(worker_id=worker_id) as url:
        s, https_url = get_https_session(balancer, url, fqdn, ciphers=NON_EC_CIPHERS)

        resp = s.get(u'{}/get'.format(url))
        assert resp.status_code == 200
        with pytest.raises(SSLError):
            s.get(u'{}/ping'.format(https_url), verify=ROOT_CA_PEM)

    domain_spec_pb.yandex_balancer.config.secondary_cert.id = secondary_cert_id
    balancer.add_cert(secondary_cert_id)
    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_l7_macro_secondary_cert', 'with_domain.lua')) as url:
        s, https_url = get_https_session(balancer, url, fqdn, ciphers=NON_EC_CIPHERS)

        resp = s.get(u'{}/get'.format(url))
        assert resp.status_code == 200
        resp = s.get(u'{}/ping'.format(https_url), verify=ROOT_CA_PEM)
        assert resp.status_code == 200


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_core_limits(balancer, worker_id):
    h = {
        b'Host': 'flat.easy-mode.yandex.net'
    }
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.3.4'
        l7_macro_pb.core.limits.req_line_max_len.value = 16 * 1024
        l7_macro_pb.core.limits.req_line_plus_headers_max_len.value = 18 * 1024

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_core_limits', u'1.lua')) as url:
        resp = requests.get(u'{}/get'.format(url), headers=h)
        assert resp.status_code == httplib.OK

        very_long_uri = u'{}/get?x={}'.format(url, u'a' * (16 * 1024))
        not_very_long_uri = u'{}/get?x={}'.format(url, u'a' * (4 * 1024))
        # hit req_line_max_len limit
        resp = requests.get(very_long_uri, headers=h)
        assert resp.status_code == httplib.REQUEST_URI_TOO_LONG

        h[b'X-Exp-Id'] = 'not s' + 'o' * 64 + ' long'
        resp = requests.get(not_very_long_uri, headers=h)
        assert resp.status_code == httplib.OK

        # hit req_line_plus_headers_max_len limit
        h[b'X-Exp-Id'] = 's' + 'o' * (16 * 1024) + ' long'
        resp = requests.get(not_very_long_uri, headers=h)
        assert resp.status_code == httplib.REQUEST_ENTITY_TOO_LARGE


@flaky(max_runs=MAX_RUNS, min_passes=1)
def test_non_idempotent_on_error(balancer, worker_id):
    h = {
        b'Host': 'by_dc.easy-mode.yandex.net'
    }
    with balancer.update_upstream_config('by_dc') as config_pb:
        l7_upstream_macro_pb = config_pb.l7_upstream_macro
        l7_upstream_macro_pb.version = u'0.0.2'
        l7_upstream_macro_pb.by_dc_scheme.balancer.retry_non_idempotent.value = False
        l7_upstream_macro_pb.by_dc_scheme.balancer.retry_http_responses.codes.append('200')
        l7_upstream_macro_pb.by_dc_scheme.on_error.static.status = 404
        l7_upstream_macro_pb.by_dc_scheme.on_error.static.content = u'Hello from on_error!'

    # old behaviour: balancer resets the connection
    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_non_idempotent_on_error', u'1.lua')) as url:
        with pytest.raises(requests.ConnectionError) as e:
            requests.post(u'{}/post'.format(url), headers=h, data='X' * 10)
        assert u'Connection reset by peer' in six.text_type(e)

    with balancer.update_upstream_config('by_dc') as config_pb:
        l7_upstream_macro_pb = config_pb.l7_upstream_macro
        l7_upstream_macro_pb.version = u'0.2.1'

    # new behaviour: failed request served by on_error section
    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_non_idempotent_on_error', u'2.lua')) as url:
        resp = requests.post(u'{}/post'.format(url), headers=h, data='X' * 10)
        assert resp.status_code == 404
        assert resp.text == u'Hello from on_error!'


def test_meta_module(balancer, worker_id):
    fqdn = 'flat.easy-mode.yandex.net'
    h = {
        b'Host': fqdn
    }
    balancer.add_domain([fqdn])
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.4.0'

    with balancer.update_upstream_config('flat') as config_pb:
        l7_upstream_macro_pb = config_pb.l7_upstream_macro
        l7_upstream_macro_pb.version = u'0.3.0'

    with balancer.start_pginx(worker_id=worker_id, path_to_expected_lua=os.path.join(u'_test_meta_module', u'1.lua')) as url:
        requests.post(u'{}/post'.format(url), headers=h, data='X' * 10)

        def check():
            with balancer.access_log() as alog:
                records = alog.readlines()
                assert 'POST /post' in records[-1]
                assert '[meta awacs-logs <::env_type:unknown::> <::namespace:easy-mode-v2.test.yandex.net::>' in records[-1]
                assert '[meta awacs-logs <::domain:flat.easy-mode.yandex.net::>' in records[-1]
                assert '[meta awacs-logs <::upstream:flat::>' in records[-1]

        wait_until(check, timeout=0.5)
