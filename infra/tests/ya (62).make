PY3_LIBRARY()

OWNER(g:rtc-sysdev)

TEST_SRCS(
    lib/defs.py
    lib/utils.py
    lib/yandex_networks.py
    conftest.py
    test_networks.py
    test_tcp_rto.py
    test_tcp_tos.py
    test_net_stat.py
    test_tclass_lock.py
)

END()

RECURSE_FOR_TESTS(
    4.19.119-30.2
    5.4.134-19
    5.4.187-35.2
)
