INCLUDE(${ARCADIA_ROOT}/billing/library/recipes/pg/recipe.inc)

DATA(arcadia/billing/hot/accounts/postgre)

USE_RECIPE(billing/library/recipes/pg/pg billing/hot/accounts/postgre -n accounts)

INCLUDE(${ARCADIA_ROOT}/billing/hot/accounts/tests/recipes/api/recipe/recipe.inc)
