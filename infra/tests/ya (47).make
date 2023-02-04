PY3TEST()

IF (NOT NO_FORK_TESTS)
    FORK_SUBTESTS()
ENDIF()

OWNER(g:cauth)

ENV(PYTEST_ADDOPTS=-l --nomigrations -vvv)
ENV(DJANGO_SETTINGS_MODULE=infra.cauth.server.public.settings)
ENV(YENV_TYPE=development.unittest)
ENV(YENV_NAME=intranet)
ENV(PG_LOCAL_DATABASE_INIT_TIMEOUT_SEC=180)

# PostgreSQL

USE_RECIPE(antiadblock/postgres_local/recipe/recipe --port 5432 --user postgres --db_name postgres)

INCLUDE(${ARCADIA_ROOT}/antiadblock/postgres_local/recipe/postgresql_bin.inc)

DEPENDS(
    antiadblock/postgres_local/recipe
)

TEST_SRCS(
    __init__.py
    conftest.py
    api/__init__.py
    api/test_group_serverusers.py
    api/test_info.py
    api/test_keys.py
    api/test_passwd.py
    api/test_rules.py
    api/test_sources.py
    api/test_sudoers.py
    utils/__init__.py
    utils/client.py
    utils/create.py
)

SIZE(MEDIUM)

PEERDIR(
    contrib/python/factory-boy
    contrib/python/pytest-django
    contrib/python/mock

    infra/cauth/server/public
)

NO_CHECK_IMPORTS([*])

END()
