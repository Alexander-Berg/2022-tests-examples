import logging

import inject
import pytest
import ujson

from awacs.lib import ya_vault
from awacs.model import events
from awacs.model.domain.operations.ctl import DomainOperationCtl as BaseDomainOperationCtl
from infra.awacs.proto import model_pb2
from awtest import wait_until, check_log, wait_until_passes
from awtest.mocks.yav import MockYavClient


DOMAIN_ID = 'domain_pb-id'
CERT_ID = 'cert-id'
NS_ID = 'namespace-id'
NS_ID_2 = 'namespace-id-2'


class DomainOperationCtl(BaseDomainOperationCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(DomainOperationCtl, self).__init__(*args, **kwargs)
        self._runners['transfer'].processing_interval = self.PROCESSING_INTERVAL


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(ya_vault.IYaVaultClient, MockYavClient)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def domain_pb(cache, zk_storage):
    return create_domain(cache, zk_storage, NS_ID, DOMAIN_ID)


def create_domain(cache, zk_storage, ns_id, domain_id):
    domain_pb = model_pb2.Domain()
    domain_pb.meta.id = domain_id
    domain_pb.meta.namespace_id = ns_id
    zk_storage.create_domain(ns_id, domain_id, domain_pb)
    wait_until_passes(lambda: cache.must_get_domain(ns_id, domain_id))
    return domain_pb


def create_cert(cache, zk_storage, ns_id=NS_ID, cert_id=CERT_ID):
    cert_pb = model_pb2.Certificate()
    cert_pb.meta.id = cert_id
    cert_pb.meta.namespace_id = ns_id
    cert_pb.spec.fields.serial_number = '120213199433569577268469'
    zk_storage.create_cert(ns_id, cert_id, cert_pb)
    wait_until_passes(lambda: cache.must_get_cert(ns_id, cert_id))


def create_ctl(cache, zk_storage):
    pb = create_transfer_pb(cache, zk_storage)
    ctl = DomainOperationCtl(NS_ID, DOMAIN_ID)
    ctl._pb = pb
    return ctl


def create_transfer_pb(cache, zk_storage):
    meta = model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    domain_pb = model_pb2.DomainOperation(meta=meta)
    domain_pb.spec.incomplete = True
    domain_pb.order.content.transfer.target_namespace_id = NS_ID_2
    domain_pb.order.progress.context['cert_id'] = ujson.dumps(CERT_ID)
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
    assert wait_until(lambda: check(cache.must_get_domain_operation(NS_ID, DOMAIN_ID)))
    return cache.must_get_domain_operation(NS_ID, DOMAIN_ID)


def update_domain(cache, zk_storage, domain_pb, check):
    for pb in zk_storage.update_domain(NS_ID, DOMAIN_ID):
        pb.CopyFrom(domain_pb)
    assert wait_until(lambda: check(cache.must_get_domain(NS_ID, DOMAIN_ID)))


def test_old_event_generation(caplog, cache, zk_storage, ctx):
    ctl = create_ctl(cache, zk_storage)
    event = events.DomainOperationUpdate(path='', pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
        assert 'Skipped event with stale generation -1' in log.records_text()
        assert 'Assigned initial state "START"' not in log.records_text()


def test_completed(caplog, cache, zk_storage, ctx):
    ctl = create_ctl(cache, zk_storage)
    ctl._pb.spec.incomplete = False
    ctl._pb.order.status.status = 'FINISHED'
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: not pb.spec.incomplete)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'started self deletion' in log.records_text()


def test_not_started(caplog, cache, zk_storage, ctx):
    ctl = create_ctl(cache, zk_storage)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Assigned initial state "STARTED"' in log.records_text()


def test_self_delete(caplog, cache, zk_storage, ctx):
    ctl = create_ctl(cache, zk_storage)
    ctl._pb.spec.incomplete = False
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: not pb.spec.incomplete)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'started self deletion' in log.records_text()


@pytest.mark.parametrize('start_state', [
    'STARTED',
    'MARKING_OLD_DOMAIN',
    'MARKING_OLD_CERT',
    'CREATING_NEW_CERT_SECRET',
    'CREATING_NEW_CERT',
    'CREATING_NEW_DOMAIN',
    'WAITING_FOR_REMOVAL_APPROVAL',
])
def test_cancel(caplog, cache, zk_storage, ctx, start_state):
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
        assert 'Current state: {}, running RemovingNewDomain.process()...'.format(start_state) in log.records_text()
        assert 'Processed, next state: WAITING_FOR_NEW_DOMAIN_REMOVAL' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_NEW_DOMAIN_REMOVAL')


