PY2_LIBRARY()

OWNER(
    frolstas
)

PEERDIR(
    contrib/python/gevent
    contrib/python/paramiko
    contrib/python/requests

    infra/nanny/nanny_rpc_client
    infra/nanny/sepelib/core
    infra/qyp/proto_lib
)

PY_SRCS(
    runner.py
    lib/vmproxy_client.py
    lib/yasmutil.py
    lib/yp_client.py
)

END()
