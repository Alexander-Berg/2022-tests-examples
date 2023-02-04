import pytest
import mock
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import (
    LockedError, UnprocessableEntityError, NotFoundError
)


def test_locks(coordinator):
    coordinator.http_post('/debug/UpdateHostInfoCache')

    # Smoke test for locks
    lock_id = coordinator.get_lock('testlk', 'me', 10, 20)
    with pytest.raises(LockedError):
        coordinator.get_lock('testlk', 'smthelse', 10, 120)
    coordinator.http_post('/locks/release', id=lock_id)

    # Expiration test
    lock_id = coordinator.get_lock('testlk', 'me', 10, 20)
    coordinator.http_post('/debug/PurgeExpiredLocks', now=5)
    coordinator.http_post('/locks/extend', id=lock_id, till=16)
    coordinator.http_post('/debug/PurgeExpiredLocks', now=14)
    coordinator.http_post('/locks/extend', id=lock_id, till=24)
    coordinator.http_post('/debug/PurgeExpiredLocks', now=22)
    with pytest.raises(UnprocessableEntityError):
        coordinator.http_post('/locks/extend', id=lock_id, till=30)

    # Alias test
    coordinator.get_lock('ecstatic/host_switch/a1', 'a1', 10, 20)
    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/a2', 'a2', 10, 20)

    # Non-binary lock test
    lock_id = coordinator.get_lock('ecstatic/host_switch/ce1', 'ce1', 10, 20)
    coordinator.get_lock('ecstatic/host_switch/ce1', 'ce1', 50, 50)
    lock_id2 = coordinator.get_lock('ecstatic/host_switch/ce1', 'ce1', 100, 200)
    assert lock_id == lock_id2

    assert len(coordinator.list_locks('ce1', 10).lock) == 0

    locks_response = coordinator.list_locks('ce1', 0)
    assert len(locks_response.lock) == 1
    lock = locks_response.lock[0]
    assert lock.name == 'ecstatic/grp_switch/rtc:maps_ce'
    assert lock.id == '9bc1294cdfd070e6d7ff0d88362ad1e2c5a09452'
    assert lock.remaining == 1
    assert lock.till == 100
    assert lock.expires_at == 200

    coordinator.get_lock('ecstatic/host_switch/ce2', 'ce2', 10, 20)
    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 10, 20)
    coordinator.http_post('/locks/release', id=lock_id)
    coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 10, 20)
    coordinator.http_post('/debug/PurgeExpiredLocks', now=50)

    # Non-binary + expiration
    coordinator.get_lock('ecstatic/host_switch/ce1', 'ce1', 60, 60)
    coordinator.get_lock('ecstatic/host_switch/ce2', 'ce2', 70, 70)
    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 80, 80)
    coordinator.http_post('/debug/PurgeExpiredLocks', now=65)
    coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 80, 80)

    # check for null holders
    coordinator.list_locks('ce2', 0)
    coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 80, 80)


def test_lock_with_all_dead_hosts(coordinator, mongo):
    coordinator.http_post('/debug/UpdateHostInfoCache')

    mongo.add_dead_hosts(['ce1', 'ce2'])
    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 10, 120)


def test_lock_with_one_dead_host(coordinator, mongo):
    coordinator.http_post('/debug/UpdateHostInfoCache')

    lock_id = coordinator.get_lock('ecstatic/host_switch/ce1', 'ce1', 10, 120)
    mongo.add_dead_hosts(['ce2'])
    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 10, 120)

    coordinator.http_post('/locks/release', id=lock_id)

    coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 10, 120)


def test_not_lock_with_dead_hosts_in_other_groups(coordinator, mongo):
    coordinator.http_post('/debug/UpdateHostInfoCache')

    mongo.add_dead_hosts(['ce1', 'ce2'])
    coordinator.get_lock('ecstatic/host_switch/cd21', 'cd21', 10, 120)


def test_not_count_locked_host_as_dead(coordinator, mongo):
    coordinator.http_post('/debug/UpdateHostInfoCache')

    coordinator.get_lock('ecstatic/host_switch/ce1', 'ce1', 10, 120)
    mongo.add_dead_hosts(['ce1'])
    coordinator.get_lock('ecstatic/host_switch/ce2', 'ce2', 10, 120)


def test_personal_lock(coordinator, mongo):
    mongo.reconfigure(config='data/ecstatic-nogroups.conf')
    coordinator.http_post('/debug/UpdateHostInfoCache')

    coordinator.get_lock('ecstatic/host_switch/a1', 'a1', 10, 120)


def test_xtlock(coordinator, mongo):
    coordinator.http_post('/debug/UpdateHostInfoCache')

    mongo.add_dead_hosts(['a1'])
    coordinator.get_lock('custom_lock', 'a2', 10, 120)


def test_duplicate_host_dc_records(coordinator, mongo):
    coordinator.http_post('/debug/UpdateHostInfoCache')

    with mock.patch('maps.infra.ecstatic.sandbox.reconfigurer.lib.reconfigure.Reconfigurer._commit_write') as commit:
        commit.side_effect = mock.Mock()
        mongo.reconfigure()

    for row in mongo.db['host_dc'].find({}):
        assert mongo.db['host_dc'].count({'host': row['host']}) == 3

    coordinator.http_post('/debug/UpdateHostInfoCache')

    coordinator.get_lock('ecstatic/host_switch/ce1', 'ce1', 10, 120)


def test_add_new_host(coordinator, mongo):
    coordinator.http_post('/debug/UpdateHostInfoCache')

    mongo.reconfigure(hosts_config='data/host-groups-b6-added.conf')
    with pytest.raises(NotFoundError):
        coordinator.get_lock('ecstatic/host_switch/b6', 'b6', 10, 120)

    coordinator.http_post('/debug/UpdateHostInfoCache')
    coordinator.get_lock('ecstatic/host_switch/b6', 'b6', 10, 120)


def test_redeployment_group(coordinator, mongo):
    coordinator.http_post('/debug/UpdateHostInfoCache')

    mongo.reconfigure(hosts_config='data/host-groups-b6-added-redeployment.conf')
    with pytest.raises(NotFoundError):
        coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 10, 120)
    coordinator.http_post('/debug/UpdateHostInfoCache')
    coordinator.get_lock('ecstatic/host_switch/ce3', 'ce3', 10, 120)
