import os
import pytest

from awtest import t
from awtest.api import create_namespace
from awtest.balancer import Balancer
from awtest.mocks.ports import MAGIC_HTTPS_PORT
from awtest.network import mocked_resolve_host
from infra.awacs.proto import modules_pb2, model_pb2


FIXTURE_DIR = t(u'fixtures/easy_mode_v2/')


@pytest.fixture
def balancer(ctx, balancer_executable_path, zk_storage, cache):
    ns_id = u'easy-mode-v2.test.yandex.net'
    create_namespace(zk_storage, cache, ns_id)
    return Balancer(ctx, 'xxx', balancer_executable_path, ns_id, FIXTURE_DIR)


def test_l7_macro_compat(balancer):
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.2.2'
        l7_macro_pb.compat.disable_tcp_listen_queue_limit = True
        l7_macro_pb.compat.maxconn.value = 9999
        l7_macro_pb.compat.maxlen.value = 99999
        l7_macro_pb.compat.maxreq.value = 99999
        l7_macro_pb.core.compat.keepalive_drop_probability.value = 0.001
        l7_macro_pb.compat.tcp_congestion_control = 'bbr'

    lua = balancer.render()
    balancer.check_lua(lua, os.path.join(u'_l7_macro_compat', u'compat.lua'))


def test_l7_macro_sd(balancer):
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.4.3'

    lua = balancer.render()
    balancer.check_lua(lua, os.path.join(u'_l7_macro_sd', u'0.4.3.lua'))


def test_no_rc4_sha_cipher(balancer):
    balancer.add_cert(u'flat.easy-mode.yandex.net')
    balancer.add_domain([u'flat.easy-mode.yandex.net'],
                        cert_id=u'flat.easy-mode.yandex.net',
                        protocol=model_pb2.DomainSpec.Config.HTTPS_ONLY)
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.compat.disable_rc4_sha_cipher.value = True
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)

    lua = balancer.render()
    expected_lua = os.path.join(u'_no_rc4_sha_cipher', u'rc4_disabled.lua')
    balancer.check_lua(lua, expected_lua)
    with open(os.path.join(FIXTURE_DIR, expected_lua)) as f:
        assert u':RC4-SHA:' not in f.read()

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.compat.disable_rc4_sha_cipher.value = False

    lua = balancer.render()
    expected_lua = os.path.join(u'_no_rc4_sha_cipher', u'rc4_enabled.lua')
    balancer.check_lua(lua, expected_lua)
    with open(os.path.join(FIXTURE_DIR, expected_lua)) as f:
        assert u':RC4-SHA:' in f.read()


