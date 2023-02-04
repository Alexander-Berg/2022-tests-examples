PY3TEST()

OWNER(g:runtime-infra)

TEST_SRCS(
    __init__.py
    test_collapsible_tree.py
    test_d3js.py
)

PEERDIR(
    infra/reconf_juggler
    infra/reconf_juggler/util/d3js
    infra/reconf_juggler/util/d3js/collapsible_tree
)

END()
