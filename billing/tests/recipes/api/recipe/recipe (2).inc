DEFAULT(PROCESSOR_RECIPE_PARAMS "")

DATA(
    arcadia/billing/hot/processor/configs/dev
)
DEPENDS(
    billing/hot/processor/tests/recipes/api/recipe
    billing/hot/processor
)
USE_RECIPE(billing/hot/processor/tests/recipes/api/recipe/recipe ${PROCESSOR_RECIPE_PARAMS})
