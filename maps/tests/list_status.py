from maps.infra.ecstatic.coordinator.bin.tests.fixtures.coordinator import resolve_hosts


def test_list_status(coordinator):
    def check_status(dataset, version, expected_counters, expected_groups):
        for line in coordinator.list_status(dataset).text.splitlines():
            if line.startswith('{0}={1}\tstable'.format(dataset, version)):
                parts = line.split('\t')
                counters = [int(p) for p in parts[2:6]]
                groups = parts[6]

        assert counters == expected_counters
        assert set(groups[1:-1]) == set(expected_groups)

    pkg_c_hash = coordinator.upload('pkg-c', '1.0', 'gen-cd1', tvm_id=2)
    pkg_d_hash = coordinator.upload('pkg-d', '1.0', 'gen-cd1', tvm_id=2)
    pkg_e_hash = coordinator.upload('pkg-e', '1.0', 'gen-e1', tvm_id=3)

    check_status('pkg-c', '1.0', [10, 0, 0, 0], '--')
    check_status('pkg-d', '1.0', [6, 0, 0, 0], '-')
    check_status('pkg-e', '1.0', [3, 0, 0, 0], '-')

    coordinator.step_in('pkg-d', '1.0', 'gen-cd1', tvm_id=2)
    coordinator.step_in('pkg-e', '1.0', 'gen-e1', tvm_id=3)
    check_status('pkg-c', '1.0', [10, 0, 0, 0], '--')
    check_status('pkg-d', '1.0', [6, 0, 0, 0], '_')
    check_status('pkg-e', '1.0', [3, 0, 0, 0], '_')

    coordinator.step_in('pkg-c', '1.0', 'gen-cd1', tvm_id=2)
    check_status('pkg-c', '1.0', [10, 0, 0, 0], '__')

    coordinator.announce(pkg_c_hash, 'c1')
    check_status('pkg-c', '1.0', [10, 1, 0, 0], '_D')

    coordinator.postdl('pkg-c', '1.0', 'c1')
    check_status('pkg-c', '1.0', [10, 1, 0, 0], '_R')

    coordinator.announce(pkg_c_hash, 'cd11')
    check_status('pkg-c', '1.0', [10, 2, 0, 0], 'dR')

    coordinator.postdl('pkg-c', '1.0', 'cd11')
    check_status('pkg-c', '1.0', [10, 2, 0, 0], 'rR')

    coordinator.announce(pkg_c_hash, resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.postdl('pkg-c', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    check_status('pkg-c', '1.0', [10, 7, 0, 0], 'rR')

    coordinator.report_versions(resolve_hosts(['rtc:maps_c']))
    check_status('pkg-c', '1.0', [10, 7, 1, 0], 'rA')

    coordinator.announce(pkg_c_hash, 'ce1')
    coordinator.postdl_failed('pkg-c', '1.0', 'ce1')
    check_status('pkg-c', '1.0', [10, 8, 1, 1], 'rA')

    coordinator.announce(pkg_c_hash, 'ce2')
    coordinator.postdl_failed('pkg-c', '1.0', 'ce2')
    check_status('pkg-c', '1.0', [10, 9, 1, 2], 'AF')

    coordinator.announce(pkg_c_hash, resolve_hosts(['rtc:maps_ce']))
    coordinator.postdl('pkg-c', '1.0', resolve_hosts(['rtc:maps_ce']))
    check_status('pkg-c', '1.0', [10, 10, 1, 0], 'RA')

    check_status('pkg-d', '1.0', [6, 0, 0, 0], '_')
    check_status('pkg-e', '1.0', [3, 0, 0, 0], '_')
    coordinator.announce(pkg_d_hash, resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.postdl('pkg-d', '1.0', resolve_hosts(['rtc:maps_cd1', 'rtc:maps_cd2']))
    coordinator.announce(pkg_d_hash, resolve_hosts(['rtc:maps_ce']))
    coordinator.announce(pkg_e_hash, resolve_hosts(['rtc:maps_ce']))
    coordinator.postdl('pkg-e', '1.0', resolve_hosts(['rtc:maps_ce']))
    check_status('pkg-d', '1.0', [6, 6, 0, 0], 'R')
    check_status('pkg-e', '1.0', [3, 3, 0, 0], 'R')

    coordinator.report_versions(resolve_hosts(['rtc:maps_cd2']))
    check_status('pkg-c', '1.0', [10, 10, 4, 0], 'aA')

    coordinator.switch_failed('pkg-c', '1.0', 'ce1')
    check_status('pkg-c', '1.0', [10, 10, 4, 1], 'Aa')
    coordinator.switch_failed('pkg-c', '1.0', 'cd11')
    check_status('pkg-c', '1.0', [10, 10, 4, 2], 'aA')
    coordinator.switch_failed('pkg-c', '1.0', 'cd12')
    check_status('pkg-c', '1.0', [10, 10, 4, 3], 'FA')

    coordinator.step_in('pkg-c', '1.0', 'gen-cd1', tvm_id=2)
    check_status('pkg-c', '1.0', [10, 10, 4, 0], 'Aa')
    coordinator.switch_failed('pkg-c', '1.0', 'ce1')
    check_status('pkg-c', '1.0', [10, 10, 4, 1], 'aA')
    coordinator.switch_failed('pkg-c', '1.0', 'cd11')
    check_status('pkg-c', '1.0', [10, 10, 4, 2], 'Aa')
    coordinator.report_versions(resolve_hosts(['rtc:maps_cd1', 'rtc:maps_ce']))
    check_status('pkg-c', '1.0', [10, 10, 8, 2], 'AA')
