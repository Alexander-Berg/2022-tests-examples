from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_order_datasets(coordinator):
    hashes = {}
    for pkg in ['c', 'd', 'e']:
        for ver in ['1', '2']:
            hashes[pkg + ver] = coordinator.upload('pkg-' + pkg, ver + '.0', 'gen-cd1', branch='stable', tvm_id=2)

    coordinator.announce(hashes['c2'], resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.postdl('pkg-c', '2.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.announce(hashes['d2'], resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.postdl('pkg-d', '2.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.announce(hashes['e2'], resolve_hosts(['rtc:maps_ce']))
    coordinator.postdl('pkg-e', '2.0', resolve_hosts(['rtc:maps_ce']))

    expected = "pkg-d\t2.0\npkg-c\t2.0\n"
    for h in resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']):
        assert expected == coordinator.versions(h).text

    expected = "pkg-e\t2.0\npkg-c\t2.0\n"
    for h in resolve_hosts(['rtc:maps_ce']):
        assert expected == coordinator.versions(h).text
