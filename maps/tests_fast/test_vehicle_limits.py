import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_vehicle_limits():
    """
    Checks that vehicle limits are used in solver correctly.
    Input data is created so, that each vehicle must get an order with the same id as vehicle's id.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data("test_vehicle_limits.json"), None,
        solver_arguments={'sa_iterations': 1000000})
    routes = result['routes']
    assert len(routes) == 5, "5 routes expected"
    for route in routes:
        loc_ids = [loc['node']['value']['id'] for loc in route['route']]
        assert loc_ids == [0, route["vehicle_id"], 0], "Wrong vehicle selected for route"


def test_zero_weight_and_volume():
    """
    Checks if zero shipment weight and volume are used by solver correctly.
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("zero_weight_volume.json"), None,
        solver_arguments={'sa_iterations': 100000})
