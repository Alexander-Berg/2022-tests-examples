import logging

import inject
import pytest
import ujson

from awacs.lib.nannyclient import INannyClient
from awacs.lib.ypliterpcclient import IYpLiteRpcClient
from awacs.model.balancer.removal import processors as p
from awacs.model.balancer.removal.processors import BalancerRemoval
from awtest import wait_until, wait_until_passes
from awtest.mocks.nanny_client import NannyMockClient
from awtest.mocks.yp_lite_client import YpLiteMockClient
from infra.awacs.proto import model_pb2


NS_ID = u'namespace-id'
BALANCER_ID = u'balancer-id_sas'


@pytest.fixture
def nanny_client():
    return NannyMockClient(url='https://nanny.yandex-team.ru/v2/', token='DUMMY')


@pytest.fixture
def yp_lite_client():
    return YpLiteMockClient()


@pytest.fixture(autouse=True)
def deps(binder, caplog, nanny_client, yp_lite_client):
    caplog.set_level(logging.DEBUG)

    def configure(b):
        b.bind(INannyClient, nanny_client)
        b.bind(IYpLiteRpcClient, yp_lite_client)
        binder(b)

    inject.clear_and_configure(configure)
    yield
    inject.clear()


def create_balancer(cache, zk_storage, mode=model_pb2.BalancerRemoval.Content.MANUAL):
    b_pb = model_pb2.Balancer()
    b_pb.meta.id = BALANCER_ID
    b_pb.meta.namespace_id = NS_ID
    b_pb.spec.deleted = True
    b_pb.spec.config_transport.nanny_static_file.service_id = 'balancer_service_id_to_remove'
    b_pb.removal.content.mode = mode
    zk_storage.create_balancer(namespace_id=NS_ID,
                               balancer_id=BALANCER_ID,
                               balancer_pb=b_pb)
    wait_until_passes(lambda: cache.must_get_balancer(NS_ID, BALANCER_ID))
    return BalancerRemoval(b_pb)


def update_balancer(cache, zk_storage, b_pb, check):
    for pb in zk_storage.update_balancer(NS_ID, BALANCER_ID):
        pb.CopyFrom(b_pb)
    wait_balancer(cache, check)


def wait_balancer(cache, check):
    assert wait_until(lambda: check(cache.must_get_balancer(NS_ID, BALANCER_ID)), timeout=1)


