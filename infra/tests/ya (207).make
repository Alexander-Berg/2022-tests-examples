EXECTEST()

OWNER(g:runtime-infra)

RUN (
    NAME version
    builder --version
)

DEPENDS (
    infra/rtc/juggler/reconf/builders/projects/yp_masters/bin
)

END()
