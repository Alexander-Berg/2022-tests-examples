INCLUDE_ONCE(yes)

ENV(YENV_TYPE=development)
SET(FAAS_RECIPE_PARAMS "--namespaces aero bnpl bnpl_income disk games mail_pro music music_mediaservices oplata taxi_light")
SET(FAAS_DEPEND "billing/hot/tests/recipes/calculators")

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
