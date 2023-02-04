DEPENDS(
    ads/bsyeti/caesar/tests/recipe/yt
    yt/packages/latest
)

DEFAULT(YT_REPLICAS primary)
SET(YT_ACL_GROUPS bigb,idm-group:163251)

USE_RECIPE(ads/bsyeti/caesar/tests/recipe/yt/yt_recipe -R ${YT_REPLICAS} -G ${YT_ACL_GROUPS})
