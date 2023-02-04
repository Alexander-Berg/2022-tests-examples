from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts

import time


def test_group_change_resets_postdl(mongo, coordinator):
    torrent_hash = \
        coordinator.upload('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.announce(torrent_hash, 'gen-ab1')

    coordinator.announce(torrent_hash, resolve_hosts(['rtc:maps_a']))
    for h in resolve_hosts(['rtc:maps_a']):
        assert 'pkg-a\t1.0\n' in coordinator.get_postdl(h).text

    coordinator.postdl('pkg-a', '1.0', resolve_hosts(['rtc:maps_a']))
    coordinator.require_version('pkg-a', '1.0', resolve_hosts(['rtc:maps_a']))
    coordinator.report_versions(resolve_hosts(['rtc:maps_a']))

    # pull a2 from deploy group
    mongo.reconfigure(hosts_config='data/host-groups-a2-pulled.conf')

    coordinator.http_post('/debug/PurgeObsoleteStatuses', now=int(time.time() + 100500))

    assert 'pkg-a' not in coordinator.versions('a2').text
    assert 'pkg-a' not in coordinator.get_postdl('a2').text
    coordinator.announce(torrent_hash, 'a2', event='stopped')

    coordinator.require_version('pkg-a', '1.0', ['a1', 'a3'])

    # restore original config
    mongo.reconfigure()

    coordinator.announce(torrent_hash, 'a2')

    assert 'pkg-a\t1.0\n' not in coordinator.versions('a2')

    assert 'pkg-a\t1.0\n' in coordinator.get_postdl('a2').text
    coordinator.postdl('pkg-a', '1.0', 'a2')
    coordinator.require_version('pkg-a', '1.0', resolve_hosts(['rtc:maps_a']))
