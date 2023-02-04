import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest
import datetime


@pytest.mark.parametrize('use_deadline', [True, False])
def test_depot_deadline(use_deadline):
    task = tools.get_test_json("depot_deadline.json")

    if not use_deadline:
        for loc in task['locations']:
            if 'delivery_deadline' in loc:
                del loc['delivery_deadline']

    result = mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': 100000})

    for route in result['routes']:
        deadline = float('inf')
        for node in route['route']:
            value = node['node']['value']
            if 'delivery_deadline' in value:
                time = datetime.datetime.strptime(value['delivery_deadline'], '%H:%M:%S')
                timedelta = datetime.timedelta(hours=time.hour, minutes=time.minute, seconds=time.second)
                cur_deadline = timedelta.total_seconds()
                deadline = min(deadline, cur_deadline)
            elif node['node']['type'] == 'depot' and deadline < float('inf'):
                assert (node['arrival_time_s'] <= deadline) == use_deadline


def test_depot_deadline_penalty():
    """
    In this test it is impossible to satisfy all deadlines,
    we check that penalties are calculated correctly
    """
    task = tools.get_test_json("depot_deadline.json")

    vehicle = task['vehicles'][0]
    vehicle['max_runs'] = 1

    expected_metrics = {
        "failed_time_window_locations_count": 0,
        "failed_time_window_locations_count_penalty": 0,
        "failed_time_window_locations_duration_penalty": 0,
        "failed_time_window_locations_duration_s": 0,
        "total_failed_delivery_deadline_count": 2,
        "total_failed_delivery_deadline_duration_s": 20708.034934440395,
        "total_failed_delivery_deadline_penalty": 5549.307860249089,
        "total_failed_time_window_count": 0,
        "total_failed_time_window_duration_s": 0,
        "total_failed_time_window_penalty": 0,
        "total_late_count": 0,
        "total_late_duration_s": 0,
        "total_late_penalty": 0,
        "total_penalty": 5549.307860249089,
    }

    mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': 100000},
        expected_metrics=expected_metrics)


@pytest.mark.parametrize('return_to_depot', [True, False])
@pytest.mark.parametrize('finish_at', [True, False])
def test_depot_deadline_return(return_to_depot, finish_at):
    """
    This test checks that pickup with depot deadline is delivered to depot even if return_to_depot is false
    """
    task = tools.get_test_json("depot_deadline_return.json")

    vehicle = task['vehicles'][0]
    vehicle['return_to_depot'] = return_to_depot
    if not finish_at:
        del vehicle['finish_at']

    expected_metrics = {
        'assigned_locations_count': 1,
        'max_vehicle_runs': 1
    }

    mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': 100000},
        expected_metrics=expected_metrics)
