PACKAGE()
OWNER(g:kernel)
SET(ENV_VIRT_MODE vm)
INCLUDE(../script/config/config-5.4.187-35.2.inc)
INCLUDE(${ARCADIA_ROOT}/infra/environments/lib/layer.inc)
END()

RECURSE_FOR_TESTS(test)
