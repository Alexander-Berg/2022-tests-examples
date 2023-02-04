import mock

import gevent
import time
import pytest
import yp.common
import yp.data_model
import yt.yson as yson
from yp_proto.yp.client.api.proto import pod_agent_pb2
from sepelib.core import config

from infra.mc_rsc.src import consts
from infra.mc_rsc.src import model
from infra.mc_rsc.src import podutil
from infra.mc_rsc.src import yputil
from infra.mc_rsc.src.circuit_breaker import CircuitBreaker
from infra.mc_rsc.src.controller.controller import Controller
from infra.mc_rsc.src.rate_limiter import RateLimiter
from infra.mc_rsc.src.storage import (ClusterStorage,
                                      PodClusterStorage,
                                      RelationClusterStorage,
                                      MultiClusterStorage,
                                      PodMultiClusterStorage)
from infra.mc_rsc.src.status_maker import MultiClusterReplicaSetStatusMaker
from infra.mc_rsc.src.lib.condition import Condition


MC_RS_ID = 'mc-rs-id'
OLD_REVISION = 1
TARGET_REVISION = 2
REPLICA_COUNT = 3
MAX_UNAVAILABLE = 2
MAX_TOLERABLE_DOWNTIME_PODS = 0
MAX_TOLERABLE_DOWNTIME_SECONDS = 0
FIRST_CLUSTER = 'sas'
SECOND_CLUSTER = 'man'
CLUSTERS = [FIRST_CLUSTER, SECOND_CLUSTER]
XDC = 'xdc'
TRANSACTION_ID = 'tid'
TIMESTAMP = 1
SYNC_TS = 1
DEFAULT_ACL = [podutil.make_default_access_control_entry(['some-user'])]
MAX_PODS_TO_PROCESS = 100
UPDATE_PODS_BATCH_SIZE = 10
REALLOCATE_PODS_BATCH_SIZE = 10
REPLACE_PODS_BATCH_SIZE = 10


class FakeYtResponseError(yp.common.YtResponseError):
    def __str__(self):
        return 'Failed'


def list_mock_calls_by_name(calls, name):
    rv = []
    for call in calls:
        if call[0] == name:
            rv.append(call)
    return rv


def make_yp_client():
    client = mock.Mock()
    client.start_transaction.return_value = (TRANSACTION_ID, TIMESTAMP)
    return client


def make_yp_clients():
    rv = {}
    for c in CLUSTERS + [XDC]:
        rv[c] = make_yp_client()
    return rv


def make_match_labels():
    rv = yp.data_model.TAttributeDictionary()
    a = rv.attributes.add()
    a.key = 'environ'
    a.value = yson.dumps('dev')
    return rv


def make_circuit_breaker():
    rv = mock.Mock()
    rv.is_open.return_value = (False, '')
    return rv


def make_rate_limiter():
    rv = mock.Mock()
    rv.is_process_allowed.return_value = (True, '')
    return rv


def make_mc_rs(clusters=CLUSTERS,
               max_unavailable=MAX_UNAVAILABLE,
               max_tolerable_downtime_pods=MAX_TOLERABLE_DOWNTIME_PODS,
               max_tolerable_downtime_seconds=MAX_TOLERABLE_DOWNTIME_SECONDS,
               replica_count=REPLICA_COUNT):
    obj = yp.data_model.TMultiClusterReplicaSet()
    obj.meta.id = MC_RS_ID
    obj.meta.fqid = 'mc-rs-fqid'
    obj.spec.revision = TARGET_REVISION
    obj.spec.deployment_strategy.max_unavailable = max_unavailable
    obj.spec.deployment_strategy.max_tolerable_downtime_pods = max_tolerable_downtime_pods
    obj.spec.deployment_strategy.max_tolerable_downtime_seconds = max_tolerable_downtime_seconds
    for cluster in clusters:
        c = obj.spec.clusters.add()
        c.cluster = cluster
        c.spec.replica_count = replica_count
        ac = c.spec.constraints.antiaffinity_constraints.add()
        ac.key = 'node'
        ac.max_pods = 1
    return model.MultiClusterReplicaSet(obj=obj, cluster=XDC)


def make_ps(labels, cluster):
    ps = yp.data_model.TPodSet()
    e = ps.meta.acl.add()
    e.action = yp.data_model.ACA_ALLOW
    e.permissions.extend([yp.data_model.ACP_READ,
                          yp.data_model.ACA_WRITE,
                          yp.data_model.ACA_CREATE,
                          yp.data_model.ACA_SSH_ACCESS,
                          yp.data_model.ACA_ROOT_SSH_ACCESS])
    e.subjects.extend(['some-user'])
    ps.labels.CopyFrom(labels)
    ps.meta.id = MC_RS_ID
    ps.meta.fqid = 'ps-fqid-{}'.format(cluster)
    ac = ps.spec.antiaffinity_constraints.add()
    ac.key = 'node'
    ac.max_pods = 1
    return ps, {}


def make_relation(rel_id, from_fqid, to_fqid):
    r = yp.data_model.TRelation()
    r.meta.id = rel_id
    r.meta.from_fqid = from_fqid
    r.meta.to_fqid = to_fqid
    return r


@pytest.fixture
def enable_heartbeat_alert():
    config.set_value('controller.disruptive_node_alerts', {'agent-heartbeat-timeout': 43200})


def create_alert(is_heartbeat=False, duration=0):
    alert_pb = yp.data_model.TNodeAlert()
    alert_pb.type = 'agent-heartbeat-timeout' if is_heartbeat else 'noname'
    alert_pb.creation_time.seconds = int(time.time()) - duration
    return alert_pb


def create_maintenance(state=yp.data_model.PMS_REQUESTED, duration=0, kind=yp.data_model.MK_REBOOT,
                       estimated_duration=0, node_set_id='', disruptive=False):
    maintenance_pb = yp.data_model.TPodStatus.TMaintenance()
    maintenance_pb.state = state
    maintenance_pb.last_updated = int((time.time() - duration) * 1000000)
    maintenance_pb.info.kind = kind
    maintenance_pb.info.disruptive = disruptive
    maintenance_pb.info.estimated_duration.seconds = estimated_duration
    maintenance_pb.info.node_set_id = node_set_id
    return maintenance_pb


def make_pods(prefix, count=1, spec_revision=TARGET_REVISION, is_ready=True, is_eviction_requested=False,
              is_eviction_acknowledged=False, maintenance=None, eviction_reason=yp.data_model.ER_NONE, node_alerts=None,
              spec_timestamps=None, target_state=None, is_spec_applied_by_pod_agent=True, agent_spec_timestamp=0,
              is_failed=False, labels=None):
    assert not (is_eviction_acknowledged and is_eviction_requested)
    if spec_timestamps is None:
        spec_timestamps = []
    else:
        assert len(spec_timestamps) == count
    pods = []
    for n in xrange(count):
        p_id = '{}-{}'.format(prefix, n + 1)
        p = yp.data_model.TPod()
        p.meta.id = p_id
        p.meta.pod_set_id = MC_RS_ID
        p.spec.pod_agent_payload.spec.revision = spec_revision
        p.status.scheduling.node_id = 'some-node'
        p.status.agent_spec_timestamp = agent_spec_timestamp
        if is_failed:
            p.status.agent.pod_agent_payload.status.revision = spec_revision
            p.status.agent.state = yp.data_model.PCS_STARTED
            p.status.agent.pod_agent_payload.status.failed.status = pod_agent_pb2.EConditionStatus_TRUE
        elif is_ready:
            p.status.agent.pod_agent_payload.status.revision = spec_revision
            p.status.agent.state = yp.data_model.PCS_STARTED
            p.status.agent.pod_agent_payload.status.ready.status = pod_agent_pb2.EConditionStatus_TRUE
        else:
            p.status.agent.state = yp.data_model.PCS_UNKNOWN
        if is_eviction_requested:
            p.status.eviction.state = yp.data_model.ES_REQUESTED
            p.status.eviction.reason = eviction_reason
        elif is_eviction_acknowledged:
            p.status.eviction.state = yp.data_model.ES_ACKNOWLEDGED
        if maintenance is not None:
            p.status.maintenance.CopyFrom(maintenance)
        if node_alerts is not None:
            p.status.node_alerts.extend(node_alerts)
        if not is_spec_applied_by_pod_agent:
            p.status.master_spec_timestamp = p.status.agent.pod_agent_payload.status.spec_timestamp + 1
        if spec_timestamps:
            pods.append((p, spec_timestamps[n]))
        else:
            pods.append((p, None))
        if target_state:
            p.spec.pod_agent_payload.spec.target_state = target_state
    labels = labels or {}
    for k, v in labels.iteritems():
        yputil.set_label(p.labels, k, v)
    return pods


