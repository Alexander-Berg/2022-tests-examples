PY2TEST()

OWNER(g:rtc-sysdev)

TEST_SRCS(
    test_switch_port_changes.py
    test_fixall.py
    test_master.py
)

PEERDIR(
    infra/netconfig/checks
    infra/netconfig/lib
    contrib/python/mock
    contrib/python/ipaddr
)

END()
