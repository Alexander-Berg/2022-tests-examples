from awacs.wrappers.l7upstreammacro import L7UpstreamMacroBalancerSettings, L7UpstreamMacroDcBalancerSettings
from awtest.wrappers import get_exception_msg
from infra.awacs.proto import modules_pb2


def test_balancer_settings():
    pb = modules_pb2.L7UpstreamMacro.BalancerSettings()
    m = L7UpstreamMacroBalancerSettings(pb)

    assert get_exception_msg(m.validate) == u'backend_timeout: is required'
    pb.backend_timeout = u'1s'

    assert get_exception_msg(m.validate) == u'at least one of the "attempt_all_endpoints", "attempts" must be specified'
    pb.attempts = 3

    assert get_exception_msg(m.validate) == (u'at least one of the "do_not_retry_http_responses", '
                                             u'"retry_http_responses" must be specified')

    pb.do_not_retry_http_responses = True
    assert get_exception_msg(m.validate) == u'either "max_reattempts_share" or "do_not_limit_reattempts" must be set'

    pb.max_reattempts_share = 0.3
    assert get_exception_msg(m.validate) == u'max_pessimized_endpoints_share: is required'

    pb.max_reattempts_share = 0
    pb.do_not_limit_reattempts = True
    assert get_exception_msg(m.validate) == u'max_pessimized_endpoints_share: is required'

    pb.max_pessimized_endpoints_share = 0.3
    assert get_exception_msg(m.validate) is None

    pb.compat.method = pb.compat.RR
    m = L7UpstreamMacroBalancerSettings(pb)
    assert get_exception_msg(
        m.validate) == u'max_pessimized_endpoints_share: can not be used with "compat.method" set to RR'

    pb.compat.method = pb.compat.WEIGHTED2
    assert get_exception_msg(
        m.validate) == u'max_pessimized_endpoints_share: can not be used with "compat.method" set to WEIGHTED2'

    pb.max_pessimized_endpoints_share = 0
    assert get_exception_msg(m.validate) is None

    pb.health_check.delay = u'10s'
    pb.health_check.request = r'GET /ping HTTP/1.1\nHost: romanovich-test-31337.yandex.net\n\n'
    m = L7UpstreamMacroBalancerSettings(pb)
    assert get_exception_msg(m.validate) == u'health_check: can not be used with "compat.method" set to WEIGHTED2'

    pb.compat.method = pb.compat.RR
    assert get_exception_msg(m.validate) == u'health_check: can not be used with "compat.method" set to RR'

    pb.compat.method = pb.compat.ACTIVE
    pb.health_check.compat.not_steady = True
    m = L7UpstreamMacroBalancerSettings(pb)
    assert get_exception_msg(m.validate) is None

    pb.ClearField('compat')
    pb.max_pessimized_endpoints_share = 0.3
    m = L7UpstreamMacroBalancerSettings(pb)
    assert get_exception_msg(
        m.validate) == u'health_check -> compat -> not_steady: can only be set if "compat.method" set to ACTIVE'

    pb.health_check.ClearField('compat')
    pb.retry_http_responses.codes.append(u'5xx')
    pb.retry_http_responses.on_last_failed_retry = pb.retry_http_responses.PROXY_RESPONSE_AS_IS
    m = L7UpstreamMacroBalancerSettings(pb)
    assert get_exception_msg(m.validate) is None

    pb.buffering = True
    m = L7UpstreamMacroBalancerSettings(pb)
    assert get_exception_msg(m.validate) is None

    pb.use_https_to_endpoints.SetInParent()
    m = L7UpstreamMacroBalancerSettings(pb)
    assert get_exception_msg(m.validate) is None

    pb.backend_timeout = u'1bububu'
    assert get_exception_msg(m.validate) == u'backend_timeout: "1bububu" is not a valid timedelta string'

    pb.backend_timeout = u'0.5m'
    pb.connect_timeout = u'1bububu'
    assert get_exception_msg(m.validate) == u'connect_timeout: "1bububu" is not a valid timedelta string'

    pb.connect_timeout = u'0.1s'
    assert get_exception_msg(m.validate) is None


def test_dc_balancer_settings():
    pb = modules_pb2.L7UpstreamMacro.DcBalancerSettings()
    m = L7UpstreamMacroDcBalancerSettings(pb)

    assert get_exception_msg(m.validate, dcs_count=1) == u'method: is required'

    pb.method = pb.BY_DC_WEIGHT
    assert get_exception_msg(m.validate,
                             dcs_count=1) == u'at least one of the "attempt_all_dcs", "attempts" must be specified'

    pb.attempts = 5
    assert get_exception_msg(m.validate, dcs_count=1) == u'attempts: exceeds the number of configured dcs (1)'

    pb.attempts = 1
    assert get_exception_msg(m.validate, dcs_count=1) == u'weights_section_id: is required'

    pb.weights_section_id = u'bygeo'
    assert get_exception_msg(m.validate, dcs_count=1) is None

    pb.attempt_all_dcs = True
    assert get_exception_msg(m.validate, dcs_count=1) is None

    pb.weights_section_id = u''
    pb.compat.disable_dynamic_weights = True
    m = L7UpstreamMacroDcBalancerSettings(pb)
    assert get_exception_msg(m.validate, dcs_count=1) is None
