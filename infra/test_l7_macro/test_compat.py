import six

from awacs.wrappers.base import wrap, ValidationCtx
from awtest.wrappers import get_wrapped_validation_exception_msg, get_exception_msg, parse_lua_into_pb
from infra.awacs.proto import modules_pb2, model_pb2
from awtest import t


def test_l7_macro_http_validate():
    pb = modules_pb2.L7Macro.HttpSettings()
    pb.compat.bind_on_instance_port = False
    pb.compat.use_instance_port_in_section_log_name = True
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'compat: use_instance_port_in_section_log_name can not be set without bind_on_instance_port'

    pb.compat.bind_on_instance_port = True
    e = get_wrapped_validation_exception_msg(pb)
    assert not e


def test_keepalive_compat():
    pb = modules_pb2.L7Macro()
    pb.version = '0.0.1'
    pb.core.compat.keepalive_drop_probability.value = 0.5
    pb.http.SetInParent()
    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.core.compat.keepalive_drop_probability.value = 0
    pb.http.SetInParent()
    e = get_wrapped_validation_exception_msg(pb)
    assert not e


def test_l7_macro_disable_rc4_sha():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.https.SetInParent()
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    pb.include_domains.SetInParent()
    pb.https.compat.disable_rc4_sha_cipher.value = True
    pb.version = '0.2.5'

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend(['https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTPS_ONLY
    domain_config_pb.cert.id = 'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL

    domain_config_pbs = {('n', 'domain1'): domain_config_pb, }
    certs = {'cert1': model_pb2.CertificateSpec()}

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/rc4_sha_disable.pb.txt')) as f:
        holder_pb_text = f.read()
    parse_lua_into_pb(holder_pb_text, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    pb.https.compat.disable_rc4_sha_cipher.value = False
    pb.include_domains.SetInParent()
    pb.version = '0.2.5'

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/rc4_sha_enable.pb.txt')) as f:
        holder_pb_text = f.read()
    parse_lua_into_pb(holder_pb_text, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb


def test_l7_macro_http_and_https_compat():
    for pb in [modules_pb2.L7Macro.HttpSettings.Compat(), modules_pb2.L7Macro.HttpsSettings.Compat()]:
        pb.assign_shared_uuid = '&'
        m = wrap(pb)
        e = get_exception_msg(m.validate)
        assert e == u'assign_shared_uuid: must match ^[a-z0-9-_]+$'

        pb.assign_shared_uuid = u'upstreams'
        e = get_exception_msg(m.validate)
        assert not e
