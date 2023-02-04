OWNER(g:tools-wiki)

INCLUDE(${ARCADIA_ROOT}/antiadblock/postgres_local/recipe/recipe.inc)
INCLUDE(${ARCADIA_ROOT}/music/recipes/mongodb/recipe.inc)
INCLUDE(${ARCADIA_ROOT}/intranet/wiki/recipes/redis/recipe.inc)
ENV(PYTEST_ADDOPTS=-l -p no:warnings -s -q)
ENV(DJANGO_SETTINGS_MODULE=wiki.test_settings)
ENV(IS_TESTS=1)
ENV(ARC=1)
ENV(PYTHONWARNINGS=ignore)
ENV(USER=user)
SIZE(MEDIUM)

PEERDIR(
    intranet/wiki/src
    intranet/wiki/tests
)

FORK_TESTS()

DATA(
    arcadia/intranet/wiki/tests/wiki_tests/test_data/cassettes
)
