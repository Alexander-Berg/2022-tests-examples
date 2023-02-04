import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_max_stops():
    route = mvrp_checker.solve_and_check(
        open(tools.arcadia_path('tests_data/max_stops.json')).read(),
        None,
        solver_arguments={'sa_iterations': 500000})

    expected_metrics = {
        "dropped_locations_count": 0,
        "early_locations_count": 0,
        "early_shifts_count": 0,
        "failed_time_window_locations_count": 0,
        "failed_time_window_shifts_count": 0,
        "late_locations_count": 0,
        "late_shifts_count": 0,
        "lateness_risk_locations_count": 0,
        "total_early_duration_s": 0.0,
        "total_failed_time_window_duration_s": 0.0,
        "total_late_duration_s": 0.0,
        "total_lateness_risk_probability": 0,
        "number_of_routes": 3,
        "used_vehicles": 2,
        "total_stop_count_penalty": 15015,
        "total_guaranteed_penalty": 15015,
        "total_penalty": 15015
    }
    tools.check_metrics_are_close(route["metrics"], expected_metrics)


def test_max_stops_multiruns():
    route = mvrp_checker.solve_and_check(
        open(tools.arcadia_path('tests_data/max_stops_multiruns.json')).read(),
        None,
        solver_arguments={'sa_iterations': 50000})

    expected_metrics = {
        "dropped_locations_count": 0,
        "early_locations_count": 0,
        "early_shifts_count": 0,
        "failed_time_window_locations_count": 0,
        "failed_time_window_shifts_count": 0,
        "late_locations_count": 0,
        "late_shifts_count": 0,
        "lateness_risk_locations_count": 0,
        "total_early_duration_s": 0.0,
        "total_failed_time_window_duration_s": 0.0,
        "total_late_duration_s": 0.0,
        "total_lateness_risk_probability": 0,
        "number_of_routes": 2,
        "total_stop_count_penalty": 18018,
        "total_guaranteed_penalty": 18018,
        "total_penalty": 18018
    }
    tools.check_metrics_are_close(route["metrics"], expected_metrics)
