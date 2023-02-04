USE_RECIPE(payplatform/fes/collector/tests/runserver/recipe/recipe)

DATA(
    arcadia/payplatform/fes/collector/config/dev.yaml
)

DEPENDS(
    payplatform/fes/collector/tests/runserver/recipe
    payplatform/fes/collector/cmd/runserver
)
