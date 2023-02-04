import requests
import pytest
import datetime
import time
from util import assert_matrix_schema, get_matrix_query, get_route_query, get_unistat_signals
from conftest import bring_up_routing_public_api
from yandex.maps.test_utils.common import wait_until
import maps.b2bgeo.test_lib.apikey_values as apikey_values


def test_no_apikey_service(routing_public_api_unreachable_apikey_service_url):
    url = routing_public_api_unreachable_apikey_service_url
    distance_matrix_query = get_matrix_query(apikey="you_cannot_check_it")
    resp = requests.get(url + distance_matrix_query)
    assert resp.status_code == requests.codes.ok


def test_no_apikey(routing_public_api_url):
    distance_matrix_query = get_matrix_query(apikey=None)
    resp = requests.get(routing_public_api_url + distance_matrix_query)
    assert resp.status_code == requests.codes.unauthorized
    assert "apikey" in resp.text


def test_unknown_apikey(routing_public_api_url):
    distance_matrix_query = get_matrix_query(apikey=apikey_values.UNKNOWN)
    resp = requests.get(routing_public_api_url + distance_matrix_query)
    assert resp.status_code == requests.codes.unauthorized
    assert_matrix_schema(resp)
    assert "errors" in resp.json()


def test_banned_apikey(routing_public_api_url):
    distance_matrix_query = get_matrix_query(apikey=apikey_values.BANNED)
    resp = requests.get(routing_public_api_url + distance_matrix_query)
    assert resp.status_code == requests.codes.unauthorized
    assert_matrix_schema(resp)
    assert "errors" in resp.json()


def test_incorrect_routing_mode(routing_public_api_url):
    query = get_matrix_query(apikey=apikey_values.ACTIVE, mode="routing_mode_invalid")
    resp = requests.get(routing_public_api_url + query)
    assert resp.status_code == requests.codes.bad_request

    query, params = get_route_query(apikey=apikey_values.ACTIVE, mode="routing_mode_invalid")
    resp = requests.get(routing_public_api_url + query, params=params)
    assert resp.status_code == requests.codes.bad_request

    actual_signals = get_unistat_signals(routing_public_api_url)

    expected_signal = "request_bad_summ"
    assert expected_signal in actual_signals


def test_departure_time(routing_public_api_url):
    timestamp = int(datetime.datetime.now().timestamp())
    distance_matrix_query = get_matrix_query(apikey=apikey_values.ACTIVE, departure_time=timestamp)
    resp = requests.get(routing_public_api_url + distance_matrix_query)
    resp.raise_for_status()

    assert_matrix_schema(resp)


@pytest.mark.parametrize('departure_time', [-1, 1500000000000, 'aaa', 62990228000])
def test_wrong_departure_time(routing_public_api_url, departure_time):
    distance_matrix_query = get_matrix_query(apikey=apikey_values.ACTIVE, departure_time=departure_time)
    resp = requests.get(routing_public_api_url + distance_matrix_query)
    assert resp.status_code == requests.codes.bad_request

    assert_matrix_schema(resp)


def test_zero_sources(routing_public_api_url):
    zero_points = [
        {
            "lat": 0,
            "lon": 0
        },
        {
            "lat": 0,
            "lon": 0
        }
    ]
    distance_matrix_query = get_matrix_query(origins=zero_points, apikey=apikey_values.ACTIVE)
    resp = requests.get(routing_public_api_url + distance_matrix_query)
    resp.raise_for_status()

    assert_matrix_schema(resp)


def test_unistat(routing_public_api_url):
    distance_matrix_query = get_matrix_query(apikey=apikey_values.ACTIVE)
    resp = requests.get(routing_public_api_url + distance_matrix_query)
    resp.raise_for_status()

    actual_signals = get_unistat_signals(routing_public_api_url)
    expected_signals = {
        'backend_request_failed_summ', 'locations_count_summ',
        'request_bad_summ', 'request_internal_error_summ',
        'rpa_apikey_reject_cache_summ', 'rpa_apikey_ok_summ', 'rpa_apikey_ok_5xx_summ', 'rpa_apikey_ok_cache_summ', 'rpa_apikey_reject_summ'
    }
    for handler in ['distancematrix', 'route']:
        for routing_mode in ['driving', 'transit', 'walking', 'truck']:
            expected_signals.add(f'{handler}-routing_mode_{routing_mode}_summ')
            expected_signals.add(f'{handler}-request_internal_error-{routing_mode}_summ')
            expected_signals.add(f'{handler}-response_time-{routing_mode}_hgram')
    for signal in expected_signals:
        assert signal in actual_signals


