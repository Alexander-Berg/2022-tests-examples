PEERDIR(infra/porto/api_py/test)
INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)
SIZE(MEDIUM)
TIMEOUT(120)
REQUIREMENTS(
    network:full
)
