import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_pickup_orders():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data("pickup_orders.json"), None,
        solver_arguments={'sa_iterations': 100000})

    routes = result['routes']
    assert len(routes) == 1, "1 route expected"

    route = routes[0]
    loc_ids = [loc['node']['value']['id'] for loc in route['route']]
    pickup_order_id = 5
    assert loc_ids[-2:] == [pickup_order_id, 0], "Pickup order can only be served the last!"
