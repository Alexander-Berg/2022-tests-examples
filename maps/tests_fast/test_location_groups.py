import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import copy
import json
import pytest


def test_location_group_simple():
    """
    This test includes:
    1. 5 locations, 1 of them must be dropped due to hard time windows
       of a depot and this location;
    2. 2 identical vehicles with a low fixed cost;

    Without location groups both vehicles must be used, otherwise we will get
    time window failures.

    When all 5 locations are included in a location group, solver
    should use 1 vehicle only, and a dropped location should not prevent
    the other locations assignement.
    """

    request = tools.get_test_json("location_group_simple.json")

    # solving with location groups
    mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            "assigned_locations_count": 4,
            "dropped_locations_count": 1,
            "used_vehicles": 1,
            "number_of_routes": 1,
            "total_failed_time_window_count": 1
        },
        expected_status='PARTIAL_SOLVED'
    )

    # solving without location groups
    del request["options"]["location_groups"]
    mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            "assigned_locations_count": 4,
            "dropped_locations_count": 1,
            "used_vehicles": 2,
            "number_of_routes": 2,
            "total_failed_time_window_count": 0
        },
        expected_status='PARTIAL_SOLVED'
    )


def test_location_groups():
    """
    A test of 4 location groups for 3 vehicles and 20 locations.
    The groups are linked to each other by pickup and delivery pairs
    and should be merged into 2 meta groups.

    Also this test verifies that 'visited locations' and 'planned route'
    work correctly with location groups.
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("location_groups.json"),
        None, solver_arguments={'sa_iterations': 100000})
    assert len(r['routes']) == 2, "Exactly 2 routes are expected in this test."


def test_location_groups_many_vehicles():
    """
    This test is similar to test_location_groups() but it includes 10 vehicles instead of 3
    and verifies that exactly two vehicles are used in the end.
    """
    r = mvrp_checker.solve_and_check(
        tools.get_test_data("location_groups_many_vehicles.json"),
        None, solver_arguments={'sa_iterations': 100000})
    assert len(r['routes']) == 2, "Exactly 2 routes are expected in this test."


def test_independent_groups():
    """
    This test includes:
    1. a location group which consists of 5 locations of weight 100kg each;
    2. a vehicle with capacity 400kg, no shifts, no multiple runs.
    The whole location group can't be assigned to the vehicle, we have
    to drop one location. It's OK because location groups are independent by default.
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data("independent_location_group.json"),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            "assigned_locations_count": 4,
            "dropped_locations_count": 1
        },
        expected_status='PARTIAL_SOLVED'
    )


def test_independent_groups_multiple_vehicles():
    """
    This test is the same as test_independent_groups but copied 100 times.
    Each of the 100 vehicles should serve 4 locations from a single location group,
    and 1 location from that group should be dropped.
    """

    task = tools.get_test_json('independent_location_group.json')
    n = 100

    for i in range(1, n):
        vehicle = copy.deepcopy(task['vehicles'][0])
        vehicle['id'] = i
        task['vehicles'].append(vehicle)

    for i in range(1, n):
        task['options']['location_groups'].append({'location_ids': []})
        for j in range(5):
            loc = copy.deepcopy(task['locations'][j])
            loc['id'] = str(10 ** 6 * (i + 1) + j)
            task['locations'].append(loc)
            task['options']['location_groups'][i]['location_ids'].append(loc['id'])

    mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 1500000},
        expected_metrics={
            "assigned_locations_count": n * 4,
            "dropped_locations_count": n
        },
        expected_status='PARTIAL_SOLVED'
    )


@pytest.mark.parametrize('use_group', [True, False])
@pytest.mark.parametrize('dependent_group', [True, False])
def test_solid_location_groups(use_group, dependent_group):
    """
    In this test there are 2 locations in solid location group, but all vehicles have capacity 1,
    so one location should be dropped. Without the group, both locations can be served in different runs.
    In case of dependent group both locations should be dropped.
    """

    if not use_group and dependent_group:
        return

    task = tools.get_test_json('solid_location_groups.json')

    if not use_group:
        del task['options']['location_groups']

    if dependent_group:
        task['options']['location_groups'][0]['dependent'] = True

    mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'assigned_locations_count': 0 if dependent_group else 1 if use_group else 2
        },
        expected_status='PARTIAL_SOLVED' if use_group else 'SOLVED'
    )


def test_location_group_with_drop_offs():
    """
    Test that drop-offs are added with pickup locations in location_group
    """
    task = tools.get_test_json('location_group_with_drop_offs.json')
    mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED'
    )
