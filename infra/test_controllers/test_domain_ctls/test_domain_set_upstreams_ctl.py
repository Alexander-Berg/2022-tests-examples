import logging

import inject
import pytest

from awacs.model import events
from awacs.model.domain.operations.ctl import DomainOperationCtl as BaseDomainOperationCtl
from infra.awacs.proto import model_pb2, modules_pb2
from awtest import wait_until, check_log, wait_until_passes


DOMAIN_ID = 'domain-id'
NS_ID = 'namespace-id'


class DomainOperationCtl(BaseDomainOperationCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(DomainOperationCtl, self).__init__(*args, **kwargs)
        self._runners['set_upstreams'].processing_interval = self.PROCESSING_INTERVAL


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def domain(zk_storage, cache):
    domain_pb = model_pb2.Domain()
    domain_pb.meta.id = DOMAIN_ID
    domain_pb.meta.namespace_id = NS_ID
    zk_storage.create_domain(NS_ID, DOMAIN_ID, domain_pb)
    wait_until_passes(lambda: cache.get_domain(NS_ID, DOMAIN_ID))


def create_ctl_with_all(cache, zk_storage):
    pb = create_set_upstreams_all_pb(cache, zk_storage)
    ctl = DomainOperationCtl(NS_ID, DOMAIN_ID)
    ctl._pb = pb
    return ctl


def create_ctl_by_ids(cache, zk_storage):
    pb = create_set_upstreams_by_ids_pb(cache, zk_storage)
    ctl = DomainOperationCtl(NS_ID, DOMAIN_ID)
    ctl._pb = pb
    return ctl


def create_set_upstreams_all_pb(cache, zk_storage):
    meta = model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    domain_pb = model_pb2.DomainOperation(meta=meta)
    domain_pb.spec.incomplete = True
    domain_pb.order.content.set_upstreams.include_upstreams.type = modules_pb2.ALL
    zk_storage.create_domain_operation(namespace_id=NS_ID,
                                       domain_id=DOMAIN_ID,
                                       domain_operation_pb=domain_pb)
    wait_domain_op(cache, lambda pb: pb)
    return domain_pb


def create_set_upstreams_by_ids_pb(cache, zk_storage):
    meta = model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    domain_pb = model_pb2.DomainOperation(meta=meta)
    domain_pb.spec.incomplete = True
    domain_pb.order.content.set_upstreams.include_upstreams.type = modules_pb2.BY_ID
    domain_pb.order.content.set_upstreams.include_upstreams.ids.extend(['upstream1', 'upstream2'])
    zk_storage.create_domain_operation(namespace_id=NS_ID,
                                       domain_id=DOMAIN_ID,
                                       domain_operation_pb=domain_pb)
    wait_domain_op(cache, lambda pb: pb)
    return domain_pb


def update_domain_op(cache, zk_storage, domain_op_pb, check):
    for pb in zk_storage.update_domain_operation(NS_ID, DOMAIN_ID):
        pb.CopyFrom(domain_op_pb)
    wait_domain_op(cache, check)


def wait_domain_op(cache, check):
    assert wait_until(lambda: check(cache.get_domain_operation(NS_ID, DOMAIN_ID)))


@pytest.mark.parametrize('ctl', [
    create_ctl_with_all,
    create_ctl_by_ids,
])
def test_old_event_generation(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    event = events.DomainOperationUpdate(path='', pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
        assert 'Skipped event with stale generation -1' in log.records_text()
        assert 'Assigned initial state "START"' not in log.records_text()


@pytest.mark.parametrize('ctl', [
    create_ctl_with_all,
    create_ctl_by_ids,
])
def test_completed(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    ctl._pb.spec.incomplete = False
    ctl._pb.order.status.status = 'FINISHED'
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: not pb.spec.incomplete)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'started self deletion' in log.records_text()


@pytest.mark.parametrize('ctl', [
    create_ctl_with_all,
    create_ctl_by_ids,
])
def test_not_started(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Assigned initial state "STARTED"' in log.records_text()


@pytest.mark.parametrize('ctl', [
    create_ctl_with_all,
    create_ctl_by_ids,
])
def test_self_delete(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    ctl._pb.spec.incomplete = False
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: not pb.spec.incomplete)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'started self deletion' in log.records_text()


@pytest.mark.parametrize('create_ctl,start_state', [
    (create_ctl_with_all, 'STARTED'),
    (create_ctl_with_all, 'CHECKING_UPSTREAM_IDS'),
    (create_ctl_by_ids, 'STARTED'),
    (create_ctl_by_ids, 'CHECKING_UPSTREAM_IDS'),
])
def test_cancel(caplog, cache, zk_storage, ctx, create_ctl, start_state):
    ctl = create_ctl(cache, zk_storage)
    ctl._pb.order.status.status = 'IN_PROGRESS'
    ctl._pb.order.progress.state.id = start_state
    ctl._pb.order.cancelled.value = True
    ctl._pb.order.cancelled.comment = 'cancelled!'
    ctl._pb.order.cancelled.author = 'robot'
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'Processed, next state: CANCELLED' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'CANCELLED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'started self deletion' in log.records_text()


@pytest.mark.parametrize('ctl', [
    create_ctl_with_all,
    create_ctl_by_ids,
])
def test_cancel_unsupported(caplog, cache, zk_storage, ctx, ctl, domain):
    ctl = ctl(cache, zk_storage)
    ctl._pb.order.status.status = 'IN_PROGRESS'
    ctl._pb.order.progress.state.id = 'SAVING_DOMAIN_SPEC'
    ctl._pb.order.cancelled.value = True
    ctl._pb.order.cancelled.comment = 'cancelled!'
    ctl._pb.order.cancelled.author = 'robot'
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'but order processor "SavingDomainSpec" cannot be cancelled' in log.records_text()
        assert 'Processed, next state: FINISHED' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'FINISHED')


@pytest.mark.parametrize('ctl', [
    create_ctl_with_all,
    create_ctl_by_ids,
])
def test_transitions(caplog, cache, zk_storage, ctx, ctl, domain):
    ctl = ctl(cache, zk_storage)
    ctl._pb.order.progress.state.id = 'STARTED'
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: STARTED' in log.records_text()
        assert 'Processed, next state: CHECKING_UPSTREAM_IDS' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'CHECKING_UPSTREAM_IDS')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CHECKING_UPSTREAM_IDS' in log.records_text()
        assert 'Processed, next state: SAVING_DOMAIN_SPEC' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'SAVING_DOMAIN_SPEC')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: SAVING_DOMAIN_SPEC' in log.records_text()
        assert 'Processed, next state: FINISHED' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'FINISHED')
