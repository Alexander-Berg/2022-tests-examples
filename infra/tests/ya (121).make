PY2TEST()

OWNER(g:oops)

PEERDIR(
    library/python/flask
    infra/oops-agent/src/lib
)

TEST_SRCS(
    test_agent.py
    test_ewma.py
    test_modules.py
)

RESOURCE(
    df1.out /df1.out
    df2.out /df2.out
    df_tsuga.out /df_tsuga.out
    du.out /du.out
    dmidecode.out /dmidecode.out
)

INCLUDE(${ARCADIA_ROOT}/library/recipes/qemu_kvm/recipe.inc)
END()
