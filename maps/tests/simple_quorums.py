from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_simple_quorums(coordinator):
    pkg_b1_hash = \
        coordinator.upload('pkg-b', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkg_b2_hash = \
        coordinator.upload('pkg-b', '2.0', 'gen-ab1', branch='stable', tvm_id=1)

    coordinator.announce(pkg_b1_hash, resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.require_version('pkg-b', '1.0', resolve_hosts(['rtc:maps_b']))

    coordinator.announce(pkg_b2_hash, ['b1', 'b2', 'b3'])
    coordinator.require_version('pkg-b', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b', '2.0', ['b1', 'b2', 'b3'])
    coordinator.require_version('pkg-b', '1.0', resolve_hosts(['rtc:maps_b']))

    coordinator.announce(pkg_b2_hash, ['b4'])
    coordinator.require_version('pkg-b', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b', '2.0', ['b4'])
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', None, ['b5'])

    coordinator.announce(pkg_b2_hash, ['b5'])
    coordinator.require_version('pkg-b', '2.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version('pkg-b', None, ['b5'])
    coordinator.postdl('pkg-b', '2.0', ['b5'])
    coordinator.require_version('pkg-b', '2.0', resolve_hosts(['rtc:maps_b']))
