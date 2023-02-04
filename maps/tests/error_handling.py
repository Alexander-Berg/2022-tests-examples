from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts
import pytest


def test_error_handling(coordinator):
    pkg_b1_hash = coordinator.upload('pkg-b', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkg_b2_hash = coordinator.upload('pkg-b', '2.0', 'gen-ab1', branch='stable', tvm_id=1)

    coordinator.announce(pkg_b1_hash, resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.announce(pkg_b2_hash, resolve_hosts(['rtc:maps_b']))

    # Failed postdl should not respawn indefinitely
    assert 'pkg-b\t2.0\n' in coordinator.get_postdl('b1').text
    coordinator.postdl_failed('pkg-b', '2.0', 'b1')
    with pytest.raises(AssertionError):
        assert 'pkg-b\t2.0\n' in coordinator.get_postdl('b1').text

    coordinator.postdl('pkg-b', '2.0', ['b2', 'b3', 'b4', 'b5'])

    # Version switch should occur despite failed postdl on `b1'
    coordinator.require_version('pkg-b', '2.0', ['b2', 'b3', 'b4', 'b5'])
    coordinator.require_version('pkg-b', None, ['b1'])

    # One more switch failure should lead to a rollback
    coordinator.switch_failed('pkg-b', '2.0', 'b5')
    coordinator.require_version('pkg-b', '1.0', resolve_hosts(['rtc:maps_b']))

    # Another step-in should clear all error status
    coordinator.step_in('pkg-b', '2.0', 'gen-ab1', tvm_id=1)
    assert 'pkg-b\t2.0\n' in coordinator.get_postdl('b1').text
    coordinator.require_version('pkg-b', '2.0', ['b2', 'b3', 'b4', 'b5'])
    coordinator.require_version('pkg-b', None, ['b1'])

    coordinator.postdl('pkg-b', '2.0', ['b1'])
    coordinator.require_version('pkg-b', '2.0', ['b1'])

    # check empty version is valid case
    coordinator.switch_failed('pkg-b', '', 'b5')
