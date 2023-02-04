import requests
import pytest
from util import assert_route_schema, get_route_query, get_unistat_signals, \
    assert_routing_mode_signals, assert_handler_version_signals, assert_handler_signals
import maps.b2bgeo.test_lib.apikey_values as apikey_values


@pytest.mark.parametrize("routing_mode, is_valid", [
    (None, True),
    ("driving", True),
    ("transit", True),
    ("walking", True),
    ("truck", True),
    ("xxx", False),
    ("bicycle", False)
])
def test_routing_mode(routing_public_api_url, routing_mode, is_valid):
    url, params = get_route_query(apikey=apikey_values.ACTIVE, mode=routing_mode)
    resp = requests.get(routing_public_api_url + url, params=params)

    if is_valid:
        resp.raise_for_status()
        assert resp.headers['X-Content-Type'] == 'nosniff'
    else:
        assert resp.status_code == requests.codes.bad_request

    assert_route_schema(resp)
    assert_routing_mode_signals(routing_public_api_url, 'route', routing_mode)


@pytest.mark.parametrize("version", [None, 'v2', 'v1', 'v1.0', 'v1.0.0'])
def test_handler_versions(routing_public_api_url, version):
    url, params = get_route_query(apikey=apikey_values.ACTIVE, version=version)
    resp = requests.get(routing_public_api_url + url, params=params)
    resp.raise_for_status()

    assert_route_schema(resp)
    assert_handler_version_signals(routing_public_api_url, 'route', version)


def test_handler_signals(routing_public_api_url):
    url, params = get_route_query(apikey=apikey_values.ACTIVE)
    resp = requests.get(routing_public_api_url + url, params=params)
    resp.raise_for_status()

    assert_route_schema(resp)
    assert_handler_signals(routing_public_api_url, 'route')


def test_internal_error(routing_public_api_unreachable_driving_router_url):
    api_url = routing_public_api_unreachable_driving_router_url
    url, params = get_route_query(apikey=apikey_values.ACTIVE)
    response = requests.get(api_url + url, params=params)
    assert response.status_code == requests.codes.server_error

    actual_signals = get_unistat_signals(api_url)
    non_zero_signals = {'request_internal_error_summ', 'route-request_internal_error-driving_summ',
                        'route-routing_mode_driving_summ', 'backend_request_failed_summ'}
    for signal in non_zero_signals:
        assert actual_signals[signal] > 0, f'{signal} == 0'
