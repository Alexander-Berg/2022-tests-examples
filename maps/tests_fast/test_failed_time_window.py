import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_late_and_early_arrival_penalties():
    """
    Test of different penalties for late arrival and early arrival/serving.
    There are time window viloations of depot, locations and shift in this test.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('failed_time_window.json'), None,
        solver_arguments={'sa_iterations': 20000})

    expected_metrics = {
        "assigned_locations_count": 3,
        "total_stops": 3,
        "used_vehicles": 1,
        "early_depot_count": 1,
        "early_locations_count": 1,
        "failed_time_window_depot_count": 2,
        "failed_time_window_depot_count_penalty": 46,
        "failed_time_window_locations_count": 2,
        "failed_time_window_locations_count_penalty": 41,
        "late_depot_count": 1,
        "late_locations_count": 1,
        "late_shifts_count": 0,
        "total_early_count": 2,
        "total_failed_time_window_count": 4,
        "total_late_count": 2
    }

    tools.check_metrics_are_close(result["metrics"], expected_metrics)


def test_late_shift_arrival_penalties():
    """
    Test of late arrival penalties for shifts.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('failed_time_window_shift.json'), None,
        solver_arguments={'sa_iterations': 20000})

    expected_metrics = {
        "assigned_locations_count": 2,
        "dropped_locations_count": 0,
        "early_depot_count": 0,
        "early_locations_count": 1,
        "failed_time_window_depot_count": 1,
        "failed_time_window_depot_count_penalty": 33,
        "failed_time_window_locations_count": 1,
        "failed_time_window_locations_count_penalty": 11,
        "late_depot_count": 1,
        "late_locations_count": 0,
        "late_shifts_count": 2,
        "overtime_shifts_count": 2,
        "overtime_shifts_count_penalty": 107,
        "total_late_count": 3,
        "total_stops": 2
    }

    tools.check_metrics_are_close(result["metrics"], expected_metrics)
