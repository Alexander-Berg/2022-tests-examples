INCLUDE(${ARCADIA_ROOT}/billing/library/recipes/pg/recipe.inc)

DATA(arcadia/billing/configdepot/postgre)

USE_RECIPE(
    billing/library/recipes/pg/pg
    billing/configdepot/postgre -n configdepot
)

INCLUDE(${ARCADIA_ROOT}/billing/configdepot/tests/runserver/recipe/recipe.inc)
