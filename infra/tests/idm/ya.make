PY3TEST()

STYLE_PYTHON()

OWNER(g:walle)

TEST_SRCS(
    test_nodes.py
    test_project_role_managers.py
    test_project_staff_id.py
    test_push_api.py
    test_role_storage.py
    test_traversal.py
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

END()