def make_controller(mc_rs, pod_storage, match_labels=None, ps_storage=None, relation_storage=None,
                    circuit_breaker=None, rate_limiter=None, client=None,
                    max_pods_to_process=MAX_PODS_TO_PROCESS, yp_clients=None,
                    replace_pods_batch_size=REPLACE_PODS_BATCH_SIZE,
                    reallocate_pods_batch_size=REALLOCATE_PODS_BATCH_SIZE):
    match_labels = match_labels or make_match_labels()
    if ps_storage is None:
        ps_storage = MultiClusterStorage()
        ps = make_ps(match_labels, FIRST_CLUSTER)
        s = ClusterStorage()
        s.sync_with_objects(objs_with_timestamps=[ps])
        ps_storage.add_storage(s, FIRST_CLUSTER)
    if relation_storage is None:
        relation_storage = RelationClusterStorage()
        for num, c in enumerate(mc_rs.list_spec_clusters()):
            ps = ps_storage.get(mc_rs.meta.id, cluster=c)
            if ps:
                r = make_relation(rel_id='relation-{}'.format(num),
                                  from_fqid=mc_rs.meta.fqid,
                                  to_fqid=ps.meta.fqid)
                relation_storage.put(r)
    mc_rs_client = mock.Mock()
    status_maker = MultiClusterReplicaSetStatusMaker()
    circuit_breaker = circuit_breaker or make_circuit_breaker()
    rate_limiter = rate_limiter or make_rate_limiter()
    ctl = Controller(
        yp_clients=yp_clients,
        mc_rs_client=mc_rs_client,
        default_acl=DEFAULT_ACL,
        match_labels=match_labels,
        max_pods_to_process=max_pods_to_process,
        update_pods_batch_size=UPDATE_PODS_BATCH_SIZE,
        reallocate_pods_batch_size=reallocate_pods_batch_size,
        replace_pods_batch_size=replace_pods_batch_size,
        clusters=CLUSTERS if yp_clients is None else yp_clients.keys(),
        pod_storage=pod_storage,
        ps_storage=ps_storage,
        relation_storage=relation_storage,
        mc_rs_status_maker=status_maker,
        circuit_breaker=circuit_breaker,
        rate_limiter=rate_limiter
    )
    if yp_clients is None:
        client = client or make_yp_client()
        ctl.get_yp_client = mock.Mock()
        ctl.get_yp_client.return_value = client
    return ctl, client


def test_actual_ready_pods():
    pod_storage = PodMultiClusterStorage()
    ps_storage = MultiClusterStorage()
    match_labels = make_match_labels()
    for c in CLUSTERS:
        pods = make_pods(prefix='some-pod', count=3)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, c)

        ps = make_ps(match_labels, c)
        s = ClusterStorage()
        s.sync_with_objects(objs_with_timestamps=[ps])
        ps_storage.add_storage(s, c)

    mc_rs = make_mc_rs()
    ctl, client = make_controller(mc_rs, pod_storage, match_labels=match_labels, ps_storage=ps_storage)
    with mock.patch('infra.mc_rsc.src.controller.cluster_task.apply_cluster_tasks') as apply_cluster_tasks:
        ctl.process(mc_rs=mc_rs, failed_clusters={})
        assert apply_cluster_tasks.call_args == mock.call([])


def test_add_deploy_child_on_pod_set_creation():
    pod_storage = PodMultiClusterStorage()
    ps_storage = MultiClusterStorage()
    match_labels = make_match_labels()

    for c in CLUSTERS:
        if c == SECOND_CLUSTER:
            continue
        pods = make_pods(prefix='some-pod', count=3)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, c)

        ps = make_ps(match_labels, c)
        ps[0].meta.fqid = 'fqid-{}'.format(c)
        s = ClusterStorage()
        s.sync_with_objects(objs_with_timestamps=[ps])
        ps_storage.add_storage(s, c)

    mc_rs = make_mc_rs()
    ctl, client = make_controller(mc_rs, pod_storage, match_labels=match_labels, ps_storage=ps_storage)
    assert ctl.relation_storage.size() == 1
    assert len(ctl.relation_storage.list_by_to_fqid('fqid-{}'.format(FIRST_CLUSTER))) == 1
    assert len(ctl.relation_storage.list_by_to_fqid('fqid-{}'.format(SECOND_CLUSTER))) == 0
    ps_fqid = 'fqid-{}'.format(SECOND_CLUSTER)
    client.create_pod_set.return_value = ('ps-id', ps_fqid)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    assert ctl.relation_storage.size() == 2
    assert len(ctl.relation_storage.list_by_to_fqid('fqid-{}'.format(FIRST_CLUSTER))) == 1
    assert len(ctl.relation_storage.list_by_to_fqid('fqid-{}'.format(SECOND_CLUSTER))) == 1
    assert not ctl.mc_rs_client.remove_deploy_child.called
    calls = ctl.mc_rs_client.method_calls
    assert len(calls) == 2

    name, _, _ = calls[1]
    assert name == 'update_status'

    name, _, kw = calls[0]
    assert name == 'add_deploy_child'
    assert kw == {'mc_rs_id': mc_rs.meta.id, 'child': ps_fqid}


def test_remove_deploy_child_if_pod_set_creation_failed():
    pod_storage = PodMultiClusterStorage()
    ps_storage = MultiClusterStorage()
    match_labels = make_match_labels()

    for c in CLUSTERS:
        if c == SECOND_CLUSTER:
            continue
        pods = make_pods(prefix='some-pod', count=3)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, c)

        ps = make_ps(match_labels, c)
        ps[0].meta.fqid = 'fqid-{}'.format(c)
        s = ClusterStorage()
        s.sync_with_objects(objs_with_timestamps=[ps])
        ps_storage.add_storage(s, c)

    mc_rs = make_mc_rs()
    ctl, client = make_controller(mc_rs, pod_storage, match_labels=match_labels, ps_storage=ps_storage)
    assert ctl.relation_storage.size() == 1
    assert len(ctl.relation_storage.list_by_to_fqid('fqid-{}'.format(FIRST_CLUSTER))) == 1
    assert len(ctl.relation_storage.list_by_to_fqid('fqid-{}'.format(SECOND_CLUSTER))) == 0
    ps_fqid = 'fqid-{}'.format(SECOND_CLUSTER)
    client.create_pod_set.return_value = ('ps-id', ps_fqid)
    client.commit_transaction.side_effect = FakeYtResponseError('Failed')
    with mock.patch.object(gevent.hub.Hub, 'handle_error'):
        ctl.process(mc_rs=mc_rs, failed_clusters={})
    assert ctl.relation_storage.size() == 1
    assert len(ctl.relation_storage.list_by_to_fqid('fqid-{}'.format(FIRST_CLUSTER))) == 1
    assert len(ctl.relation_storage.list_by_to_fqid('fqid-{}'.format(SECOND_CLUSTER))) == 0
    calls = ctl.mc_rs_client.method_calls
    assert len(calls) == 3

    name, _, kw = calls[0]
    assert name == 'add_deploy_child'
    assert kw == {'mc_rs_id': mc_rs.meta.id, 'child': ps_fqid}

    name, _, kw = calls[1]
    assert name == 'remove_deploy_child'
    assert kw == {'mc_rs_id': mc_rs.meta.id, 'child': ps_fqid}

    name, _, _ = calls[2]
    assert name == 'update_status'


def test_outdated_ready_pods():
    pod_storage = PodMultiClusterStorage()
    ps_storage = MultiClusterStorage()
    match_labels = make_match_labels()
    for c in CLUSTERS:
        pods = make_pods(prefix='some-pod', count=3, spec_revision=OLD_REVISION)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, c)

        ps = make_ps(match_labels, c)
        s = ClusterStorage()
        s.sync_with_objects(objs_with_timestamps=[ps])
        ps_storage.add_storage(s, c)

    max_unavailable = 1
    mc_rs = make_mc_rs(max_unavailable=max_unavailable)

    with mock.patch.object(Controller, 'get_yp_client') as get_yp_client:
        client = make_yp_client()
        get_yp_client.return_value = client
        ctl, client = make_controller(mc_rs, pod_storage, match_labels=match_labels, ps_storage=ps_storage)
        ctl.process(mc_rs=mc_rs, failed_clusters={})
        client_calls = client.method_calls
        assert len(client_calls) == 1
        name, args, kwargs = client_calls[0]
        assert name == 'update_pods'
        assert len(kwargs['pods']) == max_unavailable
        for p in kwargs['pods']:
            assert p.spec.pod_agent_payload.spec.revision == TARGET_REVISION


