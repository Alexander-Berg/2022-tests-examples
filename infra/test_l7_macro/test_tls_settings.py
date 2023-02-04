import pytest
import six

from awacs.wrappers.base import wrap, ValidationCtx
from awacs.wrappers.errors import ValidationError
from infra.awacs.proto import modules_pb2, model_pb2
import awtest
from awtest.wrappers import parse_lua_into_pb
from awtest import t


@pytest.mark.parametrize(u'preset,lua_file', (
    (modules_pb2.L7Macro.HttpsSettings.TlsSettings.STRONG, u'tls_settings_strong'),
    (modules_pb2.L7Macro.HttpsSettings.TlsSettings.INTERMEDIATE, u'tls_settings_intermediate')))
def test_tls_settings_strong(ctx, preset, lua_file):
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.version = u'0.3.6'
    pb.https.tls_settings.preset = preset
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    pb.include_domains.SetInParent()

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend([u'https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTPS_ONLY
    domain_config_pb.cert.id = u'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL
    domain_config_pbs = {(u'n', u'domain1'): domain_config_pb}

    certs = {'cert1': model_pb2.CertificateSpec()}

    ctx.log.info(u'Step 1: validate deprecated settings')
    pb.https.compat.disable_rc4_sha_cipher.value = True
    pb.https.compat.enable_sslv3 = True
    pb.https.enable_tlsv1_3 = True

    m = wrap(holder_pb)
    with awtest.raises(ValidationError, text=u'l7_macro -> https -> compat: "disable_rc4_sha_cipher" is deprecated '
                                             u'when strong TLS settings preset is enabled'):
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    pb.https.compat.ClearField('disable_rc4_sha_cipher')
    m = wrap(holder_pb)
    with awtest.raises(ValidationError, text=u'l7_macro -> https -> compat: "enable_sslv3" cannot be used '
                                             u'when strong TLS settings preset is enabled'):
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    pb.https.compat.enable_sslv3 = False
    m = wrap(holder_pb)
    with awtest.raises(ValidationError, text=u'l7_macro -> https -> enable_tlsv1_3: is deprecated when '
                                             u'strong TLS settings preset is enabled'):
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    pb.https.enable_tlsv1_3 = False
    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    ctx.log.info(u'Step 2: check expanded config')
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs,
                                                         certs={'cert1': model_pb2.CertificateSpec()}))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/{}.pb.txt'.format(lua_file))) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert holder_pb == expected_holder_pb
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)


def test_tls_settings_default(ctx):
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.version = u'0.3.6'
    pb.https.tls_settings.preset = modules_pb2.L7Macro.HttpsSettings.TlsSettings.DEFAULT
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    pb.include_domains.SetInParent()

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend([u'https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTPS_ONLY
    domain_config_pb.cert.id = u'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL
    domain_config_pbs = {(u'n', u'domain1'): domain_config_pb}

    certs = {'cert1': model_pb2.CertificateSpec()}

    ctx.log.info(u'Step 1: validate settings deprecated in STRONG preset')
    pb.https.compat.disable_rc4_sha_cipher.value = True
    pb.https.compat.enable_sslv3 = True
    pb.https.enable_tlsv1_3 = True

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    ctx.log.info(u'Step 2: check expanded config')
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs,
                                                         certs={'cert1': model_pb2.CertificateSpec()}))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/tls_settings_default.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb
