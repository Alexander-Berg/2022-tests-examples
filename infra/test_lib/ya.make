PY23_TEST()

OWNER(torkve)

TEST_SRCS(test.py)

TIMEOUT(25)

PEERDIR(infra/netlibus/pylib)

REQUIREMENTS(network:full)

END()
