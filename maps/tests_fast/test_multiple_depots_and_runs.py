import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


def test_multiple_depots_and_runs():
    """
    This test checks that each vehicle uses only depot assigned to it.
    """
    task = tools.get_test_json("multiple_depots_and_runs.json")
    expected_metrics = {
        "assigned_locations_count": 9,
        "number_of_routes": 5,
        "max_vehicle_runs": 3
    }

    result = mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': 100000},
        expected_metrics=expected_metrics)

    depot_ids = {}
    for vehicle in task['vehicles']:
        depot_ids[vehicle['id']] = vehicle['depot_id']

    for route in result['routes']:
        depot_id = depot_ids[route['vehicle_id']]
        for node in route['route']:
            if node['node']['type'] == 'depot':
                assert node['node']['value']['id'] == depot_id


@pytest.mark.skip(reason="different depots in planned route are currently not allowed")
@pytest.mark.parametrize('planned', [True, False])
def test_multiple_depots_and_runs_planned(planned):
    """
    In this test the vehicle visits different depots because
    it is included in visited_locations and planned_route
    """
    task = tools.get_test_json("multiple_depots_and_runs_planned.json")
    runs = 6 if planned else 3
    expected_metrics = {
        "assigned_locations_count": 9,
        "number_of_routes": runs,
        "max_vehicle_runs": runs
    }
    iters = 0 if planned else 100000

    result = mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': iters},
        expected_metrics=expected_metrics)

    correct_depots = \
        ['2nd_depot', 'main_depot',
         'main_depot', 'main_depot',
         'main_depot', '2nd_depot',
         '2nd_depot', 'main_depot',
         'main_depot', 'main_depot',
         'main_depot', '2nd_depot'] if planned else \
        ['2nd_depot', 'main_depot',
         'main_depot', '2nd_depot',
         '2nd_depot', '2nd_depot']
    depots = []
    for route in result['routes']:
        for node in route['route']:
            if node['node']['type'] == 'depot':
                depots.append(node['node']['value']['id'])
    assert depots == correct_depots
