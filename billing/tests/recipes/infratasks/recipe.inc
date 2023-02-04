USE_RECIPE(billing/configshop/tests/recipes/infratasks/recipe)

DATA(
    arcadia/billing/configshop/configs/core
    arcadia/billing/configshop/configs/processor-infra
)

DEPENDS(
    billing/configshop/tests/recipes/infratasks
    billing/configshop/cmd/runinfratasks
)
