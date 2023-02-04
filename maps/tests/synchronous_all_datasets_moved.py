from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def short(coordinator):
    hash = coordinator.upload('pkg-f', '1.0', 'gen-h1', data='sync_test', tvm_id=4)
    coordinator.announce(hash, 'h1')
    assert coordinator.get_postdl('h1', now=0).text


def test_synchronous_all_datasets_moved(coordinator, mongo):
    def check_postdl(expected, hosts):
        for host in hosts:
            response = coordinator.get_postdl(host, now=0)
            assert response.text == expected

    def check_versions(expected, hosts):
        for host in hosts:
            assert coordinator.versions(host, now=0).text == expected

    hosts = resolve_hosts(['rtc:maps_h'])
    for v in ["1.0", "2.0", "3.0"]:
        for d in ["pkg-f", "pkg-g", "pkg-h"]:
            hash = coordinator.upload(d, v, 'gen-h1', branch='stable', data='sync_test', tvm_id=4)
            coordinator.announce(hash, hosts)

    expected_postdl = '''\
pkg-h\t3.0
pkg-g\t3.0
pkg-f\t3.0
pkg-h\t2.0
pkg-g\t2.0
pkg-f\t2.0
pkg-h\t1.0
pkg-g\t1.0
pkg-f\t1.0
'''

    expected_versions = '''pkg-f\t__CURRENT__
pkg-g\t__CURRENT__
pkg-h\t__CURRENT__
'''
    check_postdl(expected_postdl, hosts)
    check_versions(expected_versions, hosts)

    coordinator.postdl('pkg-f', '1.0', hosts)
    expected_postdl = '''\
pkg-h\t3.0
pkg-g\t3.0
pkg-f\t3.0
pkg-h\t2.0
pkg-g\t2.0
pkg-f\t2.0
pkg-h\t1.0
pkg-g\t1.0
'''
    check_postdl(expected_postdl, hosts)
    check_versions(expected_versions, hosts)

    coordinator.postdl('pkg-g', '1.0', hosts)
    expected_postdl = '''\
pkg-h\t3.0
pkg-g\t3.0
pkg-f\t3.0
pkg-h\t2.0
pkg-g\t2.0
pkg-f\t2.0
pkg-h\t1.0
'''
    check_postdl(expected_postdl, hosts)
    check_versions(expected_versions, hosts)

    coordinator.postdl('pkg-h', '1.0', hosts)
    expected_postdl = '''\
pkg-h\t3.0
pkg-g\t3.0
pkg-f\t3.0
pkg-h\t2.0
pkg-g\t2.0
pkg-f\t2.0
'''
    check_postdl(expected_postdl, hosts)
    expected_versions = '''pkg-f\t1.0
pkg-g\t1.0
pkg-h\t1.0
'''
    check_versions(expected_versions, hosts)
