import mock
import pytest

import yp.data_model as data_model
import yt.yson as yson
from infra.rsc.src.model import rs_updater
from infra.rsc.src.model.pod_maker import PodMaker
from infra.rsc.src.model.rate_limiter import RateLimiter
from infra.rsc.src.model.consts import DEFAULT_OBJECT_SELECTORS


POD_IDS = ['p1', 'p2', 'p3', 'p4', 'p5', 'p6']
CURRENT_REVISION = 1
TARGET_REVISION = 2
CLUSTER = 'sas'
TRANSACTION_ID = 'tid'
TIMESTAMP = 1

IS_INPLACE_UPDATE_DISABLED = False
USE_DEPLOY_STATUS = False


def list_mock_calls_by_name(calls, name):
    ans = []
    for call in calls:
        if call[0] == name:
            ans.append(call)
    return ans


class FakePodLister():
    def __init__(self, pods):
        self.pods = pods

    def list_all(self):
        return self.pods

    def list_by_ids(self, ids):
        ids = set(ids)
        rv = []
        for p in self.pods:
            if p.meta.id in ids:
                rv.append(p)
        return rv

    def list_by_stage_id(self, stage_id, cluster):
        return self.pods


def make_pod(p_id, r):
    p = mock.Mock()
    p.meta.id = p_id
    p.spec.pod_agent_payload.spec.revision = r
    return p


def true_for(pod_ids):
    def fn(pod):
        return pod.meta.id in pod_ids
    return fn


@pytest.fixture(scope='module')
def match_labels():
    rv = data_model.TAttributeDictionary()
    a = rv.attributes.add()
    a.key = 'foo'
    a.value = yson.dumps('bar')
    return rv


@pytest.fixture(scope='module')
def rs():
    rs = data_model.TReplicaSet()
    rs.meta.id = 'rs'
    rs.spec.revision_id = str(TARGET_REVISION)
    rs.spec.deployment_strategy.max_unavailable = 2
    rs.spec.replica_count = 6
    c = rs.spec.constraints.antiaffinity_constraints.add()
    c.key = 'node'
    c.max_pods = 1
    return rs


@pytest.fixture(scope='module')
def ps(rs, match_labels):
    ps = data_model.TPodSet()
    ps.labels.CopyFrom(match_labels)
    ps.meta.id = 'ps'
    ps.spec.account_id = rs.spec.account_id
    ac = rs.spec.constraints.antiaffinity_constraints
    ps.spec.antiaffinity_constraints.MergeFrom(ac)
    return ps


@pytest.fixture
def yp_client():
    client = mock.Mock()
    client.start_transaction.return_value = (TRANSACTION_ID, TIMESTAMP)
    return client


def test_actual_alive_pods(rs, ps, match_labels, yp_client):
    yp_client.get_pod_set_ignore.return_value = ps
    pods = [make_pod(p_id, TARGET_REVISION) for p_id in POD_IDS]
    pod_lister = FakePodLister(pods)

    with mock.patch('infra.rsc.src.lib.podutil.is_pod_alive', return_value=True), \
         mock.patch('infra.rsc.src.lib.podutil.is_pod_spec_updateable', return_value=False), \
         mock.patch('infra.rsc.src.lib.podutil.update_pod_set_needed', return_value=False):

        ctl = rs_updater.ReplicaSetPodsUpdater(client=yp_client,
                                               match_labels=match_labels,
                                               is_inplace_update_disabled=IS_INPLACE_UPDATE_DISABLED,
                                               use_deploy_status=USE_DEPLOY_STATUS,
                                               pod_maker=PodMaker(['root-user'], match_labels),
                                               rate_limiter=RateLimiter())
        ctl.update_allocations(rs=rs, pod_lister=pod_lister)
        assert yp_client.update_replica_set_status.call_count == 1
        assert not yp_client.create_pods.called
        assert not yp_client.remove_pods.called
        assert not yp_client.update_pods.called
        assert not yp_client.update_pods.called