def test_max_pods_to_process():
    pod_storage = PodMultiClusterStorage()
    ps_storage = MultiClusterStorage()
    match_labels = make_match_labels()
    for c in CLUSTERS:
        pods = make_pods(prefix='outdated-ready', count=3, spec_revision=OLD_REVISION)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, c)

        ps = make_ps(match_labels, c)
        s = ClusterStorage()
        s.sync_with_objects(objs_with_timestamps=[ps])
        ps_storage.add_storage(s, c)

    max_unavailable = 2
    max_pods_to_process = 1
    mc_rs = make_mc_rs(max_unavailable=max_unavailable)

    ctl, client = make_controller(mc_rs, pod_storage, match_labels=match_labels, ps_storage=ps_storage, max_pods_to_process=1)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, args, kwargs = client_calls[0]
    assert name == 'update_pods'
    assert len(kwargs['pods']) == max_pods_to_process
    for p in kwargs['pods']:
        assert p.spec.pod_agent_payload.spec.revision == TARGET_REVISION


def test_actual_in_progress_pods_reduced_window():
    # replica_count = 3, max_unavailable = 2
    # actual_ready = 0
    # actual_in_progress = 1
    # outdated_ready = 2
    # outdated_in_progress = 0
    # It is possible to deploy 1 pod.
    pod_storage = PodMultiClusterStorage()

    actual_in_progress_pods = make_pods(prefix='actual-in-progress', is_ready=False)
    pods = make_pods(prefix='some-pod', count=2, spec_revision=OLD_REVISION)
    pods.extend(actual_in_progress_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER])

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, args, kwargs = client_calls[0]
    assert name == 'update_pods'
    assert len(kwargs['pods']) == 1
    for p in kwargs['pods']:
        assert p.spec.pod_agent_payload.spec.revision == TARGET_REVISION


def test_actual_failed_pods_reduced_window():
    # replica_count = 4, max_unavailable = 3
    # actual_ready = 0
    # actual_in_progress = 1
    # actual_failed = 1
    # outdated_ready = 2
    # outdated_in_progress = 0
    # outdated_failed = 0
    # It is possible to deploy 1 pod.
    pod_storage = PodMultiClusterStorage()

    actual_failed_pods = make_pods(prefix='actual-failed', is_failed=True)
    actual_in_progress_pods = make_pods(prefix='actual-in-progress', is_ready=False)
    pods = make_pods(count=2, prefix='outdated-ready', spec_revision=OLD_REVISION)
    pods.extend(actual_in_progress_pods)
    pods.extend(actual_failed_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=3, clusters=[FIRST_CLUSTER], replica_count=4)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, args, kwargs = client_calls[0]
    assert name == 'update_pods'
    assert len(kwargs['pods']) == 1
    assert kwargs['pods'][0].spec.pod_agent_payload.spec.revision == TARGET_REVISION
    assert kwargs['pods'][0].meta.id in ['outdated-ready-1', 'outdated-ready-1']


def test_wait_actual_in_progress_pods():
    # replica_count = 3, max_unavailable = 2
    # actual_ready = 0
    # actual_in_progress = 2
    # outdated_ready = 1
    # outdated_in_progress = 0
    # We need to wait until actual_inprogress pod becomes ready.
    pod_storage = PodMultiClusterStorage()

    actual_in_progress_pods = make_pods(prefix='actual-in-progress', count=2, is_ready=False)
    pods = make_pods(prefix='some-pod', spec_revision=OLD_REVISION)
    pods.extend(actual_in_progress_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER])

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 0


def test_create_enumerated_id_pods():
    # replica_count = 3, max_unavailable = 2
    # actual_ready = 0
    # actual_in_progress = 0
    # outdated_ready = 0
    # outdated_in_progress = 0
    pod_storage = PodMultiClusterStorage()
    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER])
    yputil.set_label(mc_rs.spec.pod_template_spec.labels, consts.ENABLE_ENUMERATED_POD_IDS_LABEL, True)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls

    create_calls = list_mock_calls_by_name(client_calls, 'create_pods')
    assert len(create_calls) == 1
    name, args, kwargs = create_calls[0]
    pod_indexes = []
    for p in kwargs['pods']:
        assert p.meta.id.startswith(mc_rs.meta.id)
        p_idx = int(yputil.get_label(p.labels, consts.POD_INDEX_LABEL))
        assert p_idx == int(p.meta.id.rsplit('-', 1)[1])
        pod_indexes.append(p_idx)
    assert pod_indexes == range(3)


def test_replace_pods_batch_size():
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='some-pod', count=10, spec_revision=OLD_REVISION)

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=3,
                       clusters=[FIRST_CLUSTER],
                       replica_count=10)
    dvr = mc_rs.spec.pod_template_spec.spec.disk_volume_requests.add()
    dvr.storage_class = '123'
    yputil.set_label(mc_rs.labels, consts.DISABLE_SET_TARGET_STATE_REMOVED_LABEL, True)

    ctl, client = make_controller(mc_rs, pod_storage, replace_pods_batch_size=2)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    remove_calls = list_mock_calls_by_name(client_calls, 'remove_pods')
    assert len(remove_calls) == 2
    name, args, kwargs = remove_calls[0]
    assert len(kwargs['pod_ids']) == 2
    name, args, kwargs = remove_calls[1]
    assert len(kwargs['pod_ids']) == 1

    create_calls = list_mock_calls_by_name(client_calls, 'create_pods')
    assert len(create_calls) == 2
    name, args, kwargs = create_calls[0]
    assert len(kwargs['pods']) == 2
    name, args, kwargs = create_calls[1]
    assert len(kwargs['pods']) == 1


def test_replace_enumerated_id_pods():
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='some-pod', count=3, spec_revision=OLD_REVISION)

    pod_indexes = set()
    for n, p in enumerate(pods):
        yputil.set_label(p[0].labels, consts.POD_INDEX_LABEL, n)
        pod_indexes.add(n)

    pod_ids = set(p[0].meta.id for p in pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    max_unavailable = 3
    mc_rs = make_mc_rs(max_unavailable=max_unavailable, clusters=[FIRST_CLUSTER])
    dvr = mc_rs.spec.pod_template_spec.spec.disk_volume_requests.add()
    dvr.storage_class = '123'
    yputil.set_label(mc_rs.spec.pod_template_spec.labels, consts.ENABLE_ENUMERATED_POD_IDS_LABEL, True)
    yputil.set_label(mc_rs.labels, consts.DISABLE_SET_TARGET_STATE_REMOVED_LABEL, True)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    create_calls = list_mock_calls_by_name(client_calls, 'create_pods')
    name, args, kwargs = create_calls[0]
    assert len(kwargs['pods']) == max_unavailable
    new_pod_ids = set()
    new_pod_indexes = set()
    for p in kwargs['pods']:
        new_pod_ids.add(p.meta.id)
        new_pod_indexes.add(yputil.get_label(p.labels, consts.POD_INDEX_LABEL))
    assert new_pod_ids == pod_ids
    assert new_pod_indexes == pod_indexes


def test_disable_pods_move_label_no_replace():
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='some-pod', count=3, spec_revision=OLD_REVISION)

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    max_unavailable = 3
    mc_rs = make_mc_rs(max_unavailable=max_unavailable, clusters=[FIRST_CLUSTER])
    mc_rs.spec.pod_template_spec.spec.resource_requests.memory_limit = 123
    yputil.set_label(mc_rs.spec.pod_template_spec.labels, consts.DISABLE_PODS_MOVE_LABEL, True)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods'
    assert len(kwargs['pods']) == 3


def test_disable_pods_move_label_eviction():
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='some-pod', count=3, is_eviction_requested=True)
    pods.extend(make_pods(prefix='some-pod-in-progress', count=3, is_eviction_requested=True, is_ready=False))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    max_unavailable = 6
    mc_rs = make_mc_rs(max_unavailable=max_unavailable, clusters=[FIRST_CLUSTER], replica_count=6)
    yputil.set_label(mc_rs.spec.pod_template_spec.labels, consts.DISABLE_PODS_MOVE_LABEL, True)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_eviction'
    assert not kwargs['use_evict']
    assert set(kwargs['pod_ids']) == {
        'some-pod-1', 'some-pod-2', 'some-pod-3',
        'some-pod-in-progress-1', 'some-pod-in-progress-2', 'some-pod-in-progress-3'
    }


def test_eviction_requested_pods():
    # replica_count = 3, max_unavailable = 2
    # actual_ready = 0
    # actual_in_progress = 0
    # outdated_ready = 3 (with eviction_requested = 1)
    # outdated_in_progress = 0
    pod_storage = PodMultiClusterStorage()

    eviction_requested_pods = make_pods(prefix='eviction-requested', is_eviction_requested=True)
    pods = make_pods(prefix='some-pod', count=2)
    pods.extend(eviction_requested_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER])

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_eviction'
    assert not kwargs['use_evict']
    assert set(kwargs['pod_ids']) == {'eviction-requested-1'}


