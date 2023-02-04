# Tests are actually declared in peerdir library
PEERDIR(infra/porto/tests/ut)

INCLUDE(${ARCADIA_ROOT}/infra/porto/build/env.inc)

DEPENDS(
    ${PORTO_BUILD_DIR}
)

REQUIREMENTS(
    cpu:4
    ram:8
    disk_usage:40
    network:full
)

DEFAULT(PRECISE_LAYER_RESOURCE 1314402657)
DEFAULT(PRECISE_LAYER_TGZ ubuntu-precise.tgz)
DEFAULT(XENIAL_LAYER_RESOURCE 1458058184)
DEFAULT(XENIAL_LAYER_TGZ ubuntu-xenial.tar.zst)
DEFAULT(DOCKER_LAYER_RESOURCE 1801421485)
DEFAULT(DOCKER_LAYER_TGZ docker-xenial.tar.gz)

DATA(
    sbr://${PRECISE_LAYER_RESOURCE}
    sbr://${XENIAL_LAYER_RESOURCE}
    sbr://${DOCKER_LAYER_RESOURCE}
)

SIZE(MEDIUM)
