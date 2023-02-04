import requests
import json
import os
import http.client
import pytest
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY,
    check_response_optional_fields,
    fill_dummy_test_options,
    post_task_and_check_async_error_message,
    post_task_and_check_error_message,
    TASK_DIRECTORY,
    wait_task,
)


def check_headers(headers, task_id, origin):
    assert "Location" in headers
    assert "/result/" + task_id in headers["Location"]

    assert 'Access-Control-Allow-Origin' in headers
    assert headers['Access-Control-Allow-Origin'] == origin

    assert 'Content-Type' in headers
    assert headers['Content-Type'] == 'application/json'


def test_no_time_zone(async_backend_url):
    backend_url = async_backend_url
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)
    del task_value["options"]["time_zone"]
    response = requests.post(backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert not response.ok

    j = response.json()
    assert 'error' in j
    error = j['error']
    assert 'incident_id' in error
    assert 'message' in error
    assert 'Request parameters do not meet the requirements' in error['message']


def test_invalid_date_format(async_backend_url):
    backend_url = async_backend_url
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    task_value["options"]["date"] = "invalid_date"
    response = requests.post(backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.status_code == requests.codes.bad_request
    assert response.json()["error"]["message"] == """Invalid date format: invalid_date."""


def test_validate_parameters_fail(async_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    INVALID_LOCATION_ID = 9999999
    task_value["vehicles"][0]["visited_locations"] = [
        {
            "id": INVALID_LOCATION_ID
        }
    ]
    post_task_and_check_async_error_message(
        async_backend_url, task_value, f"Cannot find location with id={INVALID_LOCATION_ID}")


def test_task(async_backend_url):
    allowed_origins = (
        "https://yandex.ru",
        "https://yandex.com",
        "https://yandex.com.tr",
        "https://l7test.yandex.ru",
        "https://l7test.yandex.com",
        "https://l7test.yandex.com.tr",
        "https://test-stand-9443-un-map-tile-border.stands.b2bgeo.yandex.com",
        "http://localhost:3333",
        "http://localhost.msup.yandex.ru:8081",
        "https://localhost.msup.yandex.com:8081",
        "http://localhost.msup.yandex.com.tr:8081",
    )

    backend_url = async_backend_url
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)
    for origin in allowed_origins:
        response = requests.post(
            backend_url + "/add/mvrp?apikey={}".format(API_KEY),
            json.dumps(task_value),
            headers={'Origin': origin},
        )
        assert response.ok, response.text
        task_id = response.json()['id']
        check_headers(response.headers, task_id, origin)

        response = requests.get(backend_url + "/result/" + task_id)
        assert response.ok, response.text
        assert response.headers.get('Access-Control-Allow-Origin') is None

        response = requests.get(backend_url + "/result/" + task_id, headers={'Origin': origin})
        assert response.headers.get('Access-Control-Allow-Origin') == origin

        j = wait_task(backend_url, task_id)
        assert "status" in j
        assert "calculated" in j["status"]
        assert j["message"] == 'Task successfully completed', j["message"]
        assert "result" in j
        assert j["result"]["metrics"]["total_probable_penalty"] == 0
        assert not j["result"]["options"]["minimize_lateness_risk"]

        assert "matrix_statistics" in j
        matrix_statistics = j["matrix_statistics"]
        assert len(matrix_statistics) == 1
        ms = matrix_statistics['driving']
        assert ms["total_distances"] > 0
        assert ms["requested_router"] == "geodesic"
        assert ms["used_router"] == "geodesic"
        assert ms["geodesic_distances"] == ms["total_distances"]
        check_response_optional_fields(j)


def test_incorrect_time_windows(async_backend_url):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)
    fill_dummy_test_options(task)

    task['locations'][0]['time_window'] = "11:00-10:00"
    post_task_and_check_error_message(async_backend_url, task, "delivery id 0: Invalid time range format: 11:00-10:00.")

    task['locations'][0]['time_window'] = "10:00-11:00"
    task['depot']['time_window'] = "11:00-10:00"
    post_task_and_check_error_message(async_backend_url, task, "Invalid time range format: 11:00-10:00.")

    task['depots'] = [task['depot']]
    del task['depot']
    post_task_and_check_error_message(async_backend_url, task, "Invalid time range format: 11:00-10:00.")


def test_incorrect_balanced_group_ids(async_backend_url):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)
    fill_dummy_test_options(task)

    task['options']['balanced_groups'] = [{"id": "0"}]
    task['vehicles'][0]['shifts'] = [{"id": "0", "time_window": "10:00-15:00", "balanced_group_id": "1"}]
    post_task_and_check_async_error_message(
        async_backend_url, task, "balanced route group id '1' is not defined in 'balanced_groups' option.")


def test_missing_apikey(async_backend_url):
    resp = requests.post(async_backend_url + '/add/mvrp', json={})
    assert resp.status_code == http.client.UNAUTHORIZED
    assert "Query parameter 'apikey' is missing" == resp.json()['error']['message']


@pytest.mark.parametrize('time_zone', [3, "Europe/Moscow"])
def test_time_zone_as_string(async_backend_url, time_zone):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task_value = json.load(f)

    fill_dummy_test_options(task_value)
    task_value["options"]["time_zone"] = time_zone

    response = requests.post(async_backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert response.ok, response.text
    task_id = response.json()['id']
    j = wait_task(async_backend_url, task_id)
    assert 'calculated' in j['status']
    assert j["result"]["options"]["time_zone"] == time_zone


def test_zero_coordinates(async_backend_url):
    backend_url = async_backend_url
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)
    fill_dummy_test_options(task_value)
    task_value["locations"][0]["point"]["lat"] = 0.0
    task_value["locations"][0]["point"]["lon"] = 0.0
    response = requests.post(backend_url + "/add/mvrp?apikey={}".format(API_KEY), json.dumps(task_value))
    assert not response.ok

    j = response.json()
    error = j['error']
    assert 'delivery id 0: invalid coordinates: lat = 0, lon = 0.' in error['message']


@pytest.mark.parametrize('matrix_router', [False, True])
@pytest.mark.parametrize('solver_time_limit_s', [False, True])
def test_override_task_options_with_cgi_parameters(async_backend_url, matrix_router, solver_time_limit_s):
    file_path = source_path(os.path.join(TASK_DIRECTORY, '10_locs.json'))
    with open(file_path, "r") as f:
        task_value = json.load(f)

    if matrix_router:
        task_value['options']['matrix_router'] = 'main'
    if solver_time_limit_s:
        task_value['options']['solver_time_limit_s'] = 3.0

    new_matrix_router = 'geodesic'
    new_solver_time_limit_s = 0
    post_task_url = async_backend_url + f"/add/mvrp?apikey={API_KEY}&matrix_router={new_matrix_router}&solver_time_limit_s={new_solver_time_limit_s}"

    response = requests.post(post_task_url, json.dumps(task_value))
    assert response.ok, response.text
    task_id = response.json()['id']
    j = wait_task(async_backend_url, task_id)
    assert 'calculated' in j['status']

    response = requests.get(async_backend_url + f"/log/request/{task_id}")
    assert response.json()['options']['matrix_router'] == new_matrix_router
    assert response.json()['options']['solver_time_limit_s'] == new_solver_time_limit_s
