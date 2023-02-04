PY2TEST()

OWNER(
    g:nanny
)

INCLUDE(../ya.make.inc)

SRCDIR(${SEPELIB_TEST_SRCDIR})

TEST_SRCS(
    ${SEPELIB_TEST_SRCS}
    test_core_config.py
    test_fs_util.py
    test_util_fs.py
    test_util_log_formatters.py
    test_util_net_mail.py
    test_util_retry.py
    test_prof.py
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
