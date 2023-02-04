INCLUDE(${ARCADIA_ROOT}/kikimr/public/tools/ydb_recipe/recipe.inc)

DATA(arcadia/billing/hot/diod/ydb)

USE_RECIPE(kikimr/public/tools/ydb_recipe --suppress-version-check)

INCLUDE(${ARCADIA_ROOT}/billing/hot/diod/tests/runserver/recipe/recipe.inc)
