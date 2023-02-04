EXECTEST()

OWNER(g:runtime-infra)

RUN (
    NAME version
    builder --version
)

DEPENDS (
    infra/reconf_juggler/examples/complex/bin
)

END()
