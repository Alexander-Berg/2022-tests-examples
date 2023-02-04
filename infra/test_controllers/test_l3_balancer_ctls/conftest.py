import logging

import inject
import pytest

from awacs.lib import l3mgrclient
from awacs.model.balancer.state_handler import L7BalancerStateHandler
from awacs.model.balancer.vector import BalancerVersion
from awacs.model.l3_balancer.order.processors import L3BalancerOrder
from awtest import wait_until, wait_until_passes
from infra.awacs.proto import model_pb2


L3_BALANCER_ID = u'l3-id'
NS_ID = u'namespace-id'
LOGIN = u'robot'


@pytest.fixture(autouse=True)
def deps(binder, caplog, l3_mgr_client, dns_resolver_mock, dns_manager_client_mock):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(l3mgrclient.IL3MgrClient, l3_mgr_client)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_l3_order_pb(cache, zk_storage,
                       traffic_type=model_pb2.L3BalancerOrder.Content.EXTERNAL,
                       protocol=model_pb2.L3BalancerOrder.Content.HTTP):
    meta = model_pb2.L3BalancerMeta(id=L3_BALANCER_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    meta.author = LOGIN
    l3_balancer_pb = model_pb2.L3Balancer(meta=meta)
    l3_balancer_pb.spec.incomplete = True
    l3_balancer_pb.order.content.traffic_type = traffic_type
    l3_balancer_pb.order.content.protocol = protocol
    zk_storage.create_l3_balancer(namespace_id=NS_ID,
                                  l3_balancer_id=L3_BALANCER_ID,
                                  l3_balancer_pb=l3_balancer_pb)
    wait_l3_balancer(cache, lambda pb: pb)
    return l3_balancer_pb


def create_l3_balancer_order(cache, zk_storage,
                             traffic_type=model_pb2.L3BalancerOrder.Content.EXTERNAL,
                             protocol=model_pb2.L3BalancerOrder.Content.HTTP):
    return L3BalancerOrder(create_l3_order_pb(cache, zk_storage, traffic_type, protocol))


@pytest.fixture
def l3_balancer_order(cache, zk_storage):
    return create_l3_balancer_order(cache, zk_storage)


def update_l3_balancer(cache, zk_storage, l3_balancer_pb, check):
    for pb in zk_storage.update_l3_balancer(NS_ID, L3_BALANCER_ID):
        pb.CopyFrom(l3_balancer_pb)
    wait_l3_balancer(cache, check)


def wait_l3_balancer(cache, check):
    assert wait_until(lambda: check(cache.get_l3_balancer(NS_ID, L3_BALANCER_ID)))


def create_l7_balancer(cache, zk_storage, balancer_id=u'xxx', announce_check=True, announce_check_url=u'/ping'):
    b_pb = model_pb2.Balancer()
    b_pb.meta.id = balancer_id
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.location.type = model_pb2.BalancerMeta.Location.YP_CLUSTER
    b_pb.meta.location.yp_cluster = u'SAS'
    b_pb.meta.version = u'999'
    b_pb.spec.yandex_balancer.config.l7_macro.SetInParent()
    if announce_check:
        b_pb.spec.yandex_balancer.config.l7_macro.announce_check_reply.url_re = announce_check_url
    zk_storage.create_balancer(namespace_id=NS_ID,
                               balancer_id=balancer_id,
                               balancer_pb=b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, balancer_id))
    return b_pb


def create_l7_balancer_state(cache, zk_storage, balancer_id):
    state_pb = model_pb2.BalancerState()
    state_pb.namespace_id = NS_ID
    state_pb.balancer_id = balancer_id
    zk_storage.create_balancer_state(NS_ID, balancer_id, state_pb)
    wait_until_passes(lambda: cache.must_get_balancer_state(NS_ID, balancer_id))


def create_active_l3_state(cache, zk_storage):
    l3_state_pb = model_pb2.L3BalancerState(l3_balancer_id=L3_BALANCER_ID, namespace_id=NS_ID)
    rev_pb = l3_state_pb.l3_balancer.l3_statuses.add(revision_id=u'xxx')
    rev_pb.active.status = u'True'
    zk_storage.create_l3_balancer_state(NS_ID, L3_BALANCER_ID, l3_state_pb)
    wait_until(lambda: cache.must_get_l3_balancer_state(NS_ID, L3_BALANCER_ID).l3_balancer.l3_statuses[0].active.status)


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
