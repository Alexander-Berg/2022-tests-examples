# -*- coding: utf-8 -*-

import requests
from balancer.test.util.sd import SDCacheConfig, api


def make_sd_request(cluster_name, endpoint_set_id, client_name):
    request = api.TReqResolveEndpoints()
    request.cluster_name = cluster_name
    request.endpoint_set_id = endpoint_set_id
    request.client_name = client_name
    return request


def perform_sd_request(backend, resolve_request):
    r = requests.post('http://localhost:{}/resolve_endpoints?'.format(backend.server_config.port), data=resolve_request.SerializeToString())
    result = api.TRspResolveEndpoints()
    result.ParseFromString(r.content)
    return result


def test_client_name(ctx):
    ctx.start_backend(SDCacheConfig(), name='backend1')

    request = make_sd_request('sas-test', 'test-service', '')
    try:
        perform_sd_request(ctx.backend1, request)
        assert False
    except requests.exceptions.ConnectionError:
        pass


def test_resolve(ctx):
    ctx.start_backend(SDCacheConfig(), name='backend1')

    request = make_sd_request('sas-test', 'test-service', 'balancer_functional_test')

    ctx.backend1.state.set_endpointset(
        request.cluster_name, request.endpoint_set_id, [{'fqdn': 'test-fqdn', 'port': 8080}])
    ctx.backend1.state.set_timestamp(12345)

    result = perform_sd_request(ctx.backend1, request)

    assert result.timestamp == 12345
    assert result.endpoint_set.endpoint_set_id == request.endpoint_set_id
    assert result.endpoint_set.endpoints[0].fqdn == 'test-fqdn'
    assert result.endpoint_set.endpoints[0].port == 8080

    ctx.backend1.state.set_endpointset(
        request.cluster_name, request.endpoint_set_id, [{'fqdn': 'test-fqdn2', 'port': 8080}])
    ctx.backend1.state.set_timestamp(12346)

    result = perform_sd_request(ctx.backend1, request)

    assert result.timestamp == 12346
    assert result.endpoint_set.endpoint_set_id == request.endpoint_set_id
    assert result.endpoint_set.endpoints[0].fqdn == 'test-fqdn2'
    assert result.endpoint_set.endpoints[0].port == 8080
