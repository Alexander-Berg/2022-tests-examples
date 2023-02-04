import pytest
import six

from awacs.wrappers.base import wrap, ValidationCtx
from awtest.wrappers import parse_lua_into_pb
from infra.awacs.proto import modules_pb2, model_pb2
from awtest import t


def test_l7_macro_uaas():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.include_domains.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.version = '0.2.0'
    pb.headers.add().uaas.service_name = 'test_uaas'
    h_pb = pb.headers.add().create
    h_pb.target = 'h1'
    h_pb.value = 'v1'

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend(['http_and_https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_pb.cert.id = 'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL
    domain_config_pb.redirect_to_https.permanent = True

    domain_config_pbs = {('n', 'domain1'): domain_config_pb}
    cert_spec_pbs = {'cert1': model_pb2.CertificateSpec()}

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=cert_spec_pbs))

    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=cert_spec_pbs))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/uaas_domains_0_2_0.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.include_domains.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.version = '0.2.1'
    pb.headers.add().uaas.service_name = 'test_uaas'
    h_pb = pb.headers.add().create
    h_pb.target = 'h1'
    h_pb.value = 'v1'

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=cert_spec_pbs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=cert_spec_pbs))
    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/uaas_domains_0_2_1.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.version = '0.2.0'
    pb.headers.add().uaas.service_name = 'test_uaas'
    h_pb = pb.headers.add().create
    h_pb.target = 'h1'
    h_pb.value = 'v1'

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx())
    m.expand_immediate_contained_macro(ctx=ValidationCtx())

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/uaas_upstreams_0_2_0.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.version = '0.2.1'
    pb.headers.add().uaas.service_name = 'test_uaas'
    h_pb = pb.headers.add().create
    h_pb.target = 'h1'
    h_pb.value = 'v1'

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx())
    m.expand_immediate_contained_macro(ctx=ValidationCtx())
    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/uaas_upstreams_0_2_1.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb


@pytest.mark.parametrize('l7_macro_version', ['0.2.12', '0.3.0', '0.3.1'])
def test_l7_macro_uaas_version(l7_macro_version):
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.version = l7_macro_version
    pb.headers.add().uaas.service_name = 'test_uaas'

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx())
    m.expand_immediate_contained_macro(ctx=ValidationCtx())
    expected_holder_pb = modules_pb2.Holder()

    filename = t('test_wrappers/test_l7_macro/fixtures/uaas_version_{}.pb.txt').format(
        l7_macro_version.replace('.', '_'))
    with open(filename) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)

    if six.text_type(holder_pb) != six.text_type(expected_holder_pb):
        with open(filename + '.new', 'w') as f:
            f.write(six.text_type(holder_pb))
        raise AssertionError()
    assert holder_pb == expected_holder_pb
