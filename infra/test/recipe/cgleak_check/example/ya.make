PY3TEST()

OWNER(g:kernel g:rtc-sysdev)

TEST_SRCS(test.py)

INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)

INCLUDE(${ARCADIA_ROOT}/infra/kernel/test/recipe/cgleak_check/recipe.inc)

END()
