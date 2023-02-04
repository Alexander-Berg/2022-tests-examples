USE_RECIPE(billing/configdepot/tests/runserver/recipe/recipe)

DATA(
    arcadia/billing/configdepot/config/dev.yaml
)

DEPENDS(
    billing/configdepot/tests/runserver/recipe
    billing/configdepot/cmd/runserver
)