def test_eviction_requested_failed_pods():
    # replica_count = 4, max_unavailable = 1
    # actual_ready = 2
    # actual_ready = 1 (with eviction_requested)
    # actual_failed = 1 (with eviction_requested)
    # actual_in_progress = 0
    # actual_failed = 0
    # outdated_ready =  0 (with eviction_requested)
    # outdated_failed = 0 (with eviction_requested)
    # outdated_in_progress = 0
    pod_storage = PodMultiClusterStorage()

    eviction_requested_actual_failed_pods = make_pods(prefix='actual-failed-eviction-requested', is_failed=True,
                                                      is_eviction_requested=True)
    eviction_requested_actual_ready_pods = make_pods(prefix='actual-ready-eviction-requested',
                                                     is_eviction_requested=True)
    pods = make_pods(prefix='actual-ready', count=2)
    pods.extend(eviction_requested_actual_failed_pods)
    pods.extend(eviction_requested_actual_ready_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=1, clusters=[FIRST_CLUSTER], replica_count=4)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_eviction'
    assert not kwargs['use_evict']
    assert len(kwargs['pod_ids']) == 1
    assert kwargs['pod_ids'][0] == 'actual-failed-eviction-requested-1'


def test_max_tolerable_downtime_pods_eviction():
    # replica_count = 2, max_unavailable = 2, max_tolerable_downtime_pods = 1
    # actual_ready = 1 (with eviction_requested = 1)
    # actual_in_progress = 1
    # outdated_ready = 0
    # outdated_in_progress = 0
    pod_storage = PodMultiClusterStorage()

    eviction_requested_pods = make_pods(prefix='eviction-requested', is_eviction_requested=True)
    pods = make_pods(prefix='some-pod', is_ready=False)
    pods.extend(eviction_requested_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=2, max_tolerable_downtime_pods=1, replica_count=2)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 0


def test_max_tolerable_downtime_pods_deploy():
    # replica_count = 2, max_unavailable = 2, max_tolerable_downtime_pods = 1
    # actual_ready = 1 (with eviction_requested = 1)
    # actual_in_progress = 1
    # outdated_ready = 0
    # outdated_in_progress = 0
    pod_storage = PodMultiClusterStorage()

    in_progress_pods = make_pods(prefix='in-progress', count=2, is_ready=False)
    pods = make_pods(prefix='some-pod', count=8, spec_revision=OLD_REVISION, is_ready=False)
    pods.extend(in_progress_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=5, max_tolerable_downtime_pods=1, replica_count=10)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods'
    assert len(kwargs['pods']) == 3


def test_eviction_requested_pods_one_dead_pod():
    # replica_count = 1, max_unavailable = 1
    # actual_ready = 0
    # actual_in_progress = 0
    # outdated_ready = 0
    # outdated_in_progress = 1 (with eviction_requested = 1)
    pod_storage = PodMultiClusterStorage()

    eviction_requested_pods = make_pods(prefix='eviction-requested', is_ready=False, is_eviction_requested=True)
    pods = make_pods(prefix='some-pod', count=2)
    pods.extend(eviction_requested_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_eviction'
    assert not kwargs['use_evict']
    assert set(kwargs['pod_ids']) == {'eviction-requested-1'}


def test_remove_not_in_spec_pod_sets():
    pod_storage = PodMultiClusterStorage()
    match_labels = make_match_labels()

    pods = make_pods(prefix='some-pod', count=3)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    # Case 1: remove ps in SECOND_CLUSTER because there is no SECOND_CLUSTER in
    # mc_rs spec.
    ps_storage = MultiClusterStorage()
    first_cluster_ps = make_ps(match_labels, FIRST_CLUSTER)
    s = ClusterStorage()
    s.sync_with_objects(objs_with_timestamps=[first_cluster_ps])
    ps_storage.add_storage(s, FIRST_CLUSTER)
    second_cluster_ps = make_ps(match_labels, SECOND_CLUSTER)
    s = ClusterStorage()
    s.sync_with_objects(objs_with_timestamps=[second_cluster_ps])
    ps_storage.add_storage(s, SECOND_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER])
    yp_clients = make_yp_clients()

    rel_storage = RelationClusterStorage()
    r1 = make_relation(rel_id='relation-1',
                       from_fqid=mc_rs.meta.fqid,
                       to_fqid=first_cluster_ps[0].meta.fqid)
    r2 = make_relation(rel_id='relation-2',
                       from_fqid=mc_rs.meta.fqid,
                       to_fqid=second_cluster_ps[0].meta.fqid)
    rel_storage.put(r1)
    rel_storage.put(r2)

    ctl, client = make_controller(mc_rs, pod_storage, match_labels=match_labels, yp_clients=yp_clients,
                                  ps_storage=ps_storage, relation_storage=rel_storage)
    assert ctl.relation_storage.size() == 2
    assert len(ctl.relation_storage.list_by_to_fqid(first_cluster_ps[0].meta.fqid)) == 1
    assert len(ctl.relation_storage.list_by_to_fqid(second_cluster_ps[0].meta.fqid)) == 1
    with mock.patch('sepelib.core.config.get_value', return_value=True):
        ctl.process(mc_rs=mc_rs, failed_clusters={})
    assert ctl.relation_storage.size() == 1
    assert len(ctl.relation_storage.list_by_to_fqid(first_cluster_ps[0].meta.fqid)) == 1
    assert len(ctl.relation_storage.list_by_to_fqid(second_cluster_ps[0].meta.fqid)) == 0
    assert yp_clients[SECOND_CLUSTER].remove_pod_set.called
    assert yp_clients[SECOND_CLUSTER].start_transaction.called
    assert ctl.mc_rs_client.remove_deploy_child.called
    assert not yp_clients[FIRST_CLUSTER].remove_pod_set.called

    # Case 2: do not remove ps in SECOND_CLUSTER because it has unmatched
    # labels.
    for c in yp_clients.itervalues():
        c.reset_mock()

    attr = second_cluster_ps[0].labels.attributes.add()
    attr.key = 'environ'
    attr.value = yson.dumps('pre')

    ctl.process(mc_rs=mc_rs, failed_clusters={})
    assert not yp_clients[SECOND_CLUSTER].remove_pod_sets.called
    assert not yp_clients[FIRST_CLUSTER].remove_pod_sets.called


def test_ready_actual_pods_with_failed_cluster():
    # replica_count = 6
    # max_unavailable = 6
    # actual_ready = 3
    # actual_in_progress = 0
    # outdated_ready = 0
    # outdated_in_progress = 0
    # pods_on_failed_clusters = 3
    pod_storage = PodMultiClusterStorage()
    ps_storage = MultiClusterStorage()
    match_labels = make_match_labels()
    for c in CLUSTERS:
        pods = make_pods(prefix='some-pod', count=3)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, c)

        ps = make_ps(match_labels, c)
        s = ClusterStorage()
        s.sync_with_objects(objs_with_timestamps=[ps])
        ps_storage.add_storage(s, c)

    mc_rs = make_mc_rs(clusters=CLUSTERS)
    mc_rs.spec.deployment_strategy.max_unavailable = 6

    ctl, client = make_controller(mc_rs, pod_storage, match_labels, ps_storage=ps_storage)
    failed_condition = Condition(succeeded=False,
                                 reason='',
                                 message='',
                                 last_transition_time=1)
    ctl.process(mc_rs=mc_rs, failed_clusters={SECOND_CLUSTER: failed_condition})
    client_calls = client.method_calls
    assert len(client_calls) == 0


def test_stop_deploy_with_failed_cluster():
    pod_storage = PodMultiClusterStorage()
    ps_storage = MultiClusterStorage()
    match_labels = make_match_labels()
    for c in CLUSTERS:
        pods = make_pods(prefix='some-pod', count=3, spec_revision=OLD_REVISION)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, c)

        ps = make_ps(match_labels, c)
        s = ClusterStorage()
        s.sync_with_objects(objs_with_timestamps=[ps])
        ps_storage.add_storage(s, c)

    mc_rs = make_mc_rs(clusters=CLUSTERS)

    # Case 1.
    # replica_count = 6
    # max_unavailable = 2
    # actual_ready = 0
    # actual_in_progress = 0
    # outdated_ready = 6
    # outdated_in_progress = 0
    # pods_on_failed_clusters = 3
    ctl, client = make_controller(mc_rs, pod_storage, match_labels, ps_storage=ps_storage)
    failed_condition = Condition(succeeded=False,
                                 reason='',
                                 message='',
                                 last_transition_time=1)
    ctl.process(mc_rs=mc_rs, failed_clusters={SECOND_CLUSTER: failed_condition})
    client_calls = client.method_calls
    assert len(client_calls) == 0

    # Case 2.
    # replica_count = 6
    # max_unavailable = 4
    # actual_ready = 0
    # actual_in_progress = 0
    # outdated_ready = 6
    # outdated_in_progress = 0
    # pods_on_failed_clusters = 3
    mc_rs.spec.deployment_strategy.max_unavailable = 4
    ctl.process(mc_rs=mc_rs, failed_clusters={SECOND_CLUSTER: failed_condition})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods'
    assert len(kwargs['pods']) == 1


