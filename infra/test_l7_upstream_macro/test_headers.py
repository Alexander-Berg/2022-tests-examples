import mock

from awacs.wrappers.l7upstreammacro import L7UpstreamMacro, L7UpstreamMacroFlatScheme, L7UpstreamMacroMatcherHeader
from awtest.wrappers import get_exception_msg
from infra.awacs.proto import modules_pb2


def test_header_actions():
    pb = modules_pb2.L7UpstreamMacro()
    pb.version = u'0.0.1'
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

    h = pb.headers.add().create
    h.target = u'---'
    h.value = u'any'
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'headers[0] -> create -> target: invalid header name: "---"'

    h.target = u'header'
    h.ClearField('value')
    h.func = u'wrong'
    assert get_exception_msg(m.validate) == u'headers[0] -> create: invalid func "wrong" for header "header". ' \
                                            u'Maybe you meant "value" instead of "func"'
    h.ClearField('func')
    h.value = u'wrong'

    h = pb.headers.add().append
    h.target = u'---'
    h.value = u'any'
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'headers[1] -> append -> target: invalid header name: "---"'

    h.target = u'header'
    h.ClearField('value')
    h.func = u'wrong'
    assert get_exception_msg(m.validate) == u'headers[1] -> append: invalid func "wrong" for header "header". ' \
                                            u'Maybe you meant "value" instead of "func"'

    del pb.headers[:]
    h = pb.headers.add().uaas
    h.SetInParent()
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'headers[0] -> uaas -> service_name: is required'

    h.service_name = u'uaas_svc'
    m.validate()

    del pb.headers[:]
    h = pb.headers.add().laas
    h.SetInParent()
    m = L7UpstreamMacro(pb)
    m.validate()


def test_matcher_header():
    pb = modules_pb2.L7UpstreamMacro.Matcher.Header()
    m = L7UpstreamMacroMatcherHeader(pb)

    m.update_pb(pb)
    assert get_exception_msg(m.validate) == u'name: is required'

    pb.name = u'X-Name'
    assert get_exception_msg(m.validate) == u're: is required'

    pb.re = u'.*'
    assert get_exception_msg(m.validate) is None

    pb.name = u'#'
    assert get_exception_msg(m.validate) == u'name: invalid header name: "#"'

    pb.name = u'X-Name'
    pb.re = u'('
    assert u're: is not a valid regexp' in get_exception_msg(m.validate)
