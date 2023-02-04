import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def solve_and_check_routes_number(fname, expected_counts, iterations):
    result = mvrp_checker.solve_and_check(
        tools.get_test_data(fname), None,
        solver_arguments={'sa_iterations': iterations, 'sa_temperature': 100})
    vehicle_cnt = len(result['routes'])
    assert vehicle_cnt in expected_counts, "routes expected: %s, got: %d" % (str(expected_counts), vehicle_cnt)


def test_vehicles_number_100_locations():
    """
    Checks that solver optimizes vehicles number.
    Optimal solution of this task uses 2 vehicles out of 100.
    """
    solve_and_check_routes_number("test_vehicles_number.json", [2], 100000)


def test_vehicles_number_1k_locations():
    """
    Checks that solver optimizes vehicles number.
    Optimal solution of this task uses 34-36 vehicles depending on late risks.
    """
    solve_and_check_routes_number("test_vehicles_number_1k_locs.json", [34, 35, 36], 1100000)
