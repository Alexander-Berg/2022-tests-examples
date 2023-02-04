PY3TEST()

OWNER(g:rtc-sysdev)

TEST_SRCS(test.py)

SET(RECIPE_TMP_FILE_NAME nvme.img)
SET(RECIPE_TMP_FILE_SIZE 10485760) # 10M
INCLUDE(${ARCADIA_ROOT}/infra/diskmanager/tests/recipes/tmp_file/recipe.inc)

END()
