OWNER(g:tentacles)

EXECTEST()

RUN (
    NAME check_tentacles_config
    unified_agent --config ${ARCADIA_ROOT}/infra/rtc_sla_tentacles/backend/conf/unified_agent_config.yml check-config
)

DEPENDS (
    logbroker/unified_agent/bin
)

DATA(
    arcadia/infra/rtc_sla_tentacles/backend/conf/unified_agent_config.yml
)

END()
