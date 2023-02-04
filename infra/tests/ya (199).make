EXECTEST()

OWNER(g:runtime-infra)

RUN (
    NAME version
    builder --version
)

DEPENDS (
    infra/rtc/juggler/reconf/builders/projects/sysdev_hbf_drops/bin
)

END()
