DEFAULT(FAAS_RECIPE_PARAMS "")

DEPENDS(
    billing/hot/faas/python/faas/tests/recipes/faas/recipe
    ${FAAS_DEPEND}
)

USE_RECIPE(billing/hot/faas/python/faas/tests/recipes/faas/recipe/recipe --bin-path ${FAAS_DEPEND} ${FAAS_RECIPE_PARAMS})
