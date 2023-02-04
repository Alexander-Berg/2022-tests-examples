import pytest
import kern


legacy_common = [
    ('cgroup.clone_children', (), ()),
    ('cgroup.procs', (), ()),
    ('tasks', (), ()),
    ('notify_on_release', (), ()),
    ('cgroup.sane_behavior', ('only-root',), ()),
    ('release_agent', ('only-root',), ()),
]

cpu_common = [
    ('cpu.cfs_period_us', (), ()),
    ('cpu.cfs_quota_us', (), ()),
    ('cpu.rt_period_us', (), ()),
    ('cpu.rt_runtime_us', (), ()),
    ('cpu.shares', (), ()),
    ('cpu.stat', (), ()),
]

cpu_yandex = [
    ('cpu.smart', (), ('4.4.36-13',)),      # Deprecated
    ('cpu.cfs_burst_us', (), ('4.19.84-18', ('5.4.14-1', '5.4.72-5'))),                   # https://st.yandex-team.ru/KERNEL-319
    ('cpu.cfs_reserve_shares', (), ('4.4.11-2', '4.19.0-0', '5.4.0-0')),    # https://st.yandex-team.ru/KERNEL-314
    ('cpu.cfs_reserve_us', (), ('4.4.11-2', '4.19.0-0', '5.4.0-0')),        # https://st.yandex-team.ru/KERNEL-314
    ('cpu.cfs_burst_usage', (), ('5.4.178-30',)),         # https://st.yandex-team.ru/KERNEL-711
    ('cpu.cfs_burst_load',  (), ('5.4.178-30',)),         # https://st.yandex-team.ru/KERNEL-711
    ('cpu.cfs_throttled', (), ('5.4.184-33',)),           # https://st.yandex-team.ru/KERNEL-711
    ('cpu.cfs_nr_running',    (), ('5.4.161-26',)),       # https://st.yandex-team.ru/KERNEL-711
    ('cpu.cfs_nr_runnable',   (), ('5.4.178-30',)),       # https://st.yandex-team.ru/KERNEL-711
    ('cpu.cfs_occupied_rqs',  (), ('5.4.161-26',)),       # https://st.yandex-team.ru/KERNEL-711
    ('cpu.cfs_reserve_rqs_limit',  (), ('5.4.161-26',)),  # https://st.yandex-team.ru/KERNEL-711
]

cpu_stat_common = [
    ('nr_periods', (), ()),
    ('nr_throttled', (), ()),
    ('throttled_time', (), ()),
]

cpu_stat_yandex = [
    ('burst_time', (), ('4.19.84-18', ('5.4.14-1', '5.4.72-5'))),     # https://st.yandex-team.ru/KERNEL-319
    ('burst_load', (), ('5.4.184-34',)),
    ('burst_usage', (), ('5.4.184-34',)),
    ('h_throttled_time', (), ('5.4.184-34',)),
]

cpu_stat_schedstats = [
    ('wait_sum', ('non-root',), ()),
    ('cwait_sum', ('non-root',), (('5.4.161-26', '5.4.182-32'),)),  # https://st.yandex-team.ru/KERNEL-711
]

cpuacct_common = [
    ('cpuacct.cons',         (), (('5.4.161-26', '5.4.182-32'),)),  # https://st.yandex-team.ru/KERNEL-711
    ('cpuacct.cwait_percpu', (), (('5.4.161-26', '5.4.182-32'),)),  # https://st.yandex-team.ru/KERNEL-711
    ('cpuacct.stat', (), ()),
    ('cpuacct.usage', (), ()),
    ('cpuacct.usage_all', (), ('4.8',)),
    ('cpuacct.usage_percpu', (), ()),
    ('cpuacct.usage_percpu_sys', (), ('4.7',)),
    ('cpuacct.usage_percpu_user', (), ('4.7',)),
    ('cpuacct.usage_sys', (), ('4.7',)),
    ('cpuacct.usage_user', (), ('4.7',)),
]

cpuacct_yandex = [
    ('cpuacct.wait', (), ('4.4.39-16', '4.19.17-2', '5.4.14-1')),           # https://st.yandex-team.ru/KERNEL-313
    ('cpuacct.wait_percpu', (), ('4.4.39-16', '4.19.17-2', '5.4.14-1')),
    ('cpuacct.ipc.cycles', (), (('5.4.184-33', '5.4.184-34'),)),                            # https://st.yandex-team.ru/KERNEL-462
    ('cpuacct.ipc.instructions', (), (('5.4.184-33', '5.4.184-34'),)),                      # https://st.yandex-team.ru/KERNEL-462
]

cpuacct_stat_common = [
    ('user', (), ()),
    ('system', (), ()),
]

cpuacct_stat_yandex = [
    ('ipc.cycles', (), ('5.4.184-34',)),                            # https://st.yandex-team.ru/KERNEL-462
    ('ipc.instructions', (), ('5.4.184-34',)),                      # https://st.yandex-team.ru/KERNEL-462
]

cpuset_common = [
    ('cpuset.cpu_exclusive', (), ()),
    ('cpuset.cpus', (), ()),
    ('cpuset.effective_cpus', (), ()),
    ('cpuset.effective_mems', (), ()),
    ('cpuset.mem_exclusive', (), ()),
    ('cpuset.mem_hardwall', (), ()),
    ('cpuset.memory_migrate', (), ()),
    ('cpuset.memory_pressure', (), ()),
    ('cpuset.memory_pressure_enabled', ('only-root',), ()),
    ('cpuset.memory_spread_page', (), ()),
    ('cpuset.memory_spread_slab', (), ()),
    ('cpuset.mems', (), ()),
    ('cpuset.sched_load_balance', (), ()),
    ('cpuset.sched_relax_domain_level', (), ()),
]

devices_common = [
    ('devices.allow', (), ()),
    ('devices.deny', (), ()),
    ('devices.list', (), ()),
]

freezer_common = [
    ('freezer.parent_freezing', ('non-root',), ()),
    ('freezer.self_freezing', ('non-root',), ()),
    ('freezer.state', ('non-root',), ()),
]

pids_common = [
    ('pids.current', ('non-root',), ()),
    ('pids.events', ('non-root',), ('4.8',)),
    ('pids.max', ('non-root',), ()),
]

pids_events = [
    ('max', (), ()),
]

memory_common = [
    ('cgroup.event_control', (), ()),
    ('memory.failcnt', (), ()),
    ('memory.force_empty', (), ()),
    ('memory.kmem.failcnt', (), ('4.19',)),
    ('memory.kmem.limit_in_bytes', (), ('4.19',)),
    ('memory.kmem.max_usage_in_bytes', (), ('4.19',)),
    ('memory.kmem.slabinfo', (), ('4.19',)),
    ('memory.kmem.tcp.failcnt', (), ('4.19',)),
    ('memory.kmem.tcp.limit_in_bytes', (), ('4.19',)),
    ('memory.kmem.tcp.max_usage_in_bytes', (), ('4.19',)),
    ('memory.kmem.tcp.usage_in_bytes', (), ('4.19',)),
    ('memory.kmem.usage_in_bytes', (), ('4.19',)),
    ('memory.limit_in_bytes', (), ()),
    ('memory.max_usage_in_bytes', (), ()),
    ('memory.move_charge_at_immigrate', (), ()),
    ('memory.numa_stat', (), ()),
    ('memory.oom_control', (), ()),
    ('memory.pressure_level', (), ()),
    ('memory.soft_limit_in_bytes', (), ()),
    ('memory.stat', (), ()),
    ('memory.swappiness', (), ()),
    ('memory.usage_in_bytes', (), ()),
    ('memory.use_hierarchy', (), ()),
]

memory_yandex = [
    ('memory.anon.limit', (), ()),      # https://st.yandex-team.ru/KERNEL-347
    ('memory.anon.max_usage', (), ()),
    ('memory.anon.usage', (), ()),
    ('memory.anon.only', ('non-root',), ('4.4.131-54',)),   # Deprecated, https://st.yandex-team.ru/KERNEL-141
    ('memory.high_limit_in_bytes', (), ()),
    ('memory.dirty_limit_in_bytes', (), ()),    # https://st.yandex-team.ru/KERNEL-60
    ('memory.low_limit_in_bytes', (), ()),      # https://st.yandex-team.ru/KERNEL-349
    ('memory.mlock_policy', (), ('5.4.174-29',)),  # https://st.yandex-team.ru/KERNEL-778
    ('memory.numa_balance_vmprot', (), ('5.4.142-22',)),  # https://st.yandex-team.ru/KERNEL-700
    ('memory.recharge_on_pgfault', (), ()),     # https://st.yandex-team.ru/KERNEL-348
    ('memory.writeback_blkio', (), ('4.4.174-71', '4.19.25-3', '5.4.14-1')),    # https://st.yandex-team.ru/KERNEL-212
    ('memory.oom.group', ('non-root',), ('4.19.60-12', '5.4.14-1')),            # https://st.yandex-team.ru/KERNEL-269
    ('memory.fs_bps_limit', (), ('4.4.0-0', '4.19.48-7')),          # Deprecated
    ('memory.fs_write_bps_limit', (), ('4.4.0-0', '4.19.48-7')),    # Deprecated
    ('memory.fs_iops_limit', (), ('4.4.0-0', '4.19.48-7')),         # Deprecated
]

memory_stat_common = [
    ('cache', (), ()),
    ('rss', (), ()),
    ('rss_huge', (), ()),
    ('shmem', (), ('4.19',)),
    ('mapped_file', (), ()),
    ('dirty', (), ()),
    ('writeback', (), ()),
    ('pgpgin', (), ()),
    ('pgpgout', (), ()),
    ('pgfault', (), ()),
    ('pgmajfault', (), ()),
    ('inactive_anon', (), ()),
    ('active_anon', (), ()),
    ('inactive_file', (), ()),
    ('active_file', (), ()),
    ('unevictable', (), ()),
    ('hierarchical_memory_limit', (), ()),
    ('total_cache', (), ()),
    ('total_rss', (), ()),
    ('total_rss_huge', (), ()),
    ('total_shmem', (), ('4.19',)),
    ('total_mapped_file', (), ()),
    ('total_dirty', (), ()),
    ('total_writeback', (), ()),
    ('total_pgpgin', (), ()),
    ('total_pgpgout', (), ()),
    ('total_pgfault', (), ()),
    ('total_pgmajfault', (), ()),
    ('total_inactive_anon', (), ()),
    ('total_active_anon', (), ()),
    ('total_inactive_file', (), ()),
    ('total_active_file', (), ()),
    ('total_unevictable', (), ()),
    ('recent_rotated_anon', (), ()),
    ('recent_rotated_file', (), ()),
    ('recent_scanned_anon', (), ()),
    ('recent_scanned_file', (), ()),
]

memory_stat_yandex = [
    ('low_events', (), ()),
    ('high_events', (), ()),
    ('max_events', (), ()),
    ('oom_events', (), ()),
    ('oom_kill', (), ()),
    ('total_low_events', (), ('4.4.73-34',)),
    ('total_high_events', (), ('4.4.73-34',)),
    ('total_max_events', (), ('4.4.73-34',)),
    ('total_oom_events', (), ('4.4.73-34',)),
    ('total_oom_kill', (), ('4.4.73-34',)),
    ('recharge', (), ('4.4.73-34',)),                       # Deprecated
    ('total_recharge', (), ('4.4.73-34',)),                 # Deprecated
    ('fs_io_bytes', (), ('4.4.35-12', '4.19.48-7')),        # Deprecated
    ('fs_io_write_bytes', (), ('4.4.35-12', '4.19.48-7')),  # Deprecated
    ('fs_io_operations', (), ('4.4.35-12', '4.19.48-7')),   # Deprecated
    ('dirtied', (), ('4.19.44-5', '5.4.14-1')),     # https://st.yandex-team.ru/KERNEL-237
    ('written', (), ('4.19.44-5', '5.4.14-1')),
    ('total_dirtied', (), ('4.19.44-5', '5.4.14-1')),
    ('total_written', (), ('4.19.44-5', '5.4.14-1')),
    ('total_max_rss', (), ()),
    ('pglazyfree', (), ('4.19.17-2', '5.4.14-1')),
    ('pglazyfree_failed', (), ('4.19.17-2', '5.4.14-1')),
    ('pglazyfree_reused', (), ('4.19.17-2', '5.4.14-1')),
    ('pglazyfreed', (), ('4.19.17-2', '5.4.14-1')),
    ('total_pglazyfree', (), ('4.19.17-2', '5.4.14-1')),
    ('total_pglazyfree_failed', (), ('4.19.17-2', '5.4.14-1')),
    ('total_pglazyfree_reused', (), ('4.19.17-2', '5.4.14-1')),
    ('total_pglazyfreed', (), ('4.19.17-2', '5.4.14-1')),
    ('pgrefill', (), ('4.19.119-30', '5.4.38-2')),  # https://st.yandex-team.ru/KERNEL-392
    ('pgsteal_kswapd', (), ('4.19.119-30', '5.4.38-2')),
    ('pgsteal_direct', (), ('4.19.119-30', '5.4.38-2')),
    ('pgscan_kswapd', (), ('4.19.119-30', '5.4.38-2')),
    ('pgscan_direct', (), ('4.19.119-30', '5.4.38-2')),
    ('pgactivate', (), ('4.19.119-30', '5.4.38-2')),
    ('pgdeactivate', (), ('4.19.119-30', '5.4.38-2')),
    ('total_pgrefill', (), ('4.19.119-30', '5.4.38-2')),
    ('total_pgsteal_kswapd', (), ('4.19.119-30', '5.4.38-2')),
    ('total_pgsteal_direct', (), ('4.19.119-30', '5.4.38-2')),
    ('total_pgscan_kswapd', (), ('4.19.119-30', '5.4.38-2')),
    ('total_pgscan_direct', (), ('4.19.119-30', '5.4.38-2')),
    ('total_pgactivate', (), ('4.19.119-30', '5.4.38-2')),
    ('total_pgdeactivate', (), ('4.19.119-30', '5.4.38-2')),
]

memory_oom_control = [
    ('oom_kill_disable', (), ()),
    ('under_oom', (), ()),
    ('oom_kill', (), ()),
]

blkio_common = [
    ('blkio.reset_stats', (), ()),
]

blkio_throttle_common = [
    ('blkio.throttle.io_service_bytes', (), ()),
    ('blkio.throttle.io_service_bytes_recursive', (), ()),
    ('blkio.throttle.io_serviced', (), ()),
    ('blkio.throttle.io_serviced_recursive', (), ()),
    ('blkio.throttle.read_bps_device', (), ()),
    ('blkio.throttle.read_iops_device', (), ()),
    ('blkio.throttle.write_bps_device', (), ()),
    ('blkio.throttle.write_iops_device', (), ()),
]

blkio_throttle_yandex = [
    ('blkio.throttle.io_service_time', (), ('4.19.17-2',)),             # Deprecated
    ('blkio.throttle.io_service_time_recursive', (), ('4.19.17-2',)),   # Deprecated
    ('blkio.throttle.io_wait_time', (), ('4.19.17-2',)),                # Deprecated
    ('blkio.throttle.io_wait_time_recursive', (), ('4.19.17-2',)),      # Deprecated
    ('blkio.throttle.io_throttled_time', (), ('4.4.171-70.3', '4.4.180-73', '4.19.44-5', '5.4.14-1')),  # https://st.yandex-team.ru/KERNEL-236
]

blkio_cfq_common = [
    ('blkio.io_merged', (), (('3.18', '5.4'),)),
    ('blkio.io_merged_recursive', (), (('3.18', '5.4'),)),
    ('blkio.io_queued', (), (('3.18', '5.4'),)),
    ('blkio.io_queued_recursive', (), (('3.18', '5.4'),)),
    ('blkio.io_service_bytes', (), (('3.18', '5.4'),)),
    ('blkio.io_service_bytes_recursive', (), (('3.18', '5.4'),)),
    ('blkio.io_serviced', (), (('3.18', '5.4'),)),
    ('blkio.io_serviced_recursive', (), (('3.18', '5.4'),)),
    ('blkio.io_service_time', (), (('3.18', '5.4'),)),
    ('blkio.io_service_time_recursive', (), (('3.18', '5.4'),)),
    ('blkio.io_wait_time', (), (('3.18', '5.4'),)),
    ('blkio.io_wait_time_recursive', (), (('3.18', '5.4'),)),
    ('blkio.leaf_weight', (), (('3.18', '5.4'),)),
    ('blkio.leaf_weight_device', (), (('3.18', '5.4'),)),
    ('blkio.sectors', (), (('3.18', '5.4'),)),
    ('blkio.sectors_recursive', (), (('3.18', '5.4'),)),
    ('blkio.time', (), (('3.18', '5.4'),)),
    ('blkio.time_recursive', (), (('3.18', '5.4'),)),
    ('blkio.weight', (), (('3.18', '5.4'),)),
    ('blkio.weight_device', (), (('3.18', '5.4'),)),
]

blkio_bfq_common = [
    ('blkio.bfq.avg_queue_size', (), ('5.4',)),
    ('blkio.bfq.dequeue', (), ('5.4',)),
    ('blkio.bfq.empty_time', (), ('5.4',)),
    ('blkio.bfq.group_wait_time', (), ('5.4',)),
    ('blkio.bfq.idle_time', (), ('5.4',)),
    ('blkio.bfq.io_merged', (), ('5.4',)),
    ('blkio.bfq.io_merged_recursive', (), ('5.4',)),
    ('blkio.bfq.io_queued', (), ('5.4',)),
    ('blkio.bfq.io_queued_recursive', (), ('5.4',)),
    ('blkio.bfq.io_service_bytes', (), ('4.19',)),
    ('blkio.bfq.io_service_bytes_recursive', (), ('4.19',)),
    ('blkio.bfq.io_serviced', (), ('4.19',)),
    ('blkio.bfq.io_serviced_recursive', (), ('4.19',)),
    ('blkio.bfq.io_service_time', (), ('4.19',)),
    ('blkio.bfq.io_service_time_recursive', (), ('4.19',)),
    ('blkio.bfq.io_wait_time', (), ('4.19',)),
    ('blkio.bfq.io_wait_time_recursive', (), ('4.19',)),
    ('blkio.bfq.sectors', (), ('5.4',)),
    ('blkio.bfq.sectors_recursive', (), ('5.4',)),
    ('blkio.bfq.time', (), ('5.4',)),
    ('blkio.bfq.time_recursive', (), ('5.4',)),
    ('blkio.bfq.weight', ('non-root',), ('4.19',)),
    ('blkio.bfq.weight_device', ('non-root',), ('4.19.114-29', '5.4')),
]

blkio_cost_yandex = [
    ('blkio.cost.weight', ('non-root',), ('5.4.38-2',)),
    ('blkio.cost.model', ('only-root',), ('5.4.38-2',)),
    ('blkio.cost.qos',  ('only-root',), ('5.4.38-2',)),
]

hugetlb_common = [
    ('hugetlb.2MB.limit_in_bytes', (), ()),
    ('hugetlb.2MB.usage_in_bytes', (), ()),
    ('hugetlb.2MB.max_usage_in_bytes', (), ()),
    ('hugetlb.2MB.failcnt', (), ()),
    ('hugetlb.1GB.limit_in_bytes', ('skip',), ()),
    ('hugetlb.1GB.usage_in_bytes', ('skip',), ()),
    ('hugetlb.1GB.max_usage_in_bytes', ('skip',), ()),
    ('hugetlb.1GB.failcnt', ('skip',), ()),
]

netcls_common = [
    ('net_cls.classid', (), ()),
]

netcls_yandex = [
    ('net_cls.priority', (), (('4.4.128-53', '4.4.171-70.2'), ('4.19.17-2', '4.19.25-3'))),     # Deprecated
    ('net_cls.ya.priority', (), ('4.4.171-70.2', ('4.19.25-3', '4.19.109-26'))),    # Deprecated, https://st.yandex-team.ru/PORTO-368
]

unified_common = [
    ('cgroup.events', ('non-root',), ()),
    ('cgroup.procs', (), ()),
    ('cgroup.max.descendants', (), ()),
    ('cgroup.type', ('non-root',), ()),
    ('cgroup.stat', (), ()),
    ('cgroup.threads', (), ()),
    ('cgroup.freeze', ('non-root',), ('5.4',)),
    ('cgroup.controllers', (), ()),
    ('cgroup.subtree_control', (), ()),
    ('cgroup.max.depth', (), ()),
    ('io.pressure', (), ('5.4',)),
    ('memory.pressure', (), ('5.4',)),
    ('cpu.pressure', (), ('5.4',)),
    ('cpu.stat', ('non-root',), ()),
]

unified_stat = [
    ('nr_descendants', (), ()),
    ('nr_dying_descendants', (), ()),
]

unified_events = [
    ('populated', (), ()),
    ('frozen', (), ('5.2',)),
]

subsys_attrs = [
    ('blkio', legacy_common + blkio_common + blkio_throttle_common + blkio_throttle_yandex + blkio_cfq_common + blkio_bfq_common + blkio_cost_yandex),
    ('cpu', legacy_common + cpu_common + cpu_yandex),
    ('cpuacct', legacy_common + cpuacct_common + cpuacct_yandex),
    ('cpuset', legacy_common + cpuset_common),
    ('devices', legacy_common + devices_common),
    ('freezer', legacy_common + freezer_common),
    ('hugetlb', legacy_common + hugetlb_common),
    ('memory', legacy_common + memory_common + memory_yandex),
    ('net_cls', legacy_common + netcls_common + netcls_yandex),
    ('pids', legacy_common + pids_common),
    ('unified', unified_common),
]

subsys_stats = [
    ('memory', 'memory.stat', memory_stat_common + memory_stat_yandex, []),
    ('memory', 'memory.oom_control', memory_oom_control, []),
    ('cpu', 'cpu.stat', cpu_stat_common + cpu_stat_yandex + cpu_stat_schedstats, [("kernel.sched_schedstats", 1)]),
    ('cpu', 'cpu.stat', cpu_stat_common + cpu_stat_yandex, [("kernel.sched_schedstats", 0)]),
    ('cpuacct', 'cpuacct.stat', cpuacct_stat_common + cpuacct_stat_yandex, []),
    ('pids', 'pids.events', pids_events, []),
    ('unified', 'cgroup.stat', unified_stat, []),
    ('unified', 'cgroup.events', unified_events, []),
]


def expected_keys(keys, level, present):
    expected = []
    for key, flags, releases in keys:
        if level == 'root':
            if 'non-root' in flags:
                continue
        else:
            if 'only-root' in flags:
                continue

        if 'skip' in flags:
            if key in present:
                expected.append(key)
            continue

        assert isinstance(releases, tuple)
        if releases and not kern.kernel_in(*releases):
            continue

        expected.append(key)
    return expected


@pytest.mark.parametrize('level', ['root', ''])
@pytest.mark.parametrize('subsys,attrs', subsys_attrs)
def test_attrs(level, subsys, attrs, make_cgroup):
    if not kern.subsys_mounted(subsys):
        pytest.skip('subsys {} not mounted'.format(subsys))

    if level == 'root':
        cg = kern.root_cgroup(subsys)
    else:
        cg = make_cgroup(subsys)

    present = cg.attrs()
    expected = expected_keys(attrs, level, present)

    for attr in expected:
        if attr not in present:
            pytest.fail('cgroup {} expected attribute {} not found'.format(subsys, attr))

    for attr in present:
        prefix = attr.split('.', 2)[0]
        if prefix not in [subsys, 'cgroup'] and kern.subsys_bound(subsys, prefix):
            continue
        if attr not in expected:
            pytest.fail('cgroup {} unexpected attribute {} found'.format(subsys, attr))


@pytest.mark.parametrize('level', ['root', ''])
@pytest.mark.parametrize('subsys,attr,keys,sysctl_knobs', subsys_stats)
def test_stats(level, subsys, attr, keys, sysctl, sysctl_knobs, make_cgroup):
    if not kern.subsys_mounted(subsys):
        pytest.skip('subsys {} not mounted'.format(subsys))

    for k, v in sysctl_knobs:
        if k not in sysctl:
            pytest.skip('sysctl {} is absent'.format(k))
        sysctl[k] = v

    if level == 'root':
        cg = kern.root_cgroup(subsys)
    else:
        cg = make_cgroup(subsys)

    if not cg.has_attr(attr):
        pytest.skip('attr {} not found'.format(attr))

    present = cg.get_stats(attr).keys()
    expected = expected_keys(keys, level, present)

    for key in expected:
        if key not in present:
            pytest.fail('cgroup {} expeceted key {} {} not found'.format(subsys, attr, key))

    for key in present:
        if key not in expected:
            pytest.fail('cgroup {} unexpected key {} {} found'.format(subsys, attr, key))
