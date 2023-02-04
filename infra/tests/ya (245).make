PY2TEST()

OWNER(g:walle)

TEST_SRCS(
    conftest.py
    tools.py
    utils.py
    test_number_ranges.py
    test_checks.py
    test_walle_gpu.py
    test_walle_disk.py
    test_walle_link.py
    test_walle_meta.py
    test_walle_memory.py
    test_tainted_kernel.py
    test_walle_bmc.py
    test_walle_cpu.py
    test_walle_cpu_capping.py
    test_walle_cpu_caches.py
    test_walle_clocksource.py
    test_walle_reboots.py
    test_walle_fs.py
    test_walle_fstab.py
    test_walle_infiniband.py
    test_common.py
)

PEERDIR(
    infra/wall-e/checks
    contrib/python/pytest
    contrib/python/mock
)



END()
