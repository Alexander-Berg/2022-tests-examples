from yp_proto.yp.client.api.proto import deploy_pb2
import yp.data_model

from infra.mc_rsc.src import state
from infra.mc_rsc.src import status_maker
import pytest


def copy_pod(p, new_id):
    rv = yp.data_model.TPod()
    rv.CopyFrom(p)
    rv.meta.id = new_id
    return rv


def test_make_status(first_cluster,
                     second_cluster,
                     mc_rs,
                     ready_pod,
                     failed_pod,
                     in_progress_pod,
                     replica_count,
                     target):
    current_state = state.MultiClusterCurrentState(max_tolerable_downtime_seconds=0)
    current_state.add(ready_pod, first_cluster)
    current_state.add(failed_pod, first_cluster)
    p = copy_pod(ready_pod, '{}-2'.format(ready_pod.meta.id))
    p.status.agent.pod_agent_payload.status.revision = target - 1
    p.spec.pod_agent_payload.spec.revision = target - 1
    current_state.add(p, first_cluster)
    current_state.add(in_progress_pod, first_cluster)
    ctl_errors = {second_cluster: Exception('test')}
    maker = status_maker.MultiClusterReplicaSetStatusMaker()
    s, _ = maker.make_status(mc_rs=mc_rs,
                             current_state=current_state,
                             ctl_errors=ctl_errors,
                             failed_clusters={})
    assert s.in_progress.status == yp.data_model.CS_TRUE
    assert s.ready.status == yp.data_model.CS_FALSE
    mc_details = s.multi_cluster_deploy_status.details
    assert mc_details.total_progress.pods_total == 4
    assert mc_details.total_progress.pods_in_progress == 1
    assert mc_details.total_progress.pods_ready == 2
    assert mc_details.total_progress.pods_failed == 1
    assert mc_details.current_revision_progress.pods_total == replica_count * len(mc_rs.spec.clusters)
    assert mc_details.current_revision_progress.pods_in_progress == 1
    assert mc_details.current_revision_progress.pods_ready == 1
    assert mc_details.controller_status.last_attempt.succeeded.status == yp.data_model.CS_FALSE
    assert set(mc_details.revisions.keys()) == {target - 1, target}

    first_details = s.cluster_deploy_statuses[first_cluster].details
    assert first_details.total_progress.pods_total == 4
    assert first_details.total_progress.pods_in_progress == 1
    assert first_details.total_progress.pods_ready == 2
    assert first_details.current_revision_progress.pods_total == replica_count
    assert first_details.current_revision_progress.pods_in_progress == 1
    assert first_details.current_revision_progress.pods_ready == 1
    assert first_details.controller_status.last_attempt.succeeded.status == yp.data_model.CS_TRUE
    assert set(first_details.revisions.keys()) == {target - 1, target}

    second_details = s.cluster_deploy_statuses[second_cluster].details
    assert second_details.controller_status.last_attempt.succeeded.status == yp.data_model.CS_FALSE

    mc_rs.status.CopyFrom(s)
    new_s, _ = maker.make_status(mc_rs=mc_rs,
                                 current_state=current_state,
                                 ctl_errors=ctl_errors,
                                 failed_clusters={})
    assert s == new_s


def test_make_status_in_progress_old_revision_pods(first_cluster,
                                                   second_cluster,
                                                   mc_rs,
                                                   ready_pod,
                                                   target):
    # Even if we have only one pod in old revision we need to stay in
    # in_progress status.
    current_state = state.MultiClusterCurrentState(max_tolerable_downtime_seconds=0)
    current_state.add(ready_pod, first_cluster)
    ready_pod_2 = copy_pod(ready_pod, '{}-2'.format(ready_pod.meta.id))
    current_state.add(ready_pod_2, first_cluster)
    outdated_pod = copy_pod(ready_pod, '{}-3'.format(ready_pod.meta.id))
    outdated_pod.status.agent.pod_agent_payload.status.revision = target - 1
    outdated_pod.spec.pod_agent_payload.spec.revision = target - 1
    current_state.add(outdated_pod, first_cluster)

    ready_pod_3 = copy_pod(ready_pod, '{}-3'.format(ready_pod.meta.id))
    current_state.add(ready_pod, second_cluster)
    current_state.add(ready_pod_2, second_cluster)
    current_state.add(ready_pod_3, second_cluster)

    maker = status_maker.MultiClusterReplicaSetStatusMaker()
    s, _ = maker.make_status(mc_rs=mc_rs,
                             current_state=current_state,
                             ctl_errors={},
                             failed_clusters={})

    assert s.ready.status == yp.data_model.CS_FALSE
    assert s.in_progress.status == yp.data_model.CS_TRUE


