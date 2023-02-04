import mock

from awacs.wrappers.l7upstreammacro import L7UpstreamMacro, L7UpstreamMacroFlatScheme
from awtest.wrappers import get_exception_msg
from infra.awacs.proto import modules_pb2


def test_rps_limiter():
    pb = modules_pb2.L7UpstreamMacro()
    pb.version = u'0.1.0'
    pb.id = u'default'
    pb.matcher.any = True
    pb.flat_scheme.balancer.backend_timeout = u'1s'
    pb.flat_scheme.balancer.attempt_all_endpoints = True
    pb.flat_scheme.balancer.do_not_retry_http_responses = True
    pb.flat_scheme.balancer.do_not_limit_reattempts = True
    pb.flat_scheme.balancer.max_pessimized_endpoints_share = 0.5
    pb.flat_scheme.backend_ids.append(u'b1')
    pb.flat_scheme.on_error.static.status = 500
    m = L7UpstreamMacro(pb)
    with mock.patch.object(L7UpstreamMacroFlatScheme, u'validate'):
        assert get_exception_msg(m.validate) is None

    pb.rps_limiter.SetInParent()
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'rps_limiter: at least one of the "external", "local" must be specified'

    pb.rps_limiter.local.SetInParent()
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'rps_limiter -> local -> max_requests: is required'

    pb.rps_limiter.local.max_requests = -1
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'rps_limiter -> local -> interval: is required'

    pb.rps_limiter.local.interval = u'abc'
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'rps_limiter -> local -> max_requests: must be greater than 0'

    pb.rps_limiter.local.max_requests = 1
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'rps_limiter -> local -> interval: "abc" is not a valid timedelta string'

    pb.rps_limiter.local.interval = '100m'
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'rps_limiter -> local -> interval: must be less or equal to 60m'

    pb.rps_limiter.local.interval = '1s'
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) is None

    pb.rps_limiter.local.max_requests_in_queue.value = -10
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(
        m.validate) == u'rps_limiter -> local -> max_requests_in_queue: must be greater or equal to 0'

    pb.rps_limiter.local.max_requests_in_queue.value = 10000
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(
        m.validate) == u'rps_limiter -> local -> max_requests_in_queue: must be less or equal to 1000'

    pb.rps_limiter.local.max_requests_in_queue.value = 1000
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) is None

    pb.rps_limiter.ClearField('local')
    pb.rps_limiter.external.SetInParent()
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'rps_limiter -> external -> record_name: is required'

    pb.rps_limiter.external.record_name = 'test_record_name'
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) is None
