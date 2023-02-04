PY3_LIBRARY()

OWNER(g:tasklet)

PY_SRCS(
    __init__.py
    conftest.py
    models.py
    server_mock.py
    utils.py
)

PEERDIR(
    contrib/libs/grpc/python
    contrib/python/PyYAML
    contrib/python/Jinja2

    library/python/pytest
    ydb/public/sdk/python

    tasklet/api/v2
)

END()
