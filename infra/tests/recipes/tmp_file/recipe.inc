# Create file in /tmp with given name and size on start and remove it on stop

DEFAULT(RECIPE_TMP_FILE_NAME tmp_file.recipe)
DEFAULT(RECIPE_TMP_FILE_SIZE 0)

DEPENDS(infra/diskmanager/tests/recipes/tmp_file)
USE_RECIPE(infra/diskmanager/tests/recipes/tmp_file/tmp_file --name $RECIPE_TMP_FILE_NAME --size $RECIPE_TMP_FILE_SIZE)
