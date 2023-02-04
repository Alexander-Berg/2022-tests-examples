import copy
import logging

import inject
import mock
import pytest

from awacs.lib import l3mgrclient
from awacs.model.components import ComponentConfig
from awacs.model.namespace.operations import op_import_vs_from_l3mgr as p
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


def prepare_l3mgr(l3_mgr_client):
    l3_mgr_client.awtest_set_default_config()

    vs = copy.deepcopy(l3_mgr_client.vs[0])
    vs[u'ip'] = u'127.1.1.1'
    vs[u'port'] = u'443'
    config = vs[u'config']
    config[u'SCHEDULER'] = u'mh'
    config[u'MH_PORT'] = True
    config[u'CHECK_TYPE'] = u'HTTP_GET'
    config[u'CONNECT_IP'] = u'127.1.1.1'
    config[u'CONNECT_PORT'] = u'5'
    config[u'ANNOUNCE'] = False
    l3_mgr_client.awtest_add_vs(vs)

    vs = copy.deepcopy(l3_mgr_client.vs[0])
    vs[u'ip'] = u'127.1.1.2'
    vs[u'port'] = u'80'
    vs[u'config'][u'CHECK_TYPE'] = u'SSL_GET'
    l3_mgr_client.awtest_add_vs(vs)


def test_locking_l3_service(cache, zk_storage, ctx, l3_mgr_client):
    prepare_l3mgr(l3_mgr_client)
    l3_pb = prepare_namespace(cache, zk_storage, l7_balancer_ids=[L7_ID], preactivate_l7=True)
    operation = create_operation(l3_pb, model_pb2.NamespaceOperationOrder.Content.ImportVirtualServersFromL3mgr)
    rv = p.LockingL3MgrService.process(ctx, operation)

    l3_mgr_client.update_meta.assert_called_once_with(
        svc_id=u'xxx',
        data={
            u'OWNER': u'awacs',
            u'LINK': u'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/ns-with-op/'
                     u'l3-balancers/list/l3.ya.fu/show/',
            u'LOCKED': True,
        })

    assert rv.next_state is p.State.UPDATING_L7_CONTAINER_SPEC


@mock.patch.object(ComponentConfig, 'get_latest_published_version', lambda *_, **__: u'over9000')
def test_updating_l7_spec(cache, zk_storage, ctx, l3_mgr_client):
    prepare_l3mgr(l3_mgr_client)

    l3_pb = prepare_namespace(cache, zk_storage, l7_balancer_ids=[L7_ID], preactivate_l7=True)
    operation = create_operation(l3_pb, model_pb2.NamespaceOperationOrder.Content.ImportVirtualServersFromL3mgr)
    rv = p.UpdatingL7ContainerSpec.process(ctx, operation)
    assert rv.next_state is p.State.UPDATING_L7_CONTAINER_SPEC
    l7_pb = zk_storage.must_get_balancer(NS_ID, L7_ID)
    assert l7_pb.spec.components.shawshank_layer.state is model_pb2.BalancerSpec.ComponentsSpec.Component.SET
    container_spec_pb = l7_pb.spec.container_spec
    assert len(container_spec_pb.virtual_ips) == 3
    assert container_spec_pb.virtual_ips[0].ip == u'127.1.1.1'
    assert container_spec_pb.virtual_ips[1].ip == u'127.1.1.2'
    assert container_spec_pb.virtual_ips[2].ip == u'2a02:6b8:0:3400:0:2da:0:2'
    assert len(container_spec_pb.inbound_tunnels) == 1
    assert container_spec_pb.inbound_tunnels[0].HasField('fallback_ip6')
    assert len(container_spec_pb.outbound_tunnels) == 1
    assert len(container_spec_pb.outbound_tunnels[0].rules) == 2
    assert container_spec_pb.outbound_tunnels[0].rules[0].from_ip == u'127.1.1.1'
    assert container_spec_pb.outbound_tunnels[0].rules[1].from_ip == u'127.1.1.2'

    rv = p.UpdatingL7ContainerSpec.process(ctx, operation)
    assert rv.next_state is p.State.UPDATING_L7_CONTAINER_SPEC
    assert isinstance(rv.content_pb, model_pb2.NamespaceOperationOrder.OrderFeedback.L7LocationsInProgress)
    assert rv.content_pb.locations == [u'SAS']

    activate_l7(zk_storage, cache, L7_ID)
    rv = p.UpdatingL7ContainerSpec.process(ctx, operation)
    assert rv.next_state is p.State.UPDATING_L3_SPEC


def test_updating_l3_spec(cache, zk_storage, ctx, l3_mgr_client):
    prepare_l3mgr(l3_mgr_client)

    l3_pb = prepare_namespace(cache, zk_storage, l7_balancer_ids=[L7_ID], preactivate_l7=True,
                              l3_config_mode=model_pb2.L3BalancerSpec.MODE_REAL_SERVERS_ONLY)
    operation = create_operation(l3_pb, model_pb2.NamespaceOperationOrder.Content.ImportVirtualServersFromL3mgr)
    operation.pb.order.content.import_virtual_servers_from_l3mgr.enforce_configs = True
    rv = p.UpdatingL3Spec.process(ctx, operation)

    l3_pb = zk_storage.must_get_l3_balancer(NS_ID, L3_ID)
    assert len(l3_pb.spec.virtual_servers) == 3
    vs_pbs = sorted(l3_pb.spec.virtual_servers, key=lambda pb: pb.ip)

    vs_pb = vs_pbs[0]
    assert vs_pb.ip == u'127.1.1.1'
    assert vs_pb.port == 443
    assert vs_pb.health_check_settings.check_type == vs_pb.health_check_settings.CT_SSL_GET

    vs_pb = vs_pbs[1]
    assert vs_pb.ip == u'127.1.1.2'
    assert vs_pb.port == 80
    assert vs_pb.health_check_settings.check_type == vs_pb.health_check_settings.CT_HTTP_GET

    vs_pb = vs_pbs[2]
    assert vs_pb.ip == u'2a02:6b8:0:3400:0:2da:0:2'
    assert vs_pb.port == 80
    assert vs_pb.health_check_settings.check_type == vs_pb.health_check_settings.CT_HTTP_GET

    assert rv.next_state is p.State.WAITING_FOR_L3_ACTIVATION


def test_waiting_for_l3_activation(cache, zk_storage, ctx):
    l3_pb = prepare_namespace(cache, zk_storage, l7_balancer_ids=[L7_ID], preactivate_l7=True)
    operation = create_operation(l3_pb, model_pb2.NamespaceOperationOrder.Content.ImportVirtualServersFromL3mgr)
    rv = p.WaitingForL3Activation.process(ctx, operation)
    assert rv.next_state is p.State.WAITING_FOR_L3_ACTIVATION
    activate_l3(zk_storage, cache, L3_ID)
    rv = p.WaitingForL3Activation.process(ctx, operation)
    assert rv.next_state is p.State.FINISHED
