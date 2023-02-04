import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_baltika():
    route = mvrp_checker.solve_and_check(
        tools.get_test_data('choose_vehicles.json'),
        None,
        solver_arguments={'sa_iterations': 3000000})

    expected_metrics = {
        "total_cost_with_penalty": 170858.866
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics)
