import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def get_max_lateness(response):
    lateness = 0
    for route in response['routes']:
        for node in route['route']:
            if 'failed_time_window' in node:
                failed_window = node['failed_time_window']
                if failed_window['how'] == 'LATE':
                    lateness = max(lateness, float(failed_window['duration_s']))
    return lateness


def get_failed_windows_count(response):
    return int(response['metrics']['failed_time_window_locations_count'])


def test_soft_time_window():
    """
    Check that task without flexible time windows leads to single huge lateness.
    """
    request = tools.get_test_json('soft_time_windows.json')
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")

    assert get_failed_windows_count(response) == 1
    assert get_max_lateness(response) > 600


def test_flexible_time_window():
    """
    Check that task with flexible time windows leads to several small latenesses.
    """
    request = tools.get_test_json('flexible_time_windows.json')
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")

    assert get_failed_windows_count(response) == 3
    assert get_max_lateness(response) < 60


def test_flexible_wait_until():
    """
    Check that wait_until_window works correctly with flexible windows:
        if penalty for each early minute > 50 it's better to wait until soft window
        otherwise it's better to wait only until start of hard window
    """
    request = tools.get_test_json('flexible_wait_until.json')
    assert request['locations'][0]['penalty']['early']['minute'] == 51
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")

    assert response['routes'][0]['route'][1]["departure_time_s"] == 36000  # start of soft time window

    request['locations'][0]['penalty']['early']['minute'] = 49
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")

    assert response['routes'][0]['route'][1]["departure_time_s"] == 34200  # start of hard time window


def test_flexible_shift_max_duration():
    """
    With defined `hard_max_duration_s` it's possible to serve only 1 of 3 locations with no shift overtime.
    If the same `max_duration_s` is defined instead we serve all of 3 locations but with overtime.
    """
    request = tools.get_test_json('shift_hard_max_duration.json')

    assert request['vehicles'][0]['shifts'][0]['hard_max_duration_s'] == 300

    partial_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="PARTIAL_SOLVED")

    assert partial_response['metrics']['assigned_locations_count'] == 1
    assert partial_response['metrics']['overtime_duration_s'] == 0

    request['vehicles'][0]['shifts'][0]['max_duration_s'] = 300
    del request['vehicles'][0]['shifts'][0]['hard_max_duration_s']

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")

    assert response['metrics']['assigned_locations_count'] == 3
    assert response['metrics']['overtime_duration_s'] > 0
