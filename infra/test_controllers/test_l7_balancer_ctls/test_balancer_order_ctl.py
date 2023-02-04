import inject
import logging
import mock
import pytest
import ujson

from awacs.lib.nannyrpcclient import INannyRpcClient
from awacs.lib.staffclient import IStaffClient
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.model import events
from awacs.model.balancer.order.order_ctl import BalancerOrderCtl as BaseBalancerOrderCtl
from awacs.model.balancer.order.processors import CreatingNannyService, ValidatingAwacsBalancer, ActivatingAwacsBalancer
from infra.awacs.proto import model_pb2
from awtest.mocks.abc_client import AbcMockClient
from awtest.mocks.staff_client import StaffMockClient
from awtest.mocks.yp_lite_client import YpLiteMockClient
from awtest.mocks.nanny_rpc_client import NannyRpcMockClient
from infra.swatlib.auth.abc import IAbcClient
from awtest import wait_until, check_log, wait_until_passes

NS_ID = u'namespace-id'
BALANCER_ID = u'balancer-id_sas'


class BalancerOrderCtl(BaseBalancerOrderCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(BalancerOrderCtl, self).__init__(*args, **kwargs)
        self._state_runner.processing_interval = self.PROCESSING_INTERVAL


@pytest.fixture(autouse=True)
def deps(binder_with_nanny_client, caplog):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(IYpLiteRpcClient, YpLiteMockClient())
        b.bind(INannyRpcClient, NannyRpcMockClient())
        b.bind(IStaffClient, StaffMockClient())
        b.bind(IAbcClient, AbcMockClient())
        binder_with_nanny_client(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


@pytest.fixture
def ctl(cache, zk_storage, balancer_pb):
    ctl = BalancerOrderCtl(NS_ID, BALANCER_ID)
    ctl._pb = balancer_pb
    return ctl


@pytest.fixture
def balancer_pb(cache, zk_storage, create_default_namespace):
    create_default_namespace(NS_ID)
    b_pb = model_pb2.Balancer()
    b_pb.meta.id = BALANCER_ID
    b_pb.meta.namespace_id = NS_ID
    b_pb.meta.version = 'xxx'
    b_pb.order.content.allocation_request.location = 'SAS'
    b_pb.order.content.allocation_request.preset.type = \
        model_pb2.BalancerOrder.Content.LocationalYpLiteAllocationRequest.Preset.NANO
    b_pb.order.content.allocation_request.preset.instances_count = 1
    b_pb.order.content.abc_service_id = 999
    b_pb.order.content.activate_balancer = True
    b_pb.spec.incomplete = True
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


def test_old_event_generation(caplog, ctx, ctl):
    event = events.BalancerUpdate(path='', pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
        assert 'Skipped event with stale generation -1' in log.records_text()
        assert 'Assigned initial state "START"' not in log.records_text()


def test_not_started(caplog, ctx, ctl):
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Assigned initial state "START"' in log.records_text()


def test_finished(caplog, cache, zk_storage, ctx, ctl, balancer_pb):
    balancer_pb.order.status.status = 'FINISHED'
    update_balancer(cache, zk_storage, balancer_pb, check=lambda pb: pb.order.status.status == 'FINISHED')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already FINISHED' in log.records_text()


def test_transitions(caplog, cache, zk_storage, ctx, ctl, balancer_pb):
    balancer_pb.order.progress.state.id = 'START'
    update_balancer(cache, zk_storage, balancer_pb, check=lambda pb: pb.order.progress.state.id == 'START')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: START' in log.records_text()
        assert 'Processed, next state: ALLOCATING_YP_LITE_RESOURCES' in log.records_text()
    wait_balancer(cache, lambda pb: (pb.order.progress.state.id == 'ALLOCATING_YP_LITE_RESOURCES' and
                                     pb.order.progress.state.attempts == 0))

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: ALLOCATING_YP_LITE_RESOURCES' in log.records_text()
        assert 'Processed, next state: GETTING_ABC_ROLE_STAFF_ID' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'GETTING_ABC_ROLE_STAFF_ID')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: GETTING_ABC_ROLE_STAFF_ID' in log.records_text()
        assert 'Processed, next state: CREATING_NANNY_SERVICE' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'CREATING_NANNY_SERVICE')

    with check_log(caplog) as log, \
            mock.patch.object(CreatingNannyService, '_create_nanny_service'):
        ctl._process(ctx)
        assert 'Current state: CREATING_NANNY_SERVICE' in log.records_text()
        assert 'Processed, next state: SETTING_UP_CLEANUP_POLICY' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'SETTING_UP_CLEANUP_POLICY')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: SETTING_UP_CLEANUP_POLICY' in log.records_text()
        assert 'Processed, next state: SETTING_UP_REPLICATION_POLICY' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'SETTING_UP_REPLICATION_POLICY')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: SETTING_UP_REPLICATION_POLICY' in log.records_text()
        assert 'Processed, next state: CREATING_ENDPOINT_SET' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'CREATING_ENDPOINT_SET')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CREATING_ENDPOINT_SET' in log.records_text()
        assert 'Processed, next state: CREATING_USER_ENDPOINT_SET' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'CREATING_USER_ENDPOINT_SET')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CREATING_USER_ENDPOINT_SET' in log.records_text()
        assert 'Processed, next state: CREATING_AWACS_BALANCER' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'CREATING_AWACS_BALANCER')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CREATING_AWACS_BALANCER' in log.records_text()
        assert 'Processed, next state: CREATING_BALANCER_BACKEND' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'CREATING_BALANCER_BACKEND')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Current state: CREATING_BALANCER_BACKEND' in log.records_text()
        assert 'Processed, next state: VALIDATING_AWACS_BALANCER' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'VALIDATING_AWACS_BALANCER')

    with check_log(caplog) as log, \
            mock.patch.object(ValidatingAwacsBalancer, '_is_balancer_valid', return_value=False):
        ctl._process(ctx)
        assert 'Current state: VALIDATING_AWACS_BALANCER' in log.records_text()
        assert 'Processed, next state: VALIDATING_AWACS_BALANCER' in log.records_text()
    wait_balancer(cache, lambda pb: (pb.order.progress.state.id == 'VALIDATING_AWACS_BALANCER' and
                                     pb.order.progress.state.attempts == 1))

    with check_log(caplog) as log, \
            mock.patch.object(ValidatingAwacsBalancer, '_is_balancer_valid', return_value=True):
        ctl._process(ctx)
        assert 'Current state: VALIDATING_AWACS_BALANCER' in log.records_text()
        assert 'Processed, next state: ACTIVATING_AWACS_BALANCER' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'ACTIVATING_AWACS_BALANCER')

    with check_log(caplog) as log, \
            mock.patch.object(ActivatingAwacsBalancer, '_get_snapshot_to_activate',
                              return_value=model_pb2.SnapshotId()):
        ctl._process(ctx)
        assert 'Current state: ACTIVATING_AWACS_BALANCER' in log.records_text()
        assert 'Processed, next state: FINISH' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'FINISH')


