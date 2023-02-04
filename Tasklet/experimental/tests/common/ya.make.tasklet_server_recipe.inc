DEPENDS(
    tasklet/experimental/cmd/server
    tasklet/registry/common
)

#ENV(YDB_KQP_RESULT_ROWS_LIMIT=10000)
#ENV(YDB_DEFAULT_LOG_LEVEL="CRIT")
#ENV(YDB_ADDITIONAL_LOG_CONFIGS="GRPC_SERVER:DEBUG,TICKET_PARSER:WARN")
ENV(YDB_USE_IN_MEMORY_PDISKS=true)
ENV(YDB_TYPE=LOCAL)

INCLUDE(../../../../kikimr/public/tools/ydb_recipe/recipe_stable.inc)
