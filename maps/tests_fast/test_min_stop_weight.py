import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_min_stop_weight():
    response = mvrp_checker.solve_and_check(
        tools.get_test_data('min_stop_weight.json'),
        None,
        solver_arguments={'sa_iterations': 10000})

    assert response["metrics"]["total_min_stop_weight_penalty"] == 0
