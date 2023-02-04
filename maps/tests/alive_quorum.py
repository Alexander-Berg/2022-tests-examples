from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_alive_quorum(coordinator):
    def heartbeat(now, hosts):
        for h in hosts:
            coordinator.torrents(host=h, now=now, format='proto')

    def purge(now):
        coordinator.http_post('/debug/PurgeUnobservedHosts', now=now)

    dataset = 'pkg-b'
    test_hosts = resolve_hosts(['rtc:maps_b'])
    heartbeat(0, test_hosts)

    pkgb1_hash = coordinator.upload(dataset, '1.0', 'gen-ab1', branch='stable', tvm_id=1)
    pkgb2_hash = coordinator.upload(dataset, '2.0', 'gen-ab1', branch='stable', tvm_id=1)

    coordinator.announce(pkgb1_hash, test_hosts)
    coordinator.postdl(dataset, '1.0', test_hosts)
    coordinator.require_version(dataset, '1.0', test_hosts)

    coordinator.step_in(dataset, '2.0', 'gen-ab1', branch='stable/hold', tvm_id=1)

    coordinator.announce(pkgb2_hash, test_hosts)
    coordinator.postdl(dataset, '2.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version(dataset, '1.0', test_hosts)

    heartbeat(360, ['b2', 'b3', 'b4', 'b5'])
    purge(360)

    coordinator.step_in(dataset, '2.0', 'gen-ab1', tvm_id=1)

    coordinator.require_version(dataset, '1.0', test_hosts)

    heartbeat(450, ['b1'])

    coordinator.require_version(dataset, '2.0', ['b1', 'b2', 'b3', 'b4'])
    coordinator.require_version(dataset, '__NONE__', ['b5'])

    coordinator.switch_failed(dataset, '2.0', 'b1')
    purge(1000)
    coordinator.require_version(dataset, '__CURRENT__', ['b1', 'b2', 'b3', 'b4'])
