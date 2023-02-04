EXECTEST()

OWNER(g:runtime-infra)

RUN (
    NAME version
    moncover --help
)

DEPENDS (
    infra/rtc/juggler/reconf/moncover/bin
)

END()
