import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_multidepots():
    mvrp_checker.solve_and_check(
        tools.get_test_data("multidepots.json"), None,
        solver_arguments={'sa_iterations': 100000})


def test_depot_id():
    mvrp_checker.solve_and_check(
        tools.get_test_data("depot_id.json"), None,
        solver_arguments={'sa_iterations': 10000})
