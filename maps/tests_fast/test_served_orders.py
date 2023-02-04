import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools

import json


def test_served_orders():
    """
    В задаче:
        1) delivery;
        2) 2хdelivery мультизаказ;
        3) pickup->delivery_to_any: dropoff;
        4) pickup->delivery_to: delivery;
        5) pickup->depot
        Итого 6 заказов.
    """
    request = tools.get_test_json('served_orders.json')
    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000}
    )
    expected_metrics = {"total_served_orders": 6}
    tools.check_metrics_are_close(response["metrics"], expected_metrics)
