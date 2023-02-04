from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_move_hold_tags(coordinator):
    def check_versions_count(versions_count):
        for h in resolve_hosts(['rtc:maps_b']):
            assert versions_count == coordinator.versions(h).text.count('\n')

    pkg_b1_hash = coordinator.upload('pkg-b:tag1', '1.0', 'gen-ab1', tvm_id=1)
    coordinator.announce(pkg_b1_hash, resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b:tag1', '1.0', resolve_hosts(['rtc:maps_b']))
    pkg_b2_hash = coordinator.upload('pkg-b:tag2', '1.0', 'gen-ab1', tvm_id=1)
    coordinator.announce(pkg_b2_hash, resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b:tag2', '1.0', resolve_hosts(['rtc:maps_b']))

    coordinator.step_in('pkg-b:tag1', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    check_versions_count(2)
    coordinator.require_version('pkg-b:tag1', '1.0',  resolve_hosts(['rtc:maps_b']))
    coordinator.require_version('pkg-b:tag2', '1.0',  resolve_hosts(['rtc:maps_b']))

    coordinator.step_in('pkg-b:tag2', '1.0', 'gen-ab1', branch='stable/hold', tvm_id=1)
    check_versions_count(2)
    coordinator.require_version('pkg-b:tag1', '__CURRENT__',  resolve_hosts(['rtc:maps_b']))
    coordinator.require_version('pkg-b:tag2', '__CURRENT__',  resolve_hosts(['rtc:maps_b']))

    coordinator.remove('pkg-b:tag2', '1.0', 'gen-ab1', tvm_id=1)
    check_versions_count(1)
    coordinator.require_version('pkg-b:tag1', '__CURRENT__',  resolve_hosts(['rtc:maps_b']))

    coordinator.step_in('pkg-b', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.require_version('pkg-b:tag1', '1.0',  resolve_hosts(['rtc:maps_b']))
