# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import RpcRewrite
from awtest.wrappers import get_validation_exception


def test_rpc_rewrite():
    pb = modules_pb2.RpcRewriteModule()
    m = RpcRewrite(pb)

    e = get_validation_exception(m.validate, chained_modules=True)
    e.match('rpc: is required')

    pb.rpc.SetInParent()
    m.update_pb(pb)
    with mock.patch.object(m.rpc, 'validate') as rpc_validate:
        m.validate(chained_modules=True)
    assert rpc_validate.called

    pb.on_rpc_error.SetInParent()
    m.update_pb(pb)
    with mock.patch.object(m.rpc, 'validate'), \
         mock.patch.object(m.on_rpc_error, 'validate') as on_rpc_error_validate:
        m.validate(chained_modules=True)
    assert rpc_validate.called
    assert on_rpc_error_validate.called
