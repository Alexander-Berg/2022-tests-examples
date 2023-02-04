USE_RECIPE({{cookiecutter.project_path}}/tests/runserver/recipe/recipe)

DATA(
    arcadia/{{cookiecutter.project_path}}/configs/core/dev.yaml
)

DEPENDS(
    {{cookiecutter.project_path}}/tests/runserver/recipe
    {{cookiecutter.project_path}}/cmd/runserver
)
