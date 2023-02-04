from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_retry(coordinator):
    def check_postdl(expected, now, hosts):
        for host in hosts:
            assert coordinator.get_postdl(host, now=now).text == expected

    def check_versions(expected, now, hosts):
        for host in hosts:
            assert coordinator.versions(host, now=now).text == expected

    def post_switch_failed(version, now, hosts):
        for host in hosts:
            coordinator.switch_failed('pkg-a', version, host, now=now)

    def post_postdl_failed(version, now, hosts):
        for host in hosts:
            coordinator.postdl_failed('pkg-a', version, host, now=now)

    for v in ["1.0", "2.0", "3.0"]:
        hash = coordinator.upload(
            'pkg-a', v, 'gen-ab1', data='retry_test', branch='stable', tvm_id=1)
        coordinator.announce(hash, resolve_hosts(['rtc:maps_a']))

    time = 0
    expected_postdl = '''\
pkg-a\t3.0
pkg-a\t2.0
pkg-a\t1.0
'''
    expected_versions = 'pkg-a\t__CURRENT__\n'
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    # posdl 1.0 filed 4 times
    post_postdl_failed("1.0", time, resolve_hosts(['rtc:maps_a']))

    # retry time < retry interval
    time = time + 20
    expected_postdl = 'pkg-a\t3.0\npkg-a\t2.0\n'
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    # retry time = retry interval
    time = time + 40
    expected_postdl = '''\
pkg-a\t3.0
pkg-a\t2.0
pkg-a\t1.0
'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_postdl_failed("1.0", time, resolve_hosts(['rtc:maps_a']))
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_postdl_failed("1.0", time, resolve_hosts(['rtc:maps_a']))
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_postdl_failed("1.0", time, resolve_hosts(['rtc:maps_a']))
    expected_postdl = '''\
pkg-a\t3.0
pkg-a\t2.0
'''
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    # postdl 2.0 failed 2 times
    post_postdl_failed("2.0", time, resolve_hosts(['rtc:maps_a']))
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_postdl_failed("2.0", time, resolve_hosts(['rtc:maps_a']))
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    # postdl 2.0
    coordinator.postdl('pkg-a', '2.0', resolve_hosts(['rtc:maps_a']))
    expected_postdl = '''pkg-a\t3.0\n'''
    expected_versions = '''pkg-a\t2.0\n'''
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    # switch failed 4 times
    post_switch_failed('2.0', time, resolve_hosts(['rtc:maps_a']))
    # retry time < retry interval
    time = time + 59
    expected_versions = '''pkg-a\t__NONE__\n'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    time = time + 1
    expected_versions = '''pkg-a\t2.0\n'''
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_switch_failed('2.0', time, resolve_hosts(['rtc:maps_a']))
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_switch_failed('2.0', time, resolve_hosts(['rtc:maps_a']))
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_switch_failed('2.0', time, resolve_hosts(['rtc:maps_a']))
    expected_versions = 'pkg-a\t__CURRENT__\n'
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    # switch failed 3 times
    coordinator.postdl('pkg-a', '3.0', resolve_hosts(['rtc:maps_a']))
    expected_postdl = ''
    expected_versions = 'pkg-a\t3.0\n'
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_switch_failed('3.0', time, resolve_hosts(['rtc:maps_a']))
    expected_versions = 'pkg-a\t3.0\n'
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_switch_failed('3.0', time, resolve_hosts(['rtc:maps_a']))
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    post_switch_failed('3.0', time, resolve_hosts(['rtc:maps_a']))
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))

    # version activated
    coordinator.report_versions(resolve_hosts(['rtc:maps_a']))
    expected_versions = 'pkg-a\t3.0\n'
    time = time + 60
    check_postdl(expected_postdl, time, resolve_hosts(['rtc:maps_a']))
    check_versions(expected_versions, time, resolve_hosts(['rtc:maps_a']))
