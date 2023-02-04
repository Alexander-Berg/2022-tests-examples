import json
import pytest
import requests

from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, API_KEY, generate_task


def _generate_svrp(location_count=None):
    if location_count is None:
        location_count = 10
    task_value = generate_task(location_count=location_count, solver_time_limit_s=0.01)
    task_value['vehicle'] = task_value['vehicles'][0]
    del task_value['vehicles']
    return task_value


def test_redirect(async_backend_url):
    task_value = _generate_svrp()

    response = requests.post(
        f'{async_backend_url}/solve/svrp?apikey={API_KEY}&origin=ya_courier',
        json.dumps(task_value), allow_redirects=False)
    assert response.status_code == requests.codes.temporary_redirect, response.text

    response = requests.post(f'{response.headers["Location"]}', json.dumps(task_value))
    assert response.ok, response.text


def test_sync(async_backend_url):
    task_value = _generate_svrp()

    response = requests.post(
        f'{async_backend_url}/solve/svrp?apikey={API_KEY}&origin=ya_courier', json.dumps(task_value))
    assert response.status_code == requests.codes.ok, response.text
    assert response.json()['message'] == 'Task successfully completed'


def test_async(async_backend_url):
    task_value = _generate_svrp(50)

    response = requests.post(
        f'{async_backend_url}/solve/svrp?apikey={API_KEY}&origin=ya_courier', json.dumps(task_value))
    assert response.status_code == requests.codes.accepted, response.text

    j = wait_task(async_backend_url, response.json()['id'])
    assert j['message'] == 'Task successfully completed'


def test_localized(async_backend_url):
    task_value = _generate_svrp(50)

    response = requests.post(
        f'{async_backend_url}/solve/svrp?apikey={API_KEY}&origin=ya_courier', json.dumps(task_value))
    assert response.status_code == requests.codes.accepted, response.text

    j = wait_task(async_backend_url, response.json()['id'])

    assert '__localized_string__' not in str(j)


def test_invalid_mvrp(async_backend_url):
    task_value = generate_task(location_count=10, solver_time_limit_s=3)

    response = requests.post(f'{async_backend_url}/solve/svrp?apikey={API_KEY}', json.dumps(task_value))
    assert response.status_code == requests.codes.bad_request, response.text


def test_invalid_no_depot(async_backend_url):
    task_value = _generate_svrp()
    del task_value['depot']

    response = requests.post(f'{async_backend_url}/solve/svrp?apikey={API_KEY}', json.dumps(task_value))
    assert response.status_code == requests.codes.bad_request, response.text


def test_invalid_no_locations(async_backend_url):
    task_value = _generate_svrp()
    del task_value['locations']

    response = requests.post(f'{async_backend_url}/solve/svrp?apikey={API_KEY}', json.dumps(task_value))
    assert response.status_code == requests.codes.bad_request, response.text


@pytest.mark.parametrize(
    ('lat', 'lon', 'used_router'),
    [
        (55.735525, 37.642474, 'main'),  # Russia
        (-53.152122, -70.898012, 'global'),  # Chile
    ]
)
def test_async_auto_router(async_backend_url, lat, lon, used_router):
    task_value = _generate_svrp(50)
    task_value['options']['matrix_router'] = 'auto'
    task_value['depot']['point'] = {
        'lat': lat,
        'lon': lon,
    }

    response = requests.post(
        f'{async_backend_url}/solve/svrp?apikey={API_KEY}&origin=ya_courier', json.dumps(task_value))
    assert response.status_code == requests.codes.accepted, response.text

    j = wait_task(async_backend_url, response.json()['id'])

    ms = j['matrix_statistics']['driving']
    assert ms['requested_router'] == 'auto'
    assert ms['used_router'] == used_router
