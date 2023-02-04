import logging

import inject
import mock
import pytest
from sepelib.core import config as appconfig

from awacs.lib import l3mgrclient
from awacs.model.components import ComponentConfig
from awacs.model.namespace.operations import op_add_ip_address_to_l3_balancer as p
from infra.awacs.proto import model_pb2
from .operations_util import (
    create_operation,
    prepare_namespace,
    activate_l7,
    activate_l3,
    NS_ID, L3_ID, L7_ID,
)


@pytest.fixture(autouse=True)
def deps(binder, caplog, l3_mgr_client):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(l3mgrclient.IL3MgrClient, l3_mgr_client)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.mark.parametrize(u'disable_autoconfig_in_ns,disable_autoconfig_global,expected_next_state',
                         [(False, False, u'UPDATING_L7_CONTAINER_SPEC'),
                          (False, True, u'UPDATING_L3_SPEC'),
                          (True, False, u'UPDATING_L3_SPEC'),
                          (True, True, u'UPDATING_L3_SPEC')])
def test_requesting_ip_address(cache, zk_storage, ctx,
                               disable_autoconfig_in_ns, disable_autoconfig_global, expected_next_state):
    if disable_autoconfig_global:
        appconfig.set_value(u'run.disable_l7_tunnels_autoconfig_on_l3_update', True)
    l3_pb = prepare_namespace(cache, zk_storage, l7_balancer_ids=[L7_ID],
                              disable_autoconfig_in_ns=disable_autoconfig_in_ns)
    operation = create_operation(l3_pb, model_pb2.NamespaceOperationOrder.Content.AddIpAddressToL3Balancer)
    rv = p.RequestingIpAddress.process(ctx, operation)
    assert rv.next_state.name == expected_next_state
    assert operation.context[u'temporary_vs_id'] == 1
    assert operation.context[u'ip_address'] == u'::1'


@mock.patch.object(ComponentConfig, 'get_latest_published_version', lambda *_, **__: u'over9000')
def test_updating_l7_spec(cache, zk_storage, ctx):
    l3_pb = prepare_namespace(cache, zk_storage, l7_balancer_ids=[L7_ID], preactivate_l7=True)
    operation = create_operation(l3_pb, model_pb2.NamespaceOperationOrder.Content.AddIpAddressToL3Balancer)
    operation.context[u'temporary_vs_id'] = 1
    operation.context[u'ip_address'] = u'::1'
    rv = p.UpdatingL7ContainerSpec.process(ctx, operation)
    assert rv.next_state is p.State.UPDATING_L7_CONTAINER_SPEC
    l7_pb = zk_storage.must_get_balancer(NS_ID, L7_ID)
    assert l7_pb.spec.components.shawshank_layer.state is model_pb2.BalancerSpec.ComponentsSpec.Component.SET
    container_spec_pb = l7_pb.spec.container_spec
    assert len(container_spec_pb.virtual_ips) == 2
    assert container_spec_pb.virtual_ips[0].ip == u'127.0.0.1'  # from prepared L3 spec
    assert container_spec_pb.virtual_ips[1].ip == operation.context[u'ip_address']
    assert len(container_spec_pb.inbound_tunnels) == 1
    assert container_spec_pb.inbound_tunnels[0].HasField('fallback_ip6')
    assert len(container_spec_pb.outbound_tunnels) == 1
    assert len(container_spec_pb.outbound_tunnels[0].rules) == 1
    assert container_spec_pb.outbound_tunnels[0].rules[0].from_ip == u'127.0.0.1'

    rv = p.UpdatingL7ContainerSpec.process(ctx, operation)
    assert rv.next_state is p.State.UPDATING_L7_CONTAINER_SPEC
    assert isinstance(rv.content_pb, model_pb2.NamespaceOperationOrder.OrderFeedback.L7LocationsInProgress)
    assert rv.content_pb.locations == [u'SAS']

    activate_l7(zk_storage, cache, L7_ID)
    rv = p.UpdatingL7ContainerSpec.process(ctx, operation)
    assert rv.next_state is p.State.UPDATING_L3_SPEC


def test_waiting_for_l3_activation(cache, zk_storage, ctx):
    l3_pb = prepare_namespace(cache, zk_storage, l7_balancer_ids=[L7_ID], preactivate_l7=True)
    operation = create_operation(l3_pb, model_pb2.NamespaceOperationOrder.Content.AddIpAddressToL3Balancer)
    rv = p.WaitingForL3Activation.process(ctx, operation)
    assert rv.next_state is p.State.WAITING_FOR_L3_ACTIVATION

    activate_l3(zk_storage, cache, L3_ID)
    rv = p.WaitingForL3Activation.process(ctx, operation)
    assert rv.next_state is p.State.FINISHED
