import pytest
import requests

from maps.b2bgeo.test_lib import apikey_values
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY,
    get_apikey_counters
)
from maps.b2bgeo.mvrp_solver.backend.async_backend.tests_lib.cp_util import (
    post_cp_task,
    wait_cp_task
)


def test_no_apikey(async_backend_url):
    post_cp_task(async_backend_url, apikey='', expected_status_code=requests.codes.unauthorized)


def test_invalid_json(async_backend_url):
    post_cp_task(async_backend_url, body='j', expected_status_code=requests.codes.bad_request)


def test_request_inconsistent_with_schema(async_backend_url):
    j = post_cp_task(async_backend_url, body='j', expected_status_code=requests.codes.bad_request)
    assert 'Request parameters do not meet the requirements' in j['error']['message']


def test_lang(async_backend_url):
    j_en = post_cp_task(async_backend_url, body='j', expected_status_code=requests.codes.bad_request)
    j_ru = post_cp_task(async_backend_url, body='j', expected_status_code=requests.codes.bad_request, lang='ru')
    assert j_en != j_ru


def test_invalid_lang(async_backend_url):
    post_cp_task(async_backend_url, expected_status_code=requests.codes.bad_request, lang='invalid')


def test_caching(async_backend_url_with_hashing):
    first = post_cp_task(async_backend_url_with_hashing)
    second = post_cp_task(async_backend_url_with_hashing)
    assert first['id'] == second['id']


def test_tasks_limit(async_backend_small_default_concurrent_task_limit):
    for _ in range(2):
        post_cp_task(async_backend_small_default_concurrent_task_limit)
    post_cp_task(async_backend_small_default_concurrent_task_limit, expected_status_code=requests.codes.too_many_requests)


def test_apikey_counters(async_backend_with_mocked_apikeys):
    backend_url, apikeys_url = async_backend_with_mocked_apikeys
    j = post_cp_task(backend_url, apikey=apikey_values.ACTIVE_MOCK_CP)
    j = wait_cp_task(backend_url, j['id'], apikey=apikey_values.ACTIVE_MOCK_CP)
    resp = requests.get(
        apikeys_url + '/api/get_link_info',
        params={'key': apikey_values.ACTIVE_MOCK_CP, 'user_ip': 1,
                'service_token': apikey_values.TOKEN_VRP},
        headers={"Authorization": "OAuth TEST_AUTH"})
    counters = get_apikey_counters(resp.json()['link_info'])
    assert 'tasks_calendar_planning_total_solved' in counters
    assert counters['tasks_calendar_planning_total_solved'] == 1
    sum(counters.values()) == 1


# TODO: Move this func to test_get_request.py
@pytest.mark.parametrize('resource', ['status', 'result', 'request'])
def test_cors_headers_are_set_correctly_for_our_hosts(async_backend_url, resource):
    task_id = 'bf35cff0-5432-47b1-8196-b910279366f5'  # random task id
    status_path = f'{async_backend_url}/calendar_planning/tasks/{task_id}/{resource}?apikey={API_KEY}'
    for origin in (
        'https://yandex.ru',
        'https://yandex.com',
        'https://yandex.com.tr',
        'https://l7test.yandex.ru',
        'https://l7test.yandex.com',
        'https://l7test.yandex.com.tr',
        'https://test-stand-9443-un-map-tile-border.stands.b2bgeo.yandex.com',
        'http://localhost:3333',
        "https://localhost.msup.yandex.ru:8081",
        "http://localhost.msup.yandex.com:8081",
        "https://localhost.msup.yandex.com.tr:8081",
    ):
        response = requests.get(status_path, headers={'Origin': origin})
        assert response.status_code == requests.codes.not_found
        assert response.headers.get('Access-Control-Allow-Origin') == origin

    for origin in (
        'https://fake-yandex.ru',
        'https://ayndex.com',
    ):
        response = requests.get(status_path, headers={'Origin': origin})
        assert response.status_code == requests.codes.not_found
        assert response.headers.get('Access-Control-Allow-Origin') is None
