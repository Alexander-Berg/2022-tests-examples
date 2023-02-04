PY2TEST()

OWNER(
    g:nanny
)

FORK_TEST_FILES()

INCLUDE(ya.make.inc)

SRCDIR(${INSTANCECTL_TEST_SRCDIR})
TEST_SRCS(${INSTANCECTL_TEST_SRCS})


DATA(
    arcadia/infra/nanny/instancectl/tests/func
)

DEPENDS(
    infra/nanny/instancectl/bin
    infra/nanny/instancectl/sd_bin
)

PEERDIR(
    contrib/python/gevent
    contrib/python/Flask

    infra/nanny/clusterpb
    infra/nanny/instancectl/src
    infra/nanny/sepelib/subprocess
    infra/nanny/sepelib/flask
)

REQUIREMENTS(
    ram_disk:4
)



END()
