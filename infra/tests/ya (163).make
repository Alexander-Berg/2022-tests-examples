EXECTEST()

OWNER(g:hostman)

RUN (
    NAME help
    certctl --help
)

DEPENDS (
    infra/rtc/certman/bin
)

END()
