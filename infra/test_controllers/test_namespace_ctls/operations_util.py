from awacs.lib.vectors import version
from awacs.model import objects
from awacs.model.balancer.state_handler import L7BalancerStateHandler
from awacs.model.balancer.vector import BalancerVersion
from awacs.model.l3_balancer import l3_balancer
from awacs.model.namespace.operations.op_add_ip_address_to_l3_balancer import AddIpToL3BalancerOp
from awacs.model.namespace.operations.op_import_vs_from_l3mgr import ImportVsFromL3MgrOp
from awtest import wait_until_passes, wait_until
from infra.awacs.proto import model_pb2


L7_ID = u'l7_sas'
L3_ID = u'l3.ya.fu'
NS_ID = u'ns-with-op'


def prepare_namespace(cache, zk_storage, l7_balancer_ids, preactivate_l7=False, disable_autoconfig_in_ns=False,
                      l3_config_mode=model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS):
    ns_pb = model_pb2.Namespace()
    ns_pb.meta.id = NS_ID
    ns_pb.spec.easy_mode_settings.disable_l7_tunnels_autoconfig.value = disable_autoconfig_in_ns
    zk_storage.create_namespace(namespace_id=NS_ID, namespace_pb=ns_pb)
    wait_until_passes(lambda: cache.must_get_namespace(NS_ID))

    l3_pb = model_pb2.L3Balancer()
    l3_pb.meta.namespace_id = NS_ID
    l3_pb.meta.id = L3_ID
    l3_pb.meta.version = u'yyy'
    l3_pb.spec.l3mgr_service_id = u'xxx'
    l3_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BALANCERS
    if l3_config_mode == model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS:
        vs_pb = l3_pb.spec.virtual_servers.add(ip=u'127.0.0.1',
                                               port=80,
                                               traffic_type=model_pb2.L3BalancerSpec.VirtualServer.TT_EXTERNAL)
        vs_pb.health_check_settings.url = u'/ping1'
        vs_pb.health_check_settings.check_type = model_pb2.L3BalancerSpec.VirtualServer.HealthCheckSettings.CT_HTTP_GET

    l3_state_pb = model_pb2.L3BalancerState()
    l3_state_pb.namespace_id = NS_ID
    l3_state_pb.l3_balancer_id = L3_ID

    for l7_id in l7_balancer_ids:
        l7_pb = model_pb2.Balancer()
        l7_pb.meta.namespace_id = NS_ID
        l7_pb.meta.id = l7_id
        l7_pb.meta.version = u'zzz'
        l7_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
        l7_pb.meta.location.yp_cluster = l7_id.split(u'_')[-1]
        state_pb = model_pb2.BalancerState()
        state_pb.namespace_id = NS_ID
        state_pb.balancer_id = l7_id
        if preactivate_l7:
            handler = L7BalancerStateHandler(state_pb)
            curr_balancer_ver = BalancerVersion.from_pb(l7_pb)
            handler.add_new_rev(curr_balancer_ver)
            handler.set_valid_rev(curr_balancer_ver)
            handler.set_active_rev(curr_balancer_ver)
        zk_storage.create_balancer_state(NS_ID, l7_id, state_pb)
        zk_storage.create_balancer(namespace_id=NS_ID, balancer_id=l7_id, balancer_pb=l7_pb)
        wait_until_passes(lambda: cache.must_get_balancer(NS_ID, l7_id))
        wait_until_passes(lambda: cache.must_get_balancer_state(NS_ID, l7_id))
        l3_pb.spec.real_servers.balancers.add(id=l7_id)
    zk_storage.create_l3_balancer(namespace_id=NS_ID, l3_balancer_id=L3_ID, l3_balancer_pb=l3_pb)
    zk_storage.create_l3_balancer_state(namespace_id=NS_ID, l3_balancer_id=L3_ID, l3_balancer_state_pb=l3_state_pb)
    wait_until_passes(lambda: cache.must_get_l3_balancer(NS_ID, L3_ID))
    wait_until_passes(lambda: cache.must_get_l3_balancer_state(NS_ID, L3_ID))
    return l3_pb


