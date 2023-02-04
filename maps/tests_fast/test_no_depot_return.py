import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


VehicleCost = {
    "km": 8,
    "hour": 100,
    "fixed": 0
}


def _get_base_task(task_type):
    tasks = {
        'mvrp': 'test_no_depot_return.json',
        'svrp': 'test_no_depot_return_svrp.json'
    }
    return tools.get_test_json(tasks[task_type])


def _print_json_object(obj, obj_name):
    print("\n{}:".format(obj_name))
    print(json.dumps(obj, indent=4))


def _print_task(task):
    _print_json_object(task, "Task")


def _print_route(route):
    _print_json_object(route, "Route")


def test_no_depot_return_all_vehicles():
    """
    Checks that all vehicles do not return to the depot
    if common 'return_to_depot' option is disabled
    """

    task = _get_base_task('mvrp')
    task['vehicles'] = [
        {
            'id': i + 1,
            'return_to_depot': False,
            'cost': VehicleCost
        } for i in range(3)
    ]
    _print_task(task)

    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')
    _print_route(route)

    assert route['metrics']['used_vehicles'] == 3
    assert len(route['routes']) == 3
    for vehicle_route_info in route['routes']:
        vehicle_route = vehicle_route_info['route']
        assert vehicle_route[-1]['node']['type'] != 'depot'


def test_no_depot_return_one_vehicle():
    """
    Checks that all vehicles but the first return to the depot if
    'return_to_depot' option is disabled only for the first vehicle
    """

    task = _get_base_task('mvrp')
    task['vehicles'] = [
        {
            'id': 0,
            'return_to_depot': False
        },
        {
            'id': 1
        },
        {
            'id': 2
        }
    ]
    for v in task['vehicles']:
        v['cost'] = VehicleCost

    _print_task(task)

    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')
    _print_route(route)

    assert route['metrics']['used_vehicles'] == 3
    assert len(route['routes']) == 3
    for vehicle_route_info in route['routes']:
        vehicle_route = vehicle_route_info['route']
        if vehicle_route_info['vehicle_id'] == 0:
            assert vehicle_route[-1]['node']['type'] != 'depot'
        else:
            assert vehicle_route[-1]['node']['type'] == 'depot'


def test_no_depot_return_svrp():
    """
    Checks that a vehicle does not return to the depot
    if svrp vehicle option 'return_to_depot' is disabled
    """

    task = _get_base_task('svrp')
    task['vehicle']['return_to_depot'] = False
    _print_task(task)

    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        kind='svrp',
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')
    _print_route(route)

    assert route['metrics']['used_vehicles'] == 1
    assert len(route['routes']) == 1
    vehicle_route_info = route['routes'][0]
    vehicle_route = vehicle_route_info['route']
    assert vehicle_route[-1]['node']['type'] != 'depot'


def test_depot_return_svrp():
    """
    Checks that a vehicle returns to the depot
    if svrp vehicle option 'return_to_depot' is enabled
    """

    task = _get_base_task('svrp')
    task['vehicle']['return_to_depot'] = True
    _print_task(task)

    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        kind='svrp',
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')
    _print_route(route)

    assert route['metrics']['used_vehicles'] == 1
    assert len(route['routes']) == 1
    vehicle_route_info = route['routes'][0]
    vehicle_route = vehicle_route_info['route']
    assert vehicle_route[-1]['node']['type'] == 'depot'


def test_depot_return_by_default_svrp():
    """
    Checks that a vehicle returns to the depot
    if svrp vehicle option 'return_to_depot' is set by default
    """

    task = _get_base_task('svrp')
    _print_task(task)

    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        kind='svrp',
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')
    _print_route(route)

    assert route['metrics']['used_vehicles'] == 1
    assert len(route['routes']) == 1
    vehicle_route_info = route['routes'][0]
    vehicle_route = vehicle_route_info['route']
    assert vehicle_route[-1]['node']['type'] == 'depot'


@pytest.mark.parametrize('shifts', [True, False])
@pytest.mark.parametrize('return_to_depot', [True, False])
def test_max_runs(shifts, return_to_depot):
    """
    This test verifies that the last depot is used if return_to_depot is True and
    is ignored if return_to_depot is False, even if this depot is specified
    in planned_route or inserted by solver during optimization due to max_runs > 2
    and small number of iterations (the last case is simulated via planned_route).
    """

    request = tools.get_test_json('no_final_depot_and_multi_runs.json')
    vehicle = request["vehicles"][0]

    vehicle['return_to_depot'] = return_to_depot

    route = [
        {"id": "0"},
        {"id": "1"},
        {"id": "depot"}
    ]

    if shifts:
        vehicle["shifts"] = [
            {"id": "0", "time_window": "08:00 - 23:00"}
        ]
        for node in route:
            node["shift_id"] = "0"

    vehicle["planned_route"] = {"locations": route}

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 0},
        expected_metrics={
            "dropped_locations_count": 0,
            "assigned_locations_count": 2
        }
    )

    assert len(response["routes"]) == 1
    route = response["routes"][0]["route"]

    expected_loc_type = "depot" if return_to_depot else "location"
    assert route[-1]["node"]["type"] == expected_loc_type