@pytest.mark.parametrize('start_state', [
    'START',
    'ALLOCATING_YP_LITE_RESOURCES',
    'GETTING_ABC_ROLE_STAFF_ID',
    'WAITING_FOR_APPROVAL_AFTER_ALLOCATION',
    'WAITING_FOR_NEW_ABC_SERVICE_ID',
    'REVOKING_APPROVAL_AFTER_CHANGING_ABC_SERVICE',
    'GETTING_VIRTUAL_SERVICE_IDS',
])
def test_cancel(caplog, cache, zk_storage, ctx, ctl, balancer_pb, start_state):
    balancer_pb.order.progress.state.id = start_state
    balancer_pb.order.progress.context['pre_allocation_id'] = ujson.dumps('zzz')
    balancer_pb.order.status.status = 'IN_PROGRESS'
    balancer_pb.order.cancelled.value = True
    balancer_pb.order.cancelled.comment = 'cancelled!'
    balancer_pb.order.cancelled.author = 'robot'
    update_balancer(cache, zk_storage, balancer_pb, check=lambda pb: pb.order.cancelled.value)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'order was cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'running DeallocatingYpLiteResources.process()' in log.records_text()
        assert 'Overall status will be CANCELLED' in log.records_text()
    wait_balancer(cache, lambda pb: pb.order.progress.state.id == 'CANCELLED' and pb.order.status.status == 'CANCELLED')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert 'Status is already CANCELLED' in log.records_text()


@pytest.mark.parametrize('start_state', [
    'CREATING_NANNY_SERVICE',
    'SETTING_UP_CLEANUP_POLICY',
    'SETTING_UP_REPLICATION_POLICY',
    'CREATING_ENDPOINT_SET',
    'CREATING_AWACS_BALANCER',
    'CREATING_BALANCER_BACKEND',
    'VALIDATING_AWACS_BALANCER',
    'ACTIVATING_AWACS_BALANCER',
    'DEALLOCATING_YP_LITE_RESOURCES',
    'CREATING_USER_ENDPOINT_SET',
    'COPYING_NANNY_SERVICE',
    'UPDATING_COPIED_NANNY_SERVICE',
])
def test_cancel_unsupported(caplog, cache, zk_storage, ctx, ctl, balancer_pb, start_state):
    balancer_pb.order.progress.state.id = start_state
    balancer_pb.order.status.status = 'IN_PROGRESS'
    balancer_pb.order.cancelled.value = True
    balancer_pb.order.cancelled.comment = 'cancelled!'
    balancer_pb.order.cancelled.author = 'robot'
    update_balancer(cache, zk_storage, balancer_pb, check=lambda pb: pb.order.cancelled.value)

    with check_log(caplog) as log:
        try:
            ctl._process(ctx)
        except:  # we don't care about actual processing result, so we don't bother to set up the mocks
            pass
        assert 'order was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert 'cannot be cancelled' in log.records_text()
