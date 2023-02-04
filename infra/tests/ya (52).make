PY2TEST()

OWNER(g:deploy)

PEERDIR(
    infra/deploy/tools/yd_migrate/lib
    library/python/init_log
)

TEST_SRCS(
    init.py
    test_nanny.py
    test_qloud.py
)

END()
