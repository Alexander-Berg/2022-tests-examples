USE_RECIPE(billing/configshop/tests/recipes/runserver/recipe)

DATA(
    arcadia/billing/configshop/configs/core
)

DEPENDS(
    billing/configshop/tests/recipes/runserver
    billing/configshop/cmd/runserver
)
