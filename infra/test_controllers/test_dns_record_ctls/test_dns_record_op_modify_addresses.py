# coding: utf-8
import pytest

from awacs.lib.order_processor.model import FeedbackMessage
from awacs.model.dns_records.operations import modify_addresses as p
from infra.awacs.proto import model_pb2
from awtest.core import wait_until_passes
from .conftest import create_op_modify_addresses, create_dns_record_order_pb, create_dns_record_pb
from awacs.model.dao import INFRA_NAMESPACE_ID, DEFAULT_NAME_SERVERS


DNS_RECORD_ID = 'dns-record-id'
NS_ID = 'namespace-id'


@pytest.fixture(autouse=True)
def default_nses(deps, dao, cache):
    dao.create_default_name_servers()
    for ns in DEFAULT_NAME_SERVERS:
        wait_until_passes(lambda: cache.must_get_name_server(INFRA_NAMESPACE_ID, ns))


@pytest.fixture(params=["rtc.yandex.net", "rtc.yandex-team.ru"])
def awacs_managed_nameserver_id(request):
    return request.param


@pytest.fixture()
def _create_dns_record_pb_order_in_awacs_managed_zone(awacs_managed_nameserver_id, cache, zk_storage):
    nameserver_full_id = model_pb2.NameServerFullId(namespace_id='infra', id=awacs_managed_nameserver_id)
    create_dns_record_order_pb(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS, nameserver_full_id)


@pytest.fixture
def _create_dns_record_order_pb_in_dns_manager_zone(cache, zk_storage):
    nameserver_full_id = model_pb2.NameServerFullId(namespace_id='infra', id='yandex-team.ru')
    create_dns_record_order_pb(cache, zk_storage, model_pb2.DnsBackendsSelector.L3_BALANCERS, nameserver_full_id)


def test_started_in_awacs_managed_zone(_create_dns_record_pb_order_in_awacs_managed_zone, cache, zk_storage, ctx):
    op = create_op_modify_addresses(cache, zk_storage)
    assert p.Started(op).process(ctx).name == 'SYNC_DNS_RECORDS_IN_AWACS_MANAGED_ZONE'


def test_started_in_dns_manager_zone(_create_dns_record_order_pb_in_dns_manager_zone, cache, zk_storage, ctx):
    op = create_op_modify_addresses(cache, zk_storage)
    assert p.Started(op).process(ctx).name == 'SENDING_DNS_REQUEST'


def test_sending_dns_request(_create_dns_record_order_pb_in_dns_manager_zone, ctx, dao, cache, zk_storage,
                             dns_manager_client_mock, dns_resolver_mock):
    op = create_op_modify_addresses(cache, zk_storage)

    dns_manager_client_mock.create_request.reset_mock()
    prev_side_effect = dns_resolver_mock.get_address_record.side_effect
    ip_of_dns_record_to_remove = u"1050:0:0:0:5:600:300c:326b"
    dns_resolver_mock.get_address_record.side_effect = lambda *a, **kwa: {u'127.0.0.2', ip_of_dns_record_to_remove}
    try:
        assert p.SendingDnsRequest(op).process(ctx).name == 'POLLING_DNS_REQUEST'
        assert op.context[u'request_id'] == u'test-req-id'
        dns_manager_client_mock.create_request.assert_called_once_with(
            {
                u'requests': [
                    {
                        u'operation': u'create',
                        u'resource': {u'type': 'A', u'data': u'127.0.0.1', u'fqdn': u'zzz.yandex-team.ru.', u'ttl': 300}
                    },
                    {
                        u'operation': u'remove',
                        u'resource': {u'type': 'AAAA', u'data': ip_of_dns_record_to_remove, u'fqdn': u'zzz.yandex-team.ru.', u'ttl': 300}
                    }
                ],
                u'requester': u'robot',
            })
    finally:
        dns_resolver_mock.get_address_record.side_effect = prev_side_effect


def test_sync_dns_records_in_awacs_managed_zone(_create_dns_record_pb_order_in_awacs_managed_zone, ctx, dao, cache,
                                                zk_storage, dns_manager_client_mock, awacs_managed_nameserver_id):
    op = create_op_modify_addresses(cache, zk_storage)
    assert p.SyncDnsRecordsInAwacsManagedZone(op).process(ctx).name == 'FINISHING'
    dns_manager_client_mock.add_record.assert_called_once_with(
        u'zzz.{}.'.format(awacs_managed_nameserver_id),
        'A',
        300,
        u'127.0.0.1',
    )
    dns_manager_client_mock.remove_record.assert_called_once_with(
        u'zzz.{}.'.format(awacs_managed_nameserver_id),
        u'AAAA',
        u'1050:0:0:0:5:600:300c:326b',
    )


def test_polling_dns_request(_create_dns_record_order_pb_in_dns_manager_zone, ctx, dao, cache, zk_storage, dns_manager_client_mock):
    op = create_op_modify_addresses(cache, zk_storage)
    op.context[u'request_id'] = u'test-req-id'

    prev_side_effect = dns_manager_client_mock.get_request_status.side_effect
    dns_manager_client_mock.get_request_status.reset_mock()
    try:
        dns_manager_client_mock.get_request_status.side_effect = lambda *a, **kwa: {u'meta': {u'state': u'new'}}
        assert p.PollingDnsRequest(op).process(ctx).name == 'POLLING_DNS_REQUEST'
        dns_manager_client_mock.get_request_status.assert_called_once_with(u'test-req-id')

        dns_manager_client_mock.get_request_status.side_effect = lambda *a, **kwa: {u'meta': {u'state': u'error', u'error': u'err'}}
        rv = p.PollingDnsRequest(op).process(ctx)
        assert isinstance(rv, FeedbackMessage)
        assert rv.message == u'err'

        dns_manager_client_mock.get_request_status.side_effect = lambda *a, **kwa: {u'meta': {u'state': u'done'}}
        assert p.PollingDnsRequest(op).process(ctx).name == 'FINISHING'

        dns_manager_client_mock.get_request_status.side_effect = lambda *a, **kwa: {u'meta': {}}
        with pytest.raises(RuntimeError, match=u'Unknown DNS Manager request state "None"'):
            p.PollingDnsRequest(op).process(ctx)
    finally:
        dns_manager_client_mock.get_request_status.side_effect = prev_side_effect


def test_finishing(cache, zk_storage, ctx):
    op = create_op_modify_addresses(cache, zk_storage)
    assert p.Finishing(op).process(ctx).name == 'FINISHED'
    assert not op.pb.spec.incomplete


def test_cancelling_in_dns_manager_zone(_create_dns_record_order_pb_in_dns_manager_zone, cache, zk_storage, ctx, dns_manager_client_mock):
    op = create_op_modify_addresses(cache, zk_storage)
    op.pb.spec.incomplete = True
    op.context[u'request_id'] = u'test-req-id'
    assert p.Cancelling(op).process(ctx).name == 'CANCELLED'
    assert not op.pb.spec.incomplete
    dns_manager_client_mock.cancel_request.assert_called_once_with(u'test-req-id')


def test_cancelling_in_awacs_managed_zone(_create_dns_record_pb_order_in_awacs_managed_zone, cache, zk_storage, ctx, dns_manager_client_mock):
    op = create_op_modify_addresses(cache, zk_storage)
    op.pb.spec.incomplete = True
    assert p.Cancelling(op).process(ctx).name == 'CANCELLED'
    assert not op.pb.spec.incomplete
