import pytest

from awacs.model import events
from awacs.model.dns_records.removal.ctl import DnsRecordRemovalCtl as BaseDnsRecordRemovalCtl
from awacs.model.errors import NotFoundError
from infra.awacs.proto import model_pb2
from awtest.core import wait_until
from .conftest import update_dns_record, wait_dns_record, create_l3_balancer, create_dns_record_pb
from awtest import check_log, wait_until_passes
from awtest.api import create_ns


DNS_RECORD_ID = 'dns-record-id'
NS_ID = 'namespace-id'


class DnsRecordRemovalCtl(BaseDnsRecordRemovalCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(DnsRecordRemovalCtl, self).__init__(*args, **kwargs)
        self._runner.processing_interval = self.PROCESSING_INTERVAL


def create_ctl_with_l3_backend(cache, zk_storage):
    ctl = DnsRecordRemovalCtl(NS_ID, DNS_RECORD_ID)
    ctl._pb = create_dns_record_pb(cache, zk_storage,
                                   backend_type=model_pb2.DnsBackendsSelector.L3_BALANCERS, removed=True)
    return ctl


def create_ctl_with_balancer_backend(cache, zk_storage):
    ctl = DnsRecordRemovalCtl(NS_ID, DNS_RECORD_ID)
    ctl._pb = create_dns_record_pb(cache, zk_storage,
                                   backend_type=model_pb2.DnsBackendsSelector.BALANCERS, removed=True)
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
def test_finished(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    ctl._pb.removal.status.status = 'FINISHED'
    update_dns_record(cache, zk_storage, ctl._pb, check=lambda pb: pb.removal.status.status == 'FINISHED')
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Status is already FINISHED' in log.records_text()


def test_transitions_with_l3_backend(caplog, cache, zk_storage, ctx, dao):
    ns_pb = model_pb2.NameServer()
    ns_pb.meta.namespace_id = 'infra'
    ns_pb.meta.id = 'in.yandex-team.ru'
    ns_pb.spec.type = ns_pb.spec.DNS_MANAGER
    ns_pb.spec.zone = 'in.yandex-team.ru'
    dao.create_name_server_if_missing(ns_pb.meta, ns_pb.spec)
    ctl = create_ctl_with_l3_backend(cache, zk_storage)
    ctl._pb.removal.progress.state.id = 'STARTED'
    update_dns_record(cache, zk_storage, ctl._pb, check=lambda pb: pb.removal.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: STARTED' in log.records_text()
    assert 'Processed, next state: CREATING_DNS_RECORD_OPERATION' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.removal.progress.state.id == 'CREATING_DNS_RECORD_OPERATION')

    create_ns(NS_ID, cache, zk_storage)
    create_l3_balancer(NS_ID, 'l3_backend', cache, zk_storage)
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: CREATING_DNS_RECORD_OPERATION' in log.records_text()
    assert 'Processed, next state: WAITING_FOR_DNS_RECORD_OPERATION' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.removal.progress.state.id == 'WAITING_FOR_DNS_RECORD_OPERATION')
    wait_until_passes(lambda: cache.must_get_dns_record_operation(NS_ID, DNS_RECORD_ID))

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: WAITING_FOR_DNS_RECORD_OPERATION' in log.records_text()
    assert 'Processed, next state: WAITING_FOR_DNS_RECORD_OPERATION' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.removal.progress.state.id == 'WAITING_FOR_DNS_RECORD_OPERATION')

    for dns_record_pb in zk_storage.update_dns_record_operation(NS_ID, DNS_RECORD_ID):
        dns_record_pb.spec.incomplete = False
    assert wait_until(lambda: not cache.must_get_dns_record_operation(NS_ID, DNS_RECORD_ID).spec.incomplete)

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: WAITING_FOR_DNS_RECORD_OPERATION' in log.records_text()
    assert 'Processed, next state: REMOVING_DNS_RECORD' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.removal.progress.state.id == 'REMOVING_DNS_RECORD')

    with pytest.raises(NotFoundError):
        ctl._process(ctx)
    assert wait_until(lambda: cache.get_dns_record(NS_ID, DNS_RECORD_ID) is None)


def test_transitions_with_l7_backend(caplog, cache, zk_storage, ctx, dao):
    dao.create_default_name_servers()
    ctl = create_ctl_with_balancer_backend(cache, zk_storage)
    ctl._pb.removal.progress.state.id = 'STARTED'
    update_dns_record(cache, zk_storage, ctl._pb, check=lambda pb: pb.removal.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Current state: STARTED' in log.records_text()
    assert 'Processed, next state: REMOVING_DNS_RECORD' in log.records_text()
    wait_dns_record(cache, lambda pb: pb.removal.progress.state.id == 'REMOVING_DNS_RECORD')

    with pytest.raises(NotFoundError):
        ctl._process(ctx)
    assert wait_until(lambda: cache.get_dns_record(NS_ID, DNS_RECORD_ID) is None)
