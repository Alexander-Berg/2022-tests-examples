from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts
from maps.infra.ecstatic.tool.ecstatic_api.coordinator import Dataset


def test_complex_quorums(coordinator):
    hashes = {}
    for pkg in ['c', 'd', 'e']:
        for ver in ['1', '2']:
            hashes[pkg + ver] = coordinator.upload(
                'pkg-' + pkg, ver + '.0', 'gen-cd1', branch='stable', tvm_id=2)

    for h in resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']):
        for pkg in ['c', 'd']:
            assert Dataset('pkg-' + pkg, '1.0') in coordinator.torrents(host=h).all_datasets
            assert Dataset('pkg-' + pkg, '2.0') in coordinator.torrents(host=h).all_datasets
            coordinator.announce(hashes[pkg + '1'], h)
            coordinator.postdl('pkg-' + pkg,  '1.0', h)
            assert 'pkg-e' not in coordinator.torrents(host=h).all_dataset_names

    assert Dataset('pkg-c', '1.0') in coordinator.torrents(host='c1').all_datasets
    assert Dataset('pkg-c', '2.0') in coordinator.torrents(host='c1').all_datasets

    coordinator.announce(hashes['c1'], 'c1')
    coordinator.postdl('pkg-c', '1.0', 'c1')

    for h in resolve_hosts(['rtc:maps_ce']):
        for pkg in ['c', 'e']:
            for ver in ['1.0', '2.0']:
                assert Dataset('pkg-' + pkg, ver) in coordinator.torrents(host=h).all_datasets
            coordinator.announce(hashes[pkg + '1'], h)
            coordinator.postdl('pkg-' + pkg, '1.0', h)
        assert 'pkg-d' not in coordinator.torrents(host=h).all_dataset_names

    # Separate deploy group for %c
    coordinator.require_version('pkg-c', '1.0', ['c1'])
    coordinator.announce(hashes['c2'], 'c1')
    coordinator.postdl('pkg-c', '2.0', 'c1')
    coordinator.require_version('pkg-c', '2.0', ['c1'])

    coordinator.require_version('pkg-c', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.require_version('pkg-d', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.require_version('pkg-e', '1.0', resolve_hosts(['rtc:maps_ce']))

    coordinator.announce(hashes['c2'], ['cd11', 'cd12', 'cd21', 'cd22', 'ce1', 'ce2'])
    coordinator.announce(hashes['d2'], ['cd12', 'cd13', 'cd21', 'cd22'])
    coordinator.announce(hashes['e2'], ['ce1', 'ce2'])
    coordinator.postdl('pkg-c', '2.0', ['cd11', 'cd12', 'cd21', 'cd22', 'ce1', 'ce2'])
    coordinator.postdl('pkg-d', '2.0', ['cd12', 'cd13', 'cd21', 'cd22'])
    coordinator.postdl('pkg-e', '2.0', ['ce1', 'ce2'])

    # Quorum not satisfied on %cd1
    coordinator.require_version('pkg-c', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.require_version('pkg-d', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.require_version('pkg-e', '1.0', resolve_hosts(['rtc:maps_ce']))

    coordinator.announce(hashes['c2'], ['cd13'])
    coordinator.postdl('pkg-c', '2.0', ['cd13'])
    coordinator.require_version('pkg-c', '2.0', ['cd12', 'cd13', 'cd21', 'cd22', 'ce1', 'ce2'])
    coordinator.require_version('pkg-d', '2.0', ['cd12', 'cd13', 'cd21', 'cd22'])
    coordinator.require_version('pkg-e', '2.0', ['ce1', 'ce2'])

    # Hosts not having all they need to operate properly should go offline
    coordinator.require_version('pkg-c', '', ['cd11', 'cd23', 'ce3'])
    coordinator.require_version('pkg-d', '', ['cd11', 'cd23'])
    coordinator.require_version('pkg-e', '', ['ce3'])

    # All this stuff should not spoil our separate deploy group for %c
    coordinator.require_version('pkg-c', '2.0', ['c1'])

    # Rollback
    coordinator.switch_failed('pkg-e', '2.0', 'ce2')
    coordinator.require_version('pkg-c', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.require_version('pkg-d', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.require_version('pkg-e', '1.0', resolve_hosts(['rtc:maps_ce']))

    # Retry step-in
    coordinator.step_in('pkg-e', '2.0', 'gen-cd1', tvm_id=2)
    coordinator.announce(hashes['c2'], resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.announce(hashes['d2'], resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.announce(hashes['e2'], resolve_hosts(['rtc:maps_ce']))
    coordinator.postdl('pkg-c', '2.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.postdl('pkg-d', '2.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.postdl('pkg-e', '2.0', resolve_hosts(['rtc:maps_ce']))
    coordinator.require_version('pkg-c', '2.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.require_version('pkg-d', '2.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.require_version('pkg-e', '2.0', resolve_hosts(['rtc:maps_ce']))

    # Retire
    coordinator.retire('pkg-e', '2.0', 'gen-cd1', tvm_id=2)
    coordinator.require_version('pkg-c', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.require_version('pkg-d', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.require_version('pkg-e', '1.0', resolve_hosts(['rtc:maps_ce']))
