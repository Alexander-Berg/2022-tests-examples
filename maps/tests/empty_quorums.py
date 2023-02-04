from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_empty_quorums(mongo, coordinator):
    mongo.reconfigure(config='data/100prct-tolerance.conf')

    def heartbeat(now, hosts):
        for h in hosts:
            coordinator.torrents(host=h, now=now)

    def purge(now):
        coordinator.http_post('/debug/PurgeUnobservedHosts', now=now)

    heartbeat(0, resolve_hosts(['rtc:maps_a', 'rtc:maps_b', 'rtc:maps_c']))

    pkg_a_hash = \
        coordinator.upload('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    coordinator.announce(pkg_a_hash, resolve_hosts(['rtc:maps_b']))
    coordinator.postdl('pkg-a', '1.0', resolve_hosts(['rtc:maps_b']))

    heartbeat(360, resolve_hosts(['rtc:maps_a', 'rtc:maps_b']))
    purge(360)

    for host in resolve_hosts(['rtc:maps_b']):
        coordinator.current_versions(data='pkg-a\t1.0', host=host)

    coordinator.announce(pkg_a_hash, resolve_hosts(['rtc:maps_a']))
    coordinator.postdl_failed('pkg-a', '1.0', resolve_hosts(['rtc:maps_a'])[0])

    coordinator.require_version('pkg-a', '1.0', resolve_hosts(['rtc:maps_b']))
    coordinator.require_version('pkg-a', '__NONE__', resolve_hosts(['rtc:maps_a']))

    status = coordinator.list_status('pkg-a', 'stable').text.split('\n')[1:-1]
    assert len(status) == 1
    assert status[0] == 'pkg-a=1.0\tstable\t9\t8\t5\t1\t[A]'


def test_no_rollback_on_empty_quorum_group_error(mongo, coordinator):
    mongo.reconfigure(config='data/100prct-tolerance.conf')

    pkg_a_hash = \
        coordinator.upload('pkg-a', '1.0', 'gen-ab1', branch='stable', tvm_id=1)

    groups = ['rtc:maps_a', 'rtc:maps_b']
    coordinator.announce(pkg_a_hash, resolve_hosts(groups))
    coordinator.postdl('pkg-a', '1.0', resolve_hosts(groups))

    coordinator.require_version('pkg-a', '1.0', resolve_hosts(groups))
    coordinator.switch_failed('pkg-a', '1.0', resolve_hosts(['rtc:maps_a'])[0])

    coordinator.announce(pkg_a_hash, resolve_hosts(['rtc:maps_b'])[0], left=1)
    coordinator.require_version('pkg-a', '1.0', resolve_hosts(['rtc:maps_b'])[1:])