def create_l3_balancer_pb():
    l3_pb = model_pb2.L3Balancer()
    l3_pb.meta.id = NS_ID
    l3_pb.meta.namespace_id = NS_ID
    l3_pb.meta.auth.type = l3_pb.meta.auth.STAFF
    l3_pb.meta.version = u'xxx'
    l3_pb.spec.real_servers.type = model_pb2.L3BalancerRealServersSelector.BALANCERS
    l3_pb.spec.real_servers.backends.add(id=L7_ID)
    l3_pb.spec.config_management_mode = model_pb2.L3BalancerSpec.MODE_REAL_AND_VIRTUAL_SERVERS
    vs_pb = l3_pb.spec.virtual_servers.add(traffic_type=model_pb2.L3BalancerSpec.VirtualServer.TT_EXTERNAL,
                                           ip=u'127.0.0.1',
                                           port=443)
    vs_pb.health_check_settings.url = u'/pong'
    vs_pb.health_check_settings.check_type = model_pb2.L3BalancerSpec.VirtualServer.HealthCheckSettings.CT_SSL_GET
    return l3_pb


def activate_l7(zk_storage, cache, l7_id):
    l7_pb = zk_storage.must_get_balancer(NS_ID, l7_id)
    for b_pb in zk_storage.update_balancer_state(NS_ID, l7_id):
        handler = L7BalancerStateHandler(b_pb)
        curr_balancer_ver = BalancerVersion.from_pb(l7_pb)
        handler.add_new_rev(curr_balancer_ver)
        handler.set_valid_rev(curr_balancer_ver)
        handler.set_active_rev(curr_balancer_ver)
    rev_id = l7_pb.meta.version
    wait_until(lambda: rev_id in (s.revision_id for s in cache.get_balancer_state(NS_ID, l7_id).balancer.statuses))


def activate_l3(zk_storage, cache, l3_id):
    l3_pb = zk_storage.must_get_l3_balancer(NS_ID, l3_id)
    curr_ver = version.L3BalancerVersion.from_pb(l3_pb)
    for state_pb in zk_storage.update_l3_balancer_state(NS_ID, l3_id):
        handler = l3_balancer.L3BalancerStateHandler(state_pb)
        handler._add_version_if_missing(curr_ver)  # noqa
        handler.handle_l3mgr_config_activation([curr_ver])
    r = l3_pb.meta.version
    wait_until(lambda: r in (s.revision_id for s in cache.get_l3_balancer_state(NS_ID, l3_id).l3_balancer.l3_statuses))


def create_operation(l3_pb, operation):
    ns_op_pb = model_pb2.NamespaceOperation()
    ns_op_pb.meta.namespace_id = NS_ID
    ns_op_pb.meta.id = u'ns_op_id'
    ns_op_pb.meta.parent_versions.l3_versions[l3_pb.meta.id] = l3_pb.meta.version
    ns_op_pb.spec.incomplete = True
    if operation == model_pb2.NamespaceOperationOrder.Content.AddIpAddressToL3Balancer:
        content_pb = ns_op_pb.order.content.add_ip_address_to_l3_balancer
        content_pb.ip_version = model_pb2.NamespaceOperationOrder.Content.AddIpAddressToL3Balancer.VER_IPV6
        content_pb.traffic_type = model_pb2.L3BalancerSpec.VirtualServer.TT_INTERNAL
        content_pb.health_check_url = u'/ping'
        content_pb.ports.extend([80, 443])
        rv = AddIpToL3BalancerOp(ns_op_pb)
    elif operation == model_pb2.NamespaceOperationOrder.Content.ImportVirtualServersFromL3mgr:
        content_pb = ns_op_pb.order.content.import_virtual_servers_from_l3mgr
        rv = ImportVsFromL3MgrOp(ns_op_pb)
    else:
        raise AssertionError
    content_pb.l3_balancer_id = l3_pb.meta.id
    objects.NamespaceOperation.zk.create(ns_op_pb)
    wait_until_passes(lambda: objects.NamespaceOperation.cache.must_get(NS_ID, u'ns_op_id'))
    return rv
