import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
from copy import deepcopy


def task_variations():
    task = tools.get_test_json("service_duration_adjustment.json")
    tasks = {}
    unit = 5400
    curr = tasks["location_order_service"] = deepcopy(task)
    for loc in curr["locations"]:
        loc["shared_service_duration_s"] = loc["service_duration_s"] = unit
    curr = tasks["depot_order_service"] = deepcopy(task)
    for loc in curr["locations"]:
        loc["depot_duration_s"] = unit
    curr = tasks["depot_start_service"] = deepcopy(task)
    curr["depot"]["service_duration_s"] = 4 * unit
    curr = tasks["depot_finish_service"] = deepcopy(task)
    curr["depot"]["finish_service_duration_s"] = 4 * unit
    return tasks


def solve_and_assert(task, expected_routes=None, expected_late_shifts=None, **multipliers):
    for v in task["vehicles"]:
        v.update(multipliers)
    result = mvrp_checker.solve_and_check(json.dumps(task),
                                          None,
                                          solver_arguments={
                                              'sa_iterations': 10000,
                                              'sa_temperature': 50})
    vehicle_cnt = len(result['routes'])
    assert vehicle_cnt == expected_routes, \
        "routes expected: %d, got: %d" % (expected_routes, vehicle_cnt)
    late_shift_cnt = result['metrics']['late_shifts_count']
    assert late_shift_cnt == expected_late_shifts, \
        "late shifts expected: %d, got: %d" % (expected_late_shifts, late_shift_cnt)


def test_missing_service_duration_adjustment():
    """
    Test case when no service_duration_multiplier is present
    """
    for task in task_variations().values():
        solve_and_assert(task, expected_routes=1, expected_late_shifts=0)


def test_default_service_duration_adjustment():
    """
    Test case when service_duration_multipliers are specified with default values
    """
    for task in task_variations().values():
        solve_and_assert(task,
                         expected_routes=1,
                         expected_late_shifts=0,
                         service_duration_multiplier=1.0,
                         shared_service_duration_multiplier=1.0)


def test_depot_duration():
    """
    Test that depot_duration_s is unaffected by service_duration_multiplier
    """
    solve_and_assert(task_variations()["depot_order_service"],
                     expected_routes=1, expected_late_shifts=0,
                     service_duration_multiplier=2.9)


def test_slow_service():
    """
    Test case when the workers are much slower at performing non-shared service part.
    In this case all three workers are needed to fit the time window.
    """
    solve_and_assert(task_variations()["location_order_service"],
                     expected_routes=3, expected_late_shifts=0,
                     service_duration_multiplier=2.9)


def test_slow_depot_service():
    """
    Test case when the workers are much slower, and this results in late shift on depot closing
    """
    for key in ["start", "finish"]:
        solve_and_assert(task_variations()["depot_%s_service" % key],
                         expected_routes=1, expected_late_shifts=1,
                         service_duration_multiplier=2.9)


def test_slow_shared_service():
    """
    Test case when the workers are much slower at performing shared service part.
    In this case only two workers are needed to fit the window.
    """
    solve_and_assert(task_variations()["location_order_service"],
                     expected_routes=2, expected_late_shifts=0,
                     shared_service_duration_multiplier=2.9)


def test_depot_shared():
    """
    Test that depot-related timings are unaffected by shared_service_duration_multiplier
    """
    for key, value in task_variations().items():
        if key != "location_order_service":
            solve_and_assert(value, expected_routes=1, expected_late_shifts=0,
                             shared_service_duration_multiplier=2.9)
