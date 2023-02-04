import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_random_wait_if_early():
    """
    In this test adding location with wait_if_early set to true will not work.
    Random wait_if_early needed.
    """
    expected_metrics = {
        "assigned_locations_count": 102,
        "failed_time_window_depot_count": 0,
        "failed_time_window_locations_count": 100,
        "early_locations_count": 100,
    }

    mvrp_checker.solve_and_check(
        tools.get_test_data('random_wait_if_early.json'),
        None, solver_arguments={'sa_iterations': 500000},
        expected_metrics=expected_metrics)
