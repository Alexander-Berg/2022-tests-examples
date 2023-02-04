from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_retry_synchronous(coordinator):
    def check_postdl(expected, now, hosts):
        for host in hosts:
            assert coordinator.get_postdl(host, now=now).text == expected

    def check_versions(expected, now, hosts):
        for host in hosts:
            assert coordinator.versions(host, now=now).text == expected

    def post_switch_failed(dataset, version, now, hosts):
        for host in hosts:
            coordinator.switch_failed(dataset, version, host, now=now)

    def post_postdl_failed(dataset, version, now, hosts):
        for host in hosts:
            coordinator.postdl_failed(dataset, version, host, now=now)

    for v in ["1.0", "2.0", "3.0"]:
        for d in ["pkg-f", "pkg-g", "pkg-h"]:
            hash = coordinator.upload(d, v, 'gen-h1', data='retry_test', branch='stable', tvm_id=4)
            coordinator.announce(hash, resolve_hosts(['rtc:maps_h']))

    time = 0
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

    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    post_postdl_failed("pkg-f", "1.0", time, resolve_hosts(['rtc:maps_h']))
    time += 25

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

    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))
    time = time + 35

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

    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    coordinator.postdl('pkg-f', '1.0', resolve_hosts(['rtc:maps_h']))

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

    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    coordinator.postdl('pkg-g', '1.0', resolve_hosts(['rtc:maps_h']))
    expected_postdl = '''\
pkg-h\t3.0
pkg-g\t3.0
pkg-f\t3.0
pkg-h\t2.0
pkg-g\t2.0
pkg-f\t2.0
pkg-h\t1.0
'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    coordinator.postdl('pkg-h', '1.0', resolve_hosts(['rtc:maps_h']))
    expected_postdl = '''\
pkg-h\t3.0
pkg-g\t3.0
pkg-f\t3.0
pkg-h\t2.0
pkg-g\t2.0
pkg-f\t2.0
'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    expected_versions = '''pkg-f\t1.0
pkg-g\t1.0
pkg-h\t1.0
'''
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    post_switch_failed("pkg-f", "1.0", time, resolve_hosts(['rtc:maps_h']))
    expected_versions = '''pkg-f\t__NONE__
pkg-g\t__NONE__
pkg-h\t__NONE__
'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))
    time = time + 60
    expected_versions = '''pkg-f\t1.0
pkg-g\t1.0
pkg-h\t1.0
'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    coordinator.step_in('pkg-h', '1.0', 'gen-h1', branch='stable/hold', tvm_id=4)
    expected_versions = '''pkg-f\t__CURRENT__
pkg-g\t__CURRENT__
pkg-h\t__CURRENT__
'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    coordinator.step_in('pkg-h', '1.0', 'gen-h1', branch='stable', tvm_id=4)
    post_switch_failed("pkg-g", "1.0", time, resolve_hosts(['rtc:maps_h']))
    time = time + 60
    expected_versions = '''pkg-f\t1.0
pkg-g\t1.0
pkg-h\t1.0
'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))
