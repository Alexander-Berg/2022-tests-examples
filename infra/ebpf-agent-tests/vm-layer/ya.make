PACKAGE()
OWNER(g:kernel)
SET(ENV_VIRT_MODE vm)
INCLUDE(../script/config/config.inc)
INCLUDE(${ARCADIA_ROOT}/infra/environments/lib/layer.inc)
END()

RECURSE_FOR_TESTS(test)
