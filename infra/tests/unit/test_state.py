import mock

from infra.mc_rsc.src import state, consts


def test_multi_cluster_current_state(first_cluster,
                                     second_cluster,
                                     ready_pod,
                                     in_progress_pod,
                                     eviction_requested_pod,
                                     eviction_requested_pod_w_target_state_removed,
                                     removed_ready_pod,
                                     removed_in_progress_pod,
                                     target):
    s = state.MultiClusterCurrentState(max_tolerable_downtime_seconds=0)
    last_deploy_timestamp = 0
    s.add(ready_pod, first_cluster)
    in_progress_pod.spec.pod_agent_payload.spec.revision = target - 1
    removed_in_progress_pod.spec.pod_agent_payload.spec.revision = target - 1
    s.add(in_progress_pod, first_cluster)
    s.add(in_progress_pod, second_cluster)
    s.add(eviction_requested_pod, second_cluster)
    s.add(eviction_requested_pod_w_target_state_removed, first_cluster)
    s.add(removed_ready_pod, first_cluster)
    with mock.patch('time.time') as mocked_time:
        mocked_time.return_value = last_deploy_timestamp + consts.MAX_DESTROY_HOOK_PERIOD_SECS + 1
        s.add(removed_in_progress_pod, first_cluster)
    assert s.ready.count_all() == 1
    assert s.in_progress.count_all() == 3
    assert s.to_evict_in_progress.count_all() == 1
    assert set(s.ready.find(target)) == {(first_cluster, ready_pod.meta.id)}
    assert not set(s.ready.exclude(target))
    assert set(s.in_progress.find(target - 1)) == {
        (first_cluster, in_progress_pod.meta.id),
        (second_cluster, in_progress_pod.meta.id),
        # (first_cluster, removed_in_progress_pod.meta.id)
    }
    assert set(s.in_progress.exclude(target - 1)) == {
        (second_cluster, eviction_requested_pod.meta.id)
    }
    assert set(s.revisions()) == {target - 1, target}
    assert s.target_state_removed.count_all() == 3

    assert set(s.target_state_removed.all()) == {
        (first_cluster, removed_in_progress_pod.meta.id),
        (first_cluster, removed_ready_pod.meta.id),
        (first_cluster, eviction_requested_pod_w_target_state_removed.meta.id),
    }
