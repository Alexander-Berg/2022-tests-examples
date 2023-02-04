import logging

import inject
import pytest
from mock import mock

from awacs.lib.l3mgrclient import IL3MgrClient
from awacs.model.components import ComponentConfig
from awacs.model.l3_balancer.order import processors as p
from awacs.model.l3_balancer.order.processors import L3BalancerOrder
from awtest import wait_until_passes, check_log, wait_until
from infra.awacs.proto import model_pb2
from infra.swatlib.auth.abc import IAbcClient
from .conftest import (
    create_l7_balancer,
    NS_ID,
    create_l3_balancer_order,
    L3_BALANCER_ID,
    create_active_l3_state,
    activate_l7, create_l7_balancer_state,
)


@pytest.fixture(autouse=True)
def deps(binder, caplog, abc_client, l3_mgr_client):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(IAbcClient, abc_client)
        b.bind(IL3MgrClient, l3_mgr_client)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def prepare_namespace(zk_storage, cache):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    zk_storage.create_namespace(NS_ID, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))


def test_start_with_slbping(cache, zk_storage, ctx, l3_balancer_order):
    assert p.Started.process(ctx, l3_balancer_order).next_state.name == u'CREATING_SLB_PING_UPSTREAM'
    assert u'ping_url' not in l3_balancer_order.context


def test_start_without_slbping(cache, zk_storage, ctx, l3_balancer_order):
    create_l7_balancer(cache, zk_storage)
    assert p.Started.process(ctx, l3_balancer_order).next_state.name == u'GETTING_ABC_SLUGS'
    assert l3_balancer_order.context[u'ping_url'] == u'/ping'


def test_start_inconsistent_l7(cache, zk_storage, ctx, l3_balancer_order):
    create_l7_balancer(cache, zk_storage, balancer_id=u'xxx')
    create_l7_balancer(cache, zk_storage, balancer_id=u'yyy', announce_check=False)
    rv = p.Started.process(ctx, l3_balancer_order)
    assert rv.description == u'"l7_macro.announce_check_reply" must be configured in L7 balancers: "yyy"'


def test_start_inconsistent_l7_ping_url(cache, zk_storage, ctx, l3_balancer_order):
    create_l7_balancer(cache, zk_storage, balancer_id=u'xxx')
    create_l7_balancer(cache, zk_storage, balancer_id=u'yyy', announce_check_url=u'/ping2')
    rv = p.Started.process(ctx, l3_balancer_order)
    assert (rv.description == u'"l7_macro.announce_check_reply.url_re" is not identical in all L7 balancers, '
                              u'please configure them to be the same.')


def test_creating_slbping_upstream(cache, zk_storage, ctx, l3_balancer_order):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    zk_storage.create_namespace(NS_ID, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))
    assert p.CreatingSlbPingUpstream.process(ctx, l3_balancer_order).next_state.name == u'GETTING_ABC_SLUGS'
    assert zk_storage.must_get_upstream(NS_ID, p.CreatingSlbPingUpstream.SLBPING_UPSTREAM_ID)


def test_creating_slbping_upstream_with_existing(cache, zk_storage, ctx, l3_balancer_order):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    zk_storage.create_namespace(NS_ID, ns_pb)
    u_pb = model_pb2.Upstream()
    u_pb.meta.namespace_id = NS_ID
    u_pb.meta.id = u'slbping'
    zk_storage.create_upstream(NS_ID, u'slbping', u_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))
    wait_until_passes(lambda: cache.must_get_upstream(NS_ID, u'slbping'))
    assert p.CreatingSlbPingUpstream.process(ctx, l3_balancer_order).next_state.name == u'GETTING_ABC_SLUGS'
    assert zk_storage.must_get_upstream(NS_ID, p.CreatingSlbPingUpstream.SLBPING_UPSTREAM_ID)


def test_getting_abc_slugs_rclb(cache, zk_storage, ctx, l3_balancer_order):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.meta.abc_service_id = 1234
    zk_storage.create_namespace(NS_ID, ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))
    assert p.GettingAbcSlugs.process(ctx, l3_balancer_order).next_state.name == u'CREATING_L3_MGR_SERVICE'
    assert l3_balancer_order.context[u'permissions_abc_slug'] == u'1234_slug'
    assert l3_balancer_order.context[u'owner_abc_slug'] == u'rclb'


