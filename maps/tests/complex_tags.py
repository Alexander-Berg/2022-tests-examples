from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_complex_tags(coordinator):
    hashes = {}
    for pkgtag in ['pkg-c:1', 'pkg-d:4', 'pkg-e:8']:
        for ver in ['1', '2']:
            hashes[pkgtag + ver] = coordinator.upload(pkgtag, ver + '.0', 'gen-cd1', branch='stable', tvm_id=2)

    hashes['pkg-c:22'] = coordinator.upload('pkg-c:2', '2.0', 'gen-cd1', branch='stable', tvm_id=2)
    hashes['pkg-e:92'] = coordinator.upload('pkg-e:9', '2.0', 'gen-cd1', branch='stable', tvm_id=2)

    # c:1, d and e:8 satisfy quorum for version 1.0 despite existense of c:2 and e:9
    coordinator.announce(hashes['pkg-c:11'], ['cd11', 'cd12', 'cd22', 'cd23', 'ce1', 'ce3'])
    coordinator.announce(hashes['pkg-d:41'], ['cd11', 'cd12', 'cd22', 'cd23'])
    coordinator.announce(hashes['pkg-e:81'], ['ce1', 'ce3'])
    coordinator.postdl('pkg-c:1', '1.0', ['cd11', 'cd12', 'cd22', 'cd23', 'ce1', 'ce3'])
    coordinator.postdl('pkg-d:4', '1.0', ['cd11', 'cd12', 'cd22', 'cd23'])
    coordinator.postdl('pkg-e:8', '1.0', ['ce1', 'ce3'])

    coordinator.require_version('pkg-c:1', '1.0', ['cd11', 'cd12', 'cd22', 'cd23', 'ce1', 'ce3'])
    coordinator.require_version('pkg-c:1', None, ['cd13', 'cd21', 'ce2'])
    coordinator.require_version('pkg-d:4', '1.0', ['cd11', 'cd12', 'cd22', 'cd23'])
    coordinator.require_version('pkg-d:4', None, ['cd13', 'cd21'])
    coordinator.require_version('pkg-e:8', '1.0', ['ce1', 'ce3'])
    coordinator.require_version('pkg-e:8', None, ['ce2'])

    coordinator.report_versions(resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))

    coordinator.announce(hashes['pkg-c:12'], resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.announce(hashes['pkg-c:22'], resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.announce(hashes['pkg-d:42'], resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.announce(hashes['pkg-e:82'], resolve_hosts(['rtc:maps_ce']))
    coordinator.postdl('pkg-c:1', '2.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.postdl('pkg-c:2', '2.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
    coordinator.postdl('pkg-d:4', '2.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.postdl('pkg-e:8', '2.0', resolve_hosts(['rtc:maps_ce']))

    # Despite c:1=2.0, c:2=2.0, d=2.0 and e:8=2.0 are downloaded wherever possible,
    # quorum still not satifsied, since version 2.0 covers c:2 and e:9
    coordinator.require_version('pkg-c:1', '1.0', ['cd11', 'cd12', 'cd22', 'cd23', 'ce1', 'ce3'])
    coordinator.require_version('pkg-c:1', None, ['cd13', 'cd21', 'ce2'])
    coordinator.require_version('pkg-d:4', '1.0', ['cd11', 'cd12', 'cd22', 'cd23'])
    coordinator.require_version('pkg-d:4', None, ['cd13', 'cd21'])
    coordinator.require_version('pkg-e:8', '1.0', ['ce1', 'ce3'])
    coordinator.require_version('pkg-e:8', None, ['ce2'])

    coordinator.report_versions(resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))

    coordinator.announce(hashes['pkg-e:92'], ['ce1', 'ce2'])
    coordinator.postdl('pkg-e:9', '2.0', ['ce1', 'ce2'])

    coordinator.require_version('pkg-c:1', '2.0', ['cd11', 'cd12', 'cd13',  'cd21', 'cd22', 'cd23',  'ce1', 'ce2'])
    coordinator.require_version('pkg-c:2', '2.0', ['cd11', 'cd12', 'cd13',  'cd21', 'cd22', 'cd23',  'ce1', 'ce2'])
    coordinator.require_version('pkg-d:4', '2.0', ['cd11', 'cd12', 'cd13',  'cd21', 'cd22', 'cd23'])
    coordinator.require_version('pkg-e:8', '2.0', ['ce1', 'ce2'])
    coordinator.require_version('pkg-e:9', '2.0', ['ce1', 'ce2'])

    # Since ce3 does not contain everything it needs, it should go offline completely
    coordinator.require_version('pkg-c:1', None, ['ce3'])
    coordinator.require_version('pkg-c:2', None, ['ce3'])
    coordinator.require_version('pkg-e:8', None, ['ce3'])
    coordinator.require_version('pkg-e:9', None, ['ce3'])

    coordinator.report_versions(resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))

    hashes['pkg-c:23'] = coordinator.upload('pkg-c:2', '3.0', 'gen-cd1', branch='stable', tvm_id=2)
    hashes['pkg-d:43'] = coordinator.upload('pkg-d:4', '3.0', 'gen-cd1', branch='stable', tvm_id=2)
    hashes['pkg-e:93'] = coordinator.upload('pkg-e:9', '3.0', 'gen-cd1', branch='stable', tvm_id=2)

    coordinator.announce(hashes['pkg-c:23'], ['cd11', 'cd12', 'cd22', 'cd23', 'ce1', 'ce3'])
    coordinator.announce(hashes['pkg-d:43'], ['cd11', 'cd12', 'cd22', 'cd23'])
    coordinator.announce(hashes['pkg-e:93'], ['ce1', 'ce3'])
    coordinator.postdl('pkg-c:2', '3.0', ['cd11', 'cd12', 'cd22', 'cd23', 'ce1', 'ce3'])
    coordinator.postdl('pkg-d:4', '3.0', ['cd11', 'cd12', 'cd22', 'cd23'])
    coordinator.postdl('pkg-e:9', '3.0', ['ce1', 'ce3'])

    # One again, for version 3.0 downloaded c:2, d:4 and e:9 satisfy quorum
    coordinator.require_version('pkg-c:2', '3.0', ['cd11', 'cd12', 'cd22', 'cd23', 'ce1', 'ce3'])
    coordinator.require_version('pkg-c:2', None, ['cd13', 'cd21', 'ce2'])
    coordinator.require_version('pkg-d:4', '3.0', ['cd11', 'cd12', 'cd22', 'cd23'])
    coordinator.require_version('pkg-d:4', None, ['cd13', 'cd21'])
    coordinator.require_version('pkg-e:9', '3.0', ['ce1', 'ce3'])
    coordinator.require_version('pkg-e:9', None, ['ce2'])

    # However, c:1 and e:8 must still exist and be taken from previons version
    coordinator.require_version('pkg-c:1', '2.0', ['cd11', 'cd12', 'cd22', 'cd23', 'ce1', 'ce3'])
    coordinator.require_version('pkg-e:8', '2.0', ['ce1', 'ce3'])

    coordinator.report_versions(resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2', 'rtc:maps_ce']))