def test_make_status_zero_replica_count(mc_rs):
    for c in mc_rs.spec.clusters:
        c.spec.replica_count = 0
    current_state = state.MultiClusterCurrentState(max_tolerable_downtime_seconds=0)
    maker = status_maker.MultiClusterReplicaSetStatusMaker()
    s, _ = maker.make_status(mc_rs=mc_rs,
                             current_state=current_state,
                             ctl_errors={},
                             failed_clusters={})
    assert s.ready.status == yp.data_model.CS_TRUE
    assert s.in_progress.status == yp.data_model.CS_FALSE


def test_make_status_removed_ready(first_cluster,
                                   mc_rs,
                                   ready_pod,
                                   removed_ready_pod,
                                   removed_in_progress_pod):
    current_state = state.MultiClusterCurrentState(max_tolerable_downtime_seconds=0)
    current_state.add(ready_pod, first_cluster)
    current_state.add(removed_in_progress_pod, first_cluster)
    current_state.add(removed_ready_pod, first_cluster)
    maker = status_maker.MultiClusterReplicaSetStatusMaker()
    s, _ = maker.make_status(mc_rs=mc_rs,
                             current_state=current_state,
                             ctl_errors={},
                             failed_clusters={})

    assert s.ready.status == yp.data_model.CS_FALSE
    assert s.in_progress.status == yp.data_model.CS_TRUE
    mc_details = s.multi_cluster_deploy_status.details
    assert mc_details.total_progress.pods_total == 3
    assert mc_details.total_progress.pods_in_progress == 2
    assert mc_details.total_progress.pods_ready == 1


@pytest.mark.parametrize('threshold_type', ["", "MAX_UNAVAILABLE"])
@pytest.mark.parametrize('replica_count, max_unavailable, ready_target_pods_count, is_rs_ready', [
    (1, 1, 0, False),
    (5, 5, 0, False),
    (1, 1, 1, True),
    (5, 5, 1, True),
    (0, 3, 0, True),
    (10, 3, 5, False),
    (10, 3, 7, True),
    (10, 10, 0, False),
    (10, 10, 1, True),
])
def test_ready_threshold_max_unavalible_and_default(threshold_type, replica_count, max_unavailable, ready_target_pods_count, is_rs_ready):
    ready_threshold = deploy_pb2.TDeployReadyCriterion.TReadyThreshold()
    ready_threshold.type = threshold_type
    assert is_rs_ready == status_maker.ReplicaSetStatusMaker._is_ready_threshold_achieved(ready_threshold, max_unavailable,
                                                                                          replica_count, ready_target_pods_count)


@pytest.mark.parametrize('replica_count, ready_target_pods_count, is_rs_ready', [
    (0, 0, True),
    (10, 1, False),
    (10, 8, True),
    (1, 0, False),
    (1, 1, True),
    (2, 1, False),
    (2, 2, True),
    (100, 95, False),
    (100, 96, True)
])
def test_ready_threshold_auto(replica_count, ready_target_pods_count, is_rs_ready):
    ready_threshold = deploy_pb2.TDeployReadyCriterion.TReadyThreshold()
    ready_threshold.type = "AUTO"
    max_unavalible = 10
    assert is_rs_ready == status_maker.ReplicaSetStatusMaker._is_ready_threshold_achieved(
        ready_threshold, max_unavalible, replica_count, ready_target_pods_count)


@pytest.mark.parametrize('replica_count, min_percent_ready, ready_target_pods_count, is_rs_ready', [
    (0, 0, 0, True),
    (10, 50, 3, False),
    (10, 50, 5, True),
    (10, 100, 9, False),
    (10, 100, 10, True),
    (10, 0, 0, True),
])
def test_ready_threshold_min_percent(replica_count, min_percent_ready, ready_target_pods_count, is_rs_ready):
    ready_threshold = deploy_pb2.TDeployReadyCriterion.TReadyThreshold()
    ready_threshold.type = "MIN_READY_PODS_PERCENT"
    ready_threshold.min_ready_pods_percent.value = min_percent_ready
    max_unavalible = 10
    assert is_rs_ready == status_maker.ReplicaSetStatusMaker._is_ready_threshold_achieved(
        ready_threshold, max_unavalible, replica_count, ready_target_pods_count)


@pytest.mark.parametrize('replica_count, max_not_ready, ready_target_pods_count, is_rs_ready', [
    (0, 0, 0, True),
    (10, 2, 7, False),
    (10, 2, 8, True),
    (10, 0, 9, False),
    (10, 0, 10, True),
    (10, 10, 0, True),
])
def test_ready_threshold_max_not_ready(replica_count, max_not_ready, ready_target_pods_count, is_rs_ready):
    ready_threshold = deploy_pb2.TDeployReadyCriterion.TReadyThreshold()
    ready_threshold.type = "MAX_NOT_READY_PODS"
    ready_threshold.max_not_ready_pods.value = max_not_ready
    max_unavalible = 10
    assert is_rs_ready == status_maker.ReplicaSetStatusMaker._is_ready_threshold_achieved(
        ready_threshold, max_unavalible, replica_count, ready_target_pods_count)
