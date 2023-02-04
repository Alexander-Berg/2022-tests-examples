import json
import os.path
import pytest
import signal
import time
import requests
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib import verify_unistat
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY,
    TASK_DIRECTORY,
    get_unistat_value,
    get_apikey_counters,
    check_response_optional_fields)
from maps.b2bgeo.test_lib import apikey_values
from maps.b2bgeo.mvrp_solver.backend.tests_lib import fastcgi


def check_unistat(sync_backend_url):
    response = requests.get(sync_backend_url + '/unistat')
    assert response.ok
    assert response.headers['content-type'] == 'application/json'

    data = response.json()
    print(data)

    verify_unistat.verify(data)

    keys = [x[0] for x in data]
    assert len(keys) == 45

    assert get_unistat_value(data, 'solve_started_summ') == 1
    assert get_unistat_value(data, 'uptime_max') > 0
    assert get_unistat_value(data, 'haversine_matrix_requests_summ') == 1


def make_svrp_task():
    file_path = source_path(os.path.join(TASK_DIRECTORY, 'svrp_request.json'))
    with open(file_path, 'r') as fin:
        task = json.load(fin)
    test_options = {
        'options': {
            'time_zone': 3,
            'date': '2018-09-01',
            'solver_time_limit_s': 1,
            'matrix_router': 'geodesic'
        }
    }
    task.update(test_options)
    return task


def test_solve_task(sync_backend_url):
    task = make_svrp_task()
    params = {
        'apikey': API_KEY,
        'origin': 'ya_courier'
    }
    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task), params=params)

    assert response.status_code == 200, response.text
    j = response.json()
    assert j['message'] == 'Task successfully completed', j['message']
    assert 'result' in j

    assert 'matrix_statistics' in j
    matrix_statistics = j['matrix_statistics']
    assert len(matrix_statistics) == 1
    stat = matrix_statistics['driving']
    assert stat['total_distances'] > 0
    assert stat['requested_router'] == 'geodesic'
    assert stat['used_router'] == 'geodesic'
    assert stat['geodesic_distances'] == stat['total_distances']
    check_unistat(sync_backend_url)
    check_response_optional_fields(j)


def test_wrong_parameters(sync_backend_url):
    task = make_svrp_task()
    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task))

    assert response.status_code == requests.codes.unauthorized
    assert "Query parameter 'apikey' is missing" == response.json()['error']

    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task), params={'origin': 'xxx', 'apikey': API_KEY})
    assert response.status_code == requests.codes.forbidden
    assert "Origin 'xxx' is unknown" == response.json()['error']

    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task), params={'origin': 'ya_courier'})
    assert response.status_code == requests.codes.unauthorized
    assert "Query parameter 'apikey' is missing" == response.json()['error']

    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task), params={'origin': 'ya_courier', 'apikey': API_KEY})
    assert response.status_code == requests.codes.ok

    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task), params={'apikey': 'yyy', 'origin': 'ya_courier'})
    assert response.status_code == requests.codes.unauthorized


def test_task_timelimit(sync_backend_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, 'svrp_request.json'))
    with open(file_path, 'r') as fin:
        task = json.load(fin)
    test_options = {'options': {'solver_time_limit_s': 3}}
    task.update(test_options)
    params = {
        'apikey': API_KEY,
        'origin': 'ya_courier'
    }
    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task), params=params)

    assert response.status_code == 400


def test_fastcgi_ping_and_stop(sync_backend_fastcgi_and_process):
    unix_socket, process = sync_backend_fastcgi_and_process
    app = fastcgi.connect_app(unix_socket)

    _, response = fastcgi.get_request(app, '/ping')
    assert response.status == 200

    process.send_signal(signal.SIGTERM)
    time.sleep(1)

    _, response = fastcgi.get_request(app, '/ping')
    assert response.status == 503
    _, response = fastcgi.get_request(app, '/unistat')
    assert response.status == 200


def test_multi_job(sync_backend_increase_task_count_url):
    file_path = source_path(os.path.join(TASK_DIRECTORY, 'svrp_request.json'))
    with open(file_path, 'r') as fin:
        task = json.load(fin)

    task['options'].update({'task_count': 5, 'thread_count': 5, 'matrix_router': 'geodesic'})
    params = {
        'apikey': API_KEY,
        'origin': 'ya_courier'
    }
    response = requests.post(f'{sync_backend_increase_task_count_url}/solve', json.dumps(task), params=params)
    assert response.ok, response.text


def test_svrp_apikey_counters(sync_backend_with_mocked_apikeys):
    backend_url, apikeys_url = sync_backend_with_mocked_apikeys

    task = make_svrp_task()
    params = {
        'apikey': apikey_values.ACTIVE_MOCK,
        'origin': 'ya_courier'
    }
    response = requests.post(f'{backend_url}/solve', json.dumps(task), params=params)
    assert response.status_code == 200, response.text

    resp = requests.get(
        apikeys_url + '/api/get_link_info',
        params={'key': apikey_values.ACTIVE_MOCK, 'user_ip': 1,
                'service_token': apikey_values.TOKEN_VRP},
        headers={'Authorization': 'OAuth TEST_AUTH'})
    counters = get_apikey_counters(resp.json()['link_info'])
    assert 'tasks_svrp_total_solved' in counters
    assert counters['tasks_svrp_total_solved'] == 1


def test_zero_coordinates(sync_backend_url):
    task = make_svrp_task()
    task['locations'][0]['point']['lat'] = 0.0
    task['locations'][0]['point']['lon'] = 0.0
    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task), params={'origin': 'ya_courier', 'apikey': API_KEY})
    assert not response.ok

    j = response.json()
    error = j['error']
    assert 'invalid coordinates: lat = 0, lon = 0.' in error


def test_invalid_locale(sync_backend_url):
    task = make_svrp_task()
    params = {
        'origin': 'ya_courier',
        'apikey': API_KEY
    }
    response = requests.post(f'{sync_backend_url}/solve?lang=asdf', json.dumps(task), params=params)
    assert response.status_code == 400, response.text


def test_localized(sync_backend_url):
    task = make_svrp_task()
    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task), params={'origin': 'ya_courier', 'apikey': API_KEY})
    assert '__localized_string__' not in response.text


@pytest.mark.parametrize(
    ('lat', 'lon', 'used_router'),
    [
        (55.735525, 37.642474, 'main'),  # Russia
        (-53.152122, -70.898012, 'global'),  # Chile
    ]
)
def test_sync_auto_router(sync_backend_url, lat, lon, used_router):
    task_value = make_svrp_task()
    task_value['options']['matrix_router'] = 'auto'
    task_value['depot']['point'] = {
        'lat': lat,
        'lon': lon,
    }
    params = {
        'apikey': API_KEY,
        'origin': 'ya_courier'
    }
    response = requests.post(f'{sync_backend_url}/solve', json.dumps(task_value), params=params)
    assert response.status_code == requests.codes.ok, response.text

    ms = response.json()['matrix_statistics']['driving']
    assert ms['requested_router'] == 'auto'
    assert ms['used_router'] == used_router
