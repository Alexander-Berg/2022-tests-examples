USE_RECIPE(billing/configshop/tests/recipes/tasks/recipe)

DATA(
    arcadia/billing/configshop/configs/core
    arcadia/billing/configshop/configs/processor
)

DEPENDS(
    billing/configshop/tests/recipes/tasks
    billing/configshop/cmd/runtasks
)
