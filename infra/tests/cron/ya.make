PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    test_infiniband_info_sync.py
    test_shadow_hosts_sync.py
    test_tier_sync.py
)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/proto
    infra/walle/server/tests/lib
    library/python/resource
)

NO_DOCTESTS()

END()
