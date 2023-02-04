from infra.awacs.proto import modules_pb2, model_pb2
from awacs.wrappers.base import ValidationCtx
from awtest.wrappers import get_wrapped_validation_exception_msg


def test_l7_macro_webauth():
    pb = modules_pb2.L7Macro()
    pb.version = '0.0.1'
    pb.webauth.action = pb.webauth.AUTHORIZE
    pb.webauth.mode = pb.webauth.SIMPLE
    pb.http.ports.append(80)
    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.version = '0.2.11'
    e = get_wrapped_validation_exception_msg(pb)
    assert e == 'webauth: "http.redirect_to_https" must be enabled'

    pb.webauth.action = pb.webauth.SAVE_OAUTH_TOKEN
    pb.webauth.mode = pb.webauth.EXTERNAL
    e = get_wrapped_validation_exception_msg(pb)
    assert e is None
    pb.webauth.action = pb.webauth.AUTHORIZE
    pb.webauth.mode = pb.webauth.SIMPLE

    pb.http.redirect_to_https.SetInParent()
    pb.https.ports.append(443)
    pb.https.certs.add(id='test-cert')
    pb.announce_check_reply.url_re = '/ping'
    pb.health_check_reply.SetInParent()
    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.ClearField('http')
    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.webauth.mode = pb.webauth.SIMPLE
    pb.webauth.action = pb.webauth.SAVE_OAUTH_TOKEN
    e = get_wrapped_validation_exception_msg(pb)
    assert e == 'webauth -> action: must not be "SAVE_OAUTH_TOKEN" if "mode" is "SIMPLE"'

    pb.webauth.mode = pb.webauth.EXTERNAL
    pb.webauth.action = pb.webauth.SAVE_OAUTH_TOKEN
    e = get_wrapped_validation_exception_msg(pb)
    assert not e


def test_l7_macro_webauth_pass_options_requests_through():
    pb = modules_pb2.L7Macro()
    pb.version = u'0.0.1'
    pb.webauth.action = pb.webauth.AUTHORIZE
    pb.webauth.mode = pb.webauth.SIMPLE
    pb.webauth.pass_options_requests_through = True
    pb.http.ports.append(80)

    components_pb = model_pb2.BalancerSpec.ComponentsSpec()
    components_pb.pginx_binary.version = u'222-1'
    components_pb.pginx_binary.state = components_pb.pginx_binary.SET
    ctx = ValidationCtx(components_pb=components_pb)
    e = get_wrapped_validation_exception_msg(pb, ctx=ctx)
    assert e == u'webauth -> pass_options_requests_through: requires component pginx_binary of version >= 244-1, not 222-1'

    components_pb.pginx_binary.version = u'250-0'
    e = get_wrapped_validation_exception_msg(pb, ctx=ctx)
    assert not e
