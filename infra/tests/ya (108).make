PY23_TEST()

OWNER(
    g:nanny
)

SRCDIR(infra/nanny/vendor/iss3lib/tests)

TEST_SRCS(
    test_iss3lib.py
)

PEERDIR(
    contrib/python/mock
    infra/nanny/vendor/iss3lib
)

END()
