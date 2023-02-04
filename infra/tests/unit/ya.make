PY2TEST()

OWNER(
    g:deploy
    g:deploy-orchestration
)

TEST_SRCS(
    test_ydb_logs_controller.py
    test_migration_service.py
)

PEERDIR(
    contrib/python/pytest
    contrib/python/mock
    infra/nanny/sepelib/core
    infra/dproxy/proto
    infra/dproxy/src
)

END()
