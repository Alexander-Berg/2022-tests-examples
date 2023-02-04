PY2TEST()

OWNER(
    osidorkin
    g:golovan
)


TEST_SRCS(
    conftest.py
    test_aggregation.py
    test_histdb.py
    test_history_api.py
    test_msgpack.py
    test_pipeline.py
    test_ugram.py
    test_unistat.py
    test_window_record.py
    test_yasm_conf.py
    test_node_agent.py
    test_re2_validator.py
)

DEPENDS(
    infra/yasm/zoom/python
)

PEERDIR(
    infra/yasm/interfaces
    infra/node_agent/api/proto
    contrib/python/msgpack
    contrib/python/python-snappy
)

DATA(
    sbr://1513629222
)

END()
