EXECTEST()

OWNER(g:runtime-infra)

RUN (
    NAME help
    jconv --help
)

DEPENDS (
    infra/reconf_juggler/tools/jconv/bin
)

END()
