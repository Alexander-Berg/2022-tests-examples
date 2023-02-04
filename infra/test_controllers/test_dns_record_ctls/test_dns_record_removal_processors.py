# coding: utf-8
import pytest

from awacs.model.dns_records.removal import processors as p
from infra.awacs.proto import model_pb2
from awtest.core import wait_until
from .conftest import create_dns_record_removal, create_l3_balancer


DNS_RECORD_ID = 'dns-record-id'
NS_ID = 'namespace-id'


def test_start_l3_backend(cache, zk_storage, ctx, dao):
    ns_pb = model_pb2.NameServer()
    ns_pb.meta.namespace_id = 'infra'
    ns_pb.meta.id = 'in.yandex-team.ru'
    ns_pb.spec.type = ns_pb.spec.DNS_MANAGER
    ns_pb.spec.zone = 'in.yandex-team.ru'
    dao.create_name_server_if_missing(ns_pb.meta, ns_pb.spec)
    dns_record = create_dns_record_removal(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS)
    assert p.Started(dns_record).process(ctx).name == 'CREATING_DNS_RECORD_OPERATION'


def test_start_l7_backend(cache, zk_storage, ctx, dao):
    dao.create_default_name_servers()
    dns_record = create_dns_record_removal(cache, zk_storage, model_pb2.DnsBackendsSelector.BALANCERS)
    assert p.Started(dns_record).process(ctx).name == 'REMOVING_DNS_RECORD'


def test_creating_dns_record_op(ctx, dao, cache, zk_storage, dns_resolver_mock):
    dao.create_default_name_servers()
    dns_resolver_mock.get_address_record.return_value = {u'127.0.0.1'}
    dns_record = create_dns_record_removal(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS)
    create_l3_balancer(NS_ID, 'l3_backend', cache, zk_storage)
    assert p.CreatingDnsRecordOperation(dns_record).process(ctx).name == 'WAITING_FOR_DNS_RECORD_OPERATION'
    dns_record_op_pb = wait_until(lambda: cache.must_get_dns_record_operation(NS_ID, DNS_RECORD_ID))
    assert len(dns_record_op_pb.order.content.modify_addresses.requests) == 2  # one A and one AAAA
    req_1 = dns_record_op_pb.order.content.modify_addresses.requests[0]
    assert req_1.address == '127.0.0.1'
    assert req_1.action == model_pb2.DnsRecordOperationOrder.Content.ModifyAddresses.Request.REMOVE


def test_waiting_for_dns_record_op(ctx, cache, zk_storage):
    dns_record = create_dns_record_removal(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS)
    assert p.WaitingForDnsRecordOperation(dns_record).process(ctx).name == 'REMOVING_DNS_RECORD'


@pytest.mark.parametrize('backend_type', (
    model_pb2.DnsBackendsSelector.BALANCERS,
    model_pb2.DnsBackendsSelector.L3_BALANCERS
))
def test_removing_dns_record(ctx, cache, zk_storage, backend_type):
    dns_record = create_dns_record_removal(cache, zk_storage, backend_type)
    dns_record.pb.spec.deleted = True
    assert p.RemovingDnsRecord(dns_record).process(ctx).name == 'FINISHED'
    assert wait_until(lambda: cache.get_dns_record(NS_ID, DNS_RECORD_ID) is None)
