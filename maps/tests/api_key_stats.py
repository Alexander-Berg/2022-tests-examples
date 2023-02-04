import http.client
import pytz
import requests
import urllib.parse
from datetime import datetime, timedelta
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY,
    LIMITED_API_KEY,
    add_task_mvrp,
    add_task_svrp,
    get_apikey_counters)
from maps.b2bgeo.test_lib import apikey_values

TZ_NAME = 'Europe/Moscow'
TZ = pytz.timezone(TZ_NAME)


def _iso_interval(start, end):
    return urllib.parse.quote('{}/{}'.format(start.isoformat(), end.isoformat()), safe='')


def _stat_request_without_check(async_backend_url, start, end, token="TEST_AUTH"):
    headers = {"Authorization": f"OAuth {token}"}

    return requests.get(
        f'{async_backend_url}/stat/apikeys?time_interval={_iso_interval(start, end)}',
        headers=headers)


def _stat_request(async_backend_url, start, end):
    resp = _stat_request_without_check(async_backend_url, start, end)
    assert resp.ok, resp.text
    return resp.json()["api_keys"]


def _check_stat_value(stat1, stat2, names, value):
    def get(obj, names):
        for name in names:
            if name not in obj:
                return 0
            obj = obj[name]
        return int(obj)
    assert get(stat1, names) + get(stat2, names) == value, "{}: {}: {}".format(names, stat1, stat2)


def _check_day_stat(full_stat1, full_stat2, api_key, num_tasks):
    stat1 = full_stat1.get(api_key, {})
    stat2 = full_stat2.get(api_key, {})

    _check_stat_value(stat1, stat2, ["funnel_states", "accepted"], num_tasks)
    _check_stat_value(stat1, stat2, ["funnel_states", "started_downloads"], num_tasks)
    _check_stat_value(stat1, stat2, ["solvedLocations"], num_tasks * 10)


def _check_forbidden(resp):
    assert resp.status_code == http.client.FORBIDDEN, resp.text
    response = resp.json()
    assert "error" in response, response
    assert "message" in response["error"], response["error"]
    assert "AUTHORIZATION" in response["error"]["message"], response["error"]["message"]


def test_no_auth(async_backend_url_module_scope):
    now = datetime.now(tz=TZ)
    resp = _stat_request_without_check(async_backend_url_module_scope, now, now + timedelta(hours=3), token=None)
    _check_forbidden(resp)


def test_wrong_auth(async_backend_url_module_scope):
    now = datetime.now(tz=TZ)
    resp = _stat_request_without_check(async_backend_url_module_scope, now, now + timedelta(hours=3), token="WRONG_TOKEN")
    _check_forbidden(resp)


def test_apikey_stats(async_backend_url_module_scope):
    now = datetime.now(tz=TZ)
    yesterday = now - timedelta(hours=24)

    add_task_mvrp(async_backend_url_module_scope, API_KEY)
    add_task_mvrp(async_backend_url_module_scope, API_KEY)
    add_task_mvrp(async_backend_url_module_scope, LIMITED_API_KEY)

    s1 = _stat_request(async_backend_url_module_scope, now, now + timedelta(hours=3))
    s2 = _stat_request(async_backend_url_module_scope, yesterday, yesterday + timedelta(hours=3))

    _check_day_stat(s1, s2, API_KEY, 2)
    _check_day_stat(s1, s2, LIMITED_API_KEY, 1)


def test_apikey_stats_for_period(async_backend_url_module_scope):
    add_task_mvrp(async_backend_url_module_scope, API_KEY)
    now = datetime.now(tz=TZ)
    yesterday = now - timedelta(hours=24)

    invalid_time_point = "invalid_time_point"
    resp = requests.get(
        async_backend_url_module_scope + f'/stat/apikeys?time_interval={invalid_time_point}',
        headers={"Authorization": "OAuth TEST_AUTH"})
    assert resp.status_code == requests.codes.bad_request, resp.text

    resp = _stat_request_without_check(async_backend_url_module_scope, now, yesterday)
    assert resp.status_code == requests.codes.bad_request, resp.text

    resp = _stat_request_without_check(async_backend_url_module_scope, yesterday, now)
    assert resp.status_code == requests.codes.bad_request, resp.text
    assert "is too long, maximum is 4 hours" in resp.json()["error"]["message"], resp.text

    resp = _stat_request_without_check(async_backend_url_module_scope, now - timedelta(hours=3), now)
    assert resp.ok, resp.text


def test_mvrp_internal_server_error(async_backend_with_mocked_apikeys):
    backend_url, _ = async_backend_with_mocked_apikeys
    add_task_mvrp(backend_url, apikey_values.MOCK_SIMULATE_ERROR)


def test_svrp_internal_server_error(async_backend_with_mocked_apikeys):
    backend_url, _ = async_backend_with_mocked_apikeys
    add_task_svrp(backend_url, apikey_values.MOCK_SIMULATE_ERROR)


def test_mvrp_apikey_counters(async_backend_with_mocked_apikeys):
    backend_url, apikeys_url = async_backend_with_mocked_apikeys
    add_task_mvrp(backend_url, apikey_values.ACTIVE_MOCK)
    resp = requests.get(
        apikeys_url + '/api/get_link_info',
        params={'key': apikey_values.ACTIVE_MOCK, 'user_ip': 1,
                'service_token': apikey_values.TOKEN_VRP},
        headers={"Authorization": "OAuth TEST_AUTH"})
    counters = get_apikey_counters(resp.json()['link_info'])
    assert 'tasks_total_solved' in counters
    assert 'tasks_small_low_solved' in counters
    assert counters['tasks_total_solved'] == 1
    assert counters['tasks_small_low_solved'] == 1
    sum(counters.values()) == 2


def test_svrp_apikey_counters(async_backend_with_mocked_apikeys):
    backend_url, apikeys_url = async_backend_with_mocked_apikeys
    add_task_svrp(backend_url, apikey_values.ACTIVE_MOCK_SVRP)
    resp = requests.get(
        apikeys_url + '/api/get_link_info',
        params={'key': apikey_values.ACTIVE_MOCK_SVRP, 'user_ip': 1,
                'service_token': apikey_values.TOKEN_VRP},
        headers={"Authorization": "OAuth TEST_AUTH"})
    counters = get_apikey_counters(resp.json()['link_info'])
    assert 'tasks_svrp_total_solved' in counters
    assert counters['tasks_svrp_total_solved'] == 1
    sum(counters.values()) == 1
