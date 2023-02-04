PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    __init__.py
    test_jsdk.py
)

PEERDIR(
    infra/reconf_juggler
)

END()
