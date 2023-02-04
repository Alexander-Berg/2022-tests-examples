import pytest
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import LockedError


def test_parallel_datacenters(mongo, coordinator):
    mongo.reconfigure(config='data/dc_parallel.conf')

    coordinator.http_post('/debug/UpdateHostInfoCache')

    lock_1 = coordinator.get_lock('ecstatic/host_switch/i1dca', 'i1dca', 10, 20)

    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/i4dcb', 'i4dcb', 10, 20)

    lock_2 = coordinator.get_lock('ecstatic/host_switch/i2dca', 'i2dca', 10, 20)

    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/i3dca', 'i4dcb', 10, 20)

    coordinator.release_lock(lock_1)
    coordinator.release_lock(lock_2)

    coordinator.get_lock('ecstatic/host_switch/i4dcb', 'i4dcb', 10, 20)

    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/i3dca', 'i3dca', 10, 20)

    coordinator.get_lock('ecstatic/host_switch/i5dcb', 'i5dcb', 10, 20)


def test_parallel_datacenters_with_dead_hosts(mongo, coordinator):
    mongo.reconfigure(config='data/dc_parallel.conf')

    coordinator.http_post('/debug/UpdateHostInfoCache')

    mongo.add_dead_hosts(['i1dca', 'i2dca'])

    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/i4dcb', 'i4dcb', 10, 20)


def test_parallel_datacenters_one_locked_and_one_dead_host(mongo, coordinator):
    mongo.reconfigure(config='data/dc_parallel.conf')

    coordinator.http_post('/debug/UpdateHostInfoCache')

    mongo.add_dead_hosts(['i1dca'])

    coordinator.get_lock('ecstatic/host_switch/i2dca', 'i2dca', 10, 20)

    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/i4dcb', 'i4dcb', 10, 20)

    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/i3dca', 'i3dca', 10, 20)

    coordinator.torrents(host='i1dca')

    coordinator.get_lock('ecstatic/host_switch/i3dca', 'i3dca', 10, 20)