@pytest.mark.parametrize(u'cert_algos,tls_preset,expected_lua', [
    ((u'rsa', u'ecc'), None, u'rsa_ecc_no_preset'),
    ((u'ecc', u'rsa'), None, u'ecc_rsa_no_preset'),
    ((u'rsa', None), None, u'rsa_no_preset'),
    ((u'ecc', None), None, u'ecc_no_preset'),

    ((u'rsa', u'ecc'), modules_pb2.L7Macro.HttpsSettings.TlsSettings.DEFAULT, u'rsa_ecc_preset_default'),
    ((u'ecc', u'rsa'), modules_pb2.L7Macro.HttpsSettings.TlsSettings.DEFAULT, u'ecc_rsa_preset_default'),
    ((u'rsa', None), modules_pb2.L7Macro.HttpsSettings.TlsSettings.DEFAULT, u'rsa_preset_default'),
    ((u'ecc', None), modules_pb2.L7Macro.HttpsSettings.TlsSettings.DEFAULT, u'ecc_preset_default'),

    ((u'rsa', u'ecc'), modules_pb2.L7Macro.HttpsSettings.TlsSettings.STRONG, u'rsa_ecc_preset_strong'),
    ((u'ecc', u'rsa'), modules_pb2.L7Macro.HttpsSettings.TlsSettings.STRONG, u'ecc_rsa_preset_strong'),
    ((u'rsa', None), modules_pb2.L7Macro.HttpsSettings.TlsSettings.STRONG, u'rsa_preset_strong'),
    ((u'ecc', None), modules_pb2.L7Macro.HttpsSettings.TlsSettings.STRONG, u'ecc_preset_strong'),
])
def test_tls_presets_with_domains(balancer, cert_algos, tls_preset, expected_lua):
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)
        if tls_preset is not None:
            l7_macro_pb.https.tls_settings.preset = tls_preset

    fqdn_1 = u'flat.easy-mode.yandex.net'
    fqdn_2 = u'ec.yandex.net'
    primary_cert_id_1 = u'flat.easy-mode.yandex.net'

    if cert_algos[0] == u'rsa':
        primary_cert_id_2 = u'ec.yandex.net_rsa'
    else:
        primary_cert_id_2 = u'ec.yandex.net_ec'

    if cert_algos[1] == u'rsa':
        secondary_cert_id_2 = u'ec.yandex.net_rsa'
    elif cert_algos[1] == u'ecc':
        secondary_cert_id_2 = u'ec.yandex.net_ec'
    else:
        secondary_cert_id_2 = None

    balancer.add_cert(primary_cert_id_1)

    balancer.add_cert(primary_cert_id_2, is_ecc=cert_algos[0] == u'ecc')
    if secondary_cert_id_2 is not None:
        balancer.add_cert(secondary_cert_id_2, is_ecc=cert_algos[1] == u'ecc')

    balancer.add_domain([fqdn_1],
                        cert_id=primary_cert_id_1,
                        protocol=model_pb2.DomainSpec.Config.HTTPS_ONLY)
    balancer.add_domain([fqdn_2],
                        cert_id=primary_cert_id_2,
                        secondary_cert_id=secondary_cert_id_2,
                        protocol=model_pb2.DomainSpec.Config.HTTPS_ONLY)

    lua = balancer.render()
    balancer.check_lua(lua, os.path.join(u'_l7_macro_tls_settings', expected_lua + u'.lua'))


@pytest.mark.parametrize(u'cert_algos,tls_preset,expected_lua', [
    ((u'rsa', u'ecc'), None, u'wo_domains_rsa_ecc_no_preset'),
    ((u'ecc', u'rsa'), None, u'wo_domains_ecc_rsa_no_preset'),
    ((u'rsa', None), None, u'wo_domains_rsa_no_preset'),
    ((u'ecc', None), None, u'wo_domains_ecc_no_preset'),

    ((u'rsa', u'ecc'), modules_pb2.L7Macro.HttpsSettings.TlsSettings.DEFAULT, u'wo_domains_rsa_ecc_preset_default'),
    ((u'ecc', u'rsa'), modules_pb2.L7Macro.HttpsSettings.TlsSettings.DEFAULT, u'wo_domains_ecc_rsa_preset_default'),
    ((u'rsa', None), modules_pb2.L7Macro.HttpsSettings.TlsSettings.DEFAULT, u'wo_domains_rsa_preset_default'),
    ((u'ecc', None), modules_pb2.L7Macro.HttpsSettings.TlsSettings.DEFAULT, u'wo_domains_ecc_preset_default'),

    ((u'rsa', u'ecc'), modules_pb2.L7Macro.HttpsSettings.TlsSettings.STRONG, u'wo_domains_rsa_ecc_preset_strong'),
    ((u'ecc', u'rsa'), modules_pb2.L7Macro.HttpsSettings.TlsSettings.STRONG, u'wo_domains_ecc_rsa_preset_strong'),
    ((u'rsa', None), modules_pb2.L7Macro.HttpsSettings.TlsSettings.STRONG, u'wo_domains_rsa_preset_strong'),
    ((u'ecc', None), modules_pb2.L7Macro.HttpsSettings.TlsSettings.STRONG, u'wo_domains_ecc_preset_strong'),
])
def test_tls_settings_without_domains(balancer, cert_algos, tls_preset, expected_lua):
    if cert_algos[0] == u'rsa':
        primary_cert_id = u'ec.yandex.net_rsa'
    else:
        primary_cert_id = u'ec.yandex.net_ec'

    if cert_algos[1] == u'rsa':
        secondary_cert_id = u'ec.yandex.net_rsa'
    elif cert_algos[1] == u'ecc':
        secondary_cert_id = u'ec.yandex.net_ec'
    else:
        secondary_cert_id = None

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)
        l7_macro_pb.ClearField('include_domains')
        l7_macro_pb.https.certs.add(id=primary_cert_id, secondary_id=secondary_cert_id)
        if tls_preset is not None:
            l7_macro_pb.https.tls_settings.preset = tls_preset

    balancer.add_cert(primary_cert_id, is_ecc=cert_algos[0] == u'ecc')
    if secondary_cert_id is not None:
        balancer.add_cert(secondary_cert_id, is_ecc=cert_algos[1] == u'ecc')

    lua = balancer.render()
    balancer.check_lua(lua, os.path.join(u'_l7_macro_tls_settings', expected_lua + u'.lua'))


