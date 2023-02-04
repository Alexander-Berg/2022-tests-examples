EXECTEST()

OWNER(g:runtime-infra)

RUN (
    NAME version
    quorum --version
)

DEPENDS (
    infra/reconf_juggler/util/quorum/bin
)

END()
