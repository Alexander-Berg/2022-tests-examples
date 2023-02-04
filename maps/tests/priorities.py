from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_priorities(coordinator):
    hashes = {}
    for pkg in ['c', 'd', 'e']:
        hashes[pkg] = coordinator.upload('pkg-' + pkg, '1.0', 'gen-cd1', branch='stable', tvm_id=2)

    priorities = {hashes['c']: 2, hashes['d']: 100, hashes['e']: 1}

    for h in resolve_hosts(['rtc:maps_gen-cd1', 'rtc:maps_gen-cd2']):
        proto = coordinator.torrents(host=h)
        assert priorities == proto.priorities

    assert coordinator.torrents(host='storage11', tvm_id=12345).priorities == priorities
