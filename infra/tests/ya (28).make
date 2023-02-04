PY3TEST()

INCLUDE(${ARCADIA_ROOT}/yp/python/ya_programs.make.inc)

OWNER(g:yp-dns)

PEERDIR(
    infra/libs/local_yp
    infra/yp_dns/config
    infra/yp_dns/libs/python/daemon
    infra/yp_yandex_dns_export/libs/python
    yp/python/local
)

TIMEOUT(600)

SIZE(MEDIUM)

TEST_SRCS(
    conftest.py
    test.py
)

REQUIREMENTS(ram:13)

END()
