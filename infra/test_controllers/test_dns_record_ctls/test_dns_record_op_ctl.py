import pytest
import flaky

from awacs.model.dao import INFRA_NAMESPACE_ID, DEFAULT_NAME_SERVERS
from awacs.model import events
from awacs.model.dns_records.operations.ctl import DnsRecordOperationCtl as BaseDnsRecordOperationCtl
from infra.awacs.proto import model_pb2
from .conftest import update_dns_record_op, cancel_order, wait_dns_record_op, create_op_modify_addresses_pb, \
    create_dns_record_pb
from awtest import check_log, wait_until_passes
from awtest.api import create_ns


MAX_RUNS = 3
DNS_RECORD_ID = 'dns-record-id'
NS_ID = 'namespace-id'


class DnsRecordOperationCtl(BaseDnsRecordOperationCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(DnsRecordOperationCtl, self).__init__(*args, **kwargs)
        self._runners['modify_addresses'].processing_interval = self.PROCESSING_INTERVAL


@pytest.fixture(autouse=True)
def default_nses(deps, dao, cache):
    dao.create_default_name_servers()
    for ns in DEFAULT_NAME_SERVERS:
        wait_until_passes(lambda: cache.must_get_name_server(INFRA_NAMESPACE_ID, ns))


@pytest.fixture(params=['ctl_for_dns_manager_zone', 'ctl_for_awacs_managed_zone'])
def ctl(request):
    return request.getfixturevalue(request.param)


@pytest.fixture
def ctl_for_dns_manager_zone(cache, zk_storage):
    """Ctl for dns record from nameserver
    which is served by DNS Manager (dns_record.spec.nameserver_full_id -> nameserver.spec.type = "DNS_MANAGER")
    :type cache: awacs.model.cache.AwacsCache
    :type zk_storage: awacs.model.zk.ZkStorage
    :rtype: awacs.model.dns_records.operations.ctl.DnsRecordOperationCtl
    """
    nameserver_full_id = model_pb2.NameServerFullId(namespace_id='infra', id='yandex-team.ru')
    create_dns_record_pb(cache, zk_storage, backend_type=model_pb2.DnsBackendsSelector.L3_BALANCERS, nameserver_full_id=nameserver_full_id)
    _ctl = DnsRecordOperationCtl(NS_ID, DNS_RECORD_ID)
    _ctl._pb = create_op_modify_addresses_pb(cache, zk_storage)
    return _ctl


@pytest.fixture
def ctl_for_awacs_managed_zone(request, cache, zk_storage):
    """Ctl for dns record from nameserver
     which is served by awacs (dns_record.spec.nameserver_full_id -> nameserver.spec.type = "AWACS_MANAGED")
    :type request: _type_
    :type cache: awacs.model.cache.AwacsCache
    :type zk_storage: awacs.model.zk.ZkStorage 
    :rtype: awacs.model.dns_records.operations.ctl.DnsRecordOperationCtl
    """
    nameserver_full_id = model_pb2.NameServerFullId(namespace_id='infra', id='rtc.yandex.net')
    create_dns_record_pb(cache, zk_storage, backend_type=model_pb2.DnsBackendsSelector.L3_BALANCERS, nameserver_full_id=nameserver_full_id)
    _ctl = DnsRecordOperationCtl(NS_ID, DNS_RECORD_ID)
    _ctl._pb = create_op_modify_addresses_pb(cache, zk_storage)
    return _ctl


def test_old_event_generation(caplog, cache, zk_storage, ctx, ctl):
    event = events.DnsRecordOperationUpdate(path='', pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
    assert 'Skipped event with stale generation -1' in log.records_text()
    assert 'Assigned initial state "START"' not in log.records_text()


def test_not_started(caplog, cache, zk_storage, ctx, ctl):
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Assigned initial state "STARTED"' in log.records_text()


def test_finished(caplog, cache, zk_storage, ctx, ctl):
    ctl._pb.order.status.status = 'FINISHED'
    update_dns_record_op(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.status.status == 'FINISHED')
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'Status is already FINISHED' in log.records_text()


@pytest.mark.parametrize('start_state', [
    'STARTED',
    'SENDING_DNS_REQUEST',
    'POLLING_DNS_REQUEST',
])
def test_cancel(caplog, cache, zk_storage, ctx, start_state, ctl):
    cancel_order(ctl._pb, start_state)
    update_dns_record_op(cache, zk_storage, ctl._pb, check=lambda _pb: _pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'order was cancelled by "robot" with comment "cancelled!"' in log.records_text()
    assert 'Processed, next state: CANCELLED' in log.records_text()
    wait_dns_record_op(cache, lambda pb: pb.order.progress.state.id == 'CANCELLED')


@pytest.mark.parametrize('start_state', [
    'CANCELLING',
    'FINISHING',
])
def test_cancel_unsupported(caplog, cache, zk_storage, ctx, start_state, ctl):
    cancel_order(ctl._pb, start_state)
    update_dns_record_op(cache, zk_storage, ctl._pb, check=lambda _pb: _pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
    assert 'order was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
    assert 'cannot be cancelled' in log.records_text()
    wait_dns_record_op(cache, lambda pb: pb.order.progress.state.id == start_state)


@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
def test_transitions_for_awacs_managed_dns_zone(dao, caplog, cache, zk_storage, ctx, ctl_for_awacs_managed_zone):
    """Test transitions for operation for dns records in such zones
    which are served by dns manager nameservers (dns_record.spec.nameserver_full_id -> nameserver.spec.type = "AWACS_MANAGED") 
    :type dao: awacs.model.dao.Dao
    :type caplog: _type_
    :type cache: awacs.model.cache.AwacsCache
    :type zk_storage: awacs.model.zk.ZkStorage 
    :type ctx: context.OpCtx 
    :type ctl_for_dns_manager_zone: awacs.model.dns_records.operations.ctl.DnsRecordOperationCtl
    """

    ctl_for_awacs_managed_zone._pb.order.progress.state.id = 'STARTED'
    update_dns_record_op(cache, zk_storage, ctl_for_awacs_managed_zone._pb, check=lambda pb: pb.order.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl_for_awacs_managed_zone._process(ctx)

    assert 'Current state: STARTED' in log.records_text()
    assert 'Processed, next state: SYNC_DNS_RECORDS_IN_AWACS_MANAGED_ZONE' in log.records_text()
    wait_dns_record_op(cache, lambda pb: pb.order.progress.state.id == 'SYNC_DNS_RECORDS_IN_AWACS_MANAGED_ZONE')

    with check_log(caplog) as log:
        ctl_for_awacs_managed_zone._process(ctx)
    assert 'Current state: SYNC_DNS_RECORDS_IN_AWACS_MANAGED_ZONE' in log.records_text()
    assert 'Processed, next state: FINISHING' in log.records_text()
    wait_dns_record_op(cache, lambda pb: pb.order.progress.state.id == 'FINISHING')

    with check_log(caplog) as log:
        ctl_for_awacs_managed_zone._process(ctx)
    assert 'Current state: FINISHING' in log.records_text()
    assert 'Processed, next state: FINISHED' in log.records_text()
    wait_dns_record_op(cache, lambda pb: pb.order.progress.state.id == 'FINISHED')


@flaky.flaky(max_runs=MAX_RUNS, min_passes=1)
def test_transitions_for_dns_manager_dns_zone(dao, caplog, cache, zk_storage, ctx, ctl_for_dns_manager_zone):
    """Test transitions for operation for dns records in such zones
    which are served by dns manager nameservers (dns_record.spec.nameserver_full_id -> nameserver.spec.type = "DNS_MANAGER") 
    :type dao: awacs.model.dao.Dao
    :type caplog: _type_
    :type cache::type cache: awacs.model.cache.AwacsCache
    :type zk_storage: awacs.model.zk.ZkStorage 
    :type ctx: context.OpCtx 
    :type ctl_for_dns_manager_zone: awacs.model.dns_records.operations.ctl.DnsRecordOperationCtl
    """

    ctl_for_dns_manager_zone._pb.order.progress.state.id = 'STARTED'
    update_dns_record_op(cache, zk_storage, ctl_for_dns_manager_zone._pb, check=lambda pb: pb.order.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl_for_dns_manager_zone._process(ctx)
    assert 'Current state: STARTED' in log.records_text()
    assert 'Processed, next state: SENDING_DNS_REQUEST' in log.records_text()
    wait_dns_record_op(cache, lambda pb: pb.order.progress.state.id == 'SENDING_DNS_REQUEST')

    with check_log(caplog) as log:
        ctl_for_dns_manager_zone._process(ctx)
    assert 'Current state: SENDING_DNS_REQUEST' in log.records_text()
    assert 'Processed, next state: POLLING_DNS_REQUEST' in log.records_text()
    wait_dns_record_op(cache, lambda pb: pb.order.progress.state.id == 'POLLING_DNS_REQUEST')

    with check_log(caplog) as log:
        ctl_for_dns_manager_zone._process(ctx)
    assert 'Current state: POLLING_DNS_REQUEST' in log.records_text()
    assert 'Processed, next state: FINISHING' in log.records_text()
    wait_dns_record_op(cache, lambda pb: pb.order.progress.state.id == 'FINISHING')
    wait_until_passes(lambda: cache.must_get_dns_record_operation(NS_ID, DNS_RECORD_ID))

    with check_log(caplog) as log:
        ctl_for_dns_manager_zone._process(ctx)
    assert 'Current state: FINISHING' in log.records_text()
    assert 'Processed, next state: FINISHED' in log.records_text()
    wait_dns_record_op(cache, lambda pb: pb.order.progress.state.id == 'FINISHED')