def test_outdated_alive_pods(rs, ps, match_labels, yp_client):
    yp_client.get_pod_set_ignore.return_value = ps
    pods = [make_pod(p_id, CURRENT_REVISION) for p_id in POD_IDS]
    pod_lister = FakePodLister(pods)

    with mock.patch('infra.rsc.src.lib.podutil.is_pod_alive', return_value=True), \
         mock.patch('infra.rsc.src.lib.podutil.is_pod_spec_updateable', return_value=False), \
         mock.patch('infra.rsc.src.lib.podutil.update_pod_set_needed', return_value=False):
        ctl = rs_updater.ReplicaSetPodsUpdater(client=yp_client,
                                               match_labels=match_labels,
                                               is_inplace_update_disabled=IS_INPLACE_UPDATE_DISABLED,
                                               use_deploy_status=USE_DEPLOY_STATUS,
                                               pod_maker=PodMaker(['root-user'], match_labels),
                                               rate_limiter=RateLimiter())
        ctl.update_allocations(rs=rs, pod_lister=pod_lister)

        calls = yp_client.method_calls
        names = [call[0] for call in calls]
        assert names == ['get_pod_set_ignore',
                         'start_transaction',
                         'remove_pods',
                         'create_pods',
                         'commit_transaction',
                         'update_replica_set_status']

        c = list_mock_calls_by_name(calls, 'get_pod_set_ignore')[0]
        assert c == mock.call.get_pod_set_ignore(rs.meta.id, timestamp=None, selectors=DEFAULT_OBJECT_SELECTORS)

        c = list_mock_calls_by_name(calls, 'remove_pods')[0]
        args = c[1]
        removed, tid = args
        pod_ids = set(p.meta.id for p in removed)
        assert len(pod_ids) == 2
        assert pod_ids.issubset(POD_IDS)
        assert tid == TRANSACTION_ID

        c = list_mock_calls_by_name(calls, 'create_pods')[0]
        kw = c[2]
        assert len(kw['pods']) == 2
        assert kw['transaction_id'] == TRANSACTION_ID

        c = list_mock_calls_by_name(calls, 'commit_transaction')[0]
        assert c == mock.call.commit_transaction(TRANSACTION_ID)


def test_replace_with_reduced_window(rs, ps, match_labels, yp_client):
    yp_client.get_pod_set_ignore.return_value = ps
    alive = POD_IDS[1:]
    p1 = make_pod(POD_IDS[0], TARGET_REVISION)
    p2 = make_pod(POD_IDS[1], CURRENT_REVISION)
    p3 = make_pod(POD_IDS[2], CURRENT_REVISION)
    pods = [p1, p2, p3] + [make_pod(p_id, CURRENT_REVISION) for p_id in POD_IDS[3:]]
    pod_lister = FakePodLister(pods)

    with mock.patch('infra.rsc.src.lib.podutil.is_pod_alive', true_for(alive)), \
         mock.patch('infra.rsc.src.lib.podutil.is_pod_spec_updateable', return_value=False), \
         mock.patch('infra.rsc.src.lib.podutil.update_pod_set_needed', return_value=False):
        # replica_count = 6, max_unavailable = 2
        # actual_alive = 0
        # actual_inprogress = 1
        # outdated_alive = 5
        # outdated_inprogress = 0
        # It is possible to replace 1 pod.
        ctl = rs_updater.ReplicaSetPodsUpdater(client=yp_client,
                                               match_labels=match_labels,
                                               is_inplace_update_disabled=IS_INPLACE_UPDATE_DISABLED,
                                               use_deploy_status=USE_DEPLOY_STATUS,
                                               pod_maker=PodMaker(['root-user'], match_labels),
                                               rate_limiter=RateLimiter())
        ctl.update_allocations(rs=rs, pod_lister=pod_lister)

        calls = yp_client.method_calls
        names = [call[0] for call in calls]
        assert names == ['get_pod_set_ignore',
                         'start_transaction',
                         'remove_pods',
                         'create_pods',
                         'commit_transaction',
                         'update_replica_set_status']

        c = list_mock_calls_by_name(calls, 'get_pod_set_ignore')[0]
        assert c == mock.call.get_pod_set_ignore(rs.meta.id, timestamp=None, selectors=DEFAULT_OBJECT_SELECTORS)

        c = list_mock_calls_by_name(calls, 'remove_pods')[0]
        removed, tid = c[1]
        pod_ids = set(p.meta.id for p in removed)
        assert len(pod_ids) == 1
        assert pod_ids.issubset(alive)
        assert tid == TRANSACTION_ID

        c = list_mock_calls_by_name(calls, 'create_pods')[0]
        kw = c[2]
        assert len(kw['pods']) == 1
        assert kw['transaction_id'] == TRANSACTION_ID

        c = list_mock_calls_by_name(calls, 'commit_transaction')[0]
        assert c == mock.call.commit_transaction(TRANSACTION_ID)


