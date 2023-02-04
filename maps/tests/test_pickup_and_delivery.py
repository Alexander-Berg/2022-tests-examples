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


def _initialize_locations(task):
    task['locations'][0]['id'] = 0
    task['locations'][1]['id'] = 1
    task['locations'][0]['delivery_to'] = 1
    task['locations'][0]['type'] = 'pickup'


def test_pickup_and_delivery_bad_types(async_backend_url):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)

    fill_dummy_test_options(task)
    _initialize_locations(task)

    del task['locations'][0]['type']
    post_task_and_check_error_message(async_backend_url, task, "delivery id 0: delivery_to field defined, but only pickups can have it.")

    task['locations'][0]['type'] = 'pickup'
    task['locations'][0]['delivery_to'] = 10
    post_task_and_check_async_error_message(async_backend_url, task, "Cannot find location with id=10")
    task['locations'][0]['delivery_to'] = 1

    task['locations'][1]['type'] = 'pickup'
    post_task_and_check_async_error_message(async_backend_url, task, "Location with id 1 is referenced by delivery_to field, and therefore it must be of type 'delivery'")

    task['locations'][1]['type'] = 'delivery'
    task['locations'][0]['delivery_to_any'] = []
    post_task_and_check_error_message(async_backend_url, task, "pickup id 0: both delivery_to and delivery_to_any fields defined. Please specify only one of them.")

    del task['locations'][0]['delivery_to_any']
    resp = requests.post(async_backend_url + '/add/mvrp?apikey={}'.format(API_KEY), json.dumps(task))
    assert resp.ok
    wait_task(async_backend_url, resp.json()['id'])


def test_pickup_and_delivery_equal_shipment_size(async_backend_url):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)

    fill_dummy_test_options(task)
    _initialize_locations(task)

    task['locations'][0]['shipment_size'] = {'weight_kg': 5}
    task['locations'][1]['shipment_size'] = {'weight_kg': 4}
    post_task_and_check_async_error_message(async_backend_url, task, "Locations with ids 0 and 1 are pickup and delivery pair, therefore they must have equal weight.")


def test_incompatible_load_types_in_pickup_and_delivery(async_backend_url):
    with open(source_path(os.path.join(TASK_DIRECTORY, '10_locs.json')), 'r') as f:
        task = json.load(f)

    fill_dummy_test_options(task)
    _initialize_locations(task)

    task['locations'][0]['load_types'] = ['first']
    task['locations'][1]['load_types'] = ['second']
    task['options']['incompatible_load_types'] = [['first', 'second']]

    post_task_and_check_async_error_message(async_backend_url, task, "Locations with incompatible load types found")
