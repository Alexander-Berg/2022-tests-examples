OWNER(torkve g:skynet)

PY23_LIBRARY()

TEST_SRCS(
    test_loghandler.py
)

PEERDIR(
    infra/logger
)

END()
RECURSE_FOR_TESTS(py2 py3 mypy_tests)
