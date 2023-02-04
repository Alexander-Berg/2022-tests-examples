from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


def test_dangling_symlinks(mongo, coordinator):
    torrent_hash = coordinator.upload(
        'pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)

    assert torrent_hash in coordinator.torrents(host='a1').all_torrents
    assert Dataset('pkg-a', '1.0') in coordinator.torrents(host='a1').all_datasets

    # Simulate torrent deletion during request
    mongo.db['storage_assignments'].remove({"torrent_hash": torrent_hash})

    coordinator.http_post('/debug/ClearTorrentBodyCache')
    assert torrent_hash not in coordinator.torrents(host='a1').all_torrents
    assert Dataset('pkg-a', '1.0') not in coordinator.torrents(host='a1').all_datasets
