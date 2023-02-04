PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    __init__.py
    conftest.py
    mocks.py
    test_slayer_dns.py
    test_dns_fixer.py
    test_rurikk_dns.py
)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/tests/lib
    library/python/resource
)

NO_DOCTESTS()

END()
