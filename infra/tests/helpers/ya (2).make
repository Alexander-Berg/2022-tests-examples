PY23_LIBRARY()

OWNER(
    g:deploy
    g:deploy-orchestration
)

PY_SRCS(
    __init__.py
    helpers.py
)

PEERDIR(
    yt/yt/python/yt_yson_bindings
    yp/python/client
    yp/yp_proto/yp/client/api/proto
)

END()
