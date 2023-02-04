USE_RECIPE(billing/hot/accounts/tests/recipes/api/recipe/recipe)

DATA(
    arcadia/billing/hot/accounts/configs/api/dev.yaml
    arcadia/billing/hot/accounts/configs/settings/dev.yaml
)

DEPENDS(
    billing/hot/accounts/tests/recipes/api/recipe
    billing/hot/accounts/cmd/api
)
