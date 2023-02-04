import json
import time
import requests
import os
from retry import retry
import logging

from yandex.maps.test_utils.common import wait_until


SOLVER_TEST_APIKEY = '992044a6-d49b-4e69-a6a1-d5dc26600bf0'

CLOUD_TYPE = os.environ.get('YC_CLOUD', 'rtc')
assert CLOUD_TYPE in ('rtc', 'aws'), "Invalid cloud environment type"

if CLOUD_TYPE == 'rtc':
    ADDITIONAL_HEADERS = {}
    ADDITIONAL_PARAMS = {}
else:
    ADDITIONAL_HEADERS = {'Host': 'api.test.routeq.com'}
    ADDITIONAL_PARAMS = {'apikey': SOLVER_TEST_APIKEY}

API_ENDPOINT = os.environ.get(
    'SOLVER_API_ENDPOINT',
    'https://test.courier.yandex.ru/vrs/api/v1')
VRP_SOLVER_URL = os.environ.get(
    'VRP_SOLVER_URL',
    'http://b2bgeo-syncsolver.testing.maps.yandex.net')

ADD_TASK_QUERY_PATTERN = '/add/{}'
ADD_SVRP_TASK_QUERY = ADD_TASK_QUERY_PATTERN.format('svrp')
ADD_MVRP_TASK_QUERY = ADD_TASK_QUERY_PATTERN.format('mvrp')
ADD_CALENDAR_PLANNING_TASK_QUERY = '/calendar_planning/tasks'

TASK_SVRP_RESULT_QUERY_PATTERN = '/result/svrp/{}' if CLOUD_TYPE == 'rtc' else '/result/{}'
TASK_MVRP_RESULT_QUERY_PATTERN = '/result/{}'

POINT = {
    'lat': 55.767420,
    'lon': 37.592191
}
TIME_WINDOW = '07:00:00-23:59:59'


logging.basicConfig(
    level=(logging.DEBUG if os.environ.get('VERBOSE') else logging.INFO),
    format='%(asctime)-15s %(levelname)s %(filename)s %(message)s',
)


class SolverError(Exception):
    def __init__(self, solver_response):
        Exception.__init__(
            self,
            f'Unexpected solver response {solver_response.status_code}: {solver_response.text}')


def make_request(method, url, **kwargs):
    kwargs['headers'] = ADDITIONAL_HEADERS | kwargs.get('headers', {})
    kwargs['params'] = ADDITIONAL_PARAMS | kwargs.get('params', {})
    return requests.request(method, url, **kwargs)


@retry(tries=5, delay=1, backoff=2)
def requests_get_with_retry(url, **kwargs):
    resp = make_request('get', url, **kwargs)
    return resp


@retry(tries=5, delay=1, backoff=2)
def requests_post_with_retry(url, **kwargs):
    resp = make_request('post', url, **kwargs)
    return resp


def start_task(url, task, params={'apikey': SOLVER_TEST_APIKEY}):
    logging.info(f'Starting new task with time_window {task.get("depot", {}).get("time_window")} and {len(task.get("locations", []))} locations')
    resp = requests_post_with_retry(
        url,
        headers={
            'Content-Type': 'application/json'
        },
        params=params,
        json=task
    )
    if not resp.ok:
        raise SolverError(resp)
    parsed = resp.json()
    logging.debug(json.dumps(parsed, indent=4))
    task_id = parsed['id']
    logging.info(f'Task with id {task_id} created')
    return task_id


def get_task_result(url, task_id, timeout=1650., **kwargs):
    started_at = time.time()
    while time.time() - started_at < timeout:
        resp = requests_get_with_retry(url, **kwargs)
        if resp.status_code == 200:
            logging.info(f'Task {task_id} completed')
            return json.loads(resp.text)
        elif resp.ok:
            logging.info(f'Waiting task {task_id}')
            time.sleep(1)
        else:
            raise SolverError(resp)

    assert False, f'timeout reached for task_id: {task_id}'


def solve_task(task, timeout, mvrp=True):
    url_base = API_ENDPOINT if mvrp else VRP_SOLVER_URL
    task_id = start_task(url_base + (ADD_MVRP_TASK_QUERY if mvrp else ADD_SVRP_TASK_QUERY), task)
    return get_task_result(
        url=(url_base + TASK_MVRP_RESULT_QUERY_PATTERN if mvrp else TASK_SVRP_RESULT_QUERY_PATTERN).format(task_id),
        task_id=task_id,
        timeout=timeout
    )


