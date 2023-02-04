import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_custom_capacity():
    """
    In this test vehicle 0 can only deliver order to location 0,
    vehicle 1 can only deliver order to location 1
    and location 2 can not be served.
    """
    task = tools.get_test_json("custom_capacity.json")

    expected_metrics = {
        "assigned_locations_count": 2,
        "dropped_locations_count": 1,
    }

    result = mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': 10000},
        expected_metrics=expected_metrics,
        expected_status="PARTIAL_SOLVED")

    for route in result['routes']:
        for node in route['route']:
            if node['node']['type'] == 'location':
                assert node['node']['value']['id'] == route['vehicle_id']
