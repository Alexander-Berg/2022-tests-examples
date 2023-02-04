PY2TEST()

OWNER(g:hostman)

TEST_SRCS(
    test_apt.py
    test_kernel.py
    test_server_info.py
    test_yasm.py
    test_manager.py
    test_virtual.py
    test_hostctl.py
    test_salt_component.py
)

DATA(
    arcadia/infra/ya_salt/lib/components/tests/lui-config.json
)
PEERDIR(
    contrib/python/mock

    infra/ya_salt/lib
)

NO_CHECK_IMPORTS(
)

END()
