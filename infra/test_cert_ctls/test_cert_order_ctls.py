import inject
import logging
import mock
import pytest

from awacs.lib import certificator, ya_vault, startrekclient
from awacs.lib import certs
from awacs.model import events
from awacs.model.certs.order_ctl import CertOrderCtl as BaseCertOrderCtl
from awacs.model.certs.renewal_order_ctl import CertRenewalOrderCtl as BaseCertRenewalOrderCtl
from infra.awacs.proto import model_pb2
from awtest import wait_until, check_log
from awtest.mocks.yav import MockYavClient
from awtest.mocks.certificator import MockCertificatorClient
from awtest.mocks.startrek import MockStartrekClient


CERT_ID = 'cert-id'
NS_ID = 'namespace-id'


class CertOrderCtl(BaseCertOrderCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(CertOrderCtl, self).__init__(*args, **kwargs)
        self._state_runner.processing_interval = self.PROCESSING_INTERVAL


class CertRenewalOrderCtl(BaseCertRenewalOrderCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(CertRenewalOrderCtl, self).__init__(*args, **kwargs)
        self._state_runner.processing_interval = self.PROCESSING_INTERVAL


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(certificator.ICertificatorClient, MockCertificatorClient)
        b.bind(ya_vault.IYaVaultClient, MockYavClient)
        b.bind(startrekclient.IStartrekClient, MockStartrekClient)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_ctl(cache, zk_storage):
    pb = create_cert_pb(cache, zk_storage)
    ctl = CertOrderCtl(NS_ID, CERT_ID)
    ctl._pb = pb
    return ctl


def create_renewal_ctl(cache, zk_storage):
    pb = create_cert_renewal_pb(cache, zk_storage)
    create_cert_pb(cache, zk_storage)
    ctl = CertRenewalOrderCtl(NS_ID, CERT_ID)
    ctl._pb = pb
    return ctl


def create_cert_pb(cache, zk_storage):
    meta = model_pb2.CertificateMeta(id=CERT_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    meta.auth.staff.owners.logins.append('robot')
    cert = model_pb2.Certificate(meta=meta)
    cert.spec.incomplete = True
    cert.order.content.common_name = 'test.common.name'
    cert.order.content.subject_alternative_names.extend(['test1.common.name', 'test2.common.name'])
    cert.order.content.abc_service_id = 999
    cert.order.content.ca_name = 'Internal'
    zk_storage.create_cert(namespace_id=NS_ID,
                           cert_id=CERT_ID,
                           cert_pb=cert)
    assert wait_until(lambda: cache.get_cert(NS_ID, CERT_ID), timeout=1)
    return cert


def create_cert_renewal_pb(cache, zk_storage):
    meta = model_pb2.CertificateRenewalMeta(id=CERT_ID, namespace_id=NS_ID)
    meta.mtime.GetCurrentTime()
    cert = model_pb2.CertificateRenewal(meta=meta)
    cert.spec.incomplete = True
    cert.order.content.common_name = 'test.common.name'
    cert.order.content.subject_alternative_names.extend(['test1.common.name', 'test2.common.name'])
    cert.order.content.abc_service_id = 999
    cert.order.content.ca_name = 'Internal'
    zk_storage.create_cert_renewal(namespace_id=NS_ID,
                                   cert_renewal_id=CERT_ID,
                                   cert_renewal_pb=cert)
    assert wait_until(lambda: cache.get_cert_renewal(NS_ID, CERT_ID), timeout=1)
    return cert


def update_cert_(cache, zk_storage, cert_pb, check):
    for pb in zk_storage.update_cert(NS_ID, CERT_ID):
        pb.CopyFrom(cert_pb)
    assert wait_until(lambda: check(cache.get_cert(NS_ID, CERT_ID)), timeout=1)


def update_cert_renewal_(cache, zk_storage, cert_renewal_pb, check):
    for pb in zk_storage.update_cert_renewal(NS_ID, CERT_ID):
        pb.CopyFrom(cert_renewal_pb)
    assert wait_until(lambda: check(cache.get_cert_renewal(NS_ID, CERT_ID)), timeout=1)


def wait_cert_(cache, check):
    assert wait_until(lambda: check(cache.get_cert(NS_ID, CERT_ID)), timeout=1)


def wait_cert_renewal_(cache, check):
    assert wait_until(lambda: check(cache.get_cert_renewal(NS_ID, CERT_ID)), timeout=1)


@pytest.mark.parametrize('ctl', [create_ctl, create_renewal_ctl])
def test_old_event_generation(cache, zk_storage, caplog, ctx, ctl):
    ctl = ctl(cache, zk_storage)
    event = events.CertUpdate(path='', pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
        assert 'Skipped event with stale generation -1' in log.records_text()
        assert 'Assigned initial state "START"' not in log.records_text()


@pytest.mark.parametrize('ctl,update_cert', [
    (create_ctl, update_cert_),
    (create_renewal_ctl, update_cert_renewal_),
])
def test_completed(caplog, cache, zk_storage, ctx, ctl, update_cert):
    ctl = ctl(cache, zk_storage)
    ctl._pb.spec.incomplete = False
    ctl._pb.order.status.status = 'FINISHED'
    update_cert(cache, zk_storage, ctl._pb, check=lambda pb: not pb.spec.incomplete)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already FINISHED, nothing to process' in log.records_text()


@pytest.mark.parametrize('ctl,update_cert,start_state', [
    (create_ctl, update_cert_, 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR'),
    (create_renewal_ctl, update_cert_renewal_, 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR'),
])
def test_not_started(caplog, cache, zk_storage, ctx, ctl, update_cert, start_state):
    ctl = ctl(cache, zk_storage)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Assigned initial state "{}"'.format(start_state) in log.records_text()


@pytest.mark.parametrize('ctl,update_cert', [
    (create_ctl, update_cert_),
    (create_renewal_ctl, update_cert_renewal_),
])
def test_finished(caplog, cache, zk_storage, ctx, ctl, update_cert):
    ctl = ctl(cache, zk_storage)
    ctl._pb.order.status.status = 'FINISHED'
    update_cert(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.status.status == 'FINISHED')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already FINISHED' in log.records_text()


@pytest.mark.parametrize('ctl,update_cert,wait_cert,status,start_state', [
    (create_ctl, update_cert_, wait_cert_, 'CREATED', 'START'),
    (create_ctl, update_cert_, wait_cert_, 'IN_PROGRESS', 'START'),
    (create_renewal_ctl, update_cert_renewal_, wait_cert_renewal_,
     'CREATED', 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR'),
    (create_renewal_ctl, update_cert_renewal_, wait_cert_renewal_,
     'IN_PROGRESS', 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR'),
])
def test_transition_cancel(caplog, cache, zk_storage, ctx, ctl, update_cert, wait_cert, status, start_state):
    ctl = ctl(cache, zk_storage)
    ctl._pb.order.status.status = status
    ctl._pb.order.progress.state.id = start_state
    ctl._pb.order.cancelled.value = True
    ctl._pb.order.cancelled.comment = 'cancelled!'
    ctl._pb.order.cancelled.author = 'robot'
    update_cert(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'Overall status will be CANCELLED' in log.records_text()
    wait_cert(cache, lambda pb: pb.order.progress.state.id == 'CANCELLED' and pb.order.status.status == 'CANCELLED')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already CANCELLED' in log.records_text()


@pytest.mark.parametrize('ctl,update_cert,wait_cert,start_state', [
    (create_ctl, update_cert_, wait_cert_, 'CANCELLING'),
    (create_renewal_ctl, update_cert_renewal_, wait_cert_renewal_, 'CANCELLING'),
])
def test_transition_cancel_unsupported(caplog, cache, zk_storage, ctx, ctl, update_cert, wait_cert, start_state):
    ctl = ctl(cache, zk_storage)
    ctl._pb.order.status.status = 'IN_PROGRESS'
    ctl._pb.order.progress.state.id = start_state
    ctl._pb.order.cancelled.value = True
    ctl._pb.order.cancelled.comment = 'cancelled!'
    ctl._pb.order.cancelled.author = 'robot'
    update_cert(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'but order processor "Cancelling" cannot be cancelled' in log.records_text()
        assert 'Overall status will be CANCELLED' in log.records_text()
    wait_cert(cache, lambda pb: pb.order.progress.state.id == 'CANCELLED' and pb.order.status.status == 'CANCELLED')


@pytest.mark.parametrize('ctl,update_cert,wait_cert', [
    (create_ctl, update_cert_, wait_cert_),
    (create_renewal_ctl, update_cert_renewal_, wait_cert_renewal_),
])
def test_transitions_ctl(caplog, cache, zk_storage, ctx, ctl, update_cert, wait_cert):
    ctl = ctl(cache, zk_storage)
    ctl._pb.order.progress.state.id = 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR'
    ctl._pb.meta.auth.staff.owners.logins.extend(['robot'])
    update_cert(cache, zk_storage, ctl._pb,
                check=lambda pb: pb.order.progress.state.id == 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: SENDING_CREATE_REQUEST_TO_CERTIFICATOR' in log.records_text()
        assert 'Processed, next state: ADDING_COMMENT_TO_SECTASK' in log.records_text()
    wait_cert(cache, lambda pb: pb.order.progress.state.id == 'ADDING_COMMENT_TO_SECTASK')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: ADDING_COMMENT_TO_SECTASK' in log.records_text()
        assert 'Processed, next state: ADDING_CERT_OWNERS_TO_SECTASK' in log.records_text()
    wait_cert(cache, lambda pb: pb.order.progress.state.id == 'ADDING_CERT_OWNERS_TO_SECTASK')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: ADDING_CERT_OWNERS_TO_SECTASK' in log.records_text()
        assert 'Processed, next state: ADDING_AWACS_ON_DUTY_TO_SECTASK' in log.records_text()
    wait_cert(cache, lambda pb: pb.order.progress.state.id == 'ADDING_AWACS_ON_DUTY_TO_SECTASK')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: ADDING_AWACS_ON_DUTY_TO_SECTASK' in log.records_text()
        assert 'Processed, next state: POLLING_CERTIFICATOR_FOR_STORAGE_INFO' in log.records_text()
    wait_cert(cache, lambda pb: pb.order.progress.state.id == 'POLLING_CERTIFICATOR_FOR_STORAGE_INFO')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: POLLING_CERTIFICATOR_FOR_STORAGE_INFO' in log.records_text()
        assert 'Processed, next state: FETCHING_CERT_INFO_FROM_STORAGE' in log.records_text()
    wait_cert(cache, lambda pb: pb.order.progress.state.id == 'FETCHING_CERT_INFO_FROM_STORAGE')

    with check_log(caplog) as log, \
            mock.patch.object(certs, 'get_end_entity_cert', return_value='fake_cert'), \
            mock.patch.object(certs, 'fill_cert_fields', return_value=None):
        ctl._process(ctx)
        assert 'Current state: FETCHING_CERT_INFO_FROM_STORAGE' in log.records_text()
        assert 'Processed, next state: MODIFYING_YAV_SECRET_ACCESS' in log.records_text()
    wait_cert(cache, lambda pb: pb.order.progress.state.id == 'MODIFYING_YAV_SECRET_ACCESS')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: MODIFYING_YAV_SECRET_ACCESS' in log.records_text()
        assert 'Processed, next state: FINISH' in log.records_text()
    wait_cert(cache, lambda pb: pb.order.progress.state.id == 'FINISH')


def test_ctl_legacy_transitions(caplog, cache, zk_storage, ctx):
    ctl = create_ctl(cache, zk_storage)

    ctl._pb.order.progress.state.id = 'START'
    update_cert_(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.progress.state.id == 'START')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: START' in log.records_text()
        assert 'Processed, next state: SENDING_CREATE_REQUEST_TO_CERTIFICATOR' in log.records_text()
    assert wait_until(
        lambda: cache.get_cert(NS_ID, CERT_ID).order.progress.state.id == 'SENDING_CREATE_REQUEST_TO_CERTIFICATOR',
        timeout=1)

    ctl._pb.order.progress.state.id = 'SENT_REQUEST_TO_CERTIFICATOR'
    update_cert_(cache, zk_storage, ctl._pb,
                 check=lambda pb: pb.order.progress.state.id == 'SENT_REQUEST_TO_CERTIFICATOR')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: SENT_REQUEST_TO_CERTIFICATOR' in log.records_text()
        assert 'Processed, next state: ADDING_COMMENT_TO_SECTASK' in log.records_text()
    assert wait_until(
        lambda: cache.get_cert(NS_ID, CERT_ID).order.progress.state.id == 'ADDING_COMMENT_TO_SECTASK',
        timeout=1)

    ctl._pb.order.progress.state.id = 'GOT_STORAGE_INFO'
    update_cert_(cache, zk_storage, ctl._pb,
                 check=lambda pb: pb.order.progress.state.id == 'GOT_STORAGE_INFO')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: GOT_STORAGE_INFO' in log.records_text()
        assert 'Processed, next state: FETCHING_CERT_INFO_FROM_STORAGE' in log.records_text()
    assert wait_until(
        lambda: cache.get_cert(NS_ID, CERT_ID).order.progress.state.id == 'FETCHING_CERT_INFO_FROM_STORAGE',
        timeout=1)

    ctl._pb.order.progress.state.id = 'GOT_CERT_INFO'
    update_cert_(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.progress.state.id == 'GOT_CERT_INFO')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: GOT_CERT_INFO' in log.records_text()
        assert 'Processed, next state: FINISH' in log.records_text()
    assert wait_until(
        lambda: cache.get_cert(NS_ID, CERT_ID).order.progress.state.id == 'FINISH',
        timeout=1)
