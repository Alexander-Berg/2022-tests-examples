import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_walking_courier():
    """
    Checks route with walking parts is shorter than the one without
    """

    task = tools.get_test_json('walking_courier.json')

    walking_route = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')

    del task['vehicles'][0]['walking_courier']
    no_walking_route = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')

    assert walking_route['metrics']['total_walking_distance_m'] > 0
    assert walking_route['metrics']['total_duration_s'] < no_walking_route['metrics']['total_duration_s']
