import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


"""
    Checks location load type incompatibility.
    Locations have load types and incompatibility of load types
    is defined globally in options. There should not be incompatible
    load types in the same route or in the same location. All these checks
    are implemented in mvrp_checker.
"""


def test_load_types_incompatibility():
    mvrp_checker.solve_and_check(
        tools.get_test_data("load_types.json"),
        solver_arguments={'sa_iterations': 100000})


def test_load_types_incompatibility_by_shifts():
    mvrp_checker.solve_and_check(
        tools.get_test_data("load_types_with_shifts.json"),
        solver_arguments={'sa_iterations': 10000})


def test_load_types_per_run_client_request():
    mvrp_checker.solve_and_check(
        tools.get_test_data("load_types_per_run_client_request.json"),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")


def test_load_types_per_run_simple_1():
    mvrp_checker.solve_and_check(
        tools.get_test_data("load_types_per_run_simple_1.json"),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")


def test_load_types_per_shift_simple_1():
    mvrp_checker.solve_and_check(
        tools.get_test_data("load_types_per_shift_simple_1.json"),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")


def test_load_types_per_run_drop_unfeasible():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data("load_types_per_run_drop_unfeasible.json"),
        solver_arguments={'sa_iterations': 10000},
        expected_status="PARTIAL_SOLVED")

    assert len(result['routes']) == 2


def test_unfeasible_load_types_in_planned_route():
    """
    In this test two orders with incompatible load types are assigned to
    a vehicle via planned_route, but they are placed in a non-optimal order there:
    [1, 0] where a location time window is violated, - while the optimal route is [0, 1]
    and it doesn't have time window violations.
    Previously the route was not optimized completely in this case and
    the planned route order was used, so we are checking here, that
    the optimal route is computed.
    """
    route = mvrp_checker.solve_and_check(
        tools.get_test_data('incompatible_load_types_in_planned_route.json'),
        solver_arguments={'sa_iterations': 10000},
        expected_status="UNFEASIBLE")

    expected_metrics = {
        "dropped_locations_count": 0,
        "assigned_locations_count": 2,
        "failed_time_window_locations_count": 0,
        "total_cost_with_penalty": 2000003264.58,
        "total_penalty": 2000000000.0
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics)


def test_separate_incompatible_load_types():
    """
    Check incompatible_load_types option on vehicle level
    """
    response = mvrp_checker.solve_and_check(
        tools.get_test_data("separate_incompatible_load_types.json"),
        solver_arguments={'sa_iterations': 10000},
        expected_status="PARTIAL_SOLVED")

    assert response["metrics"]["used_vehicles"] == 3
    assert response["metrics"]["dropped_locations_count"] == 1
    dropped = response["dropped_locations"][0]
    assert dropped["id"] == 0
    assert dropped["drop_reason"] == "No compatible vehicles: \nvehicle with id 1 (+ 2 more): location load types are incompatible in this vehicle"
