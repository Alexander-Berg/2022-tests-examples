PACKAGE()
OWNER(g:kernel)
INCLUDE(../script/config/config-5.4.187-35.2.inc)
INCLUDE(${ARCADIA_ROOT}/infra/environments/lib/image.inc)
END()

RECURSE_FOR_TESTS(test)