@pytest.mark.parametrize('start_state', [
    'REMOVING_OLD_DOMAIN',
    'WAITING_FOR_OLD_DOMAIN_REMOVAL',
    'UNMARKING_NEW_DOMAIN',
    'UNMARKING_NEW_CERT',
    'FINISHING',
    'REMOVING_NEW_DOMAIN',
    'WAITING_FOR_NEW_DOMAIN_REMOVAL',
    'REMOVING_NEW_CERT',
    'WAITING_FOR_NEW_CERT_REMOVAL',
    'UNMARKING_OLD_CERT',
    'UNMARKING_OLD_DOMAIN',
    'CANCELLING',
])
def test_cancel_unsupported(caplog, cache, zk_storage, ctx, domain_pb, start_state):
    create_domain(cache, zk_storage, NS_ID_2, DOMAIN_ID)
    create_cert(cache, zk_storage, NS_ID)
    create_cert(cache, zk_storage, NS_ID_2)
    ctl = create_ctl(cache, zk_storage)
    ctl._pb.order.status.status = 'IN_PROGRESS'
    ctl._pb.order.progress.state.id = start_state
    ctl._pb.order.cancelled.value = True
    ctl._pb.order.cancelled.comment = 'cancelled!'
    ctl._pb.order.cancelled.author = 'robot'
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'cannot be cancelled' in log.records_text()


@pytest.mark.parametrize('with_cert', [
    True,
    False,
])
def test_transitions(caplog, cache, zk_storage, ctx, domain_pb, with_cert, log):
    ctl = create_ctl(cache, zk_storage)
    if with_cert:
        domain_pb.spec.yandex_balancer.config.cert.id = CERT_ID
        update_domain(cache, zk_storage, domain_pb, check=lambda pb: pb.spec.yandex_balancer.config.cert.id == CERT_ID)
    else:
        del ctl._pb.order.progress.context['cert_id']
        update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: 'cert_id' not in domain_pb.order.progress.context)
    ctl._pb.order.progress.state.id = 'STARTED'
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: STARTED' in log.records_text()
        assert 'Processed, next state: MARKING_OLD_DOMAIN' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'MARKING_OLD_DOMAIN')

    if with_cert:
        next_state = 'MARKING_OLD_CERT'
    else:
        next_state = 'CREATING_NEW_DOMAIN'
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: MARKING_OLD_DOMAIN' in log.records_text()
        assert 'Processed, next state: {}'.format(next_state) in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == next_state)

    if with_cert:
        create_cert(cache, zk_storage)
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert 'Current state: MARKING_OLD_CERT' in log.records_text()
            assert 'Processed, next state: CREATING_NEW_CERT_SECRET' in log.records_text()
        wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'CREATING_NEW_CERT_SECRET')

        with check_log(caplog) as log:
            ctl._process(ctx)
            assert 'Current state: CREATING_NEW_CERT_SECRET' in log.records_text()
            assert 'Processed, next state: CREATING_NEW_CERT' in log.records_text()
        wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'CREATING_NEW_CERT')

        with check_log(caplog) as log:
            ctl._process(ctx)
            assert 'Current state: CREATING_NEW_CERT' in log.records_text()
            assert 'Processed, next state: CREATING_NEW_DOMAIN' in log.records_text()
        wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'CREATING_NEW_DOMAIN')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CREATING_NEW_DOMAIN' in log.records_text()
        assert 'Processed, next state: WAITING_FOR_REMOVAL_APPROVAL' in log.records_text()
    domain_op_pb = wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_REMOVAL_APPROVAL')

    domain_op_pb.order.approval.before_removal = True
    update_domain_op(cache, zk_storage, domain_op_pb, check=lambda pb: pb.order.approval.before_removal)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: WAITING_FOR_REMOVAL_APPROVAL' in log.records_text()
        assert 'Processed, next state: REMOVING_OLD_DOMAIN' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'REMOVING_OLD_DOMAIN')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: REMOVING_OLD_DOMAIN' in log.records_text()
        assert 'Processed, next state: WAITING_FOR_OLD_DOMAIN_REMOVAL' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_OLD_DOMAIN_REMOVAL')

    if with_cert:
        next_state = 'REMOVING_OLD_CERT'
    else:
        next_state = 'UNMARKING_NEW_DOMAIN'
    zk_storage.remove_domain(NS_ID, DOMAIN_ID)
    assert wait_until(lambda: cache.get_domain(NS_ID, DOMAIN_ID) is None)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: WAITING_FOR_OLD_DOMAIN_REMOVAL' in log.records_text()
        assert 'Processed, next state: {}'.format(next_state) in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == next_state)

    if with_cert:
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert 'Current state: REMOVING_OLD_CERT' in log.records_text()
            assert 'Processed, next state: WAITING_FOR_OLD_CERT_REMOVAL' in log.records_text()
        wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_OLD_CERT_REMOVAL')

        zk_storage.remove_cert(NS_ID, CERT_ID)
        assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID) is None)
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert 'Current state: WAITING_FOR_OLD_CERT_REMOVAL' in log.records_text()
            assert 'Processed, next state: UNMARKING_NEW_CERT' in log.records_text()
        wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'UNMARKING_NEW_CERT')

        with check_log(caplog) as log:
            ctl._process(ctx)
            assert 'Current state: UNMARKING_NEW_CERT' in log.records_text()
            assert 'Processed, next state: UNMARKING_NEW_DOMAIN' in log.records_text()
        wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'UNMARKING_NEW_DOMAIN')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: UNMARKING_NEW_DOMAIN' in log.records_text()
        assert 'Processed, next state: FINISHING' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'FINISHING')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: FINISHING' in log.records_text()
        assert 'Processed, next state: FINISHED' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'FINISHED' and not pb.spec.incomplete)