def test_getting_abc_slugs_custom(cache, zk_storage, ctx, l3_balancer_order):
    l3_balancer_order.pb.order.content.abc_service_id = 1234
    assert p.GettingAbcSlugs.process(ctx, l3_balancer_order).next_state.name == u'CREATING_L3_MGR_SERVICE'
    assert l3_balancer_order.context[u'permissions_abc_slug'] == u'1234_slug'
    assert l3_balancer_order.context[u'owner_abc_slug'] == u'1234_slug'


@pytest.mark.parametrize(u'config_mode', (model_pb2.L3BalancerSpec.MODE_REAL_SERVERS_ONLY,
                                          model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS))
def test_creating_l3mgr_service(ctx, zk_storage, l3_balancer_order, l3_mgr_client, config_mode):
    l3_balancer_order.context[u'owner_abc_slug'] = u'rclb'
    l3_balancer_order.pb.order.content.config_management_mode = config_mode
    expected_data = {
        u'meta-OWNER': u'awacs',
        u'meta-LINK': u'https://nanny.yandex-team.ru/ui/#/awacs/namespaces/list/'
                      u'namespace-id/l3-balancers/list/l3-id/show/'
    }
    if config_mode == model_pb2.L3BalancerSpec.MODE_REAL_SERVERS_ONLY:
        next_state = u'UPDATING_L3_MGR_SERVICE_PERMISSIONS'
    else:
        expected_data[u'meta-LOCKED'] = True
        next_state = u'ACQUIRING_IPV6_ADDRESS'
    assert p.CreatingL3mgrService.process(ctx, l3_balancer_order).next_state.name == next_state
    assert l3_balancer_order.context[u'svc_id'] == u'999'
    l3_mgr_client.create_service.assert_called_with(abc_code=u'rclb', fqdn=u'', data=expected_data)


def test_creating_l3mgr_service_existing(cache, zk_storage, ctx, l3_balancer_order, l3_mgr_client):
    l3_balancer_order.context[u'owner_abc_slug'] = u'rclb'
    l3_balancer_order.pb.order.content.fqdn = u'existing'
    l3_mgr_client.list_services_by_fqdn = lambda *_, **__: [{u'fqdn': u'existing', u'id': u'ok'}, ]
    result = p.CreatingL3mgrService.process(ctx, l3_balancer_order)
    assert result.next_state.name == u'UPDATING_L3_MGR_SERVICE_PERMISSIONS'
    assert l3_balancer_order.context[u'svc_id'] == u'ok'


def test_updating_l3mgr_permissions(cache, zk_storage, ctx, l3_balancer_order, caplog):
    l3_balancer_order.context[u'permissions_abc_slug'] = u'rclb'
    l3_balancer_order.context[u'svc_id'] = u'999'
    with check_log(caplog) as log:
        result = p.UpdatingL3mgrServicePermissions.process(ctx, l3_balancer_order)
        assert result.next_state.name == u'ACQUIRING_IPV6_ADDRESS'
        assert u'l3mgr.editrs_service' not in log.records_text()  # mock has this permission
        assert u'l3mgr.editvs_service' in log.records_text()
        assert u'l3mgr.deploy_service' in log.records_text()


def test_acquiring_ipv6_address(cache, zk_storage, ctx, l3_balancer_order):
    l3_balancer_order.context[u'owner_abc_slug'] = u'rclb'
    assert p.AcquiringIPv6Address.process(ctx, l3_balancer_order).next_state.name == u'ACQUIRING_IPV4_ADDRESS'
    assert l3_balancer_order.context[u'ipv6_addr'] == u'::1'


def test_acquiring_ipv6_address_internal(cache, zk_storage, ctx):
    l3_balancer_order = create_l3_balancer_order(cache, zk_storage,
                                                 traffic_type=model_pb2.L3BalancerOrder.Content.INTERNAL)
    l3_balancer_order.context[u'owner_abc_slug'] = u'rclb'
    assert p.AcquiringIPv6Address.process(ctx, l3_balancer_order).next_state.name == u'CREATING_VIRTUAL_SERVERS'
    assert l3_balancer_order.context[u'ipv6_addr'] == u'::1'


