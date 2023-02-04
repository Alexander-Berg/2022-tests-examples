from infra.awacs.proto import model_pb2 as awacs_model_pb2
from infra.shawshank.proto import tuns_pb2
from infra.shawshank.lib import util


def test_awacs_instance_spec_pb_to_internal_pb():
    # construct awacs spec
    awacs_spec_pb = awacs_model_pb2.BalancerContainerSpec()
    awacs_tunnel_pb = awacs_spec_pb.outbound_tunnels.add()
    awacs_tunnel_pb.id = 'myshinytunnel'
    awacs_tunnel_pb.remote_ip = '8.8.8.8'
    awacs_tunnel_pb.rules.add(from_ip='127.0.0.1', to_ip='::1')
    awacs_tunnel_pb.mode = awacs_model_pb2.BalancerContainerSpec.OutboundTunnel.IP6IP6
    awacs_tunnel_pb.mtu = 123
    awacs_tunnel_pb.advmss = 456

    awacs_tunnel_pb = awacs_spec_pb.outbound_tunnels.add()
    awacs_tunnel_pb.id = 'myshinytunnel-2'
    awacs_tunnel_pb.remote_ip = '9.9.9.9'
    awacs_tunnel_pb.rules.add(from_ip='1.2.3.4')
    awacs_tunnel_pb.mode = awacs_model_pb2.BalancerContainerSpec.OutboundTunnel.IPIP6
    awacs_tunnel_pb.mtu = 1
    awacs_tunnel_pb.advmss = 2

    # construct expected internal spec
    expected_internal_pb = tuns_pb2.ContainerSpec()
    tunnel_pb = expected_internal_pb.tunnels.add(
        name='myshinytunnel',
        remote_ip='8.8.8.8',
        mtu=123,
        advmss=456,
    )
    tunnel_pb.mode = tunnel_pb.IP6IP6
    rule = tunnel_pb.rules.add(
        from_ip='127.0.0.1',
        to_ip='::1',
        priority=util.DEFAULT_AWACS_OUTBOUND_TUNNEL_RULES_PRIORITY,
        table_id=util.DEFAULT_AWACS_OUTBOUND_TUNNEL_TABLE_ID_OFFSET,
    )
    rule.default_v4_route.SetInParent()
    rule.default_v6_route.SetInParent()
    tunnel_pb.encap.SetInParent()
    tunnel_pb = expected_internal_pb.tunnels.add(
        name='myshinytunnel-2',
        remote_ip='9.9.9.9',
        mtu=1,
        advmss=2,
    )
    tunnel_pb.mode = tunnel_pb.IPIP6
    rule = tunnel_pb.rules.add(
        from_ip='1.2.3.4',
        priority=util.DEFAULT_AWACS_OUTBOUND_TUNNEL_RULES_PRIORITY + 1,
        table_id=util.DEFAULT_AWACS_OUTBOUND_TUNNEL_TABLE_ID_OFFSET + 1,
    )
    rule.default_v4_route.SetInParent()
    rule.default_v6_route.SetInParent()
    tunnel_pb.encap.SetInParent()
    expected_internal_pb.default_v4_route.SetInParent()
    expected_internal_pb.default_v6_route.SetInParent()

    # check whether actual internal spec is the same as expected
    actual_internal_pb = util.awacs_instance_spec_pb_to_internal_pb(awacs_spec_pb)
    assert actual_internal_pb == expected_internal_pb
