import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_no_time_travel():
    data = tools.get_test_json("wait_in_multiorders_no_time_travel.json")

    mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 10000},
        expected_status="PARTIAL_SOLVED"
    )
