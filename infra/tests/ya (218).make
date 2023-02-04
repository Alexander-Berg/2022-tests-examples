OWNER(g:hostman)
PY2TEST()

TEST_SRCS(
    test_node_info_mem.py
    test_node_info.py
    test_os_info.py
    test_oops_disks2.py
    test_location_info.py
    test_node_info_net.py
    test_yp.py
    test_statefile.py
    test_node_info_numa.py
    test_overrides.py
    test_lshw_cpu.py
)

PEERDIR(
    contrib/python/mock
    infra/rtc/nodeinfo/lib
    infra/rtc/nodeinfo/lib/yp_util
)

END()
