OWNER(g:awacs)

PY3_LIBRARY(awtest)

PY_SRCS(
    NAMESPACE awtest

    __init__.py
    api.py
    balancer/__init__.py
    balancer/balancer.py
    core.py
    ctl_runner.py
    https_adapter.py
    mocks/__init__.py
    mocks/abc_client.py
    mocks/appconfig.py
    mocks/certificator.py
    mocks/dns_manager_client.py
    mocks/httpbin_server.py
    mocks/idm_client.py
    mocks/l3mgr_client.py
    mocks/mongo.py
    mocks/nanny_client.py
    mocks/nanny_rpc_client.py
    mocks/ports.py
    mocks/python_server.py
    mocks/resolver.py
    mocks/sandbox_client.py
    mocks/staff_client.py
    mocks/startrek.py
    mocks/yasm_client.py
    mocks/yav.py
    mocks/yp_lite_client.py
    mocks/yp_sd.py
    mocks/zookeeper.py
    mocks/racktables.py
    network.py
    pb.py
    wrappers.py
    xdist.py
    conftest.py
    l3util.py
    l7util.py
    dns_record_util.py
)

PEERDIR(
    contrib/python/mock
    contrib/python/pytz
    contrib/python/freezegun
    contrib/python/psutil
    contrib/python/cachetools
    contrib/python/filelock
    contrib/python/netifaces
    infra/swatlib
    infra/awacs/vendor/awacs
    infra/nanny/sepelib/subprocess
)

END()

RECURSE(
    mocks/httpbin_server_app
    mocks/sdstub_app
)
