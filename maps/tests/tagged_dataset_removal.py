from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts
import pytest
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


def test_tagged_dataset_removal(coordinator):
    pkg_a1_hash = coordinator.upload(
        'pkg-a:1', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkg_a2_hash = coordinator.upload(
        'pkg-a:2', '1.0', 'gen-ab1', branch='stable', tvm_id=1)

    coordinator.announce(pkg_a1_hash, resolve_hosts(['rtc:maps_a']))
    coordinator.postdl('pkg-a:1', '1.0', resolve_hosts(['rtc:maps_a']))
    coordinator.announce(pkg_a2_hash, resolve_hosts(['rtc:maps_a']))
    coordinator.postdl('pkg-a:2', '1.0', resolve_hosts(['rtc:maps_a']))

    for h in resolve_hosts(['rtc:maps_a']):
        assert Dataset('pkg-a:1', '1.0') in coordinator.torrents(host=h).all_datasets
        assert Dataset('pkg-a:2', '1.0') in coordinator.torrents(host=h).all_datasets

    assert coordinator.remove('pkg-a:3', '1.0', 'gen-ab1', tvm_id=1) == set()
    for h in resolve_hosts(['rtc:maps_a']):
        assert Dataset('pkg-a:1', '1.0') in coordinator.torrents(host=h).all_datasets
        assert Dataset('pkg-a:2', '1.0') in coordinator.torrents(host=h).all_datasets

    assert coordinator.remove('pkg-a:2', '1.0', 'gen-ab1', tvm_id=1) == {Dataset('pkg-a:2', '1.0')}
    for h in resolve_hosts(['rtc:maps_a']):
        assert Dataset('pkg-a:1', '1.0') in coordinator.torrents(host=h).all_datasets
        with pytest.raises(AssertionError):
            assert Dataset('pkg-a:2', '1.0') in coordinator.torrents(host=h).all_datasets


def test_remove_all_tags(coordinator):
    pkg_a1_hash = coordinator.upload(
        'pkg-a:1', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkg_a2_hash = coordinator.upload(
        'pkg-a:2', '1.0', 'gen-ab1', branch='stable', tvm_id=1)

    coordinator.announce(pkg_a1_hash, resolve_hosts(['rtc:maps_a']))
    coordinator.postdl('pkg-a:1', '1.0', resolve_hosts(['rtc:maps_a']))
    coordinator.announce(pkg_a2_hash, resolve_hosts(['rtc:maps_a']))
    coordinator.postdl('pkg-a:2', '1.0', resolve_hosts(['rtc:maps_a']))

    for h in resolve_hosts(['rtc:maps_a']):
        assert Dataset('pkg-a:1', '1.0') in coordinator.torrents(host=h).all_datasets
        assert Dataset('pkg-a:2', '1.0') in coordinator.torrents(host=h).all_datasets

    assert coordinator.remove('pkg-a', '1.0', 'gen-ab1', all_tags=True, tvm_id=1) == {Dataset('pkg-a:1', '1.0'),
                                                                                      Dataset('pkg-a:2', '1.0')}
    for h in resolve_hosts(['rtc:maps_a']):
        for tag in ['1', '2']:
            assert Dataset(f'pkg-a:{tag}', '1.0') not in coordinator.torrents(host=h).all_datasets

    assert coordinator.remove('pkg-a:1', '1.0', 'gen-ab1', tvm_id=1) == set()
    assert coordinator.remove('pkg-a:', '1.0', 'gen-ab1', all_tags=True, tvm_id=1) == set()
