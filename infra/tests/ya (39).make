OWNER(g:capacity_planning)

PY3TEST()

PEERDIR(
    infra/capacity_planning/utils/quota_mover/src

    contrib/python/mock
)

TEST_SRCS(
    unit/lib/test_quota_operations.py
)

SIZE(MEDIUM)

ALL_PY_SRCS()

NO_DOCTESTS()

NO_CHECK_IMPORTS()

END()