@pytest.mark.parametrize('with_cert', [
    True,
    False,
])
def test_cancelled_transitions(caplog, cache, zk_storage, ctx, domain_pb, with_cert):
    ctl = create_ctl(cache, zk_storage)
    if with_cert:
        domain_pb.spec.yandex_balancer.config.cert.id = CERT_ID
        update_domain(cache, zk_storage, domain_pb, check=lambda pb: pb.spec.yandex_balancer.config.cert.id == CERT_ID)
    else:
        del ctl._pb.order.progress.context['cert_id']
        update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: 'cert_id' not in domain_pb.order.progress.context)
    ctl._pb.order.status.status = 'IN_PROGRESS'
    ctl._pb.order.progress.state.id = 'REMOVING_NEW_DOMAIN'
    update_domain_op(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.status.status == 'IN_PROGRESS')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: REMOVING_NEW_DOMAIN' in log.records_text()
        assert 'Processed, next state: WAITING_FOR_NEW_DOMAIN_REMOVAL' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_NEW_DOMAIN_REMOVAL')

    if with_cert:
        next_state = 'REMOVING_NEW_CERT'
    else:
        next_state = 'UNMARKING_OLD_DOMAIN'
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: WAITING_FOR_NEW_DOMAIN_REMOVAL' in log.records_text()
        assert 'Processed, next state: {}'.format(next_state) in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == next_state)

    if with_cert:
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert 'Current state: REMOVING_NEW_CERT' in log.records_text()
            assert 'Processed, next state: WAITING_FOR_NEW_CERT_REMOVAL' in log.records_text()
        wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_NEW_CERT_REMOVAL')

        with check_log(caplog) as log:
            ctl._process(ctx)
            assert 'Current state: WAITING_FOR_NEW_CERT_REMOVAL' in log.records_text()
            assert 'Processed, next state: UNMARKING_OLD_CERT' in log.records_text()
        wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'UNMARKING_OLD_CERT')

        create_cert(cache, zk_storage)
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert 'Current state: UNMARKING_OLD_CERT' in log.records_text()
            assert 'Processed, next state: UNMARKING_OLD_DOMAIN' in log.records_text()
        wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'UNMARKING_OLD_DOMAIN')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: UNMARKING_OLD_DOMAIN' in log.records_text()
        assert 'Processed, next state: CANCELLING' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'CANCELLING')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CANCELLING' in log.records_text()
        assert 'Processed, next state: CANCELLED' in log.records_text()
    wait_domain_op(cache, lambda pb: pb.order.progress.state.id == 'CANCELLED' and not pb.spec.incomplete)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'started self deletion' in log.records_text()
