from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_retry_fallback(coordinator):
    def check_postdl(expected, now, hosts):
        for host in hosts:
            assert coordinator.get_postdl(host, now=now).text == expected

    def check_versions(expected, now, hosts):
        for host in hosts:
            assert coordinator.versions(host, now=now).text == expected

    def post_switch_failed(dataset, version, now, hosts):
        for host in hosts:
            coordinator.switch_failed(dataset, version, host, now=now)

    time = 0
    for v in ["1.0", "2.0", "3.0"]:
        for d in ["pkg-f", "pkg-g", "pkg-h"]:
            hash = coordinator.upload(d, v, 'gen-h1', data='retry_test', branch='stable', tvm_id=4)
            coordinator.announce(hash, resolve_hosts(['rtc:maps_h']))

    # postdl and activate version 1.0
    for d in ["pkg-f", "pkg-g", "pkg-h"]:
        coordinator.postdl(d, '1.0', resolve_hosts(['rtc:maps_h']))
    coordinator.report_versions(resolve_hosts(['rtc:maps_h']))

    expected_postdl = '''\
pkg-h\t3.0
pkg-g\t3.0
pkg-f\t3.0
pkg-h\t2.0
pkg-g\t2.0
pkg-f\t2.0
'''

    expected_versions = '''pkg-f\t1.0
pkg-g\t1.0
pkg-h\t1.0
'''

    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    # postdl version 2.0
    time += 25
    for d in ["pkg-f", "pkg-g", "pkg-h"]:
        coordinator.postdl(d, '2.0', resolve_hosts(['rtc:maps_h']))

    expected_postdl = '''\
pkg-h\t3.0
pkg-g\t3.0
pkg-f\t3.0
'''

    expected_versions = '''pkg-f\t2.0
pkg-g\t2.0
pkg-h\t2.0
'''

    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    # switch failed 2 times - disable datasets
    post_switch_failed("pkg-g", "2.0", time, resolve_hosts(['rtc:maps_h']))
    expected_versions = '''pkg-f\t__NONE__
pkg-g\t__NONE__
pkg-h\t__NONE__
'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    post_switch_failed("pkg-g", "2.0", time, resolve_hosts(['rtc:maps_h']))
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))

    # switch failed 3rd time - fallback to version 1.0
    post_switch_failed("pkg-g", "2.0", time, resolve_hosts(['rtc:maps_h']))
    expected_versions = '''pkg-f\t1.0
pkg-g\t1.0
pkg-h\t1.0
'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_h']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_h']))
