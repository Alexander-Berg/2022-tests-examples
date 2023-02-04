PY2TEST()

OWNER(
    g:deploy
    g:deploy-orchestration
)

TEST_SRCS(
    test_deploy_progress_maker.py
    test_patch_applied_checker.py
    test_patch_progress_maker.py
)

PEERDIR(
    yp/python/local
    contrib/python/pytest
    contrib/python/mock
    infra/release_status_controller/src
    infra/release_status_controller/tests/helpers
)

END()
