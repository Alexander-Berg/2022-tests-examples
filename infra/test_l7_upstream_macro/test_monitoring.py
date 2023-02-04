from awacs.wrappers.l7upstreammacro import L7UpstreamMacroMonitoringSettings
from awtest.wrappers import get_exception_msg, FixtureStorage
from infra.awacs.proto import modules_pb2


fixture_storage = FixtureStorage(u'test_l7_upstream_macro')


def test_monitoring_settings():
    pb = modules_pb2.L7UpstreamMacro()
    m = L7UpstreamMacroMonitoringSettings(pb.monitoring)

    pb.monitoring.uuid = u'a.a.a'
    assert get_exception_msg(m.validate) == u'uuid: must match ^[a-z0-9_-]+$'

    pb.monitoring.uuid = u'ok'
    pb.monitoring.ranges = u'abcd'
    assert get_exception_msg(m.validate) == u'ranges: "abcd" is not a valid timedeltas string'


def test_response_codes():
    pb = modules_pb2.L7UpstreamMacro()
    m = L7UpstreamMacroMonitoringSettings(pb.monitoring)

    pb.monitoring.uuid = u'ok'
    pb.monitoring.ranges = u'40s'
    pb.monitoring.response_codes.extend([u'101', u'404', u'aaa'])
    assert get_exception_msg(m.validate) == u'response_codes: unknown status code: aaa'

    del pb.monitoring.response_codes[:]
    pb.monitoring.response_codes.extend([u'101', u'404'])
    m.validate()

    holder_pb = modules_pb2.Holder()
    upstream_macro_pb = holder_pb.l7_upstream_macro
    upstream_macro_pb.version = u'0.2.0'
    b_pb = upstream_macro_pb.flat_scheme
    b_pb.balancer.attempts = 1
    b_pb.balancer.max_reattempts_share = 1
    b_pb.balancer.max_pessimized_endpoints_share = 1
    b_pb.balancer.do_not_retry_http_responses = True
    b_pb.on_error.static.status = 404
    b_pb.on_error.static.content = u'bad'

    upstream_macro_pb.monitoring.CopyFrom(pb.monitoring)

    fixture_storage.assert_pb_is_equal_to_file(holder_pb, u'monitoring.pb.txt')
