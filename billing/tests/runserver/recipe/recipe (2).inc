USE_RECIPE(billing/hot/diod/tests/runserver/recipe/recipe)

DATA(
    arcadia/billing/hot/diod/config/dev.yaml
)

DEPENDS(
    billing/hot/diod/tests/runserver/recipe
    billing/hot/diod
)
