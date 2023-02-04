import logging

import inject
import pytest
import ujson

from awacs.model import events
from awacs.model.domain.order.ctl import DomainOrderCtl as BaseDomainOrderCtl
from infra.awacs.proto import model_pb2
from awtest import wait_until, check_log
from awtest.api import create_ns


DOMAIN_ID = 'domain-id'
CERT_ID = 'cert-id'
NS_ID = 'namespace-id'


class DomainOrderCtl(BaseDomainOrderCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(DomainOrderCtl, self).__init__(*args, **kwargs)
        self._state_runner.processing_interval = self.PROCESSING_INTERVAL


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_ctl_with_cert_order(cache, zk_storage):
    pb = create_domain_pb_with_cert_order(cache, zk_storage)
    ctl = DomainOrderCtl(NS_ID, DOMAIN_ID)
    ctl._pb = pb
    return ctl


def create_ctl_with_cert_ref(cache, zk_storage):
    pb = create_domain_pb_with_cert_ref(cache, zk_storage)
    ctl = DomainOrderCtl(NS_ID, DOMAIN_ID)
    ctl._pb = pb
    return ctl


def create_domain_pb_with_cert_order(cache, zk_storage):
    meta = model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    domain_pb = model_pb2.Domain(meta=meta)
    domain_pb.spec.incomplete = True
    domain_pb.order.content.cert_order.content.common_name = 'test.common.name'
    domain_pb.order.content.cert_order.content.subject_alternative_names.extend(['test1.common.name',
                                                                                 'test2.common.name'])
    domain_pb.order.content.cert_order.content.abc_service_id = 999
    domain_pb.order.content.cert_order.content.ca_name = 'Internal'
    domain_pb.order.content.cert_order.content.public_key_algorithm_id = 'ec'
    domain_pb.order.content.secondary_cert_order.content.common_name = 'test.common.name'
    domain_pb.order.content.secondary_cert_order.content.subject_alternative_names.extend(['test1.common.name',
                                                                                           'test2.common.name'])
    domain_pb.order.content.secondary_cert_order.content.abc_service_id = 999
    domain_pb.order.content.secondary_cert_order.content.ca_name = 'Internal'
    domain_pb.order.content.secondary_cert_order.content.public_key_algorithm_id = 'rsa'
    zk_storage.create_domain(namespace_id=NS_ID,
                             domain_id=DOMAIN_ID,
                             domain_pb=domain_pb)
    wait_domain(cache, lambda pb: pb)
    return domain_pb


def create_domain_pb_with_cert_ref(cache, zk_storage):
    meta = model_pb2.DomainMeta(id=DOMAIN_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    domain_pb = model_pb2.Domain(meta=meta)
    domain_pb.spec.incomplete = True
    domain_pb.order.content.cert_ref.id = CERT_ID
    domain_pb.order.content.secondary_cert_ref.id = CERT_ID
    zk_storage.create_domain(namespace_id=NS_ID,
                             domain_id=DOMAIN_ID,
                             domain_pb=domain_pb)
    wait_domain(cache, lambda pb: pb)
    return domain_pb


def create_cert(domain_pb, cache, zk_storage, cert_id):
    cert_meta = model_pb2.CertificateMeta(id=cert_id, namespace_id=NS_ID)
    cert_pb = model_pb2.Certificate(meta=cert_meta)
    cert_pb.order.content.CopyFrom(domain_pb.order.content.cert_order.content)
    zk_storage.create_cert(NS_ID, cert_id, cert_pb)
    assert wait_until(lambda: cache.get_cert(NS_ID, cert_id))
    return cert_pb


def update_domain(cache, zk_storage, domain_pb, check):
    for pb in zk_storage.update_domain(NS_ID, DOMAIN_ID):
        pb.CopyFrom(domain_pb)
    wait_domain(cache, check)


def wait_domain(cache, check):
    assert wait_until(lambda: check(cache.get_domain(NS_ID, DOMAIN_ID)))


@pytest.mark.parametrize('ctl', [
    create_ctl_with_cert_order,
    create_ctl_with_cert_ref,
])
def test_old_event_generation(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    event = events.DomainUpdate(path='', pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
        assert 'Skipped event with stale generation -1' in log.records_text()
        assert 'Assigned initial state "START"' not in log.records_text()


@pytest.mark.parametrize('ctl', [
    create_ctl_with_cert_order,
    create_ctl_with_cert_ref,
])
def test_completed(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    ctl._pb.spec.incomplete = False
    ctl._pb.order.status.status = 'FINISHED'
    update_domain(cache, zk_storage, ctl._pb, check=lambda pb: not pb.spec.incomplete)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already FINISHED, nothing to process' in log.records_text()


@pytest.mark.parametrize('ctl', [
    create_ctl_with_cert_order,
    create_ctl_with_cert_ref,
])
def test_not_started(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Assigned initial state "STARTED"' in log.records_text()


@pytest.mark.parametrize('ctl', [
    create_ctl_with_cert_order,
    create_ctl_with_cert_ref,
])
def test_finished(caplog, cache, zk_storage, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    ctl._pb.order.status.status = 'FINISHED'
    update_domain(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.status.status == 'FINISHED')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already FINISHED' in log.records_text()


@pytest.mark.parametrize('start_state', [
    'STARTED',
    'CHECKING_CERT_INFO',
    'CREATING_CERT_ORDER',
])
def test_cancel_with_cert_ref(caplog, cache, zk_storage, ctx, start_state, log):
    ctl = create_ctl_with_cert_ref(cache, zk_storage)
    ctl._pb.order.status.status = 'IN_PROGRESS'
    ctl._pb.order.progress.state.id = start_state
    ctl._pb.order.cancelled.value = True
    ctl._pb.order.cancelled.comment = 'cancelled!'
    ctl._pb.order.cancelled.author = 'robot'
    update_domain(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'Processed, next state: WAITING_FOR_CERT_CANCEL' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_CERT_CANCEL')


@pytest.mark.parametrize('start_state', [
    'STARTED',
    'CREATING_CERT_ORDER',
    'WAITING_FOR_CERT_ORDER',
])
def test_cancel_with_cert_order(caplog, cache, zk_storage, ctx, start_state):
    ctl = create_ctl_with_cert_order(cache, zk_storage)
    create_cert(ctl._pb, cache, zk_storage, cert_id=DOMAIN_ID)
    ctl._pb.order.status.status = 'IN_PROGRESS'
    ctl._pb.order.progress.state.id = start_state
    ctl._pb.order.cancelled.value = True
    ctl._pb.order.cancelled.comment = 'cancelled!'
    ctl._pb.order.cancelled.author = 'robot'
    ctl._pb.order.progress.context['cert_order_id'] = ujson.dumps(DOMAIN_ID)
    update_domain(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'but order processor "Start" cannot be cancelled' not in log.records_text()
        assert 'Processed, next state: WAITING_FOR_CERT_CANCEL' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_CERT_CANCEL')
    assert wait_until(lambda: cache.get_cert(NS_ID, DOMAIN_ID).order.cancelled.value)

    for cert_pb in zk_storage.update_cert(NS_ID, DOMAIN_ID):
        cert_pb.order.status.status = 'CANCELLED'
        cert_pb.order.progress.state.id = 'CANCELLED'
    assert wait_until(lambda: cache.must_get_cert(NS_ID, DOMAIN_ID).order.status.status == 'CANCELLED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Overall status will be CANCELLED' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'CANCELLED' and pb.order.status.status == 'CANCELLED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already CANCELLED' in log.records_text()


def test_cancel_unsupported_with_order(caplog, cache, zk_storage, ctx):
    ctl = create_ctl_with_cert_order(cache, zk_storage)
    ctl._pb.order.status.status = 'IN_PROGRESS'
    ctl._pb.order.progress.state.id = 'CANCELLING'
    ctl._pb.order.cancelled.value = True
    ctl._pb.order.cancelled.comment = 'cancelled!'
    ctl._pb.order.cancelled.author = 'robot'
    ctl._pb.order.progress.context['cert_order_id'] = ujson.dumps(DOMAIN_ID)
    update_domain(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'but order processor "Cancelling" cannot be cancelled' in log.records_text()
        assert 'Processed, next state: WAITING_FOR_CERT_CANCEL' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_CERT_CANCEL')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Overall status will be CANCELLED' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'CANCELLED' and pb.order.status.status == 'CANCELLED')


def test_cancel_unsupported_with_ref(caplog, cache, zk_storage, ctx):
    ctl = create_ctl_with_cert_ref(cache, zk_storage)
    ctl._pb.order.status.status = 'IN_PROGRESS'
    ctl._pb.order.progress.state.id = 'CANCELLING'
    ctl._pb.order.cancelled.value = True
    ctl._pb.order.cancelled.comment = 'cancelled!'
    ctl._pb.order.cancelled.author = 'robot'
    update_domain(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'but order processor "Cancelling" cannot be cancelled' in log.records_text()
        assert 'Processed, next state: WAITING_FOR_CERT_CANCEL' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_CERT_CANCEL')


def test_transitions_ctl_with_cert_order(caplog, cache, zk_storage, ctx):
    ctl = create_ctl_with_cert_order(cache, zk_storage)
    ctl._pb.order.progress.state.id = 'STARTED'
    update_domain(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: STARTED' in log.records_text()
        assert 'Processed, next state: CHECKING_CERT_INFO' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'CHECKING_CERT_INFO')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CHECKING_CERT_INFO' in log.records_text()
        assert 'Processed, next state: CREATING_CERT_ORDER' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'CREATING_CERT_ORDER')

    create_ns(NS_ID, cache, zk_storage)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CREATING_CERT_ORDER' in log.records_text()
        assert 'Processed, next state: WAITING_FOR_CERT_ORDER' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_CERT_ORDER')
    assert wait_until(lambda: cache.get_cert(NS_ID, 'test.common.name_ec'))
    assert wait_until(lambda: cache.get_cert(NS_ID, 'test.common.name_rsa'))

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: WAITING_FOR_CERT_ORDER' in log.records_text()
        assert 'Processed, next state: WAITING_FOR_CERT_ORDER' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'WAITING_FOR_CERT_ORDER')

    for cert_id in ('test.common.name_ec', 'test.common.name_rsa'):
        for cert_pb in zk_storage.update_cert(NS_ID, cert_id):
            cert_pb.order.status.status = 'FINISHED'
            cert_pb.order.progress.state.id = 'FINISH'
        assert wait_until(lambda: cache.must_get_cert(NS_ID, cert_id).order.status.status == 'FINISHED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: WAITING_FOR_CERT_ORDER' in log.records_text()
        assert 'Processed, next state: SAVING_DOMAIN_SPEC' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'SAVING_DOMAIN_SPEC')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: SAVING_DOMAIN_SPEC' in log.records_text()
        assert 'Processed, next state: FINISHED' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'FINISHED')


def test_transitions_ctl_with_cert_ref(caplog, cache, zk_storage, ctx):
    ctl = create_ctl_with_cert_ref(cache, zk_storage)
    ctl._pb.order.progress.state.id = 'STARTED'
    update_domain(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: STARTED' in log.records_text()
        assert 'Processed, next state: CHECKING_CERT_INFO' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'CHECKING_CERT_INFO')

    create_cert(ctl._pb, cache, zk_storage, cert_id=CERT_ID)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CHECKING_CERT_INFO' in log.records_text()
        assert 'Processed, next state: SAVING_DOMAIN_SPEC' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'SAVING_DOMAIN_SPEC')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: SAVING_DOMAIN_SPEC' in log.records_text()
        assert 'Processed, next state: FINISHED' in log.records_text()
    wait_domain(cache, lambda pb: pb.order.progress.state.id == 'FINISHED')
