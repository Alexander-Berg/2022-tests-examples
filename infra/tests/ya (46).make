PY3TEST()

IF (NOT NO_FORK_TESTS)
    FORK_SUBTESTS()
ENDIF()

OWNER(g:cauth)

ENV(PYTEST_ADDOPTS=-l --nomigrations -vvv)
ENV(DJANGO_SETTINGS_MODULE=infra.cauth.server.master.settings)
ENV(YENV_TYPE=development.unittest)
ENV(YENV_NAME=intranet)
ENV(PG_LOCAL_DATABASE_INIT_TIMEOUT_SEC=180)

# PostgreSQL

USE_RECIPE(antiadblock/postgres_local/recipe/recipe --port 5432 --user postgres --db_name postgres)

INCLUDE(${ARCADIA_ROOT}/antiadblock/postgres_local/recipe/postgresql_bin.inc)
INCLUDE(${ARCADIA_ROOT}/library/recipes/s3mds/recipe.inc)

DEPENDS(
    antiadblock/postgres_local/recipe
    library/recipes/s3mds
)

TEST_SRCS(
    __init__.py
    api/__init__.py
    api/test_api_node.py
    api/test_dsts.py
    api/test_idm_hooks.py
    api/test_servers.py
    client.py
    conftest.py
    importers/__init__.py
    importers/test_batch_push_node_to_idm.py
    sources/__init__.py
    sources/test_bot.py
    sources/test_golem.py
    sources/test_hd.py
    sources/test_yp.py
    tasks/__init__.py
    tasks/test_import.py
    tasks/test_import_staff.py
    tasks/test_notifications.py
    tasks/test_puncher.py
    utils.py
)

SIZE(MEDIUM)

PEERDIR(
    contrib/python/factory-boy
    contrib/python/freezegun
    contrib/python/pytest-django
    contrib/python/mock

    infra/cauth/server/master
)

NO_CHECK_IMPORTS([*])

END()