def test_controller_rate_limiter():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='some-pod', count=3, spec_revision=OLD_REVISION)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    rate_limiter = RateLimiter(is_enabled=True, delay_secs=60)
    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1)
    yputil.set_label(mc_rs.labels, consts.DISABLE_SET_TARGET_STATE_REMOVED_LABEL, True)

    with mock.patch('infra.mc_rsc.src.podutil.is_pod_spec_updateable', return_value=False):
        # rate_limiter is enabled
        # replica_count = 3
        # max_unavailable = 1
        # actual_ready = 0
        # actual_in_progress = 0
        # outdated_ready = 3
        # outdated_in_progress = 0
        ctl, client = make_controller(mc_rs, pod_storage, rate_limiter=rate_limiter)
        ctl.process(mc_rs=mc_rs, failed_clusters={})
        ctl.process(mc_rs=mc_rs, failed_clusters={})

        client_calls = client.method_calls
        create_calls = list_mock_calls_by_name(client_calls, 'create_pods')
        assert len(create_calls) == 1
        name, args, kwargs = create_calls[0]
        assert len(kwargs['pods']) == 1
        remove_calls = list_mock_calls_by_name(client_calls, 'remove_pods')
        assert len(remove_calls) == 1
        name, args, kwargs = remove_calls[0]
        assert len(kwargs['pod_ids']) == 1


def test_controller_circuit_breaker():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='some-pod', count=3, spec_revision=OLD_REVISION)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    circuit_breaker = CircuitBreaker(is_enabled=True, max_tries=1)
    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1)

    with mock.patch.object(gevent.hub.Hub, 'handle_error'):
        # circuit_breaker is enabled
        # replica_count = 3
        # max_unavailable = 1
        # actual_ready = 0
        # actual_in_progress = 0
        # outdated_ready = 3
        # outdated_in_progress = 0
        client = make_yp_client()
        client.update_pods.side_effect = yp.common.GrpcError('Failed')
        ctl, client = make_controller(mc_rs, pod_storage, circuit_breaker=circuit_breaker, client=client)
        ctl.process(mc_rs=mc_rs, failed_clusters={})
        ctl.process(mc_rs=mc_rs, failed_clusters={})

        client_calls = client.method_calls
        update_calls = list_mock_calls_by_name(client_calls, 'update_pods')
        assert len(update_calls) == 1
        name, args, kwargs = update_calls[0]
        assert len(kwargs['pods']) == 1


def test_acknowledge_maintenance():
    # replica_count = 1, max_unavailable = 1
    # actual_ready = 0
    # actual_in_progress = 0
    # outdated_ready = 0
    # outdated_in_progress = 1 (with maintenance_requested = 1)
    pod_storage = PodMultiClusterStorage()

    for maintenance in (create_maintenance(kind=yp.data_model.MK_REBOOT),
                        create_maintenance(kind=yp.data_model.MK_TEMPORARY_UNREACHABLE),
                        create_maintenance(kind=yp.data_model.MK_PROFILE)):
        maintenance_requested_pods = make_pods(prefix='maintenance-requested', maintenance=maintenance)
        pods = make_pods(prefix='some-pod')
        pods.extend(maintenance_requested_pods)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, FIRST_CLUSTER)

        mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, max_tolerable_downtime_seconds=3600,
                           replica_count=2)

        ctl, client = make_controller(mc_rs, pod_storage)
        ctl.process(mc_rs=mc_rs, failed_clusters={})
        client_calls = client.method_calls
        assert len(client_calls) == 1
        name, _, kwargs = client_calls[0]
        assert name == 'update_pods_acknowledge_maintenance'
        assert set(kwargs['pod_ids']) == {'maintenance-requested-1'}


def test_acknowledge_maintenance_disruptive():
    # replica_count = 1, max_unavailable = 1
    # actual_ready = 0
    # actual_in_progress = 0
    # outdated_ready = 0
    # outdated_in_progress = 1 (with maintenance_requested = 1)
    pod_storage = PodMultiClusterStorage()

    for maintenance in (create_maintenance(kind=yp.data_model.MK_NONE),
                        create_maintenance(kind=yp.data_model.MK_POWER_OFF),
                        create_maintenance(disruptive=True)):
        maintenance_requested_pods = make_pods(prefix='maintenance-requested', maintenance=maintenance)
        pods = make_pods(prefix='some-pod')
        pods.extend(maintenance_requested_pods)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, FIRST_CLUSTER)

        mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, max_tolerable_downtime_seconds=3600,
                           replica_count=2)

        ctl, client = make_controller(mc_rs, pod_storage)
        ctl.process(mc_rs=mc_rs, failed_clusters={})
        client_calls = client.method_calls
        assert len(client_calls) == 1
        name, _, kwargs = client_calls[0]
        assert name == 'update_pods_acknowledge_eviction'
        assert kwargs['use_evict']
        assert set(kwargs['pod_ids']) == {'maintenance-requested-1'}


def test_acknowledge_maintenance_overtimed():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='maintenance-acknowledged',
                     maintenance=create_maintenance(node_set_id='1', state=yp.data_model.PMS_ACKNOWLEDGED,
                                                    estimated_duration=1, duration=10))
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, max_tolerable_downtime_seconds=5,
                       replica_count=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_eviction'
    assert kwargs['use_evict']
    assert set(kwargs['pod_ids']) == {'maintenance-acknowledged-1'}


def test_acknowledge_maintenance_by_node_set_id():
    pod_storage = PodMultiClusterStorage()

    pods = (make_pods(prefix='maintenance-acknowledged',
                      maintenance=create_maintenance(node_set_id='1', state=yp.data_model.PMS_ACKNOWLEDGED)) +
            make_pods(prefix='maintenance-requested', count=2, maintenance=create_maintenance(node_set_id='1')))
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=2, max_tolerable_downtime_seconds=1200,
                       replica_count=3)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_eviction'
    assert kwargs['use_evict']
    assert set(kwargs['pod_ids']) == {'maintenance-acknowledged-1'}


def test_acknowledge_maintenance_by_node_set_id2():
    pod_storage = PodMultiClusterStorage()

    pods = (make_pods(prefix='maintenance-acknowledged1',
                      maintenance=create_maintenance(node_set_id='2', state=yp.data_model.PMS_ACKNOWLEDGED)) +
            make_pods(prefix='maintenance-requested1', maintenance=create_maintenance(node_set_id='2')) +
            make_pods(prefix='maintenance-requested2', maintenance=create_maintenance(node_set_id='1')))
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=2, max_tolerable_downtime_seconds=1200,
                       replica_count=3)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_maintenance'
    assert set(kwargs['pod_ids']) == {'maintenance-requested1-1'}


def test_acknowledge_maintenance_and_eviction():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='maintenance-requested', is_eviction_requested=True, maintenance=create_maintenance())
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, max_tolerable_downtime_seconds=3600,
                       replica_count=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_eviction'
    assert not kwargs['use_evict']
    assert set(kwargs['pod_ids']) == {'maintenance-requested-1'}


def test_acknowledge_maintenance_before_eviction():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='maintenance-requested', maintenance=create_maintenance())
    pods.extend(make_pods(prefix='eviction-requested', is_eviction_requested=True))
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, max_tolerable_downtime_seconds=3600,
                       replica_count=2)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_maintenance'
    assert set(kwargs['pod_ids']) == {'maintenance-requested-1'}


def test_acknowledge_maintenance_dead_before_alive():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='maintenance-requested-ready', maintenance=create_maintenance())
    pods.extend(make_pods(prefix='maintenance-requested-in-progress', is_ready=False, maintenance=create_maintenance()))
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=2, max_tolerable_downtime_seconds=3600,
                       replica_count=2)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_maintenance'
    assert set(kwargs['pod_ids']) == {'maintenance-requested-in-progress-1'}


def test_hfsm_eviction():
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='some-pod', count=3, is_eviction_requested=True, eviction_reason=yp.data_model.ER_HFSM)

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER])

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    assert len(client_calls) == 0


def test_node_alerted(enable_heartbeat_alert):
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='new-alert', count=1, node_alerts=[create_alert(is_heartbeat=True, duration=0)])
    pods.extend(make_pods(prefix='old-alert', count=1, node_alerts=[create_alert(is_heartbeat=True, duration=1000000)]))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], replica_count=2)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_eviction'
    assert set(kwargs['pod_ids']) == {'old-alert-1'}


def test_acknowledged_eviction_pods(enable_heartbeat_alert):
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='new-alert', count=1, node_alerts=[create_alert(is_heartbeat=True, duration=0)])
    pods.extend(make_pods(prefix='old-alert', count=1, is_eviction_acknowledged=True,
                          node_alerts=[create_alert(is_heartbeat=True, duration=1000000)]))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], replica_count=2)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    assert len(client_calls) == 0


