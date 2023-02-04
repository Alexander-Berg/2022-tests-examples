import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_delivery_to_any():
    """
    tests that we can move pickup locations with 'delivery_to_any' inside single route.
    For details see BBGEO-5602
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("delivery_to_any.json"), None,
        solver_arguments={'sa_iterations': 30000})
    assert len(r["routes"]) == 1
    route = r["routes"][0]
    assert route
    ids = [item["node"]["value"]["id"] for item in route["route"]]
    assert 'empty_tank' in ids[1:3]
