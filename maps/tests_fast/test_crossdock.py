import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_crossdock():
    """
    Checks crossdocks
    """

    task = tools.get_test_json('crossdock.json')
    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 100000},
        expected_status='PARTIAL_SOLVED')

    assert route['metrics']['used_vehicles'] == 5
    assert route['metrics']['dropped_locations_count'] == 2
    assert len(route['routes']) == 5