def test_wait_actual_pods(rs, ps, match_labels, yp_client):
    yp_client.get_pod_set_ignore.return_value = ps
    alive = POD_IDS[2:]
    p1 = make_pod(POD_IDS[0], TARGET_REVISION)
    p2 = make_pod(POD_IDS[1], TARGET_REVISION)
    p3 = make_pod(POD_IDS[2], CURRENT_REVISION)
    pods = [p1, p2, p3] + [make_pod(p_id, CURRENT_REVISION) for p_id in POD_IDS[3:]]
    pod_lister = FakePodLister(pods)

    with mock.patch('infra.rsc.src.lib.podutil.is_pod_alive', true_for(alive)), \
         mock.patch('infra.rsc.src.lib.podutil.is_pod_spec_updateable', return_value=False):
        # replica_count = 6, max_unavailable = 2
        # actual_alive = 0
        # actual_in_progress = 2
        # outdated_alive = 4
        # outdated_in_progress = 0
        # We need to wait until actual_inprogress pod becomes alive.
        ctl = rs_updater.ReplicaSetPodsUpdater(client=yp_client,
                                               match_labels=match_labels,
                                               is_inplace_update_disabled=IS_INPLACE_UPDATE_DISABLED,
                                               use_deploy_status=USE_DEPLOY_STATUS,
                                               pod_maker=PodMaker(['root-user'], match_labels),
                                               rate_limiter=RateLimiter())
        ctl.update_allocations(rs=rs,
                               pod_lister=pod_lister)

        assert yp_client.update_replica_set_status.call_count == 1
        assert not yp_client.create_pods.called
        assert not yp_client.remove_pods.called
        assert not yp_client.update_pods.called
        assert not yp_client.update_pods.called


def test_eviction_requested_pods(rs, ps, match_labels, yp_client):
    yp_client.get_pod_set_ignore.return_value = ps
    alive = POD_IDS[1:]
    eviction_requested = set(POD_IDS[:1])
    pods = [make_pod(p_id, TARGET_REVISION) for p_id in POD_IDS]
    pod_lister = FakePodLister(pods)

    with mock.patch('infra.rsc.src.lib.podutil.is_pod_alive', true_for(alive)), \
         mock.patch('infra.rsc.src.lib.podutil.is_pod_spec_updateable', return_value=False), \
         mock.patch('infra.rsc.src.lib.podutil.is_pod_finally_dead', true_for(eviction_requested)), \
         mock.patch('infra.rsc.src.lib.podutil.update_pod_set_needed', return_value=False):
        # replica_count = 6, max_unavailable = 2
        # actual_alive = 5
        # actual_in_progress = 0
        # outdated_alive = 0
        # outdated_in_progress = 0
        # eviction_requested = 1
        ctl = rs_updater.ReplicaSetPodsUpdater(client=yp_client,
                                               match_labels=match_labels,
                                               is_inplace_update_disabled=IS_INPLACE_UPDATE_DISABLED,
                                               use_deploy_status=USE_DEPLOY_STATUS,
                                               pod_maker=PodMaker(['root-user'], match_labels),
                                               rate_limiter=RateLimiter())
        ctl.update_allocations(rs=rs,
                               pod_lister=pod_lister)
        calls = yp_client.method_calls
        names = [call[0] for call in calls]
        assert names == ['get_pod_set_ignore',
                         'update_pods_acknowledge_eviction',
                         'update_replica_set_status']

        c = list_mock_calls_by_name(calls, 'get_pod_set_ignore')[0]
        assert c == mock.call.get_pod_set_ignore(rs.meta.id, timestamp=None, selectors=DEFAULT_OBJECT_SELECTORS)

        c = list_mock_calls_by_name(calls, 'update_pods_acknowledge_eviction')[0]
        name, args, kwargs = c
        evicted, _ = args
        assert set(p.meta.id for p in evicted) == eviction_requested


