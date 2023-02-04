EXECTEST()

OWNER(g:netmon)

RUN (
    NAME print-help
    netmon-agent -h
)

RUN (
    NAME run-once
    netmon-agent --freeze-topology --debug run_once
)

DEPENDS (
    infra/netmon/agent
)

DATA(
    sbr://211424988
)

END()
