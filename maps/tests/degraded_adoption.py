def test_degraded_adoption(coordinator):
    def heartbeat(time, hosts):
        for host in hosts:
            coordinator.torrents(host=host, now=time, tvm_id=12345)

    heartbeat(0, ['storage11', 'storage12', 'storage13'])

    torrent_hash = coordinator.upload('pkg-a', '1.0', 'gen-ab1', now=0, tvm_id=1)

    coordinator.check_adopted(torrent_hash, False)

    coordinator.announce(torrent_hash, 'storage11')
    coordinator.check_adopted(torrent_hash, False)

    coordinator.announce(torrent_hash, 'storage12')
    coordinator.check_adopted(torrent_hash)

    heartbeat(300, ['storage11', 'storage13'])
    coordinator.http_post('/debug/PurgeUnobservedHosts', now=300)

    coordinator.check_adopted(torrent_hash, False)

    coordinator.announce(torrent_hash, 'storage13')
    coordinator.check_adopted(torrent_hash)

    heartbeat(600, ['storage13'])
    coordinator.http_post('/debug/PurgeUnobservedHosts', now=600)

    coordinator.check_adopted(torrent_hash)

    coordinator.http_post('/debug/PurgeUnobservedHosts', now=900)

    coordinator.check_adopted(torrent_hash, False)

    heartbeat(1200, ['storage12'])
    coordinator.http_post('/debug/PurgeUnobservedHosts', now=1200)
    coordinator.check_adopted(torrent_hash)

    heartbeat(1500, ['storage11', 'storage12'])
    coordinator.http_post('/debug/PurgeUnobservedHosts', now=1500)
    coordinator.check_adopted(torrent_hash)

    coordinator.announce(torrent_hash, 'storage13')
    heartbeat(1800, ['storage11', 'storage12', 'storage13'])

    coordinator.http_post('/debug/PurgeUnobservedHosts', now=1800)
    coordinator.check_adopted(torrent_hash)
