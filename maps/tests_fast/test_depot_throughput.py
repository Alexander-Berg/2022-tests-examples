import json

import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def run_depot_throughput_default_penalty(throughput):
    """
       Solving task with the default throughput penalties.
       Penalty value is checked by mvrp_checker.
    """
    data = tools.get_test_json("depot_throughput.json")
    del data['depot']['penalty']
    data['depot']['throughput'] = throughput
    mvrp_checker.solve_and_check(json.dumps(data), None, solver_arguments={'sa_iterations': 20000})


def test_depot_throughput_kg_default_penalty():
    run_depot_throughput_default_penalty({'kg_per_hour': 1000})


def test_depot_throughput_units_default_penalty():
    run_depot_throughput_default_penalty({'units_per_hour': 5})


def test_depot_throughput_kg_penalty():
    """
       Solving task without flexible start time. That means the task cannot be solved
       without depot throughput exceeding, so there must be a non-zero depot penalty
       which value is checked by mvrp_checker.
    """
    data = tools.get_test_json("depot_throughput.json")
    data['depot']['flexible_start_time'] = False
    mvrp_checker.solve_and_check(json.dumps(data), None, solver_arguments={'sa_iterations': 20000})


def test_depot_throughput_custom_units_penalty():
    """
       The same as test_depot_throughput_kg_penalty but using custom units instead of kg.
    """
    data = tools.get_test_json("depot_throughput_custom_units.json")
    data['depot']['flexible_start_time'] = False
    mvrp_checker.solve_and_check(json.dumps(data), None, solver_arguments={'sa_iterations': 100000})


def test_depot_throughput_kg():
    """
        Solving task using flexible start time, enough number of iterations:
        the task must be solved without depot throughput exceeding.
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("depot_throughput.json"), None,
        solver_arguments={'sa_iterations': 20000})
    penalty = r['metrics']['total_depot_penalty']
    assert penalty == 0, "Non zero depot penalty detected: %f" % penalty


def test_depot_throughput_custom_units():
    """
        The same as test_depot_throughput_kg but using custom units instead of kg.
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("depot_throughput_custom_units.json"), None,
        solver_arguments={'sa_iterations': 100000})
    penalty = r['metrics']['total_depot_penalty']
    assert penalty == 0, "Non zero depot penalty detected: %f" % penalty


def test_depot_throughput_custom_units_shifts():
    """
        The same as test_depot_throughput_kg but using custom units instead of kg.
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("depot_throughput_custom_units_shifts.json"), None,
        solver_arguments={'sa_iterations': 100000})
    penalty = r['metrics']['total_depot_penalty']
    assert penalty == 0, "Non zero depot penalty detected: %f" % penalty


def test_depot_throughput_1pickup_1delivery():
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("depot_throughput_1pickup_1delivery.json"), None,
        solver_arguments={'sa_iterations': 10000})
    penalty = r['metrics']['total_depot_penalty']
    assert penalty == 0, "Non zero depot penalty detected: %f" % penalty


def test_depot_throughput_pickups_only_with_penalty():
    mvrp_checker.solve_and_check(
        tools.get_test_data("depot_throughput_pickups_only.json"), None,
        solver_arguments={'sa_iterations': 30000})


def test_depot_throughput_pickup_and_delivery():
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("depot_throughput_pickup_and_delivery.json"), None,
        solver_arguments={'sa_iterations': 20000})
    penalty = r['metrics']['total_depot_penalty']
    assert penalty == 0, "Non zero depot penalty detected: %f" % penalty


def test_multi_depot_throughput():
    """
    There are 3 depots in this test:
        "depot1" and "depot3" have throughput limit defined in kg per hour;
        "depot2" has throughput limit defined in units per hour.

    Some locations in this test are deliveries, some are pickups and
    there is also one pickup-and-delivery pair in the test.

    Due to different vehicle capacity in units and kilograms some
    vehicles cannot handle some locations.

    Depot service durations and depot throughputs are defined in this test
    in such a way, that throughput limit is violated for all three depots.

    The exact value of penalty for throughput violation is checked in mvrp_checker.
    """

    result = mvrp_checker.solve_and_check(
        tools.get_test_data("multi_depot_throughput.json"), None,
        solver_arguments={'sa_iterations': 20000})

    penalty = result['metrics']['total_depot_penalty']
    assert penalty > 0, "Non-zero depot penalty expected, found: %f" % penalty

    assert len(result["routes"]) == 3, "exactly 3 routes are expected"

    for route in result["routes"]:
        penalty = route['metrics']['total_depot_penalty']
        assert penalty > 0, "Non-zero depot penalty expected, found: %f" % penalty


def test_vehicles_depot_throughput():
    """
        Solving task using vehicles time window:
        the task must be solved with ~3xVehicles penalties.
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("depot_throughput_vehicles.json"), None,
        solver_arguments={'sa_iterations': 20000})
    assert tools.is_abs_close(r['metrics']['total_depot_penalty'], 300000, 1e-3)


def test_package_throughput():
    """
        В тесте надо развести 2 заказа, в одну машину помещается только 1, другая выезжает слишком рано
        и не может забрать второй без нарушения пропускной способности
    """

    response = mvrp_checker.solve_and_check(
        tools.get_test_data("package_throughput.json"),
        solver_arguments={'sa_iterations': 10000}
    )

    assert len(response["routes"]) == 2


def test_package_throughput_windows():
    """
        В тесте у окон до 11:00 задана нулевая пропускная способность и первый готовый заказ появится в 12:00
        Тест проверяет, что солвер находит решение без штрафа
    """

    response = mvrp_checker.solve_and_check(
        tools.get_test_data("package_throughput_windows.json"),
        solver_arguments={'sa_iterations': 10000}
    )

    assert float(response["routes"][0]["route"][0]["arrival_time_s"]) >= 3600 * 12


def test_timed_units_depot_throughput():
    """
        Solving task using units_per_hour time window:
        the task must be solved with ~20xunits throughput penalties.
    """
    request = tools.get_test_json("depot_throughput_units_timed.json")
    response = mvrp_checker.solve_and_check(
        json.dumps(request), None,
        solver_arguments={'sa_iterations': 100000})

    assert tools.is_abs_close(response['metrics']['total_depot_penalty'],
                              request['depot']['penalty']['throughput']['fixed'] + request['depot']['penalty']['throughput']['unit'] * 20,
                              1e-3)