def test_apikey_cache(routing_public_api_function_scope_url):
    resp = requests.get(routing_public_api_function_scope_url + "/unistat")
    resp.raise_for_status()
    start_counters = dict(resp.json())

    distance_matrix_query = get_matrix_query(apikey=apikey_values.ACTIVE)
    for i in range(3):
        resp = requests.get(routing_public_api_function_scope_url + distance_matrix_query)
        resp.raise_for_status()

    resp = requests.get(routing_public_api_function_scope_url + "/unistat")
    resp.raise_for_status()
    counters_after_matrix_queries = dict(resp.json())

    assert counters_after_matrix_queries['rpa_apikey_ok_summ'] == start_counters['rpa_apikey_ok_summ'] + 2
    assert counters_after_matrix_queries['rpa_apikey_ok_cache_summ'] == start_counters['rpa_apikey_ok_cache_summ'] + 7
    assert counters_after_matrix_queries['rpa_apikey_reject_summ'] == start_counters['rpa_apikey_reject_summ']
    assert counters_after_matrix_queries['rpa_apikey_reject_cache_summ'] == start_counters['rpa_apikey_reject_cache_summ']

    route_url, params = get_route_query(apikey=apikey_values.ACTIVE)
    for i in range(3):
        resp = requests.get(routing_public_api_function_scope_url + route_url, params=params)
        resp.raise_for_status()

    resp = requests.get(routing_public_api_function_scope_url + "/unistat")
    resp.raise_for_status()
    counters_after_route_queries = dict(resp.json())

    assert counters_after_route_queries['rpa_apikey_ok_summ'] == counters_after_matrix_queries['rpa_apikey_ok_summ']
    assert counters_after_route_queries['rpa_apikey_ok_cache_summ'] == counters_after_matrix_queries['rpa_apikey_ok_cache_summ'] + 9
    assert counters_after_matrix_queries['rpa_apikey_reject_summ'] == start_counters['rpa_apikey_reject_summ']
    assert counters_after_route_queries['rpa_apikey_reject_cache_summ'] == counters_after_matrix_queries['rpa_apikey_reject_cache_summ']

    APIKEY_CACHE_ITEM_LIFETIME = 30
    time.sleep(APIKEY_CACHE_ITEM_LIFETIME)

    counters_after_cache_item_lifetime = {}

    def cached_counters_are_pushed_to_apikey_service():
        resp = requests.get(routing_public_api_function_scope_url + "/unistat")
        resp.raise_for_status()
        nonlocal counters_after_cache_item_lifetime
        counters_after_cache_item_lifetime = dict(resp.json())
        return counters_after_cache_item_lifetime['rpa_apikey_ok_summ'] == counters_after_route_queries['rpa_apikey_ok_summ'] + 1

    wait_until(lambda: cached_counters_are_pushed_to_apikey_service(), check_interval=1, timeout=5)

    assert counters_after_cache_item_lifetime['rpa_apikey_ok_cache_summ'] == counters_after_route_queries['rpa_apikey_ok_cache_summ']
    assert counters_after_matrix_queries['rpa_apikey_reject_summ'] == start_counters['rpa_apikey_reject_summ']
    assert counters_after_cache_item_lifetime['rpa_apikey_reject_cache_summ'] == counters_after_route_queries['rpa_apikey_reject_cache_summ']


def _get_total_apikey_counter():
    resp = requests.get(
        'http://apikeys-test.paysys.yandex.net:8666/api/get_link_info?service_token=routingmatrix_a33cd4b55ea503baa144b05afd7059284630fdf6&key=f0b22049-31fa-4982-aad7-057c68ecf78b&user_ip=1')
    resp.raise_for_status()
    return resp.json()["link_info"]["limit_stats"]["routingmatrix_total_daily"]["value_rolled"]


@pytest.mark.skip(reason="Long and unstable test")
def test_apikey_increment_counter():
    total = _get_total_apikey_counter()
    assert total == ""
    with bring_up_routing_public_api() as (routing_public_api_url, process):
        distance_matrix_query = get_matrix_query(apikey="f0b22049-31fa-4982-aad7-057c68ecf78b")
        resp = requests.get(routing_public_api_url + distance_matrix_query)
        resp.raise_for_status()
        assert_matrix_schema(resp)
        assert wait_until(
            lambda: total+4 <= _get_total_apikey_counter(), check_interval=1, timeout=30*60)


@pytest.mark.skip(reason="Unstable test")
def test_apikey_banned():
    with bring_up_routing_public_api() as (routing_public_api_url, process):
        distance_matrix_query = get_matrix_query(apikey="1692babf-224a-4a10-b4ba-3eefe5888c3c")
        resp = requests.get(routing_public_api_url + distance_matrix_query)
        assert resp.status_code == requests.codes.unauthorized
        assert_matrix_schema(resp)
        assert "errors" in resp.json()
