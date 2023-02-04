from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_synchronous_one_dataset_moved(coordinator):
    hash = coordinator.upload('pkg-g', '1.0', 'gen-h1', branch='stable', tvm_id=4)
    coordinator.announce(hash, resolve_hosts(['rtc:maps_h']))
    coordinator.postdl('pkg-g', '1.0', resolve_hosts(['rtc:maps_h']))
    assert 'pkg-g\t__CURRENT__' in coordinator.versions('h1').text
