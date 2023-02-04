from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_multiple_sources(coordinator):
    pkg_q1_hash = coordinator.upload('pkg-q', '1.0', 'gen-q1', branch='stable', tvm_id=6)
    pkg_q2_hash = coordinator.upload('pkg-q', '2.0', 'gen-q1', branch='stable', tvm_id=6)

    coordinator.announce(pkg_q1_hash, resolve_hosts(['rtc:maps_comp', 'rtc:maps_c']))
    coordinator.postdl('pkg-q', '1.0', resolve_hosts(['rtc:maps_comp', 'rtc:maps_c']))
    coordinator.require_version('pkg-q', '1.0', resolve_hosts(['rtc:maps_comp', 'rtc:maps_c']))

    coordinator.announce(pkg_q2_hash, resolve_hosts(['rtc:maps_c']))
    coordinator.postdl('pkg-q', '2.0', resolve_hosts(['rtc:maps_c']))
    coordinator.require_version('pkg-q', '1.0', resolve_hosts(['rtc:maps_comp', 'rtc:maps_c']))

    coordinator.announce(pkg_q2_hash, 'q1')
    coordinator.postdl('pkg-q', '2.0', 'q1')
    coordinator.require_version('pkg-q', '1.0', resolve_hosts(['rtc:maps_comp', 'rtc:maps_c']))

    coordinator.announce(pkg_q2_hash, 'q2')
    coordinator.postdl('pkg-q', '2.0', 'q2')
    coordinator.require_version('pkg-q', '2.0', resolve_hosts(['rtc:maps_comp', 'rtc:maps_c']))