def test_acknowledged_eviction_pods2():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='maintenance-and-eviction-acknowledged', is_eviction_acknowledged=True,
                     maintenance=create_maintenance(node_set_id='1', state=yp.data_model.PMS_ACKNOWLEDGED,
                                                    estimated_duration=1, duration=10))
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, max_tolerable_downtime_seconds=5,
                       replica_count=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 0


def test_update_blocked_by_min_delay():
    spec_timestamps = [9, 11, 12]
    pods = make_pods(prefix='some-pod',
                     count=3,
                     spec_revision=OLD_REVISION,
                     spec_timestamps=spec_timestamps)
    pod_storage = PodMultiClusterStorage()
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=2)
    mc_rs.spec.deployment_strategy.deploy_speed.min_delay = 5
    mc_rs.spec.deployment_strategy.deploy_speed.update_portion = 1

    with mock.patch.object(Controller, 'get_yp_client') as get_yp_client, \
            mock.patch.object(time, 'time') as mocked_time:

        # Case 1: update is blocked by min delay
        mocked_time.return_value = 10
        client = make_yp_client()
        get_yp_client.return_value = client
        ctl, client = make_controller(mc_rs, pod_storage)
        ctl.process(mc_rs=mc_rs, failed_clusters={})
        client_calls = client.method_calls
        assert len(client_calls) == 0

        # Case 2: update is not blocked by min delay
        get_yp_client.reset_mock()
        mocked_time.return_value = 20
        ctl, client = make_controller(mc_rs, pod_storage)
        ctl.process(mc_rs=mc_rs, failed_clusters={})

        client_calls = client.method_calls
        update_calls = list_mock_calls_by_name(client_calls, 'update_pods')
        assert len(update_calls) == 1
        name, args, kwargs = update_calls[0]
        assert len(kwargs['pods']) == 1


def test_set_target_state_removed():
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='some-pod', count=10, spec_revision=OLD_REVISION, agent_spec_timestamp=1)

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=3,
                       clusters=[FIRST_CLUSTER],
                       replica_count=10)
    dvr = mc_rs.spec.pod_template_spec.spec.disk_volume_requests.add()
    dvr.storage_class = '123'

    ctl, client = make_controller(mc_rs, pod_storage, replace_pods_batch_size=2)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    set_target_state_calls = list_mock_calls_by_name(client_calls, 'update_pods_target_state')
    assert len(set_target_state_calls) == 1
    name, args, kwargs = set_target_state_calls[0]
    assert len(kwargs['pod_ids']) == 3


def test_replace_pods_from_removed():
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='some-pod', count=7, spec_revision=OLD_REVISION)
    pods.extend(make_pods(prefix='removed-pod', count=2, spec_revision=OLD_REVISION,
                          target_state=yp.data_model.EPodAgentTargetState_REMOVED))
    pods.extend(make_pods(prefix='removing-pod', count=1, spec_revision=OLD_REVISION,
                          target_state=yp.data_model.EPodAgentTargetState_REMOVED,
                          is_ready=False, spec_timestamps=[0]))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=3,
                       clusters=[FIRST_CLUSTER],
                       replica_count=10)

    ctl, client = make_controller(mc_rs, pod_storage, replace_pods_batch_size=2)
    with mock.patch('time.time') as mocked_time:
        mocked_time.return_value = 1
        ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    remove_calls = list_mock_calls_by_name(client_calls, 'remove_pods')
    assert len(remove_calls) == 1
    name, args, kwargs = remove_calls[0]
    assert len(kwargs['pod_ids']) == 2
    assert set(kwargs['pod_ids']) == {'removed-pod-1', 'removed-pod-2'}

    create_calls = list_mock_calls_by_name(client_calls, 'create_pods')
    assert len(create_calls) == 1
    name, args, kwargs = create_calls[0]
    assert len(kwargs['pods']) == 2


def test_destroy_overtimed_removing():
    pod_storage = PodMultiClusterStorage()
    spec_timestamp = 0
    pods = make_pods(prefix='some-pod', count=1, spec_revision=OLD_REVISION, is_ready=False,
                     target_state=yp.data_model.EPodAgentTargetState_REMOVED,
                     spec_timestamps=[spec_timestamp])

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=1,
                       clusters=[FIRST_CLUSTER],
                       replica_count=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    with mock.patch('time.time') as mocked_time:
        mocked_time.return_value = spec_timestamp + consts.MAX_DESTROY_HOOK_PERIOD_SECS + 1
        ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    remove_calls = list_mock_calls_by_name(client_calls, 'remove_pods')
    assert len(remove_calls) == 1
    name, args, kwargs = remove_calls[0]
    assert kwargs['pod_ids'] == ['some-pod-1']

    create_calls = list_mock_calls_by_name(client_calls, 'create_pods')
    assert len(create_calls) == 1
    name, args, kwargs = create_calls[0]
    assert len(kwargs['pods']) == 1
    assert kwargs['pods'][0].meta.id != 'some-pod-1'


def test_remove_pods_counting_target_state_removed():
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='some-pod', count=7, spec_revision=OLD_REVISION)
    pods.extend(make_pods(prefix='removed-pod', count=3, spec_revision=OLD_REVISION,
                          target_state=yp.data_model.EPodAgentTargetState_REMOVED))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=3,
                       clusters=[FIRST_CLUSTER],
                       replica_count=7)

    ctl, client = make_controller(mc_rs, pod_storage, replace_pods_batch_size=2)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    remove_calls = list_mock_calls_by_name(client_calls, 'remove_pods')
    assert len(remove_calls) == 1
    name, args, kwargs = remove_calls[0]
    assert kwargs['pod_ids'] == ['removed-pod-1', 'removed-pod-2', 'removed-pod-3']


def test_acknowledge_eviction_counting_target_state_removed():
    pod_storage = PodMultiClusterStorage()
    pods = make_pods(prefix='some-pod', count=6, spec_revision=OLD_REVISION)
    pods.extend(make_pods(prefix='removed-pod', count=3, spec_revision=OLD_REVISION,
                          target_state=yp.data_model.EPodAgentTargetState_REMOVED))
    pods.extend(make_pods(prefix='eviction-requested', is_eviction_requested=True))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=3,
                       clusters=[FIRST_CLUSTER],
                       replica_count=10)

    ctl, client = make_controller(mc_rs, pod_storage, replace_pods_batch_size=2)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    ack_calls = list_mock_calls_by_name(client.method_calls, 'update_pods_acknowledge_eviction')
    assert len(ack_calls) == 0


def test_eviction_requested_pods_w_set_target_state_removed():
    pod_storage = PodMultiClusterStorage()

    eviction_requested_pods = make_pods(prefix='eviction-requested', is_eviction_requested=True,
                                        agent_spec_timestamp=1)
    pods = make_pods(prefix='some-pod', count=2)
    pods.extend(eviction_requested_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_target_state'
    assert kwargs['target_state'] == yp.data_model.EPodAgentTargetState_REMOVED
    assert set(kwargs['pod_ids']) == {'eviction-requested-1'}


def test_eviction_requested_pods_removed_ready():
    pod_storage = PodMultiClusterStorage()
    eviction_requested_pods = make_pods(
        prefix='eviction-requested',
        is_eviction_requested=True,
        is_ready=True,
        is_spec_applied_by_pod_agent=True,
        target_state=yp.data_model.EPodAgentTargetState_REMOVED,
    )

    pods = make_pods(prefix='some-pod', count=2)
    pods.extend(eviction_requested_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_acknowledge_eviction'
    assert len(client_calls) == 1
    name, args, kwargs = client_calls[0]
    assert kwargs['pod_ids'] == ['eviction-requested-1']


def test_destroy_overtimed_evictions():
    pod_storage = PodMultiClusterStorage()
    spec_timestamp = 1
    pods = make_pods(prefix='some-pod',
                     count=1,
                     target_state=yp.data_model.EPodAgentTargetState_REMOVED,
                     is_eviction_requested=True,
                     is_spec_applied_by_pod_agent=False,
                     spec_timestamps=[spec_timestamp])

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=1,
                       clusters=[FIRST_CLUSTER],
                       replica_count=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    with mock.patch('time.time') as mocked_time:
        mocked_time.return_value = spec_timestamp + consts.MAX_DESTROY_HOOK_PERIOD_SECS + 1
        ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    assert len(client.method_calls) == 1
    eviction_calls = list_mock_calls_by_name(client_calls, 'update_pods_acknowledge_eviction')
    assert len(eviction_calls) == 1
    name, args, kwargs = eviction_calls[0]
    assert kwargs['pod_ids'] == ['some-pod-1']


def test_evict_pod_w_removing_in_progress_on_old_revision():
    """
    3 pods with 2 max_unavailable
    2 pods - in progress set target state removed (from replace pod)
    1 pod - eviction requested
    """
    pod_storage = PodMultiClusterStorage()
    spec_timestamp = 0
    pods = []
    pods.extend(make_pods(
        prefix='replace-pod',
        count=2,
        is_ready=False,
        spec_revision=OLD_REVISION,
        target_state=yp.data_model.EPodAgentTargetState_REMOVED,
        spec_timestamps=[spec_timestamp, spec_timestamp])
    )
    pods.extend(make_pods(
        prefix='evict-pod',
        count=1,
        is_eviction_requested=True
    ))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=2,
                       clusters=[FIRST_CLUSTER],
                       replica_count=3)

    ctl, client = make_controller(mc_rs, pod_storage)
    with mock.patch('time.time') as mocked_time:
        mocked_time.return_value = spec_timestamp + consts.MAX_DESTROY_HOOK_PERIOD_SECS - 1
        ctl.process(mc_rs=mc_rs, failed_clusters={})

    assert not client.method_calls


def test_remove_pod_w_target_state_removed():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='some-pod', count=3, agent_spec_timestamp=1)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, replica_count=2)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_target_state'
    assert kwargs['target_state'] == yp.data_model.EPodAgentTargetState_REMOVED
    assert set(kwargs['pod_ids']) == {'some-pod-3'}


