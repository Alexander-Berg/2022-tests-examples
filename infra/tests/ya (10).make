PY2TEST()

SIZE(MEDIUM)

OWNER(g:golovan)

TEST_SRCS(
    test_collector.py
)

DEPENDS(
    infra/yasm/collector/python
)

DATA(
    sbr://532362406
    sbr://532362250
    sbr://1065808771
)

END()
