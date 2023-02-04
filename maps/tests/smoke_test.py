from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts
import pytest
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


def test_smoke(coordinator):
    # Upload torrent
    torrent_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
    coordinator.announce(torrent_hash, 'gen-ab1')
    assert 'no' in coordinator.is_adopted(torrent_hash).text

    # Check if storages are instructed to download it
    for h in ['storage11', 'storage12', 'storage13']:
        assert torrent_hash in coordinator.torrents(host=h, tvm_id=12345).all_torrents

    # Check upload termination
    coordinator.announce(torrent_hash, 'storage11')
    assert 'no' in coordinator.is_adopted(torrent_hash).text

    coordinator.announce(torrent_hash, 'storage12')
    assert 'yes' in coordinator.is_adopted(torrent_hash).text

    coordinator.announce(torrent_hash, 'gen-ab1', event='stopped')

    # Move torrent to stable
    assert torrent_hash not in coordinator.torrents(host='a1').all_torrents

    coordinator.step_in('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
    assert torrent_hash in coordinator.torrents(host='a1').all_torrents

    assert Dataset('pkg-a', '1.0') in coordinator.torrents(host='a1').all_datasets

    for h in resolve_hosts(['rtc:maps_a']):
        # No switch yet
        assert 'pkg-a\t__CURRENT__' in coordinator.versions(h).text

        # Postdl support
        with pytest.raises(AssertionError):
            assert '' == coordinator.get_postdl(h)
        coordinator.announce(torrent_hash, h)
        assert 'pkg-a\t1.0' in coordinator.get_postdl(h).text

        coordinator.postdl('pkg-a', '1.0', h)
        with pytest.raises(AssertionError):
            assert '.' in coordinator.get_postdl(h).text

    for h in resolve_hosts(['rtc:maps_a']):
        assert 'pkg-a\t1.0' in coordinator.versions(h).text

    # Web interface at least should not fail
    coordinator.http_get('/pkg/pkg-a/1.0/status')

    # Just in case
    for h in ['storage11', 'storage12', 'storage13']:
        assert torrent_hash in coordinator.torrents(host=h, tvm_id=12345).all_torrents

    # Move torrent out of stable and ensure
    # it disappears on clients but stayes on storages
    coordinator.retire('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    for h in resolve_hosts(['rtc:maps_a']):
        assert torrent_hash not in coordinator.torrents(host=h).all_torrents

    for h in ['storage11', 'storage12', 'storage13']:
        assert torrent_hash in coordinator.torrents(host=h, tvm_id=12345).all_torrents

    # Remove torrent and ensure it disappears on storages
    coordinator.remove('pkg-a', '1.0', 'gen-ab1', tvm_id=1)
    for h in ['storage11', 'storage12', 'storage13']:
        assert torrent_hash not in coordinator.torrents(host=h, tvm_id=12345).all_torrents
