USE_RECIPE(payplatform/fes/fes/tests/runserver/recipe/recipe)

DATA(
    arcadia/payplatform/fes/fes/config/dev/dev.yaml
)

DEPENDS(
    payplatform/fes/fes/tests/runserver/recipe
    payplatform/fes/fes/cmd/runserver
)
