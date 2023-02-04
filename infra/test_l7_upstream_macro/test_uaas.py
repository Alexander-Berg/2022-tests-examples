import pytest

from awacs.wrappers.l7upstreammacro import L7UpstreamMacro
from infra.awacs.proto import modules_pb2


@pytest.mark.parametrize('l7_upstream_macro_version, exp_getter_macro_version', [
    ('0.0.1', 0),
    ('0.0.2', 3),
    ('0.1.1', 0),
    ('0.1.2', 3),
])
def test_l7_upstream_macro_uaas_version(l7_upstream_macro_version, exp_getter_macro_version):
    holder_pb = modules_pb2.Holder()
    pb = holder_pb.l7_upstream_macro
    pb.static_response.status = 200
    pb.version = l7_upstream_macro_version
    pb.headers.add().uaas.service_name = 'test_uaas'
    l7_upstream_macro = L7UpstreamMacro(pb)

    expanded_holder_pbs = l7_upstream_macro.expand()
    exp_getter_macro_pb = None
    for holder_pb in expanded_holder_pbs:
        if holder_pb.HasField('exp_getter_macro'):
            exp_getter_macro_pb = holder_pb.exp_getter_macro
            break
    assert exp_getter_macro_pb._version == exp_getter_macro_version