def test_start(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    assert p.Start.process(ctx, balancer).next_state.name == 'REMOVING_BALANCER_BACKENDS'


def test_removing_balancer_backends_noop(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    assert p.RemovingBalancerBackends.process(ctx, balancer).next_state.name == 'REMOVING_BALANCER'


def test_removing_balancer_backends(cache, zk_storage, ctx):
    b_pb = model_pb2.Backend()
    b_pb.meta.namespace_id = NS_ID
    b_pb.spec.selector.type = model_pb2.BackendSelector.BALANCERS
    b_pb.spec.selector.balancers.add(id=BALANCER_ID)
    for i in range(3):
        b_pb.meta.id = BALANCER_ID + str(i)
        zk_storage.create_backend(NS_ID, BALANCER_ID + str(i), b_pb)

    def wait_for_backends():
        for j in range(3):
            cache.must_get_backend(NS_ID, BALANCER_ID + str(j))

    wait_until_passes(wait_for_backends)

    balancer = create_balancer(cache, zk_storage)
    rv = p.RemovingBalancerBackends.process(ctx, balancer)
    assert rv.description == ('This balanced is still used in following backends: '
                              '"balancer-id_sas0", "balancer-id_sas1", "balancer-id_sas2"')
    assert rv.severity == model_pb2.FB_SEVERITY_ACTION_REQUIRED
    assert isinstance(rv.content_pb, model_pb2.BalancerRemoval.RemovalFeedback.UsedInBackendsError)
    assert rv.content_pb.backend_ids == ["balancer-id_sas0", "balancer-id_sas1", "balancer-id_sas2"]


def test_removing_balancer_backends_with_service(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage, mode=model_pb2.BalancerRemoval.Content.AUTOMATIC)
    assert p.RemovingBalancerBackends.process(ctx, balancer).next_state.name == 'SHUTTING_DOWN_SERVICE'


def test_shutting_down_service(cache, zk_storage, ctx, nanny_client):
    balancer = create_balancer(cache, zk_storage)
    assert p.ShuttingDownService.process(ctx, balancer).next_state.name == 'WAITING_FOR_TIMEOUT_OR_APPROVAL'
    assert balancer.pb.spec.config_transport.nanny_static_file.service_id in nanny_client.shutdown_service_called


def test_waiting_for_timeout_or_approval_with_approval(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    balancer.pb.removal.approval.after_service_shutdown.value = True
    assert p.WaitingForTimeoutOrApproval.process(ctx, balancer).next_state.name == 'REMOVING_SERVICE_SNAPSHOTS'


def test_waiting_for_timeout_or_approval_wo_approval(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    balancer.pb.removal.progress.state.entered_at.GetCurrentTime()
    assert p.WaitingForTimeoutOrApproval.process(ctx, balancer).next_state.name == 'WAITING_FOR_TIMEOUT_OR_APPROVAL'


def test_removing_service_snapshots(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    assert p.RemovingServiceSnapshots.process(ctx, balancer).next_state.name == 'REMOVING_SERVICE_FROM_DASHBOARDS'


def test_removing_service_snapshots_active(cache, zk_storage, ctx, nanny_client):
    balancer = create_balancer(cache, zk_storage)
    balancer.pb.spec.config_transport.nanny_static_file.service_id = 'active'
    assert p.RemovingServiceSnapshots.process(ctx, balancer).next_state.name == 'REMOVING_SERVICE_SNAPSHOTS'
    assert balancer.pb.spec.config_transport.nanny_static_file.service_id in nanny_client.remove_service_snapshot_calls


def test_removing_service_from_dashboards(cache, zk_storage, ctx, nanny_client):
    balancer = create_balancer(cache, zk_storage)
    assert p.RemovingServiceFromDashboards.process(ctx, balancer).next_state.name == 'REMOVING_POD_SET'
    service_id = balancer.pb.spec.config_transport.nanny_static_file.service_id
    dashboard_id = 'dashboard${}'.format(service_id)
    assert dashboard_id in nanny_client.last_update_dashboard_contents
    assert not nanny_client.last_update_dashboard_contents[dashboard_id]['groups'][0]['services']


def test_removing_pod_set_gencfg(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    assert p.RemovingPodSet.process(ctx, balancer).next_state.name == 'REMOVING_SERVICE'


def test_removing_pod_set_incomplete(cache, zk_storage, ctx, yp_lite_client):
    cluster = 'test_sas'
    pre_allocation_id = 'xxxx'
    pod_set_id = 'yyyy'

    balancer = create_balancer(cache, zk_storage)
    balancer.pb.meta.location.type = balancer.pb.meta.location.YP_CLUSTER
    balancer.pb.meta.location.yp_cluster = cluster
    balancer.pb.spec.incomplete = True
    balancer.pb.order.progress.context['pre_allocation_id'] = ujson.dumps(pre_allocation_id)
    balancer.pb.order.progress.context['pod_set_id'] = ujson.dumps(pod_set_id)
    assert p.RemovingPodSet.process(ctx, balancer).next_state.name == 'REMOVING_ENDPOINT_SETS'
    assert yp_lite_client.last_remove_pod_set_request_args
    req_pb = yp_lite_client.last_remove_pod_set_request_args[0]
    assert req_pb.cluster == cluster
    assert req_pb.service_id == pod_set_id
    assert req_pb.pre_allocation_id == pre_allocation_id


def test_removing_pod_set(cache, zk_storage, ctx, yp_lite_client):
    cluster = 'test_sas'

    balancer = create_balancer(cache, zk_storage)
    balancer.pb.meta.location.type = balancer.pb.meta.location.YP_CLUSTER
    balancer.pb.meta.location.yp_cluster = cluster
    assert p.RemovingPodSet.process(ctx, balancer).next_state.name == 'REMOVING_ENDPOINT_SETS'
    assert yp_lite_client.last_remove_pod_set_request_args
    req_pb = yp_lite_client.last_remove_pod_set_request_args[0]
    assert req_pb.cluster == cluster
    assert req_pb.service_id == balancer.pb.spec.config_transport.nanny_static_file.service_id


def test_removing_endpoint_sets_incomplete(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    balancer.pb.spec.incomplete = True
    assert p.RemovingEndpointSets.process(ctx, balancer).next_state.name == 'REMOVING_SERVICE'


def test_removing_endpoint_sets(cache, zk_storage, ctx, yp_lite_client):
    cluster = 'test_sas'

    balancer = create_balancer(cache, zk_storage)
    balancer.pb.meta.location.type = balancer.pb.meta.location.YP_CLUSTER
    balancer.pb.meta.location.yp_cluster = cluster
    assert p.RemovingEndpointSets.process(ctx, balancer).next_state.name == 'REMOVING_SERVICE'
    assert yp_lite_client.last_remove_endpoint_sets_request_args
    req_pb = yp_lite_client.last_remove_endpoint_sets_request_args[0]
    assert req_pb.cluster == cluster
    assert req_pb.ids == ['user_es']


def test_removing_service(cache, zk_storage, ctx, nanny_client):
    balancer = create_balancer(cache, zk_storage)
    assert p.RemovingService.process(ctx, balancer).next_state.name == 'REMOVING_BALANCER'
    assert balancer.pb.spec.config_transport.nanny_static_file.service_id in nanny_client.remove_service_called


def test_removing_balancer(cache, zk_storage, ctx):
    balancer = create_balancer(cache, zk_storage)
    assert p.RemovingBalancer.process(ctx, balancer).next_state.name == 'FINISHED'
    wait_until(lambda: cache.get_balancer(NS_ID, BALANCER_ID) is None, timeout=1)


def test_cancelling(ctx, cache, zk_storage):
    balancer = create_balancer(cache, zk_storage)
    assert p.Cancelling.process(ctx, balancer).next_state.name == u'CANCELLED'
    assert wait_until(lambda: not cache.must_get_balancer(NS_ID, BALANCER_ID).spec.deleted)


def test_restoring_balancer_backends(ctx, cache, zk_storage, dao):
    balancer = create_balancer(cache, zk_storage)

    ctx.log.info(u'Step 1: create user backend pointing to the balancer, with spec.deleted=True')
    backend_spec_pb = model_pb2.BackendSpec()
    backend_spec_pb.selector.type = model_pb2.BackendSelector.BALANCERS
    backend_spec_pb.selector.balancers.add(id=balancer.id)
    backend_spec_pb.deleted = True
    user_backend_id = balancer.id + u'_user'
    dao.create_backend_if_missing(
        meta_pb=model_pb2.BackendMeta(
            id=user_backend_id,
            namespace_id=balancer.namespace_id
        ),
        spec_pb=backend_spec_pb)
    wait_until_passes(lambda: cache.must_get_backend(balancer.namespace_id, user_backend_id))
    # we didn't create system backend
    assert cache.get_system_backend_for_balancer(balancer.namespace_id, balancer.id) is None

    ctx.log.info(u'Step 2: make sure that user backend has spec.deleted=False, '
                 u'and that system backend for balancer does exist')
    assert p.RestoringBalancerBackends.process(ctx, balancer).next_state.name == u'CANCELLING'
    assert wait_until(lambda: not cache.must_get_backend(balancer.namespace_id, user_backend_id).spec.deleted)
    assert wait_until(lambda: cache.get_system_backend_for_balancer(balancer.namespace_id, balancer.id))


def test_activating_nanny_service(cache, zk_storage, ctx, nanny_client):
    balancer = create_balancer(cache, zk_storage)
    balancer.pb.spec.config_transport.nanny_static_file.service_id = u'inactive'
    assert p.ActivatingNannyService.process(ctx, balancer).next_state.name == u'RESTORING_BALANCER_BACKENDS'
    assert nanny_client.set_snapshot_state_calls[(u'inactive', u'z')] == u'ACTIVE'
