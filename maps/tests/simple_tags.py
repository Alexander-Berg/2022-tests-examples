from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


def test_simple_tags(coordinator):
    pkg_b1_1_hash = \
        coordinator.upload('pkg-b:1', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkg_b1_2_hash = \
        coordinator.upload('pkg-b:2', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkg_b2_1_hash = \
        coordinator.upload('pkg-b:1', '2.0', 'gen-ab1', branch='stable', tvm_id=1)

    for h in resolve_hosts(['rtc:maps_b']):
        for dataset in [Dataset('pkg-b:1', '1.0'), Dataset('pkg-b:2', '1.0'), Dataset('pkg-b:1', '2.0')]:
            assert dataset in coordinator.torrents(host=h).all_datasets

    coordinator.announce(pkg_b1_1_hash, resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b:1', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.announce(pkg_b1_2_hash, resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b:2', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.require_version('pkg-b:1', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.require_version('pkg-b:2', '1.0', resolve_hosts(['rtc:maps_b']))

    coordinator.announce(pkg_b2_1_hash, resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-b:1', '2.0', resolve_hosts(['rtc:maps_b']))
    coordinator.require_version('pkg-b:2', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.require_version('pkg-b:2', '1.0', resolve_hosts(['rtc:maps_b']))
