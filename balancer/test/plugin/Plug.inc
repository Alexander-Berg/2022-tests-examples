DATA(
    arcadia/balancer/test/plugin/certs/data
)

SET(NONE_VALUE none)

DEFAULT(BUILD_DEPS yes)

IF (BUILD_DEPS)
    DEPENDS(
        contrib/libs/openssl/apps
    )
ENDIF()

PEERDIR(
    balancer/test/plugin/backend
    balancer/test/plugin/certs
    balancer/test/plugin/connection
    balancer/test/plugin/context
    balancer/test/plugin/dnsfake
    balancer/test/plugin/fs
    balancer/test/plugin/logger
    balancer/test/plugin/options
    balancer/test/plugin/port
    balancer/test/plugin/process
    balancer/test/plugin/registrar
    balancer/test/plugin/resource
    balancer/test/plugin/server
    balancer/test/plugin/settings
    balancer/test/plugin/static
    balancer/test/plugin/stream
    balancer/test/plugin/sync
)

DEFAULT(TCPDUMP_PLUGIN no)
IF (TCPDUMP_PLUGIN)
    PEERDIR(
        balancer/test/plugin/tcpdump
    )
ENDIF()

DEFAULT(AWACS_PLUGIN no)
DEFAULT(AWACS_CONFIG_PLUGIN no)
DEFAULT(L7HEAVY_PROJECT ${NONE_VALUE})
IF (AWACS_PLUGIN OR AWACS_CONFIG_PLUGIN)
    PEERDIR(
        balancer/test/plugin/awacs_config
    )

    IF (AWACS_PLUGIN)
        PEERDIR(
            balancer/test/plugin/awacs
        )
        DATA(arcadia/balancer/test/plugin/awacs/scripts)
    ENDIF()

    IF (AWACS_NAMESPACE)
        IF (BUILD_DEPS)
            DEPENDS(infra/awacs/awacsctl2/bin)
            DATA(
                arcadia/${AWACS_NAMESPACE}
                arcadia/infra/awacs/templates
            )
        ENDIF()
    ENDIF()
    IF (L7HEAVY_PROJECT)
        IF (BUILD_DEPS)
            DEPENDS(gencfg/custom_generators/balancer_gencfg/utils)
        ENDIF()
    ELSE()
        SET(L7HEAVY_PROJECT ${NONE_VALUE})
    ENDIF()
ELSE()
    SET(AWACS_NAMESPACE ${NONE_VALUE})
ENDIF()

DEFAULT(BALANCER_PLUGIN no)
IF (BALANCER_PLUGIN)
    PEERDIR(
        balancer/test/plugin/balancer
    )
    IF (BUILD_DEPS)
        IF (NOT USE_NGINX)
            DEPENDS(balancer/daemons/balancer)
        ELSE()
            DEPENDS(nginx/bin)
        ENDIF()
    ENDIF()
ENDIF()

DEFAULT(CACHEDAEMON_PLUGIN no)
IF (CACHEDAEMON_PLUGIN)
    PEERDIR(balancer/test/plugin/cachedaemon)
    IF (BUILD_DEPS)
        DEPENDS(web/daemons/cached)
    ENDIF()
ENDIF()

DEFAULT(DOLBILO_PLUGIN no)
IF (DOLBILO_PLUGIN)
    PEERDIR(balancer/test/plugin/dolbilo)
    DATA(arcadia/balancer/test/plugin/dolbilo/data)
    IF (BUILD_DEPS)
        DEPENDS(tools/dolbilo/executor)
    ENDIF()
ENDIF()

CONFIGURE_FILE(
    ${ARCADIA_ROOT}/balancer/test/plugin/build_vars.json.in
    ${BINDIR}/balancer_test_plugin_build_vars.json
)

RESOURCE(
    ${BINDIR}/balancer_test_plugin_build_vars.json /balancer/test/plugin/build_vars.json
)