def test_acquiring_ipv4_address(cache, zk_storage, ctx, l3_balancer_order):
    l3_balancer_order.context[u'owner_abc_slug'] = u'rclb'
    assert p.AcquiringIPv4Address.process(ctx, l3_balancer_order).next_state.name == u'CREATING_VIRTUAL_SERVERS'
    assert l3_balancer_order.context[u'ipv4_addr'] == u'127.0.0.1'


@pytest.mark.parametrize(u'ping_url', (None, u'/ping2'))
@pytest.mark.parametrize(u'protocol', (model_pb2.L3BalancerOrder.Content.HTTP,
                                       model_pb2.L3BalancerOrder.Content.HTTPS,
                                       model_pb2.L3BalancerOrder.Content.HTTP_AND_HTTPS))
@pytest.mark.parametrize(u'traffic_type', (model_pb2.L3BalancerOrder.Content.INTERNAL,
                                           model_pb2.L3BalancerOrder.Content.EXTERNAL))
@pytest.mark.parametrize(u'config_mode', (model_pb2.L3BalancerSpec.MODE_REAL_SERVERS_ONLY,
                                          model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS))
def test_creating_virtual_servers(cache, zk_storage, ctx, ping_url, protocol, traffic_type, config_mode):
    expected_vs_num = 1
    l3_balancer_order = create_l3_balancer_order(cache, zk_storage, protocol=protocol, traffic_type=traffic_type)
    if ping_url is not None:
        l3_balancer_order.context[u'ping_url'] = ping_url
    l3_balancer_order.context[u'svc_id'] = u'fake_svc'
    l3_balancer_order.pb.order.content.config_management_mode = config_mode

    l3_balancer_order.context[u'ipv6_addr'] = u'::1'
    addrs = [u'::1']
    if traffic_type == model_pb2.L3BalancerOrder.Content.EXTERNAL:
        expected_vs_num *= 2
        l3_balancer_order.context[u'ipv4_addr'] = u'127.0.0.1'
        addrs.append(u'127.0.0.1')

    if protocol == model_pb2.L3BalancerOrder.Content.HTTP:
        ports = [80]
    elif protocol == model_pb2.L3BalancerOrder.Content.HTTPS:
        ports = [443]
    else:
        expected_vs_num *= 2
        ports = [80, 443]

    assert p.CreatingVirtualServers.process(ctx, l3_balancer_order).next_state.name == u'SAVING_VS_CONFIG'
    assert l3_balancer_order.context[u'vs_ids'] == [i + 1 for i in range(0, expected_vs_num)]

    expected_check_url = ping_url or u'/ping'
    assert len(l3_balancer_order.pb.spec.virtual_servers) == len(addrs) * len(ports)
    for vs_pb in l3_balancer_order.pb.spec.virtual_servers:
        assert vs_pb.ip in addrs
        assert vs_pb.port in ports
        if traffic_type == model_pb2.L3BalancerOrder.Content.INTERNAL:
            assert vs_pb.traffic_type == vs_pb.TT_INTERNAL
        else:
            assert vs_pb.traffic_type == vs_pb.TT_EXTERNAL
        hc_pb = vs_pb.health_check_settings
        assert hc_pb.url == expected_check_url
        assert hc_pb.check_type == hc_pb.CT_SSL_GET if vs_pb.port == 443 else hc_pb.CT_HTTP_GET

    # check that repeated processing doesn't duplicate virtual servers in spec
    assert p.CreatingVirtualServers.process(ctx, l3_balancer_order).next_state.name == u'SAVING_VS_CONFIG'
    assert len(l3_balancer_order.pb.spec.virtual_servers) == len(addrs) * len(ports)


def test_saving_vs_config(cache, zk_storage, ctx, l3_balancer_order, l3_mgr_client, prepare_namespace):
    l3_balancer_order.context[u'vs_ids'] = [u'fake_vs1', u'fake_vs2']
    l3_balancer_order.context[u'svc_id'] = u'fake_svc'
    assert p.SavingVsConfig.process(ctx, l3_balancer_order).next_state.name == u'UPDATING_L7_CONTAINER_SPEC'
    assert l3_balancer_order.context[u'l3mgr_cfg_id'] == 1
    assert l3_balancer_order.context[u'l3mgr_cfg_processed']
    l3_mgr_client.create_config_with_vs.assert_called_with(comment=u'Add virtual servers',
                                                           svc_id=u'fake_svc',
                                                           use_etag=False,
                                                           vs_ids=[u'fake_vs1', u'fake_vs2'])
    l3_mgr_client.process_config.assert_called_with(cfg_id=1, force=True,
                                                    use_etag=True, latest_cfg_id=1, svc_id=u'fake_svc')


