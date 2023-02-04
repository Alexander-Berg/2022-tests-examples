PY3TEST()

INCLUDE(${ARCADIA_ROOT}/yp/python/ya_programs.make.inc)

OWNER(
    ismagilas
    g:yp-dns
)

PEERDIR(
    infra/box_dns_controller/libs/python
    infra/libs/local_yp
    yp/python/local
)

TIMEOUT(300)

SIZE(MEDIUM)

TEST_SRCS(
    conftest.py
    test.py
)

END()
