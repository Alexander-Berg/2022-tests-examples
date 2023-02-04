import pytest
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import LockedError


def test_meta_locks(coordinator, mongo):
    mongo.reconfigure(config='data/ecstatic-meta-switch.conf')
    coordinator.http_post('/debug/UpdateHostInfoCache')

    coordinator.get_lock('ecstatic/host_switch/a1', 'a1', 10, 120)
    coordinator.get_lock('ecstatic/host_switch/b1', 'b1', 10, 120)
    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/a2', 'a2', 10, 120)


def test_meta_locks_with_dead_hosts(coordinator, mongo):
    mongo.reconfigure(config='data/ecstatic-meta-switch.conf')
    coordinator.http_post('/debug/UpdateHostInfoCache')
    mongo.add_dead_hosts(['a1'])

    coordinator.get_lock('ecstatic/host_switch/b1', 'b1', 10, 120)

    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/b2', 'b2', 10, 120)


def test_dc_meta_locks(coordinator, mongo):
    mongo.reconfigure(config='data/ecstatic-meta-switch.conf')
    coordinator.http_post('/debug/UpdateHostInfoCache')

    coordinator.get_lock('ecstatic/host_switch/h1dca', 'h1dca', 10, 120)

    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/h2dcb', 'h2dcb', 10, 120)

    with pytest.raises(LockedError):
        coordinator.get_lock('ecstatic/host_switch/i4dcb', 'i4dcb', 10, 120)

    coordinator.get_lock('ecstatic/host_switch/i1dca', 'i1dca', 10, 120)
