import pytest

from awacs.model import events
from awacs.model.dns_records.order.ctl import DnsRecordOrderCtl as BaseDnsRecordOrderCtl
from infra.awacs.proto import model_pb2
from .conftest import update_dns_record, cancel_order, wait_dns_record, create_l3_balancer, create_dns_record_order_pb
from awtest import wait_until, check_log, wait_until_passes
from awtest.api import create_ns

DNS_RECORD_ID = 'dns-record-id'
NS_ID = 'namespace-id'


class DnsRecordOrderCtl(BaseDnsRecordOrderCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(DnsRecordOrderCtl, self).__init__(*args, **kwargs)
        self._runner.processing_interval = self.PROCESSING_INTERVAL


def create_ctl_with_l3_backend(cache, zk_storage):
    ctl = DnsRecordOrderCtl(NS_ID, DNS_RECORD_ID)
    ctl._pb = create_dns_record_order_pb(cache, zk_storage, backend_type=model_pb2.DnsBackendsSelector.L3_BALANCERS)
    return ctl


def create_ctl_with_balancer_backend(cache, zk_storage):
    ctl = DnsRecordOrderCtl(NS_ID, DNS_RECORD_ID)
    ctl._pb = create_dns_record_order_pb(cache, zk_storage, backend_type=model_pb2.DnsBackendsSelector.BALANCERS)
    return ctl


@pytest.mark.parametrize('ctl', [
    create_ctl_with_l3_backend,
    create_ctl_with_balancer_backend,
])
def test_old_event_generation(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    event = events.DnsRecordUpdate(path='', pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
    assert 'Skipped event with stale generation -1' in log.records_text()
    assert 'Assigned initial state "START"' not in log.records_text()


@pytest.mark.parametrize('ctl', [
    create_ctl_with_l3_backend,
    create_ctl_with_balancer_backend,
])
def test_not_started(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Assigned initial state "STARTED"' in log.records_text()


@pytest.mark.parametrize('ctl', [
    create_ctl_with_l3_backend,
    create_ctl_with_balancer_backend,
])
def test_finished(caplog, cache, zk_storage, ctx, ctl, log):
    ctl = ctl(cache, zk_storage)
    ctl._pb.order.status.status = 'FINISHED'
    update_dns_record(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.status.status == 'FINISHED')
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Status is already FINISHED' in log.records_text()


@pytest.mark.parametrize('start_state', [
    'STARTED',
    'GETTING_L3_IP_ADDRESSES',
    'CREATING_DNS_RECORD_OPERATION',
    'WAITING_FOR_DNS_RECORD_OPERATION',
])
@pytest.mark.parametrize('ctl', [
    create_ctl_with_l3_backend,
    create_ctl_with_balancer_backend,
])
def test_cancel(caplog, cache, zk_storage, ctx, start_state, ctl):
    ctl = ctl(cache, zk_storage)
    cancel_order(ctl._pb, start_state)
    update_dns_record(cache, zk_storage, ctl._pb, check=lambda _pb: _pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'order was cancelled by "robot" with comment "cancelled!"' in log.records_text()
    assert 'Processed, next state: CANCELLED' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == 'CANCELLED')


@pytest.mark.parametrize('start_state', [
    'CANCELLING',
    'SAVING_SPEC',
])
@pytest.mark.parametrize('ctl', [
    create_ctl_with_l3_backend,
    create_ctl_with_balancer_backend,
])
def test_cancel_unsupported(caplog, cache, zk_storage, ctx, start_state, ctl):
    ctl = ctl(cache, zk_storage)
    cancel_order(ctl._pb, start_state)
    update_dns_record(cache, zk_storage, ctl._pb, check=lambda _pb: _pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'order was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
    assert 'cannot be cancelled' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == start_state)


def test_transitions_with_l3_backend(caplog, cache, zk_storage, ctx, l3_mgr_client, dao):
    ns_pb = model_pb2.NameServer()
    ns_pb.meta.namespace_id = 'infra'
    ns_pb.meta.id = 'in.yandex-team.ru'
    ns_pb.spec.type = ns_pb.spec.DNS_MANAGER
    ns_pb.spec.zone = 'in.yandex-team.ru'
    dao.create_name_server_if_missing(ns_pb.meta, ns_pb.spec)
    l3_mgr_client.awtest_set_default_config()
    ctl = create_ctl_with_l3_backend(cache, zk_storage)
    ctl._pb.order.progress.state.id = 'STARTED'
    update_dns_record(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: STARTED' in log.records_text()
    assert 'Processed, next state: GETTING_L3_IP_ADDRESSES' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == 'GETTING_L3_IP_ADDRESSES')

    create_ns(NS_ID, cache, zk_storage)
    create_l3_balancer(NS_ID, 'l3_backend', cache, zk_storage)
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: GETTING_L3_IP_ADDRESSES' in log.records_text()
    assert 'Processed, next state: CREATING_DNS_RECORD_OPERATION' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == 'CREATING_DNS_RECORD_OPERATION')

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: CREATING_DNS_RECORD_OPERATION' in log.records_text()
    assert 'Processed, next state: WAITING_FOR_DNS_RECORD_OPERATION' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_DNS_RECORD_OPERATION')
    wait_until_passes(lambda: cache.must_get_dns_record_operation(NS_ID, DNS_RECORD_ID))

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: WAITING_FOR_DNS_RECORD_OPERATION' in log.records_text()
    assert 'Processed, next state: WAITING_FOR_DNS_RECORD_OPERATION' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_DNS_RECORD_OPERATION')

    for dns_record_pb in zk_storage.update_dns_record_operation(NS_ID, DNS_RECORD_ID):
        dns_record_pb.spec.incomplete = False
    assert wait_until(lambda: not cache.must_get_dns_record_operation(NS_ID, DNS_RECORD_ID).spec.incomplete)

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: WAITING_FOR_DNS_RECORD_OPERATION' in log.records_text()
    assert 'Processed, next state: SAVING_SPEC' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == 'SAVING_SPEC')

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: SAVING_SPEC' in log.records_text()
    assert 'Processed, next state: FINISHED' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == 'FINISHED')


def test_transitions_with_l7_backend(caplog, cache, zk_storage, ctx, dao):
    dao.create_default_name_servers()
    ctl = create_ctl_with_balancer_backend(cache, zk_storage)
    ctl._pb.order.progress.state.id = 'STARTED'
    update_dns_record(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: STARTED' in log.records_text()
    assert 'Processed, next state: SAVING_SPEC' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == 'SAVING_SPEC')

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: SAVING_SPEC' in log.records_text()
    assert 'Processed, next state: FINISHED' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.order.progress.state.id == 'FINISHED')