def test_remove_pod_w_remove_ready():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='some-pod', count=2)
    pods.extend(make_pods(prefix='remove-this-pod', count=1, target_state=yp.data_model.EPodAgentTargetState_REMOVED))
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, replica_count=2)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    remove_calls = list_mock_calls_by_name(client_calls, 'remove_pods')
    assert len(remove_calls) == 1
    name, args, kwargs = remove_calls[0]
    assert len(kwargs['pod_ids']) == 1
    assert set(kwargs['pod_ids']) == {'remove-this-pod-1'}


def test_set_target_state_active():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='some-pod', count=1,
                     target_state=yp.data_model.EPodAgentTargetState_REMOVED)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, replica_count=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods_target_state'
    assert kwargs['target_state'] == yp.data_model.EPodAgentTargetState_ACTIVE
    assert set(kwargs['pod_ids']) == {'some-pod-1'}


def test_skip_permanently_disabled_pods_activation():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='some-pod', count=1,
                     target_state=yp.data_model.EPodAgentTargetState_REMOVED,
                     labels={'deploy': {consts.DISABLE_PERMANENTLY_LABEL: True}})

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, replica_count=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 0


def test_permanently_disabled_pods_update():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='some-pod', count=1,
                     target_state=yp.data_model.EPodAgentTargetState_REMOVED,
                     labels={'deploy': {consts.DISABLE_PERMANENTLY_LABEL: True}},
                     spec_revision=OLD_REVISION)

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, replica_count=1)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    name, _, kwargs = client_calls[0]
    assert name == 'update_pods'
    assert kwargs['pods'][0].spec.pod_agent_payload.spec.target_state == yp.data_model.EPodAgentTargetState_UNKNOWN


def test_permanently_disabled_pods_replace():
    pod_storage = PodMultiClusterStorage()

    pods = make_pods(prefix='some-pod', count=1,
                     target_state=yp.data_model.EPodAgentTargetState_REMOVED,
                     labels={'deploy': {consts.DISABLE_PERMANENTLY_LABEL: True}},
                     spec_revision=OLD_REVISION)

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, replica_count=1)
    mc_rs.spec.pod_template_spec.spec.resource_requests.memory_guarantee = 123

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    call_names = [call[0] for call in client_calls]
    assert call_names == ['start_transaction', 'remove_pods', 'create_pods', 'commit_transaction']


def test_remove_delegation_remove_pods():
    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1, replica_count=5)
    yputil.set_label(mc_rs.labels, consts.DEPLOY_POLICY_LABEL, 'remove_delegation')

    pod_storage = PodMultiClusterStorage()
    pods = []
    pods.extend(make_pods(prefix='some-pod', count=8))
    pods.extend(make_pods(prefix='evicted-ready-pod', count=1, is_eviction_requested=True))
    pods.extend(make_pods(prefix='evicted-in-progress-pod', count=1, is_eviction_requested=True, is_ready=False))
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    evict_calls = list_mock_calls_by_name(client_calls, 'update_pods_request_eviction')
    assert len(evict_calls) == 1
    name, args, kwargs = evict_calls[0]
    assert len(kwargs['pod_ids']) == 3
    for p_id in kwargs['pod_ids']:
        assert p_id.startswith('some-pod')


def test_remove_delegation_update_pods():
    max_unavailable = 1
    mc_rs = make_mc_rs(max_unavailable=max_unavailable,
                       clusters=[FIRST_CLUSTER],
                       replica_count=3)
    yputil.set_label(mc_rs.labels, consts.DEPLOY_POLICY_LABEL, 'remove_delegation')

    pod_storage = PodMultiClusterStorage()
    pods = []
    pods.extend(make_pods(prefix='some-pod', count=2, spec_revision=OLD_REVISION))
    pods.extend(make_pods(prefix='evicted-pod', count=1, is_eviction_requested=True, spec_revision=OLD_REVISION))
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    update_calls = list_mock_calls_by_name(client_calls, 'update_pods')
    assert len(update_calls) == 1
    name, args, kwargs = update_calls[0]
    assert len(kwargs['pods']) == max_unavailable
    for p in kwargs['pods']:
        assert p.spec.pod_agent_payload.spec.revision == TARGET_REVISION


def test_remove_delegation_parallel_reallocation_update_pods():
    max_unavailable = 3
    mc_rs = make_mc_rs(max_unavailable=max_unavailable,
                       clusters=[FIRST_CLUSTER],
                       replica_count=3)

    mc_rs.spec.pod_template_spec.spec.resource_requests.memory_limit = 123
    yputil.set_label(mc_rs.labels, consts.DEPLOY_POLICY_LABEL, 'remove_delegation_parallel_reallocation')

    pod_storage = PodMultiClusterStorage()
    pods = []
    pods.extend(make_pods(prefix='evicted-pod', count=3, is_eviction_requested=True, spec_revision=OLD_REVISION))
    for p, _ in pods:
        yputil.set_label(p.labels, "yd.delegate_removing_required", True)

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls

    update_calls = list_mock_calls_by_name(client_calls, 'update_pods')
    assert len(update_calls) == 1
    name, args, kwargs = update_calls[0]
    assert len(kwargs['pods']) == max_unavailable
    for p in kwargs['pods']:
        assert p.spec.pod_agent_payload.spec.revision == TARGET_REVISION
        assert p.spec.resource_requests.memory_limit == 0


def test_remove_delegation_parallel_reallocation_mark_delegate_removing():
    max_unavailable = 3
    mc_rs = make_mc_rs(max_unavailable=max_unavailable,
                       clusters=[FIRST_CLUSTER],
                       replica_count=3)

    mc_rs.spec.pod_template_spec.spec.resource_requests.memory_limit = 123
    yputil.set_label(mc_rs.labels, consts.DEPLOY_POLICY_LABEL, 'remove_delegation_parallel_reallocation')

    pod_storage = PodMultiClusterStorage()
    pods = []
    pods.extend(make_pods(prefix='evicted-pod', count=3, is_eviction_requested=True, spec_revision=OLD_REVISION))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls

    update_calls = list_mock_calls_by_name(client_calls, 'mark_delegate_removing_pods')
    assert len(update_calls) == 1
    name, args, kwargs = update_calls[0]
    assert len(kwargs['pod_ids']) == max_unavailable


def test_remove_delegation_parallel_reallocation_delegate_removing():
    max_unavailable = 3
    mc_rs = make_mc_rs(max_unavailable=max_unavailable,
                       clusters=[FIRST_CLUSTER],
                       replica_count=3)

    mc_rs.spec.pod_template_spec.spec.resource_requests.memory_limit = 123
    yputil.set_label(mc_rs.labels, consts.DEPLOY_POLICY_LABEL, 'remove_delegation_parallel_reallocation')

    pod_storage = PodMultiClusterStorage()
    pods = []
    pods.extend(make_pods(prefix='evicted-pod', count=3, is_eviction_requested=False, spec_revision=OLD_REVISION))
    for p, _ in pods:
        yputil.set_label(p.labels, "yd.delegate_removing_required", True)

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls

    update_calls = list_mock_calls_by_name(client_calls, 'delegate_removing_pods')
    assert len(update_calls) == 1
    name, args, kwargs = update_calls[0]
    assert len(kwargs['pod_ids']) == max_unavailable


