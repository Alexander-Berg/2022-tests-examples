import mock

from awacs.wrappers.l7upstreammacro import L7UpstreamMacroMatcher
from awtest.wrappers import get_exception_msg
from infra.awacs.proto import modules_pb2


def test_matcher():
    kw = {u'upstream_id': u'xxx'}
    pb = modules_pb2.L7UpstreamMacro.Matcher()
    m = L7UpstreamMacroMatcher(pb)

    assert get_exception_msg(m.validate, **kw) == (
        u'at least one of the "and_", "any", "cgi_re", "header", "host_re", "method", "not_", "or_", '
        u'"path_re", "uri_re", "url_re" must be specified')

    pb.any = True
    assert get_exception_msg(m.validate, **kw) is None
    assert get_exception_msg(m.validate, upstream_id=u'default') is None

    pb.host_re = u'.*'
    assert get_exception_msg(m.validate, **kw) == (
        u'at most one of the "and_", "any", "cgi_re", "header", "host_re", "method", "not_", "or_", '
        u'"path_re", "uri_re", "url_re" must be specified')

    pb.any = False
    assert get_exception_msg(m.validate, **kw) is None
    assert get_exception_msg(m.validate, upstream_id=u'default') == u'any: must be set if upstream is "default"'

    pb.host_re = u''
    pb.header.SetInParent()
    m.update_pb(pb)
    with mock.patch.object(m.header, u'validate') as v:
        assert get_exception_msg(m.validate, **kw) is None
    assert v.called

    pb.ClearField('header')
    pb.method = u'XXX'
    m.update_pb(pb)
    assert get_exception_msg(m.validate, **kw) == (u'must be one of the following: '
                                                   u'GET, HEAD, POST, PUT, PATCH, DELETE, CONNECT, OPTIONS')

    pb.method = u'POST'
    assert get_exception_msg(m.validate, **kw) is None

    pb.method = u''
    pb.not_.SetInParent()
    m.update_pb(pb)
    assert get_exception_msg(m.validate, **kw).startswith(u'not_: at least one of the')

    pb.ClearField('not_')
    pb.and_.add(host_re=u'.*')
    m.update_pb(pb)
    assert get_exception_msg(m.validate, **kw) is None

    pb.and_.add()
    m.update_pb(pb)
    assert get_exception_msg(m.validate, **kw).startswith(u'and_[1]: at least one of the')

    del pb.and_[:]
    or_pb = pb.or_.add()
    m.update_pb(pb)
    assert get_exception_msg(m.validate, **kw).startswith(u'or_[0]: at least one of the')

    or_pb.any = True
    assert get_exception_msg(m.validate, **kw) == u'or_[0]: nested matcher must not contain "any: true"'
