import pytest
import six

from awacs.wrappers.base import wrap, ValidationCtx
from awacs.wrappers.errors import ValidationError
from awtest.wrappers import get_exception_msg, parse_lua_into_pb
from infra.awacs.proto import modules_pb2, model_pb2
from awtest import t


def test_l7_macro_domains():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.version = '0.0.1'
    pb.include_domains.SetInParent()

    pb.http.ports.append(4444)
    pb.compat.maxconn.value = 9999

    pb.https.ports.append(5555)
    pb.https.compat.enable_sslv3 = True
    pb.https.certs.add(id='xxx')

    m = wrap(holder_pb)

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend(['http_and_https_1', 'http_and_https_2'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_pb.cert.id = 'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL

    domain_spec_pb_2 = model_pb2.DomainSpec()
    domain_config_pb_2 = domain_spec_pb_2.yandex_balancer.config
    domain_config_pb_2.fqdns.extend(['http_only_1', 'http_only_2'])
    domain_config_pb_2.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY
    domain_config_pb_2.include_upstreams.type = modules_pb2.BY_ID
    domain_config_pb_2.include_upstreams.filter.id_prefix = 'prefix'

    domain_spec_pb_3 = model_pb2.DomainSpec()
    domain_config_pb_3 = domain_spec_pb_3.yandex_balancer.config
    domain_config_pb_3.fqdns.extend(['https_only_1'])
    domain_config_pb_3.protocol = model_pb2.DomainSpec.Config.HTTPS_ONLY
    domain_config_pb_3.include_upstreams.type = modules_pb2.ALL
    domain_config_pb_3.cert.id = 'cert2'
    domain_config_pb_3.secondary_cert.id = 'cert3'

    wildcard_domain_spec_pb = model_pb2.DomainSpec()
    wildcard_domain_config_pb = wildcard_domain_spec_pb.yandex_balancer.config
    wildcard_domain_config_pb.type = model_pb2.DomainSpec.Config.WILDCARD
    wildcard_domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY
    wildcard_domain_config_pb.include_upstreams.type = modules_pb2.ALL

    domain_config_pbs = {('n', 'domain2'): domain_config_pb_2}

    e = get_exception_msg(m.validate)
    assert e == u'l7_macro -> include_domains: no domains found'

    e = get_exception_msg(m.validate, ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))
    assert e == u'l7_macro -> announce_check_reply: must be set to use include_domains'

    pb.announce_check_reply.url_re = '/ping'
    m = wrap(holder_pb)
    e = get_exception_msg(m.validate, ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))
    assert e == u'l7_macro -> health_check_reply: must be set to use include_domains'

    pb.health_check_reply.SetInParent()
    m = wrap(holder_pb)
    with pytest.raises(ValidationError,
                       match='(l7_macro -> https -> certs: cannot be used with "include_domains")'):
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))

    del pb.https.certs[:]
    with pytest.raises(ValidationError,
                       match="l7_macro -> https: no HTTPS domains are found. Either remove \"https: {}\" section from "
                             "balancer config, or change domain protocol to include HTTPS"):
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))

    domain_config_pbs = {('n', 'domain3'): domain_config_pb_3}
    with pytest.raises(ValidationError,
                       match="l7_macro -> http: no HTTP domains are found. Either remove \"http: {}\" section from "
                             "balancer config, or change domain protocol to include HTTP."):
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))

    domain_config_pbs = {
        ('n', 'domain'): domain_config_pb,
        ('n', 'wildcard_domain'): wildcard_domain_config_pb
    }
    with pytest.raises(ValidationError) as e:
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs={'cert1': model_pb2.CertificateSpec()}))
    assert e.value.message == ('Wildcard domain is HTTP-only. Either remove "https: {}" section from balancer config, '
                               'or change domain protocol to include HTTPS.')

    wildcard_domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTPS_ONLY
    wildcard_domain_config_pb.cert.id = 'cert2'

    domain_config_pbs = {
        ('n', 'domain2'): domain_config_pb_2,
        ('n', 'wildcard_domain'): wildcard_domain_config_pb
    }
    with pytest.raises(ValidationError) as e:
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))
    assert e.value.message == ('Wildcard domain is HTTPS-only. Either remove "http: {}" section from balancer config, '
                               'or change domain protocol to include HTTP.')

    domain_config_pbs = {('n', 'domain1'): domain_config_pb,
                         ('n', 'domain2'): domain_config_pb_2,
                         ('n', 'domain3'): domain_config_pb_3}
    with pytest.raises(ValidationError,
                       match='l7_macro -> https: cert "cert1" not found for domain "domain1"'):
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))

    with pytest.raises(ValidationError,
                       match='l7_macro -> https: cert "cert2" not found for domain "domain3"'):
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs={'cert1': model_pb2.CertificateSpec()}))

    with pytest.raises(ValidationError,
                       match='l7_macro -> https: secondary cert "cert3" not found for domain "domain3"'):
        m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs={'cert1': model_pb2.CertificateSpec(),
                                                                                 'cert2': model_pb2.CertificateSpec()}))

    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs={'cert1': model_pb2.CertificateSpec(),
                                                                             'cert2': model_pb2.CertificateSpec(),
                                                                             'cert3': model_pb2.CertificateSpec()}))

    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs,
                                                         certs={'cert1': model_pb2.CertificateSpec(),
                                                                'cert2': model_pb2.CertificateSpec(),
                                                                'cert3': model_pb2.CertificateSpec()}))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/domains.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb


