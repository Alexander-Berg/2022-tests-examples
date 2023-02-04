DEPENDS(
    ads/quality/adv_machine/cm/tables_tool
    ads/quality/adv_machine/content_merger/cm_robot/bins
    ads/quality/adv_machine/content_merger/cm_robot/scripts
)

DATA(
    arcadia/ads/quality/adv_machine/config/adv-content-merger
    sbr://2510145131 # tables_sample
)

INCLUDE(${ARCADIA_ROOT}/ads/quality/adv_machine/lib/test_cm/recipes/medium.inc)
