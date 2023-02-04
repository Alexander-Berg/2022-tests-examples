import pytest
from maps.infra.ecstatic.coordinator.bin.tests.fixtures.status_exceptions import ServerError


def test_degraded_full_revive(coordinator):
    def require_torrent_time(hash, now, hosts):
        for host in hosts:
            assert hash in coordinator.torrents(host=host, now=now, tvm_id=12345).all_torrents

    shard = ['storage11', 'storage12', 'storage13']

    hash_1 = coordinator.upload('pkg-a', '1.0', 'gen-ab1', now=0, tvm_id=1)
    require_torrent_time(hash_1, 5, shard)

    coordinator.http_post('/debug/PurgeUnobservedHosts', now=360)

    with pytest.raises(ServerError):
        coordinator.upload('pkg-a', '2.0', 'gen-ab1', now=3600, tvm_id=1)
    require_torrent_time(hash_1, 3605, shard)

    hash_3 = coordinator.upload('pkg-a', '3.0', 'gen-ab1', now=3610, tvm_id=1)
    require_torrent_time(hash_3, 3615, shard)
