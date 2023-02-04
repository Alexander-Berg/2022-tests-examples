EXECTEST()

OWNER(g:runtime-infra)

RUN (
    NAME help
    jdiff --help
)

DEPENDS (
    infra/reconf_juggler/tools/jdiff/bin
)

END()
