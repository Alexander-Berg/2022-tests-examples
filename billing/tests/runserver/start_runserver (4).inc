INCLUDE(${ARCADIA_ROOT}/billing/library/recipes/pg/recipe.inc)

DATA(arcadia/billing/template-project/postgre)

USE_RECIPE(
    billing/library/recipes/pg/pg
    billing/template-project/postgre -n template-project
)

INCLUDE(${ARCADIA_ROOT}/billing/template-project/tests/runserver/recipe/recipe.inc)
