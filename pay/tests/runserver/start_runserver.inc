INCLUDE(${ARCADIA_ROOT}/kikimr/public/tools/ydb_recipe/recipe_stable.inc)
INCLUDE(${ARCADIA_ROOT}/kikimr/public/tools/sqs_recipe/recipe_stable.inc)

DATA(arcadia/payplatform/fes/fes/ydb)

INCLUDE(${ARCADIA_ROOT}/payplatform/fes/fes/tests/runserver/recipe/recipe.inc)
