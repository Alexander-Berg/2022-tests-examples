PY2TEST()

OWNER(
    g:deploy
    g:deploy-orchestration
)

TEST_SRCS(
    test_deploy_ticket_maker.py
    test_release_matcher.py
)

PEERDIR(
    yp/python/local
    contrib/python/pytest
    contrib/python/mock
    infra/release_controller/src
    infra/release_controller/tests/helpers
)

END()
