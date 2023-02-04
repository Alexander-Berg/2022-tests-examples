import mock

from awacs.wrappers.l7macro import to_rewrite_module_pb, L7MacroRewriteAction, L7MacroRewritePattern
from awtest.wrappers import get_wrapped_validation_exception_msg
from infra.awacs.proto import modules_pb2


def test_l7_macro_to_rewrite_module_pb():
    a_0_pb = modules_pb2.L7Macro.RewriteAction()
    a_0_pb.target = a_0_pb.URL
    a_0_pb.pattern.re = u'.*'
    a_0_pb.replacement = u'xxx'

    a_1_pb = modules_pb2.L7Macro.RewriteAction()
    a_1_pb.target = a_0_pb.PATH
    a_1_pb.pattern.re = u'\\.*'
    a_1_pb.pattern.case_sensitive.value = True
    a_1_pb.replacement = u'yyy'

    a_2_pb = modules_pb2.L7Macro.RewriteAction()
    a_2_pb.target = a_0_pb.CGI
    a_2_pb.pattern.re = u'\\.*'
    getattr(a_2_pb.pattern, 'global').value = True
    a_2_pb.pattern.literal.value = True
    a_2_pb.replacement = u'"zzz"'

    rewrite_pb = to_rewrite_module_pb(list(map(L7MacroRewriteAction, [a_0_pb, a_1_pb, a_2_pb])))
    expected_rewrite_pb = modules_pb2.RewriteModule()

    r_0_pb = expected_rewrite_pb.actions.add(
        split=u'url',
        regexp=u'.*',
        rewrite=u'xxx'
    )
    r_0_pb.case_insensitive.value = True

    r_1_pb = expected_rewrite_pb.actions.add(
        split=u'path',
        regexp=u'\\\\.*',
        rewrite=u'yyy'
    )
    r_1_pb.case_insensitive.value = False

    r_2_pb = expected_rewrite_pb.actions.add(
        split=u'cgi',
        regexp=u'\\\\.*',
        rewrite=u'\\\"zzz\\\"',
        literal=True
    )
    setattr(r_2_pb, 'global', True)
    r_2_pb.case_insensitive.value = True

    assert rewrite_pb.actions == expected_rewrite_pb.actions


def test_l7_macro_rewrite_action_validate():
    pb = modules_pb2.L7Macro.RewriteAction()
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'target: is required'

    pb.target = pb.URL
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'pattern: is required'

    pb.pattern.SetInParent()
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u'replacement: is required'

    pb.replacement = u'xxx'
    with mock.patch.object(L7MacroRewritePattern, u'validate') as pattern_validate:
        e = get_wrapped_validation_exception_msg(pb)
    assert not e
    assert pattern_validate.called


def test_l7_macro_rewrite_pattern_validate():
    pb = modules_pb2.L7Macro.RewritePattern()
    e = get_wrapped_validation_exception_msg(pb)
    assert e == u're: is required'

    pb.re = u'*'
    e = get_wrapped_validation_exception_msg(pb)
    assert u're: is not a valid regexp' in e

    pb.re = u'.*'
    e = get_wrapped_validation_exception_msg(pb)
    assert not e
