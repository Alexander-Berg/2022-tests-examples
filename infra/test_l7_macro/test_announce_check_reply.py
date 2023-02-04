from awtest.wrappers import get_wrapped_validation_exception_msg
from infra.awacs.proto import modules_pb2


def test_l7_macro_announce_check_reply():
    pb = modules_pb2.L7Macro()
    pb.version = u'0.2.6'
    pb.http.SetInParent()
    pb.announce_check_reply.url_re = u'/ping'
    pb.announce_check_reply.use_upstream_handler = True
    e = get_wrapped_validation_exception_msg(pb)
    assert not e

    pb.version = u'0.2.9'
    e = get_wrapped_validation_exception_msg(pb)
    assert e == (u'announce_check_reply -> use_upstream_handler: '
                 u'can not be set without "compat.disable_graceful_shutdown"')

    pb.announce_check_reply.compat.disable_graceful_shutdown = True
    e = get_wrapped_validation_exception_msg(pb)
    assert not e