def test_remove_delegation_replace_pods():
    mc_rs = make_mc_rs(max_unavailable=5,
                       clusters=[FIRST_CLUSTER],
                       replica_count=10)
    mc_rs.spec.pod_template_spec.spec.resource_requests.memory_limit = 123
    yputil.set_label(mc_rs.labels, consts.DEPLOY_POLICY_LABEL, 'remove_delegation')

    pod_storage = PodMultiClusterStorage()
    pods = []
    pods.extend(make_pods(prefix='some-pod', count=8, spec_revision=OLD_REVISION))
    pods.extend(make_pods(prefix='evicted-pod', count=2, is_eviction_requested=True, spec_revision=OLD_REVISION))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    ctl, client = make_controller(mc_rs, pod_storage, replace_pods_batch_size=2)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    remove_calls = list_mock_calls_by_name(client_calls, 'remove_pods')
    assert len(remove_calls) == 0

    evict_calls = list_mock_calls_by_name(client_calls, 'update_pods_request_eviction')
    assert len(evict_calls) == 1
    name, args, kwargs = evict_calls[0]
    assert len(kwargs['pod_ids']) == 3
    for p_id in kwargs['pod_ids']:
        assert p_id.startswith('some-pod')


def test_remove_delegation_parallel_reallocation_replace_pods():
    mc_rs = make_mc_rs(max_unavailable=5,
                       clusters=[FIRST_CLUSTER],
                       replica_count=10)
    mc_rs.spec.pod_template_spec.spec.resource_requests.memory_limit = 123
    yputil.set_label(mc_rs.labels, consts.DEPLOY_POLICY_LABEL, 'remove_delegation_parallel_reallocation')

    pod_storage = PodMultiClusterStorage()
    pods = []
    pods.extend(make_pods(prefix='some-pod', count=10, spec_revision=OLD_REVISION))

    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    remove_calls = list_mock_calls_by_name(client_calls, 'remove_pods')
    assert len(remove_calls) == 0

    update_calls = list_mock_calls_by_name(client_calls, 'update_pods')
    assert len(update_calls) == 0

    delegate_removing_calls = list_mock_calls_by_name(client_calls, 'delegate_removing_pods')
    assert len(delegate_removing_calls) == 1
    name, args, kwargs = delegate_removing_calls[0]
    assert len(kwargs['pod_ids']) == 5


def test_remove_delegation_evict_pods():
    mc_rs = make_mc_rs(clusters=[FIRST_CLUSTER], max_unavailable=1)
    yputil.set_label(mc_rs.labels, consts.DEPLOY_POLICY_LABEL, 'remove_delegation')

    pod_storage = PodMultiClusterStorage()
    eviction_requested_pods = make_pods(prefix='eviction-requested', is_eviction_requested=True)

    pods = make_pods(prefix='some-pod', count=2)
    pods.extend(eviction_requested_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})
    client_calls = client.method_calls
    assert len(client_calls) == 0


def test_replica_set_replica_count_decreased():
    # replica_count = 3, max_unavailable = 100
    # actual_ready = 2
    # actual_in_progress = 2
    # actual_failed = 2
    # outdated_ready =  1 (with eviction_requested)
    # outdated_in_progress = 1 (with eviction_requested)
    # outdated_failed = 1 (with eviction_requested)
    pod_storage = PodMultiClusterStorage()

    actual_ready_pods = make_pods(count=2, prefix='actual-ready', spec_revision=OLD_REVISION)
    actual_in_progress_pods = make_pods(count=2, prefix='actual-in-progress', is_ready=False,
                                        spec_revision=OLD_REVISION)
    actual_failed_pods = make_pods(count=2, prefix='actual-failed', is_failed=True, spec_revision=OLD_REVISION)
    eviction_requested_ready_pods = make_pods(prefix='outdated-ready-eviction-requested', is_eviction_requested=True)
    eviction_requested_in_progress_pods = make_pods(prefix='outdated-in-progress-eviction-requested', is_ready=False,
                                                    is_eviction_requested=True)
    pods = make_pods(prefix='outdated-failed-eviction-requested', is_eviction_requested=True, is_failed=True)
    pods.extend(actual_ready_pods)
    pods.extend(actual_in_progress_pods)
    pods.extend(actual_failed_pods)
    pods.extend(eviction_requested_ready_pods)
    pods.extend(eviction_requested_in_progress_pods)
    s = PodClusterStorage()
    s.sync_with_objects(objs_with_timestamps=pods)
    pod_storage.add_storage(s, FIRST_CLUSTER)

    mc_rs = make_mc_rs(max_unavailable=100, clusters=[FIRST_CLUSTER], replica_count=3)

    ctl, client = make_controller(mc_rs, pod_storage)
    ctl.process(mc_rs=mc_rs, failed_clusters={})

    client_calls = client.method_calls
    assert len(client_calls) == 1
    name, args, kwargs = client_calls[0]
    assert name == 'remove_pods'
    assert set(kwargs['pod_ids']) == {'actual-failed-1', 'actual-failed-2', 'outdated-failed-eviction-requested-1',
                                      'actual-in-progress-1', 'outdated-in-progress-eviction-requested-1',
                                      'outdated-ready-eviction-requested-1'}


def test_validate_mc_rs_relations():
    pod_storage = PodMultiClusterStorage()
    ps_storage = MultiClusterStorage()
    match_labels = make_match_labels()
    pod_sets = []
    for c in CLUSTERS:
        pods = make_pods(prefix='some-pod', count=3)
        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=pods)
        pod_storage.add_storage(s, c)

        ps = make_ps(match_labels, c)
        s = ClusterStorage()
        s.sync_with_objects(objs_with_timestamps=[ps])
        ps_storage.add_storage(s, c)
        pod_sets.append(ps)

    mc_rs = make_mc_rs()
    ctl, client = make_controller(mc_rs, pod_storage, match_labels=match_labels, ps_storage=ps_storage)
    stat = ctl.process(mc_rs=mc_rs, failed_clusters={})
    assert not stat.unrelated_children_found

    pod_sets[0][0].meta.fqid = 'wrong-fqid'
    stat = ctl.process(mc_rs=mc_rs, failed_clusters={})
    assert stat.unrelated_children_found


class TestReallocatePods(object):

    @staticmethod
    def _make_client_mock(failed_pods):
        client = make_yp_client()
        client.safe_update_pods.return_value = failed_pods
        return client

    def _prepare(self, update_batch_size, replace_batch_size, pod_count, failed_pods_count):
        pod_storage = PodMultiClusterStorage()
        self.pods = make_pods(prefix='some-pod', count=10, spec_revision=OLD_REVISION)

        s = PodClusterStorage()
        s.sync_with_objects(objs_with_timestamps=self.pods)
        pod_storage.add_storage(s, FIRST_CLUSTER)

        self.mc_rs = make_mc_rs(
            max_unavailable=pod_count,
            clusters=[FIRST_CLUSTER],
            replica_count=10,
        )

        self.mc_rs.spec.pod_template_spec.spec.resource_requests.memory_limit = 123
        yputil.set_label(self.mc_rs.labels, consts.DISABLE_SET_TARGET_STATE_REMOVED_LABEL, True)

        self.ctl, self.client = make_controller(
            mc_rs=self.mc_rs,
            pod_storage=pod_storage,
            reallocate_pods_batch_size=update_batch_size,
            replace_pods_batch_size=replace_batch_size,
            client=self._make_client_mock([pod_tuple[0] for pod_tuple in self.pods[:failed_pods_count]]),
        )

    def test_reallocate_pods_batch(self):
        self._prepare(update_batch_size=2, replace_batch_size=3, pod_count=3, failed_pods_count=0)
        self.ctl.process(mc_rs=self.mc_rs, failed_clusters={})

        client_calls = self.client.method_calls
        remove_calls = list_mock_calls_by_name(client_calls, 'remove_pods')
        create_calls = list_mock_calls_by_name(client_calls, 'create_pods')
        assert len(remove_calls) == 0 and len(create_calls) == 0

        safe_update_calls = list_mock_calls_by_name(client_calls, 'safe_update_pods')
        assert len(safe_update_calls) == 2
        _, __, kwargs = safe_update_calls[0]
        assert len(kwargs['pods']) == 2
        _, __, kwargs = safe_update_calls[1]
        assert len(kwargs['pods']) == 1

    def test_reallocate_pods_fail(self):
        """
        Expected calls:
        Call safe_update_pods(6), returns 5 failed
        Call update_pods_target_state with 5 failed pods
        """
        self._prepare(update_batch_size=6, replace_batch_size=3, pod_count=6, failed_pods_count=5)
        self.ctl.process(mc_rs=self.mc_rs, failed_clusters={})

        client_calls = self.client.method_calls
        safe_update_calls = list_mock_calls_by_name(client_calls, 'safe_update_pods')
        assert len(safe_update_calls) == 1
        _, __, kwargs = safe_update_calls[0]
        assert len(kwargs['pods']) == 6

        set_target_state_removed_calls = list_mock_calls_by_name(client_calls, 'update_pods_target_state')
        assert len(set_target_state_removed_calls) == 1
        _, __, kwargs = set_target_state_removed_calls[0]
        assert len(kwargs['pod_ids']) == 5
        assert kwargs['target_state'] == yp.data_model.EPodAgentTargetState_REMOVED
