PACKAGE()
OWNER(g:kernel)
INCLUDE(../script/config/config.inc)
INCLUDE(${ARCADIA_ROOT}/infra/environments/lib/image.inc)
END()

RECURSE_FOR_TESTS(test)
