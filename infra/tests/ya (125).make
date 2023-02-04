OWNER(torkve)

PY2TEST()

TEST_SRCS(
    test_auth_mode.py
    test_portotools.py
    test_futime.py
    test_iss.py
    test_shell.py
    test_ssh.py
)

RESOURCE(
    iss.json /iss.json
    iss-config.json /iss-config.json
    iss-yp.json /iss-yp.json
    iss-yp-hard.json /iss-yp-hard.json
    iss-yp-hard-acl.json /iss-yp-hard-acl.json
)

TIMEOUT(25)

PEERDIR(
    infra/portoshell
    contrib/python/mock
    library/python/resource
)

DATA(
    arcadia/infra/portoshell/tests
)

TAG(ya:not_autocheck)
FORK_TEST_FILES()
FORK_TESTS()
FORK_SUBTESTS()
NO_CHECK_IMPORTS()

END()
