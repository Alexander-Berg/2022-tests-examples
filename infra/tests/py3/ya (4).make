PY3TEST()

OWNER(
    g:nanny
)

INCLUDE(../ya.make.inc)

SRCDIR(${SEPELIB_TEST_SRCDIR})

TEST_SRCS(
    ${SEPELIB_TEST_SRCS}
)

PEERDIR(
    contrib/python/mock
    infra/nanny/sepelib/core
    infra/nanny/sepelib/http
    infra/nanny/sepelib/metrics
    infra/nanny/sepelib/mongo
    infra/nanny/sepelib/util
    infra/nanny/sepelib/gevent
    infra/nanny/sepelib/subprocess
    infra/nanny/sepelib/yandex
    infra/nanny/sepelib/flask
)



END()
