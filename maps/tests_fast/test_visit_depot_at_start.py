import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_no_depot_return_all_vehicles():
    """
    Checks that all vehicles do not start from the depot
    if common 'visit_depot_at_start' option is disabled
    """

    task = tools.get_test_json('test_visit_depot_at_start.json')
    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')

    assert route['metrics']['used_vehicles'] == 3
    assert len(route['routes']) == 3
    for vehicle_route_info in route['routes']:
        vehicle_route = vehicle_route_info['route']
        assert vehicle_route[0]['node']['type'] != 'depot'


def test_no_visit_depot_at_start_vehicle():
    """
    Checks that all vehicles but the first start from the depot if
    'visit_depot_at_start' option is disabled only for the first vehicle
    """

    task = tools.get_test_json('test_visit_depot_at_start.json')
    start_at_depot_ids = (2, 3)
    for vehicle in task['vehicles']:
        if vehicle['id'] in start_at_depot_ids:
            vehicle['visit_depot_at_start'] = True

    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')

    assert route['metrics']['used_vehicles'] == 3
    assert len(route['routes']) == 3
    for vehicle_route_info in route['routes']:
        vehicle_route = vehicle_route_info['route']
        if vehicle_route_info['vehicle_id'] not in start_at_depot_ids:
            assert vehicle_route[0]['node']['type'] != 'depot'
        else:
            assert vehicle_route[0]['node']['type'] == 'depot'


def test_no_visit_depot_at_start_vehicle_drop():
    """
    Checks that having delivery with all vehicles
    'visit_depot_at_start' option is disabled and with 'max_runs'=1
    leads to drop.
    """

    task = tools.get_test_json('test_visit_depot_at_start.json')
    delivery_id = 0
    for location in task['locations']:
        if location['id'] == delivery_id:
            location['type'] = 'delivery'

    mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='PARTIAL_SOLVED')


def test_no_visit_depot_at_start_vehicle_no_drop():
    """
    Checks that having delivery with all vehicles
    'visit_depot_at_start' option is disabled and one vehicle with 'max_runs'=2
    solves without any drops.
    """

    task = tools.get_test_json('test_visit_depot_at_start.json')
    delivery_id = 0
    for location in task['locations']:
        if location['id'] == delivery_id:
            location['type'] = 'delivery'
    task['vehicles'][0]['max_runs'] = 2

    mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')


def test_no_visit_depot_at_start_svrp():
    """
    Checks that a vehicle does not return to the depot
    if svrp vehicle option 'visit_depot_at_start' is disabled
    """

    task = tools.get_test_json('test_visit_depot_at_start_svrp.json')
    task['vehicle']['visit_depot_at_start'] = False
    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        kind='svrp',
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')

    assert route['metrics']['used_vehicles'] == 1
    assert len(route['routes']) == 1
    vehicle_route_info = route['routes'][0]
    vehicle_route = vehicle_route_info['route']
    assert vehicle_route[0]['node']['type'] != 'depot'


def test_depot_return_svrp():
    """
    Checks that a vehicle returns to the depot
    if svrp vehicle option 'visit_depot_at_start' is enabled
    """

    task = tools.get_test_json('test_visit_depot_at_start_svrp.json')
    task['vehicle']['visit_depot_at_start'] = True

    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        kind='svrp',
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')

    assert route['metrics']['used_vehicles'] == 1
    assert len(route['routes']) == 1
    vehicle_route_info = route['routes'][0]
    vehicle_route = vehicle_route_info['route']
    assert vehicle_route[0]['node']['type'] == 'depot'


def test_visit_depot_at_start_by_default_svrp():
    """
    Checks that a vehicle returns to the depot
    if svrp vehicle option 'return_to_depot' is set by default
    """

    task = tools.get_test_json('test_visit_depot_at_start_svrp.json')

    route = mvrp_checker.solve_and_check(
        json.dumps(task),
        kind='svrp',
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED')

    assert route['metrics']['used_vehicles'] == 1
    assert len(route['routes']) == 1
    vehicle_route_info = route['routes'][0]
    vehicle_route = vehicle_route_info['route']
    assert vehicle_route[0]['node']['type'] == 'depot'
