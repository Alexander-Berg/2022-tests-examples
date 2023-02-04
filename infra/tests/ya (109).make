PY23_TEST()

OWNER(
    g:nanny
)

SRCDIR(infra/nanny/vendor/object-validator/tests)

TEST_SRCS(
    test_basic_types.py
    test_collections.py
    test_validate.py
)

PEERDIR(
    contrib/python/mock
    infra/nanny/vendor/object-validator
)

END()
