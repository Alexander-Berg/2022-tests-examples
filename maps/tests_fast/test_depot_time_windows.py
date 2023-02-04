import json

import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_depot_multiple_time_windows():
    """
        Checks that total duration of route with depot windows [7:00-8:00],[22:00-23:00] is greater than
         one with [7:00-23:00], because we wait for time window to return to the depot.
    """
    data = tools.get_test_json("depot_multiple_time_windows.json")

    response_multiple = mvrp_checker.solve_and_check(json.dumps(data), None, solver_arguments={'sa_iterations': 10000})

    data['depot']['time_windows'] = [{"time_window": "07:00-23:00", "hard_time_window": "07:00-23:00"}]
    response_single = mvrp_checker.solve_and_check(json.dumps(data), None, solver_arguments={'sa_iterations': 10000})

    assert response_multiple["metrics"]["total_duration_s"] > response_single["metrics"]["total_duration_s"]
