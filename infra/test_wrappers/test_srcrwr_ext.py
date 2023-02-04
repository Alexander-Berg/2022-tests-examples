# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.base import ValidationCtx
from awacs.wrappers.main import SrcrwrExt
from awtest.wrappers import get_validation_exception


def test_srcrwr_ext():
    pb = modules_pb2.SrcrwrExtModule()
    srcrwr_ext = SrcrwrExt(pb)
    ctx = ValidationCtx(namespace_id=u'srcrwr_ext_enabled')

    e = get_validation_exception(srcrwr_ext.validate, ctx=ctx)
    e.match(u'remove_prefix: is required')
    pb.remove_prefix = u'm'

    e = get_validation_exception(srcrwr_ext.validate, ctx=ctx)
    e.match(u'domains: is required')

    pb.domains = u'yp-c.yandex.net'
    e = get_validation_exception(srcrwr_ext.validate, ctx=ctx)
    e.match(u'must have nested module')

    h_pb = pb.nested.modules.add()
    h_pb.report.uuid = u'12'
    h_pb.report.ranges = u'default'
    srcrwr_ext.update_pb(pb)
    e = get_validation_exception(srcrwr_ext.validate, ctx=ctx)
    e.match(u'must contain proxy or balancer2 with generated_proxy_backends as terminal module')

    h_pb = pb.nested.modules.add()
    h_pb.proxy.host = 'x'
    h_pb.proxy.port = 80
    srcrwr_ext.update_pb(pb)
    srcrwr_ext.validate(ctx)

    ctx = ValidationCtx(namespace_id=u'some_usual_namespace', modules_blacklist=[u'srcrwr_ext'])
    with mock.patch.object(srcrwr_ext, u'validate_composite_fields'):
        e = get_validation_exception(srcrwr_ext.validate, ctx)
    e.match(u'using srcrwr_ext is not allowed. Please contact support if you absolutely must do it.')
