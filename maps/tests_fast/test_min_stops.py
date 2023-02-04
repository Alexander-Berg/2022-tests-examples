import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_min_stops():
    route = mvrp_checker.solve_and_check(
        tools.get_test_data('min_stops.json'),
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
        "number_of_routes": 4,
        "used_vehicles": 2,
        "total_stop_count_penalty": 28484,
        "total_guaranteed_penalty": 28484,
        "total_penalty": 28484
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics)


def test_min_stops_multiruns():
    route = mvrp_checker.solve_and_check(
        tools.get_test_data('min_stops_multiruns.json'),
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
        "number_of_routes": 3,
        "total_stop_count_penalty": 35000,
        "total_guaranteed_penalty": 35000,
        "total_penalty": 35000
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics)


def test_ignore_min_stops_for_unused():
    """
    Test ignore_min_stops_for_unused option
    If there are no minimal_stops limitations, vehicles are used unevenly and route_sizes == [5, 2]
    If there are no minimal_stops limitations and ignore_min_stops_for_unused == False,
    we are forced to use all vehicles and route_sizes == [3, 3, 1]
    If ignore_min_stops_for_unused == true, we can ignore third vehicle, but first two vehicles
    are still balanced and route_sizes == [4, 3]
    """
    response = mvrp_checker.solve_and_check(
        tools.get_test_data('ignore_min_stops_for_unused.json'),
        solver_arguments={'sa_iterations': 10000})

    route_sizes = []
    for route_info in response["routes"]:
        route_size = 0
        for node_info in route_info["route"]:
            if node_info["node"]["type"] == "location":
                route_size += 1
        route_sizes.append(route_size)

    route_sizes.sort(reverse=True)
    assert route_sizes == [4, 3]
