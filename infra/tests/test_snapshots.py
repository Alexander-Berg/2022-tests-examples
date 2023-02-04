from infra.watchdog.src.lib.snapshots import count_stuck_snapshots, check_awaited_snapshot_states, count_uncleaned_snapshots
from mock import Mock
from nanny_repo import repo_pb2


def test_count_stuck_snapshots():
    s_pb = repo_pb2.Service()
    sn = s_pb.spec.snapshot.add()
    sn.target = repo_pb2.Snapshot.ACTIVE
    sn = s_pb.status.snapshot.add()
    sn.status = repo_pb2.SnapshotStatus.ACTIVE
    assert count_stuck_snapshots(s_pb).get() == 0

    s_pb = repo_pb2.Service()
    sn = s_pb.spec.snapshot.add()
    sn.target = repo_pb2.Snapshot.PREPARED
    sn = s_pb.status.snapshot.add()
    sn.status = repo_pb2.SnapshotStatus.ACTIVE
    assert count_stuck_snapshots(s_pb).get() == 1

    s_pb = repo_pb2.Service()
    s_pb.status.is_paused.value = True
    sn = s_pb.spec.snapshot.add()
    sn.target = repo_pb2.Snapshot.PREPARED
    sn = s_pb.status.snapshot.add()
    sn.status = repo_pb2.SnapshotStatus.ACTIVE
    assert count_stuck_snapshots(s_pb).get() == 0

    s_pb = repo_pb2.Service()
    sn = s_pb.spec.snapshot.add()
    sn.target = repo_pb2.Snapshot.ACTIVE
    sn = s_pb.status.snapshot.add()
    sn.status = repo_pb2.SnapshotStatus.PREPARING
    assert count_stuck_snapshots(s_pb).get() == 0


def test_check_snapshot_states():
    zk_client = Mock()

    expected_snapshot_states = {
        'test': {'sn1': {'ACTIVE': 1, 'PREPARED': 2}}
    }
    s_pb = repo_pb2.Service(id='test')
    sn = s_pb.status.snapshot.add()
    sn.id = 'sn1'
    sn.status = repo_pb2.SnapshotStatus.ACTIVE
    zk_client.get_service_state.return_value = s_pb
    assert check_awaited_snapshot_states(zk_client, expected_snapshot_states).get() == 1

    expected_snapshot_states = {
        'test': {'sn1': {'ACTIVE': 8}}
    }
    sn.status = repo_pb2.SnapshotStatus.PREPARED
    assert check_awaited_snapshot_states(zk_client, expected_snapshot_states).get() == 0


def test_count_uncleaned_stalled_snapshots():
    cleanup_pb2 = repo_pb2.CleanupPolicy()
    cleanup_pb2.spec.type = repo_pb2.CleanupPolicySpec.SIMPLE_COUNT_LIMIT
    cleanup_pb2.spec.simple_count_limit.stalled_ttl = 'PT24S'
    s_pb = repo_pb2.Service()
    sn = s_pb.spec.snapshot.add()
    sn.target = repo_pb2.Snapshot.PREPARED
    sn = s_pb.status.snapshot.add()
    sn.status = repo_pb2.SnapshotStatus.PREPARING
    sn.last_transition_time.FromSeconds(1)
    assert count_uncleaned_snapshots(cleanup_pb2, s_pb).get() == 1

    s_pb.status.is_paused.value = True
    assert count_uncleaned_snapshots(cleanup_pb2, s_pb).get() == 0


def test_count_uncleaned_removable_snapshots():
    cleanup_pb2 = repo_pb2.CleanupPolicy()
    cleanup_pb2.spec.type = repo_pb2.CleanupPolicySpec.SIMPLE_COUNT_LIMIT
    cleanup_pb2.spec.simple_count_limit.disposable_count = 0
    cleanup_pb2.spec.simple_count_limit.snapshots_count = 0
    s_pb = repo_pb2.Service()
    s_pb.status.summary.value = 'OFFLINE'
    sn = s_pb.spec.snapshot.add()
    sn.target = repo_pb2.Snapshot.PREPARED
    sn.id = '1'
    assert count_uncleaned_snapshots(cleanup_pb2, s_pb).get() == 1

    cleanup_pb2.spec.simple_count_limit.disposable_count = 0
    cleanup_pb2.spec.simple_count_limit.snapshots_count = 1
    assert count_uncleaned_snapshots(cleanup_pb2, s_pb).get() == 0

    sn.is_disposable = True
    cleanup_pb2.spec.simple_count_limit.disposable_count = 0
    cleanup_pb2.spec.simple_count_limit.snapshots_count = 1
    assert count_uncleaned_snapshots(cleanup_pb2, s_pb).get() == 1

    sn.is_disposable = True
    cleanup_pb2.spec.simple_count_limit.disposable_count = 1
    cleanup_pb2.spec.simple_count_limit.snapshots_count = 1
    assert count_uncleaned_snapshots(cleanup_pb2, s_pb).get() == 0

    s_pb.status.summary.value = 'PREPARING'
    cleanup_pb2.spec.simple_count_limit.disposable_count = 0
    cleanup_pb2.spec.simple_count_limit.snapshots_count = 0
    assert count_uncleaned_snapshots(cleanup_pb2, s_pb).get() == 0

    s_pb.status.summary.value = 'OFFLINE'
    sn.target = repo_pb2.Snapshot.ACTIVE
    cleanup_pb2.spec.simple_count_limit.disposable_count = 0
    cleanup_pb2.spec.simple_count_limit.snapshots_count = 0
    assert count_uncleaned_snapshots(cleanup_pb2, s_pb).get() == 0
