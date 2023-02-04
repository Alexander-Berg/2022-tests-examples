PY2TEST()

OWNER(g:golovan)

TEST_SRCS(
    __init__.py
    test_bindings.py
)

DEPENDS(
    infra/yasm/neh_bindings/python
)

PEERDIR(
    contrib/python/pytest-tornado
    contrib/python/futures
)

END()