def test_geobase(balancer):
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.3.10'
        l7_macro_pb.http.SetInParent()
        l7_macro_pb.headers.add().laas.SetInParent()

    with balancer.update_upstream_config('flat') as config_pb:
        l7_upstream_macro_pb = config_pb.l7_upstream_macro
        l7_upstream_macro_pb.version = u'0.2.2'
        l7_upstream_macro_pb.headers.add().laas.SetInParent()

    with mocked_resolve_host({u'laas.yandex.ru': u'2a02:6b8:0:3400::1022'}):
        lua = balancer.render()
        balancer.check_lua(lua, os.path.join(u'_l7_macro_geobase', u'0.3.10.lua'))


def test_webauth(balancer):
    fqdn = u'ec.yandex.net'
    primary_cert_id = u'ec.yandex.net_ec'

    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.3.13'
        l7_macro_pb.http.redirect_to_https.SetInParent()
        l7_macro_pb.https.ports.append(MAGIC_HTTPS_PORT)

    for mode, action, expected_lua in [
        (modules_pb2.L7Macro.WebauthSettings.SIMPLE,
         modules_pb2.L7Macro.WebauthSettings.AUTHORIZE,
         u'simple'),

        (modules_pb2.L7Macro.WebauthSettings.SIMPLE,
         modules_pb2.L7Macro.WebauthSettings.AUTHENTICATE_USING_IDM,
         u'simple-idm'),

        (modules_pb2.L7Macro.WebauthSettings.EXTERNAL,
         modules_pb2.L7Macro.WebauthSettings.AUTHORIZE,
         u'external'),

        (modules_pb2.L7Macro.WebauthSettings.EXTERNAL,
         modules_pb2.L7Macro.WebauthSettings.AUTHENTICATE_USING_IDM,
         u'external-idm'),
    ]:
        with balancer.update_l7_macro() as l7_macro_pb:
            l7_macro_pb.webauth.mode = mode
            l7_macro_pb.webauth.action = action

        balancer.add_cert(primary_cert_id)
        balancer.add_domain([fqdn],
                            cert_id=primary_cert_id,
                            protocol=model_pb2.DomainSpec.Config.HTTP_AND_HTTPS)
        balancer.remove_domain(u'by_dc.easy-mode.yandex.net')
        balancer.remove_domain(u'flat.easy-mode.yandex.net')

        with mocked_resolve_host({u'webauth.yandex-team.ru': u'2a02:6b8:0:3400:0:71d:0:3a1'}):
            lua = balancer.render()
            balancer.check_lua(lua, os.path.join(u'_l7_macro_webauth', expected_lua + u'.lua'))


def test_rps_limiter(balancer):
    with balancer.update_l7_macro() as l7_macro_pb:
        l7_macro_pb.version = u'0.3.13'
        l7_macro_pb.rps_limiter.external.record_name = 'test'

    for l7_macro_version, expected_lua in [
        (u'0.3.12', u'rps_limiter_with_0.3.12'),
        (u'0.3.13', u'rps_limiter_with_0.3.13'),
        (u'0.4.0', u'rps_limiter_with_0.4.0'),
        (u'0.4.2', u'rps_limiter_with_0.4.2'),
    ]:
        with balancer.update_l7_macro() as l7_macro_pb:
            l7_macro_pb.version = l7_macro_version

        balancer.add_rps_limiter_backends()

        lua = balancer.render()
        balancer.check_lua(lua, os.path.join(u'_l7_macro_rps_limiter', expected_lua + u'.lua'))
