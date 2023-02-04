from itertools import product
import pytest
import six

from awacs.wrappers.base import wrap, ValidationCtx
from awtest.wrappers import get_exception_msg, get_wrapped_validation_exception_msg, parse_lua_into_pb
from infra.awacs.proto import modules_pb2, model_pb2
from awtest import t


@pytest.mark.parametrize('core_trusts_x_forwarded_for_y, core_trusts_icookie, core_trusts_x_yandex_ja_x',
                         # make all combinations of length=3 from [False, True]
                         product([False, True], repeat=3)
                         )
@pytest.mark.parametrize('l7_macro_version, antirobot_version, sync_with_core', [
    ('0.3.14', '0.0.7', {"trust_x_forwarded_for_y": True, "trust_icookie": True, "trust_x_yandex_ja_x": True}),
    ('0.3.3', '0.0.6', {"trust_x_forwarded_for_y": True, "trust_icookie": True, "trust_x_yandex_ja_x": True}),
    ('0.3.2', '0.0.5', {"trust_x_forwarded_for_y": True, "trust_icookie": True, "trust_x_yandex_ja_x": False}),
    ('0.3.0', '0.0.4', {"trust_x_forwarded_for_y": True, "trust_icookie": False, "trust_x_yandex_ja_x": False}),
    ('0.2.4', '0.0.3', {"trust_x_forwarded_for_y": False, "trust_icookie": False, "trust_x_yandex_ja_x": False}),
    ('0.2.0', '0.0.2', {"trust_x_forwarded_for_y": False, "trust_icookie": False, "trust_x_yandex_ja_x": False}),
])
def test_antirobot_version_with_l7_macro_versions(l7_macro_version, core_trusts_x_forwarded_for_y,
                                                  core_trusts_icookie,
                                                  core_trusts_x_yandex_ja_x,
                                                  antirobot_version, sync_with_core):
    l7_macro_pb = modules_pb2.L7Macro()
    l7_macro_pb.version = l7_macro_version
    l7_macro_pb.http.SetInParent()
    l7_macro_pb.antirobot.SetInParent()
    l7_macro_pb.core.trust_x_forwarded_for_y = core_trusts_x_forwarded_for_y
    l7_macro_pb.core.trust_icookie = core_trusts_icookie
    l7_macro_pb.core.trust_x_yandex_ja_x = core_trusts_x_yandex_ja_x
    l7_macro = wrap(l7_macro_pb)
    expanded_holder_pbs = l7_macro.expand()
    http_section_pb = expanded_holder_pbs[0].instance_macro.sections[1].value
    module_holder_pbs = http_section_pb.nested.extended_http_macro.nested.regexp.sections[0].value.nested.modules
    antirobot_macro_pb = module_holder_pbs[0].antirobot_macro
    assert antirobot_version == antirobot_macro_pb.version
    for prop, does_must_sync in six.iteritems(sync_with_core):
        if does_must_sync:
            assert getattr(l7_macro_pb.core, prop) == getattr(antirobot_macro_pb, prop)


def test_antirobot():
    # check required fields
    pb = modules_pb2.L7Macro()
    pb.http.SetInParent()
    pb.antirobot.SetInParent()
    pb.version = u'0.2.0'
    m = wrap(pb)

    e = get_exception_msg(m.validate)
    assert e == u'announce_check_reply: must be set to use antirobot'

    pb.announce_check_reply.url_re = u'/ping'
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'health_check_reply: must be set to use antirobot'

    pb.health_check_reply.SetInParent()
    m = wrap(pb)
    m.validate()

    # with domains
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    pb.headers.add().uaas.service_name = u'test_uaas'
    pb.antirobot.SetInParent()
    pb.include_domains.SetInParent()
    pb.version = u'0.2.0'

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend([u'http_and_https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_pb.cert.id = u'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL

    domain_config_pbs = {(u'n', u'domain1'): domain_config_pb}
    certs = {u'cert1': model_pb2.CertificateSpec()}

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/antirobot_domains.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    # with upstreams
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    pb.headers.add().uaas.service_name = u'test_uaas'
    pb.antirobot.SetInParent()
    pb.version = u'0.2.0'

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx())
    m.expand_immediate_contained_macro()

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/antirobot_upstreams.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb


def test_antirobot_with_icookie():
    pb = modules_pb2.L7Macro()
    pb.http.SetInParent()
    pb.antirobot.SetInParent()
    pb.version = u'0.3.4'
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    m = wrap(pb)
    m.validate()

    pb.http.SetInParent()
    pb.headers.add().decrypt_icookie.SetInParent()

    assert get_wrapped_validation_exception_msg(pb) == 'headers[0] -> decrypt_icookie: cannot be used with antirobot'
    pb.version = u'0.3.3'
    m = wrap(pb)
    m.validate()
