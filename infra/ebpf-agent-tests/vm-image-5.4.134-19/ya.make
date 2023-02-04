PACKAGE()
OWNER(g:kernel)
INCLUDE(../script/config/config-5.4.134-19.inc)
INCLUDE(${ARCADIA_ROOT}/infra/environments/lib/image.inc)
END()

RECURSE_FOR_TESTS(test)
