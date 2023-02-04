DEPENDS(
    maps/infopoint/takeout/tests/ytrecipe
    yt/packages/latest
    contrib/tools/python
)

DATA(
    arcadia/maps/infopoint/takeout/tests/ytrecipe/cypress
)

USE_RECIPE(
    maps/infopoint/takeout/tests/ytrecipe/yt_recipe
)
