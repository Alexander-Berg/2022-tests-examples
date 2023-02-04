import mock

from awacs.wrappers.l7upstreammacro import (
    L7UpstreamMacro,
    L7UpstreamMacroFlatScheme,
    L7UpstreamMacroByDcScheme,
    L7UpstreamMacroStaticResponse
)
from awtest.wrappers import get_exception_msg
from infra.awacs.proto import modules_pb2


def test_l7_macro_get_would_be_included_full_backend_ids():
    expected_full_backend_ids = {
        (u'x', u'a'),
        (u'x', u'b'),
        (u'y', u'c'),
    }

    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_upstream_macro
    pb.flat_scheme.backend_ids.extend([u'a', u'b', u'y/c'])

    actual_full_backend_ids = L7UpstreamMacro(pb).get_would_be_included_full_backend_ids(current_namespace_id=u'x')
    assert actual_full_backend_ids == expected_full_backend_ids
    pb.by_dc_scheme.dcs.add(name=u'sas').backend_ids.extend([u'a', u'b'])
    pb.by_dc_scheme.dcs.add(name=u'man').backend_ids.extend([u'y/c'])

    actual_full_backend_ids = L7UpstreamMacro(pb).get_would_be_included_full_backend_ids(current_namespace_id=u'x')
    assert actual_full_backend_ids == expected_full_backend_ids


def test_l7_upstream_macro():
    pb = modules_pb2.L7UpstreamMacro()
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'version: is required'

    pb.version = u'0.0.1'
    assert get_exception_msg(m.validate) == u'id: is required'

    pb.id = u'default'
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(
        m.validate) == u'at least one of the "by_dc_scheme", "flat_scheme", "static_response", "traffic_split" must be specified'

    pb.flat_scheme.SetInParent()
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == u'"matcher": must be set'

    pb.matcher.any = True
    m = L7UpstreamMacro(pb)
    with mock.patch.object(L7UpstreamMacroFlatScheme, u'validate'):
        assert get_exception_msg(m.validate) is None

    pb.by_dc_scheme.SetInParent()
    m = L7UpstreamMacro(pb)
    with mock.patch.object(L7UpstreamMacroByDcScheme, u'validate'):
        assert get_exception_msg(m.validate) is None

    pb.static_response.SetInParent()
    m = L7UpstreamMacro(pb)
    with mock.patch.object(L7UpstreamMacroStaticResponse, u'validate'):
        assert get_exception_msg(m.validate) is None

    pb.headers.add().decrypt_icookie.SetInParent()
    m = L7UpstreamMacro(pb)
    with mock.patch.object(L7UpstreamMacroStaticResponse, u'validate'):
        assert get_exception_msg(
            m.validate) == u'headers[0]: the `decrypt_icookie` action is not available in l7_upstream_macro'
    del pb.headers[:]

    rewrite_pb = pb.rewrite.add()
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == 'rewrite[0] -> target: is required'

    rewrite_pb.target = modules_pb2.L7Macro.RewriteAction.CGI
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == 'rewrite[0] -> pattern: is required'

    rewrite_pb.pattern.re = 'hello'
    m = L7UpstreamMacro(pb)
    assert get_exception_msg(m.validate) == 'rewrite[0] -> replacement: is required'

    rewrite_pb.replacement = 'goodbye'
    m = L7UpstreamMacro(pb)
    with mock.patch.object(L7UpstreamMacroStaticResponse, u'validate'):
        assert get_exception_msg(m.validate) is None

    pb.matcher.host_re = u'.*'
    assert get_exception_msg(m.validate) == (
        u'matcher: at most one of the "and_", "any", "cgi_re", "header", "host_re", "method", "not_", '
        u'"or_", "path_re", "uri_re", "url_re" must be specified')

    pb.matcher.any = False
    assert get_exception_msg(m.validate) == u'matcher -> any: must be set if upstream is "default"'
