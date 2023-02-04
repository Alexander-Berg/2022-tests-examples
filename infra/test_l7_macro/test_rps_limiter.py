import pytest
import six

from awacs.wrappers.base import wrap, ValidationCtx
from awacs.wrappers.errors import ValidationError
from infra.awacs.proto import modules_pb2, model_pb2
from awtest import t
from awtest.wrappers import get_exception_msg, parse_lua_into_pb


def test_rps_limiter():
    # check required fields
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.rps_limiter.SetInParent()
    pb.version = u'0.2.0'
    m = wrap(holder_pb)
    e = get_exception_msg(m.validate, ctx=ValidationCtx())
    assert e == u'l7_macro -> announce_check_reply: must be set to use rps_limiter'

    pb.announce_check_reply.url_re = u'/ping'
    m = wrap(holder_pb)
    e = get_exception_msg(m.validate, ctx=ValidationCtx())
    assert e == u'l7_macro -> health_check_reply: must be set to use rps_limiter'

    pb.health_check_reply.SetInParent()
    m = wrap(holder_pb)
    e = get_exception_msg(m.validate, ctx=ValidationCtx())
    assert e == u'l7_macro -> rps_limiter: at least one of the "external", "local" must be specified'

    pb.rps_limiter.local.SetInParent()
    m = wrap(holder_pb)
    e = get_exception_msg(m.validate, ctx=ValidationCtx())
    assert e == u'l7_macro -> rps_limiter -> local -> max_requests: is required'

    pb.rps_limiter.local.max_requests = -1
    m = wrap(holder_pb)
    e = get_exception_msg(m.validate, ctx=ValidationCtx())
    assert e == u'l7_macro -> rps_limiter -> local -> interval: is required'

    pb.rps_limiter.local.interval = u'abc'
    m = wrap(holder_pb)
    with pytest.raises(ValidationError, match=u"l7_macro -> rps_limiter -> local -> max_requests: must be greater "
                                              u"than 0"):
        m.validate(ctx=ValidationCtx())
    pb.rps_limiter.local.max_requests = 1
    m = wrap(holder_pb)
    with pytest.raises(ValidationError, match=u'l7_macro -> rps_limiter -> local -> interval: "abc" is not a valid '
                                              u"timedelta string"):
        m.validate(ctx=ValidationCtx())
    pb.rps_limiter.local.interval = u'100m'
    m = wrap(holder_pb)
    with pytest.raises(ValidationError, match=u"l7_macro -> rps_limiter -> local -> interval: "
                                              u"must be less or equal to 60m"):
        m.validate(ctx=ValidationCtx())
    pb.rps_limiter.local.interval = u'1s'
    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx())

    pb.rps_limiter.local.max_requests_in_queue.value = -10
    m = wrap(holder_pb)
    with pytest.raises(ValidationError, match=u"l7_macro -> rps_limiter -> local -> max_requests_in_queue: must be "
                                              u"greater or equal to 0"):
        m.validate(ctx=ValidationCtx())
    pb.rps_limiter.local.max_requests_in_queue.value = 10000
    m = wrap(holder_pb)
    with pytest.raises(ValidationError, match=u"l7_macro -> rps_limiter -> local -> max_requests_in_queue: must be "
                                              u"less or equal to 1000"):
        m.validate(ctx=ValidationCtx())
    pb.rps_limiter.local.max_requests_in_queue.value = 1000
    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx())

    pb.rps_limiter.ClearField('local')
    pb.rps_limiter.external.SetInParent()
    m = wrap(holder_pb)
    with pytest.raises(ValidationError, match=u"l7_macro -> rps_limiter -> external -> record_name: is required"):
        m.validate(ctx=ValidationCtx())
    pb.rps_limiter.external.record_name = u'test_record_name'
    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx())

    # local without queue
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    pb.rps_limiter.local.max_requests = 1000
    pb.rps_limiter.local.interval = u'1s'
    pb.include_domains.SetInParent()
    pb.version = u'0.2.0'

    domain_spec_pb = model_pb2.DomainSpec()
    domain_config_pb = domain_spec_pb.yandex_balancer.config
    domain_config_pb.fqdns.extend([u'http_and_https'])
    domain_config_pb.protocol = model_pb2.DomainSpec.Config.HTTP_AND_HTTPS
    domain_config_pb.cert.id = u'cert1'
    domain_config_pb.include_upstreams.type = modules_pb2.ALL

    domain_config_pbs = {(u'n', u'domain1'): domain_config_pb, }
    certs = {u'cert1': model_pb2.CertificateSpec()}

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    expected_holder_pb = modules_pb2.Holder()
    with open(t('test_wrappers/test_l7_macro/fixtures/rps_limiter_local_no_queue.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    # local with queue
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    pb.rps_limiter.local.max_requests = 1000
    pb.rps_limiter.local.interval = u'1s'
    pb.rps_limiter.local.max_requests_in_queue.value = 10
    pb.include_domains.SetInParent()
    pb.version = u'0.2.0'

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    expected_holder_pb = modules_pb2.Holder()
    with open(t(u'test_wrappers/test_l7_macro/fixtures/rps_limiter_local.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb

    # external
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_macro
    pb.http.SetInParent()
    pb.https.SetInParent()
    pb.announce_check_reply.url_re = u'/ping'
    pb.health_check_reply.SetInParent()
    pb.rps_limiter.external.record_name = u'test_record_name'
    pb.include_domains.SetInParent()
    pb.version = u'0.2.0'

    m = wrap(holder_pb)
    m.validate(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))
    m.expand_immediate_contained_macro(ctx=ValidationCtx(domain_config_pbs=domain_config_pbs, certs=certs))

    expected_holder_pb = modules_pb2.Holder()
    with open(t(u'test_wrappers/test_l7_macro/fixtures/rps_limiter_external.pb.txt')) as f:
        lua = f.read()
    parse_lua_into_pb(lua, expected_holder_pb)
    assert six.text_type(holder_pb) == six.text_type(expected_holder_pb)
    assert holder_pb == expected_holder_pb