def test_l7_macro_domains_with_allow_ports():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.include_domains.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.version = '0.0.5'

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend(['http.and.https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_pb.cert.id = 'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL
    domain_config_pbs = {('n', 'domain1'): domain_config_pb}

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs={'cert1': model_pb2.CertificateSpec()}))

    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs,
                                                         certs={'cert1': model_pb2.CertificateSpec()}))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/domains_with_allow_ports_1.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.ports.extend([3333, 4444])
    pb.https.ports.append(5555)
    pb.include_domains.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.version = '0.0.5'
    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs={'cert1': model_pb2.CertificateSpec()}))

    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs,
                                                         certs={'cert1': model_pb2.CertificateSpec()}))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/domains_with_allow_ports_2.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb


def test_l7_macro_tld_domain():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.include_domains.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.version = '0.1.0'

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.type = model_pb2.DomainSpec.Config.YANDEX_TLD
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY
    domain_config_pb.include_upstreams.type = modules_pb2.ALL

    domain_spec_2_pb = model_pb2.DomainSpec()
    domain_config_2_pb = domain_spec_2_pb.yandex_balancer.config
    domain_config_2_pb.type = model_pb2.DomainSpec.Config.COMMON
    domain_config_2_pb.fqdns.append('common.domain')
    domain_config_2_pb.protocol = model_pb2.DomainSpec.Config.HTTP_ONLY
    domain_config_2_pb.include_upstreams.type = modules_pb2.ALL

    domain_config_pbs = {('n', 'domain1'): domain_config_pb, ('n', 'domain2'): domain_config_2_pb}

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))
    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/tld_domain.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.include_domains.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.version = '0.2.2'
    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs))
    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/tld_domain_0_2_2.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb


def test_l7_macro_domains_with_redirect_to_https():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.include_domains.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.version = '0.0.5'

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend(['http_and_https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_pb.cert.id = 'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL
    domain_config_pb.redirect_to_https.permanent = True

    domain_spec_pb_2 = model_pb2.DomainSpec()
    domain_config_pb_2 = domain_spec_pb_2.yandex_balancer.config
    domain_config_pb_2.fqdns.extend(['http_and_https_2'])
    domain_config_pb_2.protocol = model_pb2.DomainSpec.Config.HTTPS_ONLY
    domain_config_pb_2.cert.id = 'cert1'
    domain_config_pb_2.include_upstreams.type = modules_pb2.ALL
    domain_config_pb_2.redirect_to_https.permanent = False

    domain_spec_pb_3 = model_pb2.DomainSpec()
    domain_config_pb_3 = domain_spec_pb_3.yandex_balancer.config
    domain_config_pb_3.fqdns.extend(['http_and_https_3'])
    domain_config_pb_3.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_pb_3.cert.id = 'cert1'
    domain_config_pb_3.include_upstreams.type = modules_pb2.ALL

    domain_config_pbs = {('n', 'domain1'): domain_config_pb,
                         ('n', 'domain2'): domain_config_pb_2,
                         ('n', 'domain3'): domain_config_pb_3,
                         }

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs={'cert1': model_pb2.CertificateSpec()}))

    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs,
                                                         certs={'cert1': model_pb2.CertificateSpec()}))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/domains_with_redirect_to_https.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb


def test_domain_not_found():
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.include_domains.SetInParent()
    pb.version = '0.2.0'

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend(['http_and_https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_pb.cert.id = 'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL

    domain_config_pbs = {('n', 'domain1'): domain_config_pb, }
    certs = {'cert1': model_pb2.CertificateSpec()}

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/domain_not_found_rst.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    pb.include_domains.SetInParent()
    pb.version = '0.2.2'

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/domain_not_found_404.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb
