PY3_LIBRARY()

OWNER(g:kernel)

IF (AUTOCHECK)
   DEFAULT(KERNEL_TEST_SKIP_PERF 1)
ELSE()
   DEFAULT(KERNEL_TEST_SKIP_PERF 0)
ENDIF()

PY_SRCS(
    util.py
)

TEST_SRCS(
    __init__.py
    conftest.py
    test_arc_fuse.py
    test_block_interface.py
    test_bpf_features.py
    test_cgroup_blkio_knobs.py
    test_cgroup_blkio_leakage.py
    test_cgroup_blkio_throttler_stat.py
    test_cgroup_interface.py
    test_cgroup_juggler.py
    test_cgroup_net_cls_attach.py
    test_cgroup_net_cls_priority.py
    test_cgroup_writeback_switch.py
    test_ext4_lazytime_update.py
    test_ext4_falloc.py
    test_fadvise_noreuse.py
    test_fcntl_get_cached_pages.py
    test_io_uring.py
    test_ipv4_tcp_ya_decap_info.py
    test_ipv6_auto_flowlabels.py
    test_madvise.py
    test_memory_compaction_poisoning.py
    test_mount_remount_locking.py
    test_mpls_modules.py
    test_numa_balancing.py
    test_nvidia_module.py
    test_overlayfs_aio.py
    test_porto_ptrace_kludge.py
    test_project_quota.py
    test_tcp.py
    test_log_fatal_signals.py
    test_madvise_stockpile_cg_hierarchy.py
)

IF (NOT KERNEL_TEST_SKIP_PERF)
TEST_SRCS(
    test_cgroup_blkio_throttler.py
    test_cgroup_cpuacct_knobs.py
    test_cgroup_memory_oom.py
    test_iostat.py
    test_madvise_stockpile_balance.py
    test_sched_file_idle_cpu.py
    #TODO move tests below back to default group after  https://st.yandex-team.ru/DEVTOOLSSUPPORT-3405 fixed
    test_cgroup_memory_recharge.py
)
ENDIF()

PEERDIR(
    infra/kernel/test/misc/kern
    library/python/testing/yatest_common

    contrib/python/psutil
)

END()

RECURSE(
    kern
    exec
)

RECURSE_ROOT_RELATIVE(
    contrib/libs/liburing
)
