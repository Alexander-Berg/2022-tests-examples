PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    __init__.py
    test_aggregators.py
    test_autotags.py
    test_builder.py
    test_checks.py
    test_checks_docs.py
    test_declarative_builder.py
    test_flaps.py
    test_interface.py
    test_levels.py
    test_opt_factory.py
    test_proxy_builder.py
    test_trees.py
    test_workflow.py
)

PEERDIR(
    infra/reconf_juggler
    infra/reconf_juggler/trees
    infra/reconf_juggler/util
)

END()
