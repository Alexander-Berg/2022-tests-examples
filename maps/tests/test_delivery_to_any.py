import requests
import json
import os
from yatest.common import source_path
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import (
    API_KEY,
    fill_dummy_test_options,
    TASK_DIRECTORY,
    post_task_and_check_async_error_message,
    post_task_and_check_error_message,
    wait_task,
)


def test_delivery_to_any(async_backend_url):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)
    fill_dummy_test_options(task)
    task['locations'][0]['id'] = 0
    task['locations'][1]['id'] = 1

    task['locations'][0]['delivery_to_any'] = [1]
    post_task_and_check_error_message(async_backend_url, task, "delivery id 0: delivery_to_any field defined, but only pickups can have it.")

    task['locations'][0]['type'] = 'pickup'
    post_task_and_check_async_error_message(async_backend_url, task, "location id 1 is neither  a `drop_off` location")

    task['locations'][1]['type'] = 'drop_off'
    del task['locations'][1]['shipment_size']
    resp = requests.post(async_backend_url + '/add/mvrp?apikey={}'.format(API_KEY), json.dumps(task))
    assert resp
    wait_task(async_backend_url, resp.json()['id'])


def test_incompatible_load_types_in_pickup_and_drop_off(async_backend_url):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)
    fill_dummy_test_options(task)
    task['locations'][0]['id'] = 0
    task['locations'][1]['id'] = 1

    task['locations'][0]['type'] = 'pickup'
    task['locations'][1]['type'] = 'drop_off'
    del task['locations'][1]['shipment_size']
    task['locations'][0]['delivery_to_any'] = [1]

    task['locations'][0]['load_types'] = ['first']
    task['locations'][1]['load_types'] = ['second']
    task['options']['incompatible_load_types'] = [['first', 'second']]

    post_task_and_check_async_error_message(async_backend_url, task, "incompatible by load types.")
