# Запускаем YT
IF (
    NOT
    DISABLE_YT
)
    INCLUDE(${ARCADIA_ROOT}/mapreduce/yt/python/recipe/recipe.inc)
ENDIF()

# Запускаем accounter
IF (
    NOT
    DISABLE_ACCOUNTER
)
    INCLUDE(${ARCADIA_ROOT}/billing/hot/accounts/tests/recipes/api/start_api.inc)
ENDIF()

# Запускаем калькулятор
IF (
    NOT
    DISABLE_FAAS
)
    INCLUDE(${ARCADIA_ROOT}/billing/hot/faas/python/faas/tests/recipes/faas/start_api.inc)
ENDIF()

# Запускаем процессор
INCLUDE(${ARCADIA_ROOT}/billing/hot/processor/tests/recipes/api/recipe/recipe.inc)
