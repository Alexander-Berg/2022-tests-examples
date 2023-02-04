USE_RECIPE(billing/configshop/tests/recipes/testintegration/recipe)

DATA(
    arcadia/billing/configshop/configs/core
    arcadia/billing/configshop/configs/processor
)

DEPENDS(
    billing/configshop/tests/recipes/testintegration
    billing/configshop/cmd/testintegration
)
