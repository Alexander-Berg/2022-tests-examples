PY23_TEST()

OWNER(
    g:deploy
    g:deploy-orchestration
)

INCLUDE(../../../../yp/python/ya_programs.make.inc)

TEST_SRCS(
    conftest.py
    test_stage.py
)

DATA(
    arcadia/infra/dctl/tests/func
)

DEPENDS(
    infra/dctl/bin
)

PEERDIR(
    yp/python/local
    infra/libs/local_yp
    contrib/python/pytest
    contrib/python/mock
    infra/dctl/src
    infra/dctl/tests/helpers

)

REQUIREMENTS(
    cpu:4
    ram_disk:4
)

SIZE(MEDIUM)

TIMEOUT(600)

END()
