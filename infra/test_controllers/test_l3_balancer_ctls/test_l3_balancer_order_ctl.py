import pytest

from awacs.model import events
from awacs.model.l3_balancer.order.ctl import L3BalancerOrderCtl as BaseL3BalancerOrderCtl
from awtest import check_log, wait_until_passes
from awtest.api import create_ns
from awtest.pb import cancel_order
from .conftest import (
    update_l3_balancer,
    wait_l3_balancer,
    create_l3_order_pb,
    L3_BALANCER_ID,
    NS_ID,
    create_active_l3_state,
)


class L3BalancerOrderCtl(BaseL3BalancerOrderCtl):
    PROCESSING_INTERVAL = 0

    def __init__(self, *args, **kwargs):
        super(L3BalancerOrderCtl, self).__init__(*args, **kwargs)
        self._runner.processing_interval = self.PROCESSING_INTERVAL


@pytest.fixture
def ctl(cache, zk_storage):
    ctl = L3BalancerOrderCtl(NS_ID, L3_BALANCER_ID)
    ctl._pb = create_l3_order_pb(cache, zk_storage)
    return ctl


def test_old_event_generation(caplog, cache, zk_storage, ctx, ctl):
    event = events.DnsRecordUpdate(path='', pb=ctl._pb)
    event.pb.meta.generation = -1
    with check_log(caplog) as log:
        ctl._process(ctx, event)
        assert u'Skipped event with stale generation -1' in log.records_text()
        assert u'Assigned initial state "START"' not in log.records_text()


def test_not_started(caplog, cache, zk_storage, ctx, ctl):
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Assigned initial state "STARTED"' in log.records_text()


def test_finished(caplog, cache, zk_storage, ctx, ctl):
    ctl._pb.order.status.status = u'FINISHED'
    update_l3_balancer(cache, zk_storage, ctl._pb, check=lambda pb: not pb.order.status.status == u'FINISHED')
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Status is already FINISHED' in log.records_text()


@pytest.mark.parametrize(u'start_state', [
    u'STARTED',
    u'CREATING_SLB_PING_UPSTREAM',
    u'GETTING_ABC_SLUGS',
])
def test_cancel(caplog, cache, zk_storage, ctx, start_state, ctl):
    cancel_order(ctl._pb, start_state)
    update_l3_balancer(cache, zk_storage, ctl._pb, check=lambda _pb: _pb.order.cancelled.value)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'order was cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert u'Processed, next state: CANCELLED' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'CANCELLED')


@pytest.mark.parametrize(u'start_state', [
    u'CREATING_L3_MGR_SERVICE',
    u'UPDATING_L3_MGR_SERVICE_PERMISSIONS',
    u'ACQUIRING_IPV6_ADDRESS',
    u'ACQUIRING_IPV4_ADDRESS',
    u'CREATING_VIRTUAL_SERVERS',
    u'SAVING_SPEC',
    u'WAITING_FOR_ACTIVATION',
    u'ASSIGNING_FIREWALL_GRANTS',
])
def test_cancel_unsupported(caplog, cache, zk_storage, ctx, start_state, ctl):
    cancel_order(ctl._pb, start_state)
    update_l3_balancer(cache, zk_storage, ctl._pb, check=lambda _pb: _pb.order.cancelled.value)
    with check_log(caplog) as log:
        try:
            ctl._process(ctx)
        except:
            pass
        assert u'order was marked as cancelled by "robot" with comment "cancelled!"' in log.records_text()
        assert u'cannot be cancelled' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == start_state)


@pytest.mark.parametrize(u'ctl_version', (1, 2))
def test_transitions(caplog, cache, zk_storage, ctx, ctl, ctl_version):
    ctl._pb.order.progress.state.id = u'STARTED'
    ctl._pb.order.content.ctl_version = ctl_version
    update_l3_balancer(cache, zk_storage, ctl._pb, check=lambda pb: pb.order.progress.state.id == u'STARTED')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: STARTED' in log.records_text()
        assert u'Processed, next state: CREATING_SLB_PING_UPSTREAM' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'CREATING_SLB_PING_UPSTREAM')

    create_ns(NS_ID, cache, zk_storage)
    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: CREATING_SLB_PING_UPSTREAM' in log.records_text()
        assert u'Processed, next state: GETTING_ABC_SLUGS' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'GETTING_ABC_SLUGS')
    wait_until_passes(lambda: cache.must_get_upstream(NS_ID, 'slbping'))

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: GETTING_ABC_SLUGS' in log.records_text()
        assert u'Processed, next state: CREATING_L3_MGR_SERVICE' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'CREATING_L3_MGR_SERVICE')

    if ctl_version < 2:
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'Current state: CREATING_L3_MGR_SERVICE' in log.records_text()
            assert u'Processed, next state: UPDATING_L3_MGR_SERVICE_PERMISSIONS' in log.records_text()
        wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'UPDATING_L3_MGR_SERVICE_PERMISSIONS')

        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'Current state: UPDATING_L3_MGR_SERVICE_PERMISSIONS' in log.records_text()
            assert u'Processed, next state: ACQUIRING_IPV6_ADDRESS' in log.records_text()
        wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'ACQUIRING_IPV6_ADDRESS')
    else:
        with check_log(caplog) as log:
            ctl._process(ctx)
            assert u'Current state: CREATING_L3_MGR_SERVICE' in log.records_text()
            assert u'Processed, next state: ACQUIRING_IPV6_ADDRESS' in log.records_text()
        wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'ACQUIRING_IPV6_ADDRESS')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: ACQUIRING_IPV6_ADDRESS' in log.records_text()
        assert u'Processed, next state: ACQUIRING_IPV4_ADDRESS' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'ACQUIRING_IPV4_ADDRESS')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: ACQUIRING_IPV4_ADDRESS' in log.records_text()
        assert u'Processed, next state: CREATING_VIRTUAL_SERVERS' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'CREATING_VIRTUAL_SERVERS')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: CREATING_VIRTUAL_SERVERS' in log.records_text()
        assert u'Processed, next state: SAVING_VS_CONFIG' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'SAVING_VS_CONFIG')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: SAVING_VS_CONFIG' in log.records_text()
        assert u'Processed, next state: UPDATING_L7_CONTAINER_SPEC' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'UPDATING_L7_CONTAINER_SPEC')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: UPDATING_L7_CONTAINER_SPEC' in log.records_text()
        assert u'Processed, next state: SAVING_SPEC' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'SAVING_SPEC')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: SAVING_SPEC' in log.records_text()
        assert u'Processed, next state: WAITING_FOR_ACTIVATION' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'WAITING_FOR_ACTIVATION')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: WAITING_FOR_ACTIVATION' in log.records_text()
        assert u'Processed, next state: WAITING_FOR_ACTIVATION' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'WAITING_FOR_ACTIVATION')

    create_active_l3_state(cache, zk_storage)

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: WAITING_FOR_ACTIVATION' in log.records_text()
        assert u'Processed, next state: ASSIGNING_FIREWALL_GRANTS' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'ASSIGNING_FIREWALL_GRANTS')

    with check_log(caplog) as log:
        ctl._process(ctx)
        assert u'Current state: ASSIGNING_FIREWALL_GRANTS' in log.records_text()
        assert u'Processed, next state: FINISHED' in log.records_text()
    wait_l3_balancer(cache, lambda pb: pb.order.progress.state.id == u'FINISHED')
