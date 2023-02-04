INCLUDE(${ARCADIA_ROOT}/billing/library/recipes/pg/recipe.inc)

DATA(arcadia/{{cookiecutter.project_path}}/postgre)

USE_RECIPE(
    billing/library/recipes/pg/pg
    {{cookiecutter.project_path}}/postgre -n {{cookiecutter.project_name}}
)

INCLUDE(${ARCADIA_ROOT}/{{cookiecutter.project_path}}/tests/runserver/recipe/recipe.inc)
