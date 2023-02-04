PY23_TEST()

OWNER(g:tasklet)

TEST_SRCS(
    test_cons.py
    test_helper.py
    test_server_setup.py
)

PY_SRCS(
    common.py
)

PEERDIR(
    contrib/python/six

    tasklet/api
    tasklet/runtime
    tasklet/runtime/python
    tasklet/domain
    tasklet/tests/proto
    tasklet/services/server
)

REQUIREMENTS(ram:12)

END()
