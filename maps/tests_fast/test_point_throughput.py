import json

import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_throughput_vehicle():
    # Тест проверяет, что солвер умеет добавлять ожидание, чтобы избавляться от нарушений пропускной способности
    data = tools.get_test_json("point_throughput.json")
    result = mvrp_checker.solve_and_check(json.dumps(data), None, solver_arguments={'sa_iterations': 10000})
    assert result['metrics']['total_penalty'] == 0
    assert result['metrics']['total_waiting_duration_s'] >= 3 * 3600


def test_drop_off_throughput():
    # Тест проверяет, что солвер умеет добавлять ожидание перед drop_off, чтобы избавится от нарушения пропускной способности
    data = tools.get_test_json("drop_off_throughput.json")
    result = mvrp_checker.solve_and_check(json.dumps(data), None, solver_arguments={'sa_iterations': 10000})
    assert result['metrics']['total_penalty'] == 0
    assert result['metrics']['total_waiting_duration_s'] >= 3600 * 3
