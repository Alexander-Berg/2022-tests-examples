import logging

import inject
import pytest
import ujson

from infra.awacs.proto import model_pb2
from awacs.model.balancer.removal.ctl import BalancerRemovalCtl as BaseBalancerRemovalCtl
from awtest import wait_until, check_log, wait_until_passes


NS_ID = u'namespace-id'
BALANCER_ID = u'balancer-id_sas'


class BalancerRemovalCtl(BaseBalancerRemovalCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(BalancerRemovalCtl, self).__init__(*args, **kwargs)
        self._state_runner.processing_interval = self.PROCESSING_INTERVAL


@pytest.fixture(autouse=True)
def deps(binder, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def ctl(cache, zk_storage, balancer_pb):
    ctl = BalancerRemovalCtl(NS_ID, BALANCER_ID)
    ctl._pb = balancer_pb
    return ctl


@pytest.fixture
def balancer_pb(cache, zk_storage):
    b_pb = model_pb2.Balancer()
    b_pb.meta.id = BALANCER_ID
    b_pb.meta.namespace_id = NS_ID
    b_pb.spec.incomplete = False
    b_pb.spec.deleted = True
    zk_storage.create_balancer(namespace_id=NS_ID,
                               balancer_id=BALANCER_ID,
                               balancer_pb=b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID), timeout=1)
    return b_pb


def update_balancer(cache, zk_storage, b_pb, check):
    for pb in zk_storage.update_balancer(NS_ID, BALANCER_ID):
        pb.CopyFrom(b_pb)
    wait_balancer(cache, check)


def wait_balancer(cache, check):
    assert wait_until(lambda: check(cache.must_get_balancer(NS_ID, BALANCER_ID)), timeout=1)


def test_not_started(caplog, ctx, ctl):
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Assigned initial state "STARTED"' in log.records_text()


def test_finished(caplog, cache, zk_storage, ctx, ctl, balancer_pb):
    balancer_pb.removal.status.status = 'FINISHED'
    update_balancer(cache, zk_storage, balancer_pb, check=lambda pb: pb.removal.status.status == 'FINISHED')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already FINISHED' in log.records_text()


def test_transitions(caplog, cache, zk_storage, ctx, ctl, balancer_pb):
    balancer_pb.removal.progress.state.id = 'STARTED'
    update_balancer(cache, zk_storage, balancer_pb, check=lambda pb: pb.removal.progress.state.id == 'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: STARTED' in log.records_text()
        assert 'Processed, next state: REMOVING_BALANCER_BACKENDS' in log.records_text()
    wait_balancer(cache, lambda pb: pb.removal.progress.state.id == 'REMOVING_BALANCER_BACKENDS')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: REMOVING_BALANCER_BACKENDS' in log.records_text()
        assert 'Processed, next state: REMOVING_BALANCER' in log.records_text()
    wait_balancer(cache, lambda pb: pb.removal.progress.state.id == 'REMOVING_BALANCER')


def test_removing_balancer_backends(caplog, cache, zk_storage, ctx, ctl, balancer_pb):
    b_pb = model_pb2.Backend()
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.id = BALANCER_ID
    b_pb.spec.selector.type = model_pb2.BackendSelector.BALANCERS
    b_pb.spec.selector.balancers.add(id=BALANCER_ID)
    zk_storage.create_backend(NS_ID, BALANCER_ID, b_pb)
    wait_until_passes(lambda: cache.must_get_backend(NS_ID, BALANCER_ID))

    balancer_pb.removal.progress.state.id = 'REMOVING_BALANCER_BACKENDS'
    update_balancer(cache, zk_storage, balancer_pb,
                    check=lambda pb: pb.removal.progress.state.id == 'REMOVING_BALANCER_BACKENDS')

    expected_msg = 'This balanced is still used in following backends: "balancer-id_sas"'
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: REMOVING_BALANCER_BACKENDS' in log.records_text()
        assert 'Processed, next state: REMOVING_BALANCER_BACKENDS' in log.records_text()

    def check_error(pb):
        assert len(pb.removal.feedback.messages) == 1
        msg = pb.removal.feedback.messages[0]
        assert msg.text == ujson.dumps(expected_msg)
        assert msg.severity == model_pb2.FB_SEVERITY_ACTION_REQUIRED
        assert msg.used_in_backends_error.backend_ids == ["balancer-id_sas"]
        return True

    wait_balancer(cache, check_error)


@pytest.mark.parametrize('start_state', [
    'REMOVING_SERVICE_FROM_DASHBOARDS',
    'REMOVING_POD_SET',
    'REMOVING_ENDPOINT_SETS',
    'REMOVING_SERVICE',
    'REMOVING_BALANCER',
])
def test_cancel_unsupported(caplog, cache, zk_storage, ctx, ctl, balancer_pb, start_state):
    balancer_pb.removal.progress.state.id = start_state
    balancer_pb.removal.status.status = 'IN_PROGRESS'
    balancer_pb.removal.cancelled.value = True
    balancer_pb.removal.cancelled.comment = 'cancelled!'
    balancer_pb.removal.cancelled.author = 'robot'
    update_balancer(cache, zk_storage, balancer_pb, check=lambda pb: pb.removal.cancelled.value)

    with check_log(caplog) as log:
        try:
            ctl._process(ctx)
        except Exception as e:  # we don't care about actual processing result, so we don't bother to set up the mocks
            ctx.log.info(e)
            pass
        assert 'removal was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'cannot be cancelled' in log.records_text()
