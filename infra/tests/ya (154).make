PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    test_quorum.py
)

PEERDIR(
    infra/reconf_juggler/util/quorum
)

END()
