from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_move_hold(coordinator):
    hash = coordinator.upload('pkg-b', '1.0', 'gen-ab1', tvm_id=1)
    coordinator.step_in('pkg-b', '1.0', 'gen-ab1', branch='stable/hold', tvm_id=1)

    coordinator.announce(hash, resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.require_version('pkg-b', '__CURRENT__', resolve_hosts(['rtc:maps_b']))

    coordinator.step_in('pkg-b', '1.0', 'gen-ab1', tvm_id=1)
    coordinator.require_version('pkg-b', '1.0', resolve_hosts(['rtc:maps_b']))
