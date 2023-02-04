PY23_TEST()

OWNER(g:walle)

TEST_SRCS(
    test_basic_types.py
    test_collections.py
    test_validate.py
)

PEERDIR(
    infra/walle/server/contrib/object-validator
)

END()
