def test_expiration(mongo, coordinator):
    def require_torrent_time(hash, hosts):
        for h in hosts:
            if hash in coordinator.torrents(host=h, tvm_id=12345).all_torrents:
                return True
        return False

    shard_1 = ['storage11', 'storage12', 'storage13']
    shard_2 = ['storage21', 'storage22', 'storage23']

    mongo.reconfigure(config='data/shortlive-dataset.conf')
    mongo.db['storages'].insert({'hosts': ['storage21', 'storage22', 'storage23']})

    # mark hosts alive
    require_torrent_time("JUNK", shard_2)

    hashes = []
    for i in range(20):
        hashes.append(coordinator.upload(
            'pkg-a', '1.' + str(i), 'gen-ab1', data=str(i), disk_usage=20, tvm_id=1))

    shard_1_count = len([h for h in hashes if require_torrent_time(h, shard_1)])
    shard_2_count = len([h for h in hashes if require_torrent_time(h, shard_2)])
    total = len(hashes)

    # approximate random 50/50 distribution
    assert shard_1_count + shard_2_count == total
    assert shard_1_count > total / 3
    assert shard_2_count > total / 3
