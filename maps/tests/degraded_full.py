def test_degraded_full(mongo, coordinator):
    def require_torrent_time(hash, now, hosts):
        for host in hosts:
            assert hash in coordinator.torrents(host=host, now=now, tvm_id=12345).all_torrents

    def require_torrent_time_absent(hash, now, hosts):
        for host in hosts:
            assert hash not in coordinator.torrents(host=host, now=now, tvm_id=12345).all_torrents

    shard_1 = ['storage11', 'storage12', 'storage13']
    shard_2 = ['storage21', 'storage22', 'storage23']

    hash_1 = coordinator.upload('pkg-a', '1.0', 'gen-ab1', now=0, disk_usage=10, tvm_id=1)

    mongo.db['storages'].insert({'hosts': ['storage21', 'storage22', 'storage23']})

    require_torrent_time_absent('JUNK', 0, shard_2)

    require_torrent_time(hash_1, 5, shard_1)
    require_torrent_time_absent(hash_1, 5, shard_2)

    hash_2 = coordinator.upload(
        'pkg-a', '2.0', 'gen-ab1', now=10, disk_usage=20, tvm_id=1)
    require_torrent_time_absent(hash_2, 15, shard_1)
    require_torrent_time(hash_2, 15, shard_2)

    coordinator.http_post('/debug/PurgeUnobservedHosts', now=360)

    require_torrent_time(hash_1, 3595, ['storage11'])
    require_torrent_time(hash_2, 3595, ['storage21'])
    hash_3 = coordinator.upload(
        'pkg-a', '3.0', 'gen-ab1', now=3600, disk_usage=20, tvm_id=1)
    require_torrent_time(hash_3, 3605, shard_1)
    require_torrent_time_absent(hash_3, 3605, shard_2)

    coordinator.http_post('/debug/PurgeUnobservedHosts', now=3960)

    require_torrent_time(hash_3, 7195, ['storage11'])
    hash_4 = coordinator.upload(
        'pkg-a', '4.0', 'gen-ab1', now=7200, disk_usage=20, tvm_id=1)
    require_torrent_time(hash_4, 7205, shard_1)
    require_torrent_time_absent(hash_4, 7205, shard_2)
