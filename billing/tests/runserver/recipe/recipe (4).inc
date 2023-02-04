USE_RECIPE(billing/template-project/tests/runserver/recipe/recipe)

DATA(
    arcadia/billing/template-project/config/dev.yaml
)

DEPENDS(
    billing/template-project/tests/runserver/recipe
    billing/template-project/cmd/runserver
)
