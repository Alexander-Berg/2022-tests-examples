PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    test_cms_reports.py
    test_failure_report.py
)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/tests/lib
    library/python/resource
)

NO_DOCTESTS()

END()
