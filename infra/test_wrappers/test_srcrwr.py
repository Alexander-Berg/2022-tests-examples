# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import ValidationCtx
from awacs.wrappers.main import Srcrwr
from awtest.wrappers import get_validation_exception


def test_srcrwr():
    pb = modules_pb2.SrcrwrModule()
    srcrwr = Srcrwr(pb)
    ctx = ValidationCtx(namespace_id='srcrwr_enabled')

    pb.id = '179'
    srcrwr.update_pb(pb)
    e = get_validation_exception(srcrwr.validate, ctx=ctx)
    e.match('match_host: is required')

    pb.match_host = '$'
    srcrwr.update_pb(pb)
    e = get_validation_exception(srcrwr.validate, ctx=ctx)
    e.match('match_source_mask: is required')

    pb.match_source_mask = '0.0.0.0/0,::/0'
    e = get_validation_exception(srcrwr.validate, ctx=ctx)
    e.match('must have nested module')

    h_pb = pb.nested.modules.add()
    h_pb.report.uuid = '12'
    srcrwr.update_pb(pb)
    e = get_validation_exception(srcrwr.validate, ctx=ctx)
    e.match('must be followed by balancer2 module')

    pb.nested.report.uuid = '12'
    srcrwr.update_pb(pb)
    e = get_validation_exception(srcrwr.validate, ctx=ctx)
    e.match('must be followed by balancer2 module')

    pb.nested.balancer2.attempts = 2
    srcrwr.update_pb(pb)
    with mock.patch.object(srcrwr, 'validate_composite_fields'):
        e = get_validation_exception(srcrwr.validate, ctx)
    e.match('is not a valid regexp: using \\$ anchor is not allowed')

    pb.match_host = '.*'
    with mock.patch.object(srcrwr, 'validate_composite_fields'):
        srcrwr.validate(ctx)

    ctx = ValidationCtx(namespace_id='some_usual_namespace', modules_blacklist=['srcrwr'])
    with mock.patch.object(srcrwr, 'validate_composite_fields'):
        e = get_validation_exception(srcrwr.validate, ctx)
    e.match('using srcrwr is not allowed. Please contact support if you absolutely must do it.')