def test_saving_vs_config_existing(cache, zk_storage, ctx, l3_balancer_order, l3_mgr_client, prepare_namespace):
    l3_mgr_client.awtest_set_default_config()
    l3_balancer_order.context[u'l3mgr_cfg_id'] = 0
    l3_balancer_order.context[u'svc_id'] = u'fake_svc'
    assert p.SavingVsConfig.process(ctx, l3_balancer_order).next_state.name == u'UPDATING_L7_CONTAINER_SPEC'
    assert l3_balancer_order.context[u'l3mgr_cfg_id'] == 0
    assert l3_balancer_order.context[u'l3mgr_cfg_processed']
    l3_mgr_client.create_config_with_vs.assert_not_called()
    l3_mgr_client.process_config.assert_called_with(cfg_id=0, force=True,
                                                    use_etag=True, latest_cfg_id=0, svc_id=u'fake_svc')


def test_saving_vs_config_skip_tunnels(cache, zk_storage, ctx, l3_balancer_order, l3_mgr_client, prepare_namespace):
    l3_mgr_client.awtest_set_default_config()
    l3_balancer_order.context[u'l3mgr_cfg_id'] = 0
    l3_balancer_order.context[u'svc_id'] = u'fake_svc'
    another_l3_pb = model_pb2.L3Balancer()
    another_l3_pb.meta.namespace_id = NS_ID
    another_l3_pb.meta.id = u'uwu'
    zk_storage.create_l3_balancer(NS_ID, u'uwu', another_l3_pb)
    wait_until_passes(lambda: cache.must_get_l3_balancer(NS_ID, u'uwu'))
    assert p.SavingVsConfig.process(ctx, l3_balancer_order).next_state.name == u'SAVING_SPEC'


@mock.patch.object(ComponentConfig, 'get_latest_published_version', lambda *_, **__: u'over9000')
@pytest.mark.parametrize(u'addresses', (
    (u'127.1.1.1',),
    (u'127.1.1.1', u'::5'),
    (),
))
def test_updating_l7_spec(cache, zk_storage, ctx, l3_balancer_order, addresses):
    b_pb = create_l7_balancer(cache, zk_storage)
    create_l7_balancer_state(cache, zk_storage, b_pb.meta.id)
    for l3_pb in zk_storage.update_l3_balancer(NS_ID, L3_BALANCER_ID):
        l3_pb.order.content.real_servers.type = model_pb2.L3BalancerRealServersSelector.BALANCERS
        l3_pb.order.content.real_servers.balancers.add(id=b_pb.meta.id)
        for ip_addr in addresses:
            l3_pb.spec.virtual_servers.add(ip=ip_addr)

    ctx.log.info(u'1. L7 Balancer has no state and we consider it as deploying')
    l3_balancer_order = L3BalancerOrder(zk_storage.must_get_l3_balancer(NS_ID, L3_BALANCER_ID))
    rv = p.UpdatingL7ContainerSpec.process(ctx, l3_balancer_order)
    assert rv.next_state is p.State.UPDATING_L7_CONTAINER_SPEC
    assert isinstance(rv.content_pb, model_pb2.L3BalancerOrder.OrderFeedback.L7LocationsInProgress)
    assert rv.content_pb.locations == [u'SAS']

    ctx.log.info(u'2. L7 is idle, so we can configure its tunnels')
    activate_l7(zk_storage, cache, b_pb.meta.id)
    rv = p.UpdatingL7ContainerSpec.process(ctx, l3_balancer_order)
    assert rv.next_state is p.State.UPDATING_L7_CONTAINER_SPEC
    l7_pb = zk_storage.must_get_balancer(NS_ID, b_pb.meta.id)
    assert l7_pb.spec.components.shawshank_layer.state is model_pb2.BalancerSpec.ComponentsSpec.Component.SET
    container_spec_pb = l7_pb.spec.container_spec
    assert len(container_spec_pb.virtual_ips) == len(addresses)
    spec_ips = {vip.ip for vip in container_spec_pb.virtual_ips}
    for ip in addresses:
        assert ip in spec_ips
    assert len(container_spec_pb.inbound_tunnels) == 1
    assert container_spec_pb.inbound_tunnels[0].HasField('fallback_ip6')
    if u'127.1.1.1' in addresses:
        assert len(container_spec_pb.outbound_tunnels) == 1
        assert len(container_spec_pb.outbound_tunnels[0].rules) == 1
        assert container_spec_pb.outbound_tunnels[0].rules[0].from_ip == u'127.1.1.1'
    else:
        assert len(container_spec_pb.outbound_tunnels) == 0

    ctx.log.info(u'3. New L7 revision is not active, so we wait for it')
    rv = p.UpdatingL7ContainerSpec.process(ctx, l3_balancer_order)
    assert rv.next_state is p.State.UPDATING_L7_CONTAINER_SPEC

    ctx.log.info(u'4. New L7 revision is active, we can proceed')
    activate_l7(zk_storage, cache, b_pb.meta.id)
    rv = p.UpdatingL7ContainerSpec.process(ctx, l3_balancer_order)
    assert rv.next_state is p.State.SAVING_SPEC