def test_replace_recently_created_pods(rs, ps, match_labels, yp_client):
    yp_client.get_pod_set_ignore.return_value = ps
    pods = [make_pod(p_id, CURRENT_REVISION) for p_id in POD_IDS]
    pod_lister = FakePodLister(pods)

    with mock.patch('infra.rsc.src.lib.podutil.is_pod_alive', return_value=True), \
         mock.patch('infra.rsc.src.lib.podutil.is_pod_spec_updateable', return_value=False), \
         mock.patch('infra.rsc.src.lib.podutil.update_pod_set_needed', return_value=False):
        ctl = rs_updater.ReplicaSetPodsUpdater(client=yp_client,
                                               match_labels=match_labels,
                                               is_inplace_update_disabled=False,
                                               use_deploy_status=USE_DEPLOY_STATUS,
                                               pod_maker=PodMaker(['root-user'], match_labels),
                                               rate_limiter=RateLimiter(enabled=True, delay_secs=60))
        ctl.update_allocations(rs=rs, pod_lister=pod_lister)
        # replace two pods
        # now we are waiting 60 secs to replace/update once again
        ctl.update_allocations(rs=rs, pod_lister=pod_lister)

        calls = yp_client.method_calls

        c = list_mock_calls_by_name(calls, 'create_pods')
        assert len(c) == 1
        kw = c[0][2]
        assert len(kw['pods']) == 2

        c = list_mock_calls_by_name(calls, 'remove_pods')
        assert len(c) == 1
        args = c[0][1]
        assert len(args[0]) == 2
        # only 1 replace pods


def test_inplace_update_recently_created_pods(rs, ps, match_labels, yp_client):
    yp_client.get_pod_set_ignore.return_value = ps
    pods = [make_pod(p_id, CURRENT_REVISION) for p_id in POD_IDS]
    pod_lister = FakePodLister(pods)

    with mock.patch('infra.rsc.src.lib.podutil.is_pod_alive', return_value=True), \
         mock.patch('infra.rsc.src.lib.podutil.is_pod_spec_updateable', return_value=True), \
         mock.patch('infra.rsc.src.lib.podutil.update_pod_set_needed', return_value=False):
        ctl = rs_updater.ReplicaSetPodsUpdater(client=yp_client,
                                               match_labels=match_labels,
                                               is_inplace_update_disabled=False,
                                               use_deploy_status=USE_DEPLOY_STATUS,
                                               pod_maker=PodMaker(['root-user'], match_labels),
                                               rate_limiter=RateLimiter(enabled=True, delay_secs=60))
        ctl.update_allocations(rs=rs, pod_lister=pod_lister)
        # inplace update two pods
        ctl.update_allocations(rs=rs, pod_lister=pod_lister)
        # no problem to inplace update two more pods

        calls = yp_client.method_calls
        c = list_mock_calls_by_name(calls, 'update_pods')

        assert len(c) == 2
        # 2 times inplace update pods
