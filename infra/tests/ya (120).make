PY2TEST()

OWNER(g:netmon)

SIZE(MEDIUM)

TEST_SRCS(
    conftest.py
    test_aggregation.py
)

DEPENDS(
    infra/netmon/aggregator
    infra/netmon/slicer
    infra/netmon/agent
)

PEERDIR(
    contrib/python/requests
    contrib/python/tornado/tornado-4
)

DATA(
    # topology
    sbr://408085979
    # binaries
    sbr://249102634
    sbr://44769880
    sbr://83142198
)

REQUIREMENTS(ram:32)

END()