def create_task(routing_modes=['driving']):
    return {
        'depot': {
            'point': {'lat': 55.792393, 'lon': 37.682788},
            'time_window': '07:00:00-23:59:59'
        },
        'locations': [
            {
                'id': i + 1,
                'time_window': '07:00:00-23:59:59',
                'point': {'lat': 55.786421, 'lon':  37.513242},
                'required_tags': [str(i)]
            }
            for i in range(len(routing_modes))
        ],
        'options': {
            'time_zone': 3,
            'solver_time_limit_s': 1,
            'thread_count': 1,
            'task_count': 1
        },
        'vehicles': [
            {
                'id': i + 1,
                'tags': [str(i)],
                'routing_mode': routing_modes[i]
            }
            for i in range(len(routing_modes))
        ]
    }


def generate_task(location_count,
                  solver_time_limit_s,
                  uniq_location_count=None,
                  time_window=TIME_WINDOW,
                  quality=None,
                  coordinate_step=0.001,
                  matrix_router_type='geodesic'):
    uniq_location_count = uniq_location_count or location_count
    assert uniq_location_count <= location_count
    vehicle_count = max(1, location_count // 3)
    points = [
        {
            'lat': POINT['lat'] + index * coordinate_step,
            'lon': POINT['lon'] + index * coordinate_step
        } for index in range(1, uniq_location_count + 1)
    ]

    task = {
        'depot': {
            'point': POINT,
            'time_window': time_window,
        },
        'locations': [{
            'id': i + 1,
            'time_window': time_window,
            'point': points[i % uniq_location_count],
        } for i in range(location_count)],
        'options': {
            'time_zone': 3,
            'matrix_router': matrix_router_type,
            'solver_time_limit_s': solver_time_limit_s,
            'quality': 'low'
        },
        'vehicles': [{
            'id': i + 1,
            'max_runs': 3,
            'capacity': {
                'weight_kg': 5
            }
        } for i in range(vehicle_count)]
    }
    if quality:
        task['options']['quality'] = quality
    return task


def create_calendar_planning_task():
    return {
        'locations': [{
            'point': {'lat': 55.663304, 'lon': 37.556711},
            'time_window': '09:00 - 18:00'
        }],
        'options': {
            'quality': 'normal'
        },
        'depots': [{
            'id': 'some_id',
            'ref': 'some_ref',
            'time_window': '00:00 - 23:59',
            'point': {'lat': 55.7357052, 'lon': 37.6425973}
        }],
        'vehicles': [{
            'id': '1',
            'routing_mode': 'driving'
        }]
    }


def _get_cp_task_status(task_id, apikey=SOLVER_TEST_APIKEY, expected_status_code=requests.codes.ok):
    result_url = f'{API_ENDPOINT}{ADD_CALENDAR_PLANNING_TASK_QUERY}/{task_id}/status'
    response = make_request("get", result_url, params={'apikey': apikey})

    assert response.status_code == expected_status_code, response.text
    return response.json()


def _get_cp_task_result(task_id, apikey=SOLVER_TEST_APIKEY, expected_status_code=requests.codes.ok):
    result_url = f'{API_ENDPOINT}{ADD_CALENDAR_PLANNING_TASK_QUERY}/{task_id}/result'
    response = make_request("get", result_url, params={'apikey': apikey})

    assert response.status_code == expected_status_code, response.text
    return response.json()


def wait_cp_task(task_id, timeout=200, apikey=SOLVER_TEST_APIKEY):
    def task_is_completed(resp):
        return resp.get('status') == 'completed'

    wait_until(lambda: task_is_completed(_get_cp_task_status(task_id)), timeout=timeout)

    return _get_cp_task_result(task_id, apikey, requests.codes.ok)


def post_cp_task(task, apikey=SOLVER_TEST_APIKEY, expected_status_code=requests.codes.accepted):
    post_task_url = f'{API_ENDPOINT}{ADD_CALENDAR_PLANNING_TASK_QUERY}'

    response = make_request("post", post_task_url, json=task, params={'apikey': apikey})

    assert response.status_code == expected_status_code, response.text
    return response.json()


def wait_task(task_id, timeout=20, status=None):
    def task_is_completed(resp):
        if status is not None:
            return resp.json().get('status', {}).get(status) is not None or resp.status_code == 200
        return resp.status_code == 200

    wait_until(lambda: task_is_completed(make_request("get", f'{API_ENDPOINT}/result/{task_id}')), timeout=timeout)
    response = make_request("get", f'{API_ENDPOINT}/result/{task_id}')
    if status is not None:
        assert response.ok, response.text
    else:
        assert response.status_code == 200, response.text
    return response.json()
