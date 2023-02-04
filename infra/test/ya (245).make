PY2TEST()

OWNER(g:netmon)

TEST_SRCS(
    test_build_topology.py
)

PEERDIR(
    infra/netmon/build_topology/lib
    contrib/python/deepdiff
)

END()

