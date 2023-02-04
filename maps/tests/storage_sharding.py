def test_storage_sharding(coordinator, mongo):

    def require_torrent(hash, hosts):
        for host in hosts:
            assert hash in coordinator.torrents(host=host, tvm_id=12345).all_torrents

    def require_torrent_absent(hash, hosts):
        for host in hosts:
            assert hash not in coordinator.torrents(host=host, tvm_id=12345).all_torrents

    shard_1 = ["storage11", "storage12", "storage13"]
    shard_2 = ["storage21", "storage22", "storage23"]

    pkg_a13_hash = coordinator.upload(
        'pkg-a', '1.0', 'gen-ab1', data='collision', disk_usage=10, tvm_id=1)
    require_torrent(pkg_a13_hash, shard_1)

    mongo.db['storages'].insert({'hosts': ['storage21', 'storage22', 'storage23']})

    require_torrent(pkg_a13_hash, shard_1)
    require_torrent_absent(pkg_a13_hash, shard_2)

    pkg_a2_hash = coordinator.upload(
        'pkg-a', '2.0', 'gen-ab1', disk_usage=10, tvm_id=1)
    require_torrent(pkg_a13_hash, shard_1)
    require_torrent(pkg_a2_hash, shard_2)
    require_torrent_absent(pkg_a13_hash, shard_2)
    require_torrent_absent(pkg_a2_hash, shard_1)

    # Check for torrent collisions
    assert pkg_a13_hash == coordinator.upload(
        'pkg-a', '3.0', 'gen-ab1', data='collision', disk_usage=10, tvm_id=1)
    require_torrent(pkg_a13_hash, shard_1)
    require_torrent(pkg_a2_hash, shard_2)
    require_torrent_absent(pkg_a13_hash, shard_2)
    require_torrent_absent(pkg_a2_hash, shard_1)

    assert 0 == mongo.db['storages'].count({'_id': {'$ne': '__SENTINEL__'}, 'disk_usage': {'$ne': 10}})
