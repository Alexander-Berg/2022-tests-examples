PY3TEST()

OWNER(g:yp-dns)

INCLUDE(${ARCADIA_ROOT}/yp/python/ya_programs.make.inc)

PEERDIR(
    contrib/python/dnspython
    infra/libs/local_yp
    infra/yp_dns/libs/python/daemon
    library/python/resource
    library/python/spack
    yp/python/local
)

TEST_SRCS(
    conftest.py
    test_dns.py
)

SIZE(MEDIUM)

TIMEOUT(600)

REQUIREMENTS(ram:10)

END()
