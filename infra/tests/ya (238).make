PY23_TEST()

OWNER(torkve)

TEST_SRCS(
    test_parser.py
    test_validation.py
)

PEERDIR(
    infra/skylib/openssh_krl
    skynet/library/auth
)

FORK_SUBTESTS()

END()
