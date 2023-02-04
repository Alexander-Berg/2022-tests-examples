EXECTEST()

OWNER(g:runtime-infra)

RUN (
    NAME help
    jdump --help
)

DEPENDS (
    infra/reconf_juggler/tools/jdump/bin
)

END()
