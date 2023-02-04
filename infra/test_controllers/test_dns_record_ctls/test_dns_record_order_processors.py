# coding: utf-8
import pytest

from awacs.model.dns_records.order import processors as p
from awacs.model.errors import NotFoundError
from infra.awacs.proto import model_pb2
from awtest.core import wait_until, wait_until_passes
from .conftest import create_l3_balancer, create_dns_record_order


DNS_RECORD_ID = 'dns-record-id'
NS_ID = 'namespace-id'


def test_start_l3_backend(cache, zk_storage, ctx, dao):
    ns_pb = model_pb2.NameServer()
    ns_pb.meta.namespace_id = 'infra'
    ns_pb.meta.id = 'in.yandex-team.ru'
    ns_pb.spec.type = ns_pb.spec.DNS_MANAGER
    ns_pb.spec.zone = 'in.yandex-team.ru'
    dao.create_name_server_if_missing(ns_pb.meta, ns_pb.spec)
    domain = create_dns_record_order(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS)
    assert p.Started(domain).process(ctx).name == 'GETTING_L3_IP_ADDRESSES'


def test_start_l7_backend(cache, zk_storage, ctx, dao):
    dao.create_default_name_servers()
    dns_record = create_dns_record_order(cache, zk_storage, model_pb2.DnsBackendsSelector.BALANCERS)
    assert p.Started(dns_record).process(ctx).name == 'SAVING_SPEC'


def test_getting_l3_ip_addresses(ctx, cache, zk_storage, l3_mgr_client):
    l3_mgr_client.awtest_set_default_config()
    dns_record = create_dns_record_order(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS)
    with pytest.raises(NotFoundError, match='L3 balancer "namespace-id:l3_backend" not found'):
        p.GettingL3IpAddresses(dns_record).process(ctx)
    create_l3_balancer(NS_ID, 'l3_backend', cache, zk_storage)
    assert p.GettingL3IpAddresses(dns_record).process(ctx).name == 'CREATING_DNS_RECORD_OPERATION'
    assert dns_record.context[u'l3_ip_addresses'] == [u'2a02:6b8:0:3400:0:2da:0:2']


def test_creating_dns_record_op(ctx, cache, zk_storage):
    dns_record = create_dns_record_order(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS)
    dns_record.context[u'l3_ip_addresses'] = ['127.0.0.1', '::1']
    assert p.CreatingDnsRecordOperation(dns_record).process(ctx).name == 'WAITING_FOR_DNS_RECORD_OPERATION'
    dns_record_op_pb = wait_until(lambda: cache.must_get_dns_record_operation(NS_ID, DNS_RECORD_ID))
    assert len(dns_record_op_pb.order.content.modify_addresses.requests) == 2
    req_1 = dns_record_op_pb.order.content.modify_addresses.requests[0]
    assert req_1.address == '127.0.0.1'
    assert req_1.action == model_pb2.DnsRecordOperationOrder.Content.ModifyAddresses.Request.CREATE
    req_1 = dns_record_op_pb.order.content.modify_addresses.requests[1]
    assert req_1.address == '::1'
    assert req_1.action == model_pb2.DnsRecordOperationOrder.Content.ModifyAddresses.Request.CREATE


def test_waiting_for_dns_record_op(ctx, cache, zk_storage):
    dns_record = create_dns_record_order(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS)
    assert p.WaitingForDnsRecordOperation(dns_record).process(ctx).name == 'SAVING_SPEC'


@pytest.mark.parametrize('backend_type', (
    model_pb2.DnsBackendsSelector.BALANCERS,
    model_pb2.DnsBackendsSelector.L3_BALANCERS
))
def test_saving_spec(ctx, cache, zk_storage, backend_type):
    dns_record = create_dns_record_order(cache, zk_storage, backend_type)
    assert p.SavingSpec(dns_record).process(ctx).name == 'FINISHED'
    assert wait_until(lambda: not cache.must_get_dns_record(NS_ID, DNS_RECORD_ID).spec.incomplete)
    dns_record_pb = cache.must_get_dns_record(NS_ID, DNS_RECORD_ID)
    assert dns_record_pb.spec.address == dns_record.pb.order.content.address
    assert dns_record_pb.spec.type == dns_record.pb.order.content.type
    assert dns_record_pb.spec.ctl_version == dns_record.pb.order.content.ctl_version
    assert dns_record_pb.spec.name_server == dns_record.pb.order.content.name_server


def test_cancelling(ctx, cache, zk_storage):
    op_pb = model_pb2.DnsRecordOperation()
    op_pb.meta.id = DNS_RECORD_ID
    op_pb.meta.namespace_id = NS_ID
    zk_storage.create_dns_record_operation(NS_ID, DNS_RECORD_ID, op_pb)
    wait_until_passes(lambda: cache.must_get_dns_record_operation(NS_ID, DNS_RECORD_ID))
    dns_record = create_dns_record_order(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS)
    assert p.Cancelling(dns_record).process(ctx).name == 'CANCELLED'
    wait_until_passes(lambda: cache.must_get_dns_record_operation(NS_ID, DNS_RECORD_ID)
                      .order.status.status == 'CANCELLED')
