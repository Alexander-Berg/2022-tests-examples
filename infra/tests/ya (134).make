EXECTEST()

OWNER(g:runtime-infra)

RUN (
    NAME default
    cortesian
)

DEPENDS (
    infra/reconf/examples/cortesian/bin
)

END()
