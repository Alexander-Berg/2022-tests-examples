import logging

import inject
import pytest

from awacs.lib import l3mgrclient, dns_resolver, dns_manager_client
from awacs.model.dns_records.operations.modify_addresses import ModifyAddressesOperation
from awacs.model.dns_records.order.processors import DnsRecordOrder
from awacs.model.dns_records.removal.processors import DnsRecordRemoval
from infra.awacs.proto import model_pb2
from awtest import wait_until
from awtest.pb import cancel_order  # noqa


DNS_RECORD_ID = 'dns-record-id'
NS_ID = 'namespace-id'
LOGIN = 'robot'


@pytest.fixture(autouse=True)
def deps(binder, caplog, l3_mgr_client, dns_resolver_mock, dns_manager_client_mock):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(l3mgrclient.IL3MgrClient, l3_mgr_client)
        b.bind(dns_resolver.IDnsResolver, dns_resolver_mock)
        b.bind(dns_manager_client.IDnsManagerClient, dns_manager_client_mock)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_dns_record_order_pb(cache, zk_storage, backend_type, name_server_full_id=None):
    """
    :type cache: awacs.model.cache.AwacsCache
    :type zk_storage: awacs.model.zk.ZkStorage
    :type backend_type: model_pb2.DnsBackendsSelector.Type 
    :type name_server_full_id: model_pb2.NameServerFullId, optional
    :rtype: model_pb2.DnsRecordOrder
    """
    if name_server_full_id is None:
        name_server_full_id = model_pb2.NameServerFullId(namespace_id='infra', id='in.yandex-team.ru')
    meta = model_pb2.DnsRecordMeta(id=DNS_RECORD_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    meta.author = LOGIN
    dns_record_pb = model_pb2.DnsRecord(meta=meta)
    dns_record_pb.spec.incomplete = True
    dns_record_pb.order.content.name_server.namespace_id = name_server_full_id.namespace_id
    dns_record_pb.order.content.name_server.id = name_server_full_id.id
    dns_record_pb.order.content.address.zone = 'zzz'
    dns_record_pb.order.content.address.backends.type = backend_type
    if backend_type == model_pb2.DnsBackendsSelector.L3_BALANCERS:
        dns_record_pb.order.content.address.backends.l3_balancers.add(id='l3_backend')
    else:
        dns_record_pb.order.content.address.backends.balancers.add(id='l7_backend')
    zk_storage.create_dns_record(namespace_id=NS_ID,
                                 dns_record_id=DNS_RECORD_ID,
                                 dns_record_pb=dns_record_pb)
    wait_dns_record(cache, lambda pb: pb)
    return dns_record_pb


def create_dns_record_order(cache, zk_storage, backend_type):
    return DnsRecordOrder(create_dns_record_order_pb(cache, zk_storage, backend_type))


def create_dns_record_pb(cache, zk_storage, backend_type, removed=False, nameserver_full_id=None):
    if nameserver_full_id is None:
        nameserver_full_id = model_pb2.NameServerFullId(namespace_id='infra', id='in.yandex-team.ru')
    meta = model_pb2.DnsRecordMeta(id=DNS_RECORD_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    dns_record_pb = model_pb2.DnsRecord(meta=meta)
    dns_record_pb.spec.incomplete = False
    dns_record_pb.spec.deleted = removed
    dns_record_pb.spec.name_server.namespace_id = nameserver_full_id.namespace_id
    dns_record_pb.spec.name_server.id = nameserver_full_id.id
    dns_record_pb.spec.address.zone = 'zzz'
    dns_record_pb.spec.address.backends.type = backend_type
    if backend_type == model_pb2.DnsBackendsSelector.L3_BALANCERS:
        dns_record_pb.spec.address.backends.l3_balancers.add(id='l3_backend')
    else:
        dns_record_pb.spec.address.backends.balancers.add(id='l7_backend')
    zk_storage.create_dns_record(namespace_id=NS_ID,
                                 dns_record_id=DNS_RECORD_ID,
                                 dns_record_pb=dns_record_pb)
    wait_dns_record(cache, lambda pb: pb)
    return dns_record_pb


def create_dns_record_removal(cache, zk_storage, backend_type):
    return DnsRecordRemoval(create_dns_record_pb(cache, zk_storage, backend_type, removed=True))


def update_dns_record(cache, zk_storage, dns_record_pb, check):
    for pb in zk_storage.update_dns_record(NS_ID, DNS_RECORD_ID):
        pb.CopyFrom(dns_record_pb)
    wait_dns_record(cache, check)


def wait_dns_record(cache, check):
    assert wait_until(lambda: check(cache.get_dns_record(NS_ID, DNS_RECORD_ID)))


def create_l3_balancer(ns_id, l3_balancer_id, cache, zk_storage):
    l3_balancer_pb = model_pb2.L3Balancer()
    l3_balancer_pb.meta.id = l3_balancer_id
    l3_balancer_pb.meta.namespace_id = ns_id
    l3_balancer_pb.spec.l3mgr_service_id = 'test_svc'
    zk_storage.create_l3_balancer(namespace_id=NS_ID,
                                  l3_balancer_id=l3_balancer_id,
                                  l3_balancer_pb=l3_balancer_pb)
    assert wait_until(lambda: cache.must_get_l3_balancer(ns_id, l3_balancer_id))
    return l3_balancer_pb


def create_op_modify_addresses_pb(cache, zk_storage):
    meta = model_pb2.DnsRecordOperationMeta(id=DNS_RECORD_ID, namespace_id=NS_ID, dns_record_id=DNS_RECORD_ID)
    meta.mtime.GetCurrentTime()
    meta.author = LOGIN
    op_pb = model_pb2.DnsRecordOperation(meta=meta)
    op_pb.spec.incomplete = True
    op_pb.order.content.modify_addresses.requests.add(
        address='127.0.0.1',
        action=model_pb2.DnsRecordOperationOrder.Content.ModifyAddresses.Request.CREATE)
    op_pb.order.content.modify_addresses.requests.add(
        address='1050:0:0:0:5:600:300c:326b',
        action=model_pb2.DnsRecordOperationOrder.Content.ModifyAddresses.Request.REMOVE)
    zk_storage.create_dns_record_operation(namespace_id=NS_ID,
                                           dns_record_op_id=DNS_RECORD_ID,
                                           dns_record_op_pb=op_pb)
    wait_dns_record_op(cache, lambda pb: pb)
    return op_pb


def create_op_modify_addresses(cache, zk_storage):
    return ModifyAddressesOperation(create_op_modify_addresses_pb(cache, zk_storage))


def update_dns_record_op(cache, zk_storage, dns_record_op_pb, check):
    for pb in zk_storage.update_dns_record_operation(NS_ID, DNS_RECORD_ID):
        pb.CopyFrom(dns_record_op_pb)
    wait_dns_record_op(cache, check)


def wait_dns_record_op(cache, check):
    assert wait_until(lambda: check(cache.get_dns_record_operation(NS_ID, DNS_RECORD_ID)))
