PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    supported_platforms/conftest.py
    supported_platforms/test_asus.py
    supported_platforms/test_gigabyte.py
    supported_platforms/test_quanta.py
    supported_platforms/test_supermicro.py
    test_platform.py
    test_platform_manager.py
)

SIZE(MEDIUM)

TAG(sb:LINUX_XENIAL)

PY_SRCS(conftest.py)

INCLUDE(${ARCADIA_ROOT}/infra/walle/server/recipes/mongodb/recipe.inc)

PEERDIR(
    contrib/python/deepdiff
    contrib/python/mock
    infra/walle/server/walle
    infra/walle/server/tests/lib
    library/python/resource
)

END()
