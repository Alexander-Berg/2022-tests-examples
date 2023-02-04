# coding: utf-8
import mock

from infra.awacs.proto import modules_pb2
from awacs.wrappers.main import Holder, RpcRewriteMacro, RpcRewrite


def test_rpc_rewrite_macro():
    pb = modules_pb2.RpcRewriteMacro()
    m = RpcRewriteMacro(pb)

    m.validate(chained_modules=True)

    pb.generated_proxy_backends.SetInParent()
    m.update_pb(pb)
    with mock.patch.object(m.generated_proxy_backends, 'validate') as stub:
        m.validate(chained_modules=True)
    assert stub.called

    seq = list(map(Holder, m.expand()))
    assert len(seq) == 1
    rpc_rewrite_m = seq[0].module
    assert isinstance(rpc_rewrite_m, RpcRewrite)
    assert rpc_rewrite_m.rpc
    assert not rpc_rewrite_m.on_rpc_error
    assert rpc_rewrite_m.pb.file_switch == RpcRewrite.DEFAULT_FILE_SWITCH

    pb.enable_on_rpc_error = True
    m.update_pb(pb)

    seq = list(map(Holder, m.expand()))
    assert len(seq) == 1
    rpc_rewrite_m = seq[0].module
    assert isinstance(rpc_rewrite_m, RpcRewrite)
    assert rpc_rewrite_m.rpc
    assert rpc_rewrite_m.on_rpc_error
    assert rpc_rewrite_m.on_rpc_error.module.pb.status == 500
    assert rpc_rewrite_m.on_rpc_error.module.pb.content == 'Failed to rewrite request using RPC'
