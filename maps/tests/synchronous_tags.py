from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_synchronous_tags(coordinator):
    def check_postdl(expected, hosts, now):
        for host in hosts:
            assert coordinator.get_postdl(host, now=now).text == expected

    def check_versions(expected, hosts, now):
        for host in hosts:
            assert coordinator.versions(host, now=now).text == expected

    for t in [":tag1", ":tag2"]:
        for d in ["pkg-f", "pkg-g", "pkg-h"]:
            hash = coordinator.upload(d, '1.0', 'gen-h1', tag=t, data='sync_test', tvm_id=4)
            coordinator.announce(hash, resolve_hosts(['rtc:maps_h']))

    time = 0

    expected_postdl = ''
    expected_versions = '''pkg-f:tag1\t__CURRENT__
pkg-f:tag2\t__CURRENT__
pkg-g:tag1\t__CURRENT__
pkg-g:tag2\t__CURRENT__
pkg-h:tag1\t__CURRENT__
pkg-h:tag2\t__CURRENT__
'''
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    coordinator.step_in('pkg-f:tag1', '1.0', 'gen-h1', tvm_id=4)

    expected_postdl = 'pkg-f:tag2\t1.0\npkg-f:tag1\t1.0\n'
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    coordinator.postdl('pkg-f:tag1', '1.0', resolve_hosts(['rtc:maps_h']))
    expected_postdl = 'pkg-f:tag2\t1.0\n'
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    coordinator.step_in('pkg-g:tag1', '1.0', 'gen-h1', tvm_id=4)
    expected_postdl = '''\
pkg-g:tag2\t1.0
pkg-g:tag1\t1.0
pkg-f:tag2\t1.0
'''
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    coordinator.postdl('pkg-g:tag1', '1.0', resolve_hosts(['rtc:maps_h']))
    expected_postdl = 'pkg-g:tag2\t1.0\npkg-f:tag2\t1.0\n'
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    # pkg-f:tag1, pkg-g:tag1, pkg-h:tag1 - moved, postdl - no activation
    coordinator.step_in('pkg-h:tag1', '1.0', 'gen-h1', tvm_id=4)
    expected_postdl = '''\
pkg-h:tag2\t1.0
pkg-h:tag1\t1.0
pkg-g:tag2\t1.0
pkg-f:tag2\t1.0
'''

    coordinator.postdl('pkg-h:tag1', '1.0', resolve_hosts(['rtc:maps_h']))
    expected_postdl = '''\
pkg-h:tag2\t1.0
pkg-g:tag2\t1.0
pkg-f:tag2\t1.0
'''
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    coordinator.step_in('pkg-f:tag2', '1.0', 'gen-h1', tvm_id=4)
    expected_postdl = '''\
pkg-h:tag2\t1.0
pkg-g:tag2\t1.0
pkg-f:tag2\t1.0
'''
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    coordinator.postdl('pkg-f:tag2', '1.0', resolve_hosts(['rtc:maps_h']))
    expected_postdl = 'pkg-h:tag2\t1.0\npkg-g:tag2\t1.0\n'
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    coordinator.step_in('pkg-g:tag2', '1.0', 'gen-h1', tvm_id=4)
    expected_postdl = 'pkg-h:tag2\t1.0\npkg-g:tag2\t1.0\n'
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    coordinator.postdl('pkg-g:tag2', '1.0', resolve_hosts(['rtc:maps_h']))
    expected_postdl = 'pkg-h:tag2\t1.0\n'
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    coordinator.step_in('pkg-h:tag2', '1.0', 'gen-h1', tvm_id=4)
    expected_postdl = 'pkg-h:tag2\t1.0\n'
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)

    # all dataset, tags moved and postdl - version 1.0 activated
    coordinator.postdl('pkg-h:tag2', '1.0', resolve_hosts(['rtc:maps_h']))
    expected_postdl = ''
    expected_versions = '''pkg-f:tag1\t1.0
pkg-f:tag2\t1.0
pkg-g:tag1\t1.0
pkg-g:tag2\t1.0
pkg-h:tag1\t1.0
pkg-h:tag2\t1.0
'''
    check_postdl(expected_postdl, resolve_hosts(['rtc:maps_h']), time)
    check_versions(expected_versions, resolve_hosts(['rtc:maps_h']), time)
