PY3TEST()

INCLUDE(${ARCADIA_ROOT}/yp/python/ya_programs.make.inc)

OWNER(
    g:yp-controllers-lib
    g:yp-sd
    g:yp-dns
)

PEERDIR(
    infra/libs/local_yp
    infra/libs/yp_replica
    infra/libs/yp_replica/test/scenario

    yp/python/local
)

TEST_SRCS(test.py)

SIZE(MEDIUM)

REQUIREMENTS(ram:9)

END()
