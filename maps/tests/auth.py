import pytest
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import ForbiddenError, UnauthorizedError


def test_auth(coordinator):
    with pytest.raises(UnauthorizedError):
        coordinator.upload('pkg-b', '1.0', '')

    with pytest.raises(ForbiddenError):
        coordinator.upload('pkg-b', '1.0', 'gen-cd1', tvm_id=2)

    pkg_b_hash = coordinator.upload('pkg-b', '1.0', 'gen-ab1', tvm_id=1)

    with pytest.raises(UnauthorizedError):
        coordinator.step_in('pkg-b', '1.0', '')

    with pytest.raises(ForbiddenError):
        coordinator.step_in('pkg-b', '1.0', 'gen-cd1', tvm_id=2)

    coordinator.step_in('pkg-b', '1.0', 'gen-ab1', tvm_id=1)

    coordinator.upload('pkg-b', '2.0', 'gen-ab1', branch='stable', tvm_id=1)

    # Announce on wrong host should succeed...
    coordinator.announce(pkg_b_hash, 'a1')

    # ...but should not lead to postdl trigger pending
    assert 'pkg-b.1.0' not in coordinator.get_postdl('a1').text

    coordinator.postdl('pkg-b', '1.0', 'a1')

    # The same should apply to the right host, but without any authentication...
    coordinator.announce(
        pkg_b_hash, 'b1', peer_id='bforeign')
    assert 'pkg-b.1.0' not in coordinator.get_postdl('b1').text

    coordinator.announce(pkg_b_hash, 'b1', port=1)
    assert 'pkg-b\t1.0' in coordinator.get_postdl('b1').text

    pkg_a_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.announce(pkg_a_hash, resolve_hosts(['rtc:maps_a']))
    coordinator.postdl('pkg-a', '1.0', resolve_hosts(['rtc:maps_a']))