@pytest.mark.parametrize(u'config_management_mode', (
    model_pb2.L3BalancerSpec.MODE_REAL_SERVERS_ONLY,
    model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS
))
def test_saving_spec(cache, zk_storage, ctx, l3_balancer_order, config_management_mode):
    l3_balancer_order.pb.order.content.use_endpoint_weights = True
    l3_balancer_order.pb.order.content.preserve_foreign_real_servers = True
    l3_balancer_order.pb.order.content.ctl_version = 999
    l3_balancer_order.pb.order.content.config_management_mode = config_management_mode
    l3_balancer_order.pb.spec.virtual_servers.add()
    l3_balancer_order.context[u'svc_id'] = u'fake_svc'
    assert p.SavingSpec.process(ctx, l3_balancer_order).next_state.name == u'WAITING_FOR_ACTIVATION'
    assert l3_balancer_order.pb.spec.use_endpoint_weights
    assert l3_balancer_order.pb.spec.preserve_foreign_real_servers
    assert l3_balancer_order.pb.spec.ctl_version == 999
    assert l3_balancer_order.pb.spec.real_servers == l3_balancer_order.pb.order.content.real_servers
    assert l3_balancer_order.pb.spec.config_management_mode == config_management_mode
    if l3_balancer_order.config_management_mode == model_pb2.L3BalancerSpec.MODE_REAL_SERVERS_ONLY:
        assert len(l3_balancer_order.pb.spec.virtual_servers) == 0
    else:
        assert len(l3_balancer_order.pb.spec.virtual_servers) == 1


def test_cancelling(cache, zk_storage, ctx, l3_balancer_order):
    assert p.Cancelling.process(ctx, l3_balancer_order).next_state.name == u'CANCELLED'
    wait_until(lambda: not cache.must_get_l3_balancer(NS_ID, L3_BALANCER_ID).spec.incomplete)


def test_waiting_for_activation(cache, zk_storage, ctx, l3_balancer_order):
    assert p.WaitingForActivation.process(ctx, l3_balancer_order).next_state.name == u'WAITING_FOR_ACTIVATION'
    create_active_l3_state(cache, zk_storage)
    assert p.WaitingForActivation.process(ctx, l3_balancer_order).next_state.name == u'ASSIGNING_FIREWALL_GRANTS'


def test_assigning_firewall_grants(cache, zk_storage, ctx, l3_balancer_order, caplog):
    l3_balancer_order.context[u'permissions_abc_slug'] = u'custom'
    l3_balancer_order.context[u'svc_id'] = u'fake_svc'
    with check_log(caplog) as log:
        assert p.AssigningFirewallGrants.process(ctx, l3_balancer_order).next_state.name == u'FINISHED'
        assert u'Setting grants: svc_id="fake_svc", subject="svc_custom"' in log.records_text()
