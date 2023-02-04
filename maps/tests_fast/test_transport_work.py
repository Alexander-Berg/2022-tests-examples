import json

import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_transport_work():
    """
        Проверяем, что метрика transport_work вычисляется правильно
    """
    data = tools.get_test_json("transport_work.json")
    expected_metrics = {
        "total_transport_work_tonne_km": 38.35519311523438,
    }
    result = mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 2000},
        expected_metrics=expected_metrics,
        )
    for route in result["routes"]:
        value = route['metrics']['total_transport_work_tonne_km']
        assert value > 0, "Non-zero total_transport_work_tonne_km expected, found: %f" % value
