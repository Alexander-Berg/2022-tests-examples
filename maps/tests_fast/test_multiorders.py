import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest

from collections import defaultdict


def test_multiorders():
    mvrp_checker.solve_and_check(
        tools.get_test_data("multiorders.json"), None,
        solver_arguments={'sa_iterations': 100000},
        expected_status='PARTIAL_SOLVED')


def test_multiorders_penalty():
    mvrp_checker.solve_and_check(
        tools.get_test_data("multiorders_penalty.json"), None,
        solver_arguments={'sa_iterations': 100000},
        expected_metrics={
            'used_vehicles': 2,
            'number_of_routes': 4,
            'multiorders_extra_vehicles': 0
        })


def test_incompatible_load_type_multiorders():
    mvrp_checker.solve_and_check(
        tools.get_test_data("incompatible_multiorders.json"), None,
        solver_arguments={'sa_iterations': 100000})


def test_multiorder_threshold():
    """
    if some orders are not merged into a multiorder there will be
    failed time windows due to location service durations.
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("multiorder_threshold.json"), None,
        solver_arguments={'sa_iterations': 100000})
    assert r['metrics']['failed_time_window_locations_count'] == 0


def test_dont_merge_multiorders():
    """
    here we test that orders are not merged into a multiorder
    if this new multiorder is not compatible with any vehicle,
    even if the orders are assigned to a vehicle via planned_route.

    In particular, there are two orders of 1000 kg each in the request
    and a vehicle of capacity 1000 kg. So the vehicle needs to do 2 runs.
    If the orders are merged there will only be a single vehicle run and
    the solution will be unfeasible, so we check that the solution has
    status SOLVED (in solve_and_check).
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("dont_merge_multiorders.json"), None,
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'used_vehicles': 1,
            'number_of_routes': 2,
            'assigned_locations_count': 2
        })


def test_multiorder_lateness_risk():
    """
    Previously the solver considered the point which should be used for
    calculating the lateness risks for multiorders to be the service
    start of a concrete order in a multiorder. This is inconsistent
    with the fact that the service start of the whole multiorder is
    used to calculate late penalties, and thus was fixed. This test
    checks for the weird behaviour that occured earlier.
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("multiorder_lateness_risk.json"), None,
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'number_of_routes': 1,
            'lateness_risk_locations_count': 0
        })


def test_multiorders_early():
    """
    In this test the first 2 locations are in the same multiorder and should both be served early,
    but the third one has hard time window. If any of the first 2 locations are
    served on time, we are late on the third location and cannot serve it.
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data('multiorders_early.json'), None,
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'assigned_locations_count': 3,
            'dropped_locations_count': 0,
            'early_locations_count': 2,
            'total_early_duration_s': 15462.4
        })


def test_multiorders_wait():
    """
    In this test there are 3 locations in the same multiorder.
    We arrive early and should wait before serving them.
    This test checks that we only count waiting time once.
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data('multiorders_wait.json'), None,
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'assigned_locations_count': 3,
            'dropped_locations_count': 0,
            'early_locations_count': 0,
            'total_waiting_duration_s': 17581.15092610838
        })


def test_split_multiorders():
    """
    This test consists of:
    - 4 merged multiorders with hard window;
    - 4 merged multiorders with soft windows and high penalties (expected to wait time window);
    - 4 merged multiorders with soft windows and low penalties (expected to serve early).
    In each multiorder there are two orders with overlapping, but not coinciding windows.
    `merge_multiorders=true` is on, and we check if splitMultiorders() changes anything (it shouldn't).
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data('split_multiorders.json'),
        solver_arguments={'sa_iterations': 100000},
        expected_metrics={
            "failed_time_window_locations_count": 8,
            "failed_time_window_locations_count_penalty": 8,
            "total_late_count": 0,
            "total_waiting_duration_s": 9144.946,
        })


def test_client_service_duration():
    """
    Test that client_service_duration_s is computed separately for different clients in the same multiorder
    """
    response = mvrp_checker.solve_and_check(
        tools.get_test_data('client_service_duration.json'),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={})

    route = response["routes"][0]["route"]
    client_positions = defaultdict(list)
    for i, loc in enumerate(route):
        if loc["node"]["type"] == "depot":
            continue
        assert loc["multi_order"] == (i > 1)
        assert loc["departure_time_s"] - loc["arrival_time_s"] == 5200
        client_positions[loc["node"]["value"].get("client_id", "")].append(i)

    for positions in client_positions.values():
        assert len(positions) == 2
        assert positions[1] - positions[0] == 1


@pytest.mark.parametrize('merge_multiorders', [False, True])
def test_multiorders_can_be_merged(merge_multiorders):
    """
    Test that locations with can_be_merged=False aren't merged into multiorders
    """
    request = tools.get_test_json("multiorders_can_be_merged.json")
    request["options"]["merge_multiorders"] = merge_multiorders

    response = mvrp_checker.solve_and_check(
        json.dumps(request), None,
        solver_arguments={'sa_iterations': 10000})

    prev_can_be_merged = True
    merged_count = 0
    for loc_info in response["routes"][0]["route"]:
        if loc_info["node"]["type"] == "depot":
            continue
        can_be_merged = loc_info["node"]["value"]["can_be_merged"]
        if loc_info["multi_order"]:
            assert prev_can_be_merged and can_be_merged
            merged_count += 1
        prev_can_be_merged = can_be_merged
    assert merged_count == 2
