INCLUDE(${ARCADIA_ROOT}/billing/library/recipes/pg/recipe.inc)

DATA(arcadia/billing/configshop/postgre)

USE_RECIPE(
    billing/library/recipes/pg/pg
    billing/configshop/postgre -n configshop
)

INCLUDE(${ARCADIA_ROOT}/billing/configshop/tests/recipes/runserver/recipe.inc)
INCLUDE(${ARCADIA_ROOT}/billing/configshop/tests/recipes/tasks/recipe.inc)
INCLUDE(${ARCADIA_ROOT}/billing/configshop/tests/recipes/infratasks/recipe.inc)
