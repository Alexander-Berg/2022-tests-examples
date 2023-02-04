import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_compatibility():
    """
    checks vehicle/location compatibility using tags
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("test_tags.json"),
        solver_arguments={'sa_iterations': 1000},
        expected_status="PARTIAL_SOLVED")


def test_optional_tags():
    """
    Test with 3 locations and 3 vehicles.
    Each location should be served by vehicle with the most suitable tags.
    """

    route_tags_costs = [-150, -100, 0]
    route_locs = [2, 0, 1]

    result = mvrp_checker.solve_and_check(
        tools.get_test_data('optional_tags.json'),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'total_optional_tags_cost': sum(route_tags_costs)
        }
    )

    for idx, route in enumerate(result['routes']):
        assert route['metrics']['total_optional_tags_cost'] == route_tags_costs[idx]
        assert route['route'][1]['node']['value']['id'] == route_locs[idx]
