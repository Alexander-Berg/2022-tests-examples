import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_custom_walking_cost():
    # the test request has custom vehicle cost that contains some walking-related variables
    # we assume that solver will generate a solution with walking parts
    request = tools.get_test_json("custom_walking_cost.json")

    # here we crosscheck that the metrics in solver and in checker are close
    response = mvrp_checker.solve_and_check(
        json.dumps(request), None, solver_arguments={'sa_iterations': 10000})

    # and check that the route indeed contains walking nodes
    assert response['metrics']['total_walking_distance_m'] > 0
