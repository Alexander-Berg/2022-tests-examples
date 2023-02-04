from math import sqrt
import unittest
import logging
import json
from copy import deepcopy

from .common import solve_task, CLOUD_TYPE


def _create_memory_heavy_task(num_locations, num_vehicles, routing_modes=None):
    """
    Constructs a solver task where each vehicle is not compatible with only one unique location.
    """
    # Moscow center & east:
    lat_span = (55.709227, 55.791460)
    lon_span = (37.6322156, 37.746248)
    depot_loc = {
        "lat": 55.7542961,
        "lon": 37.6618796
    }

    def get_location(idx, num_items):
        """
        maps absolute index of a location to the grid
        """
        side_len = int(sqrt(num_items)) + 1
        grid_line = idx // side_len
        grid_column = idx % side_len

        return {
            'lat': lat_span[0] + (lat_span[1] - lat_span[0]) * grid_line / side_len,
            'lon': lon_span[0] + (lon_span[1] - lon_span[0]) * grid_column / side_len,
        }

    location_tags = ["{:x}".format(i) for i in range(num_locations)]
    task = {
        "depot": {
            "point": depot_loc,
            "time_window": "07:00:00-23:59:59"
        },
        "locations": [
            {
                "id": i + 1,
                "time_window": "07:00:00-23:59:59",
                "point": get_location(i, num_locations),
                "required_tags": [location_tags[i]]
            }
            for i in range(num_locations)
        ],
        "options": {
            "time_zone": 3,
            "quality": "normal"
        },
        "vehicles": [
            {
                "id": i + 1,
                "tags": [".*"],
                "excluded_tags": [location_tags[i % num_locations]]
            }
            for i in range(num_vehicles)
        ]
    }
    if routing_modes:
        for i in range(num_vehicles):
            task["vehicles"][i]["routing_mode"] = routing_modes[i % len(routing_modes)]
    return task


def get_logged_result(solver_response):
    """
    Remove too much details from the response. It's not readable otherwise.
    """
    data = deepcopy(solver_response)
    data['result'].pop('routes')
    data['result'].pop('vehicles')
    data['result'].pop('dropped_locations', None)
    return json.dumps(data, indent=2)


def _check_task_completes(task, timeout, allow_drops=False):
    logging.info("Starting task with {} locations, {} vehicles, {} threads".format(
        len(task['locations']), len(task['vehicles']), task['options']['thread_count']))
    response = solve_task(task, timeout=timeout, mvrp=True)
    assert 'result' in response
    logging.info("Task finished. Resp:\n" + get_logged_result(response))
    result = response['result']
    if not allow_drops:
        assert 'dropped_locations' not in result or len(result['dropped_locations']) == 0, result
    return result


class ResourceConsumptionTest(unittest.TestCase):
    """
    These tests are based on the internal knowledge how the solver works. The test generates a task which pushes amount
    of the required memory to its theoretical limit.
    """
    def test_memory_limit_medium_task(self):
        # empirically choosen timeouts for tasks to be executed of fail
        # AWS works slower approx 2 times, timeouts are set accordingly
        timeout_sec = 5 if CLOUD_TYPE == 'rtc' else 12

        task = _create_memory_heavy_task(num_locations=1000, num_vehicles=433)

        task['options']['thread_count'] = 10
        task['options']['task_count'] = 2
        task['options']['solver_time_limit_s'] = 30
        task['options']['matrix_router'] = 'main'

        _check_task_completes(task, timeout=timeout_sec * 60.)
        logging.info('1k TEST PASSED')

    # TODO: BBGEO-10474 - we need to optimize solver or change these synthetic tests to avoid time limit
    # For now we just disable these tests, because we know that there is no such tasks in production
    @unittest.skip("enable in BBGEO-10474")
    def test_memory_limit_big_task(self):
        task = _create_memory_heavy_task(num_locations=4500, num_vehicles=550)

        task['options']['thread_count'] = 20
        task['options']['task_count'] = 2
        task['options']['solver_time_limit_s'] = 120
        task['options']['matrix_router'] = 'geodesic'
        _check_task_completes(task, timeout=22 * 60.)
        logging.info('5k TEST PASSED')

    @unittest.skip("enable in BBGEO-10474")
    def test_memory_limit_big_task_multiple_modes(self):
        task = _create_memory_heavy_task(num_locations=4500, num_vehicles=550, routing_modes=['driving', 'truck', 'walking', 'transit'])

        task['options']['thread_count'] = 20
        task['options']['task_count'] = 2
        task['options']['solver_time_limit_s'] = 120
        task['options']['matrix_router'] = 'geodesic'
        _check_task_completes(task, timeout=26 * 60.)
        logging.info('5k multiple modes TEST PASSED')

    def test_memory_limit_json_parsing(self):
        timeout_sec = 16 if CLOUD_TYPE == 'rtc' else 30

        task = _create_memory_heavy_task(num_locations=2500, num_vehicles=3)

        task['options']['thread_count'] = 10
        task['options']['task_count'] = 2
        task['options']['solver_time_limit_s'] = 30
        task['options']['matrix_router'] = 'main'
        for loc in task['locations']:
            loc['time_window'] = "07:00:00-1.23:59:59"

        _check_task_completes(task, timeout=timeout_sec * 60., allow_drops=True)
        logging.info('Json parsing TEST PASSED')

    def test_memory_limit_small_matrix_many_vehicles(self):
        timeout_sec = 6 if CLOUD_TYPE == 'rtc' else 15

        task = _create_memory_heavy_task(num_locations=500, num_vehicles=6000)

        task['options']['thread_count'] = 10
        task['options']['task_count'] = 2
        task['options']['solver_time_limit_s'] = 30
        task['options']['matrix_router'] = 'geodesic'

        _check_task_completes(task, timeout=timeout_sec * 60., allow_drops=True)
        logging.info('Small Matrix - Many Vehicles TEST PASSED')
