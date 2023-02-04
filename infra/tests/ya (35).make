PY3TEST()

OWNER(g:awacs)

TEST_SRCS(
    test_main.py
)

PEERDIR(
    infra/awacs/tools/awacsemtool2
)

NO_CHECK_IMPORTS()

END()
