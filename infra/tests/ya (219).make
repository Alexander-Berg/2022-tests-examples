PY2TEST()

OWNER(
    max7255
    nekto0n
    olegsenin
)

DATA(
    arcadia/infra/rtc/packages/yandex-hbf-agent/share/
    arcadia/infra/rtc/packages/yandex-hbf-agent/share/rules.d/
    arcadia/infra/rtc/packages/yandex-hbf-agent/rtc/etc/yandex-hbf-agent/rules.d/
)

TEST_SRCS(
    test_agent.py
    test_config.py
    test_host_ips.py
    test_iptables.py
    test_metrics.py
    test_porto_ips.py
    test_porto_ips_runtime.py
    test_runtime_mtn_nat.py
    test_server_ips.py
    test_threadpool.py
    test_util.py
    test_vertis_docker.py
    test_vertis_docker_yacloud.py
    test_vertis_docker_common.py
    test_vertis_lxc_ips.py
)

PEERDIR(
    infra/rtc/packages/yandex-hbf-agent/hbfagent
    contrib/python/mock
    contrib/python/ipaddr
    contrib/python/netaddr
    contrib/python/dnspython
    library/python/testing/yatest_common
    infra/porto/api_py
)

REQUIREMENTS(
    network:full
)

END()
