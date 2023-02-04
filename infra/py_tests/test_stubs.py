import nanny_rpc_client

from infra.nanny.yp_lite_api.py_stubs import endpoint_sets_api_stub
from infra.nanny.yp_lite_api.py_stubs import pod_sets_api_stub


def test_endpoint_sets_api_stub():
    c = nanny_rpc_client.RetryingRpcClient(rpc_url='http://fake-url')
    endpoint_sets_api_stub.YpLiteUIEndpointSetsServiceStub(c)


def test_pod_sets_api_stub():
    c = nanny_rpc_client.RetryingRpcClient(rpc_url='http://fake-url')
    pod_sets_api_stub.YpLiteUIPodSetsServiceStub(c)
