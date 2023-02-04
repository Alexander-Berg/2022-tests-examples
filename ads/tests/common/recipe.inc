DEPENDS(
    ads/quality/adv_machine/cm/tables_tool
    ads/quality/adv_machine/tsar/cm_robot/bins
    ads/quality/adv_machine/tsar/cm_robot/scripts
    yql/udfs/common/protobuf
    ydb/library/yql/udfs/common/string
)

DATA(
    sbr://1300035919 # tables_sample
    arcadia/ads/quality/adv_machine/tsar/cm_robot/tables.json
)

INCLUDE(${ARCADIA_ROOT}/ads/quality/adv_machine/lib/test_cm/recipes/medium.inc)
