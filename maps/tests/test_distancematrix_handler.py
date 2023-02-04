import requests
import pytest
from util import assert_matrix_schema, get_matrix_query, \
    assert_routing_mode_signals, assert_handler_version_signals, assert_handler_signals
import maps.b2bgeo.test_lib.apikey_values as apikey_values


@pytest.mark.parametrize("routing_mode, is_valid", [
    (None, True),
    ("driving", True),
    ("transit", True),
    ("walking", True),
    ("truck", True),
    ("xxx", False),
])
def test_routing_mode(routing_public_api_url, routing_mode, is_valid):
    distance_matrix_query = get_matrix_query(apikey=apikey_values.ACTIVE, mode=routing_mode)
    resp = requests.get(routing_public_api_url + distance_matrix_query)

    if is_valid:
        resp.raise_for_status()
        assert resp.headers['X-Content-Type'] == 'nosniff'
    else:
        assert resp.status_code == requests.codes.bad_request

    assert_matrix_schema(resp)
    assert_routing_mode_signals(routing_public_api_url, 'distancematrix', routing_mode)


@pytest.mark.parametrize("router, is_valid", [
    (None, True),
    ("main", True),
    ("alternative", True),
    ("global", True),
    ("geodesic", False),
    ("xxx", False),
])
def test_router(routing_public_api_url, router, is_valid):
    distance_matrix_query = get_matrix_query(apikey=apikey_values.ACTIVE, router=router)
    resp = requests.get(routing_public_api_url + distance_matrix_query)

    if is_valid:
        resp.raise_for_status()
        assert resp.headers['X-Content-Type'] == 'nosniff'
    else:
        assert resp.status_code == requests.codes.bad_request


@pytest.mark.parametrize("version", [None, 'v2', 'v1', 'v1.0', 'v1.0.0'])
def test_handler_versions(routing_public_api_url, version):
    distance_matrix_query = get_matrix_query(apikey=apikey_values.ACTIVE, version=version)
    resp = requests.get(routing_public_api_url + distance_matrix_query)
    resp.raise_for_status()

    assert_matrix_schema(resp)
    assert_handler_version_signals(routing_public_api_url, 'distancematrix', version)


@pytest.mark.parametrize("routing_mode", ["driving", "transit", "walking", "truck"])
def test_fail(routing_public_api_url, routing_mode):
    origins = [{"lat": 0, "lon": 0}]
    destinations = [{"lat": 1, "lon": 1}]

    distance_matrix_query = get_matrix_query(apikey=apikey_values.ACTIVE, mode=routing_mode, origins=origins, destinations=destinations)
    resp = requests.get(routing_public_api_url + distance_matrix_query)

    resp.raise_for_status()
    assert_matrix_schema(resp)
    assert_routing_mode_signals(routing_public_api_url, 'distancematrix', routing_mode)

    j = resp.json()
    assert j["rows"][0]["elements"][0]["status"] == "FAIL", j


def test_handler_signals(routing_public_api_url):
    url = get_matrix_query(apikey=apikey_values.ACTIVE)
    resp = requests.get(routing_public_api_url + url)
    resp.raise_for_status()

    assert_matrix_schema(resp)
    assert_handler_signals(routing_public_api_url, 'distancematrix', ['locations_count_summ'])
