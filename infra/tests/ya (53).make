PY3TEST()

OWNER(torkve)

INCLUDE(../../../yp/python/ya_programs.make.inc)

PEERDIR(
    infra/deploy_ci/approve_location/impl
    infra/deploy_ci/create_release/impl

    infra/libs/local_yp
    contrib/python/pytest
)

TEST_SRCS(
    conftest.py
    test_create_release.py
)

REQUIREMENTS(
    cpu:4
    ram_disk:4
)
TIMEOUT(600)
SIZE(MEDIUM)

END()
