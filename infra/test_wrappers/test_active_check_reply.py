# coding: utf-8
from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import ActiveCheckReply
from awtest.wrappers import get_validation_exception


def test_active_check_reply():
    pb = modules_pb2.ActiveCheckReplyModule()
    m = ActiveCheckReply(pb)
    e = get_validation_exception(m.validate)
    e.match('default_weight.*is required')

    pb.default_weight = -1
    e = get_validation_exception(m.validate)
    e.match('default_weight.*must be greater or equal to 1')

    pb.default_weight = 1001
    e = get_validation_exception(m.validate)
    e.match('default_weight.*must be less or equal to 1000')

    pb.default_weight = 500
    m.validate()

    pb.use_header.value = False
    pb.use_body.value = False
    e = get_validation_exception(m.validate)
    e.match('at least one of use_header or use_body must be set')
