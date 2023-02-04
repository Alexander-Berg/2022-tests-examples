import datetime
import json

import pytest

import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_planned_route():
    route = mvrp_checker.solve_and_check(
        tools.get_test_data('planned_route.json'),
        None, solver_arguments={'sa_iterations': 10000})

    expected_metrics = {
        "dropped_locations_count": 0,
        "failed_time_window_locations_count": 5,
        "failed_time_window_shifts_count": 3,
        "total_cost_with_penalty": 60771.17044421899,
        "number_of_routes": 2,
        "used_vehicles": 1
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics)


def test_inconsistent_planned_route():
    """
    Test that when visited_locations is not prefix for planned_route, locations from
    planned_route are added after visited locations
    """
    request = tools.get_test_json('planned_route.json')
    vehicle = request["vehicles"][0]
    planned_route = vehicle["planned_route"]["locations"]
    visited_locations = vehicle["visited_locations"]
    planned_route.pop(0)
    visited_locations = visited_locations[:1]

    response = mvrp_checker.solve_and_check(
        tools.get_test_data('planned_route.json'),
        None, solver_arguments={'sa_iterations': 0})

    expected_orders = [visited_locations[0]["id"]]
    expected_orders += [loc["id"] for loc in planned_route if loc["id"] != 2080000000000000000]

    orders = []
    for route in response["routes"]:
        for loc in route["route"]:
            if loc["node"]["type"] != "depot":
                orders.append(loc["node"]["value"]["id"])

    assert orders == expected_orders


def test_location_assignment():
    """
    In this test all the locations are assinged to vehicles via planned_route option.
    A location with id N is assigned to a vehicle with the same id N.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('planned_route_2.json'),
        None, solver_arguments={'sa_iterations': 10000})

    expected_metrics = {
        "dropped_locations_count": 0,
        "number_of_routes": 10,
        "used_vehicles": 10
    }

    tools.check_metrics_are_close(result["metrics"], expected_metrics)

    for route in result['routes']:
        for item in route['route']:
            node = item['node']
            if node['type'] == 'location':
                assert node['value']['id'] == route['vehicle_id']


def test_no_planned_location_drops():
    """
    In this test only 19 out of 20 locations are planned and only 19 locations can
    be served due to vehicle capacity, so the rest single location should be dropped.
    No planned locations drops are expected.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('planned_route_3.json'),
        None, solver_arguments={'sa_iterations': 100000}, expected_status="PARTIAL_SOLVED")

    expected_metrics = {"dropped_locations_count": 1}
    tools.check_metrics_are_close(result["metrics"], expected_metrics)

    assert result['dropped_locations'][0]["id"] == "10"


def seconds_to_time(seconds):
    return (datetime.datetime(1970, 1, 1) + datetime.timedelta(seconds=seconds)).strftime("%H:%M")


def test_start_time():
    """
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('planned_route_start_time.json'),
        None, solver_arguments={'sa_iterations': 3})

    expected_metrics = {
        "dropped_locations_count": 0,
        "number_of_routes": 4,
        "used_vehicles": 4
    }

    tools.check_metrics_are_close(result["metrics"], expected_metrics)


def run_wait_if_early_test(wait_if_early, expected_metrics):
    request = tools.get_test_json('planned_route_wait_if_early.json')

    if wait_if_early is not None:
        for loc in request["vehicles"][0]["planned_route"]["locations"]:
            loc["wait_if_early"] = wait_if_early

    mvrp_checker.solve_and_check(json.dumps(request), expected_metrics=expected_metrics)


@pytest.mark.parametrize('wait_if_early', [None, True, False])
def test_wait_if_early(wait_if_early):
    """
    Planned routes option is used in UI to export solver results,
    and here we test that the option wait_if_early works as expected
    """

    metrics_with_waiting = {
        "assigned_locations_count": 3,
        "failed_time_window_depot_count": 1,
        "failed_time_window_locations_count": 2,
        "early_locations_count": 0,
        "total_waiting_duration_s": 48073.2
    }

    metrics_without_waiting = {
        "assigned_locations_count": 3,
        "failed_time_window_depot_count": 0,
        "failed_time_window_locations_count": 3,
        "early_locations_count": 3,
        "total_waiting_duration_s": 0
    }

    metrics = metrics_without_waiting if wait_if_early is False else metrics_with_waiting

    run_wait_if_early_test(wait_if_early, metrics)


def test_planed_route_non_optimal():
    """
    In this test wait_if_early is set incorrectly in planned route and
    solver should fix it.
    """
    test = tools.get_test_json('planned_route_non_optimal.json')
    result = mvrp_checker.solve_and_check(
        json.dumps(test), solver_arguments={'sa_iterations': 10000})
    del test["vehicles"][0]["planned_route"]

    result_no_plan = mvrp_checker.solve_and_check(
        json.dumps(test), solver_arguments={'sa_iterations': 10000})

    tools.check_metrics_are_close(result["metrics"], result_no_plan["metrics"])


def test_fix_planned_shifts():
    """
    In this test it is optimal to swap shifts of locations from planned route,
    but `fix_planned_shifts` option is set, so planned route should not be changed.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('fix_planned_shifts.json'), None,
        solver_arguments={'sa_iterations': 10000})

    planned_shifts = {}
    planned_route = result['vehicles'][0]['planned_route']['locations']
    for loc in planned_route:
        planned_shifts[loc['id']] = loc['shift_id']

    result_shifts = {}
    for route in result['routes']:
        shift_id = route['shift']['id']
        for loc in route['route']:
            node = loc['node']
            if node['type'] == 'depot':
                continue
            loc_id = node['value']['id']
            result_shifts[loc_id] = shift_id

    assert result_shifts == planned_shifts


def test_fixed_planned_route():
    """
    Same as test_fix_planned_shifts() but instead of using `fix_planned_shifts` option
    we use fixed_planned_route vehicle option to fix planned_route
    """
    request = tools.get_test_json('fix_planned_shifts.json')
    del request['options']['fix_planned_shifts']
    request['vehicles'][0]['fixed_planned_route'] = True

    result = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 1000})

    planned_route = result['vehicles'][0]['planned_route']['locations']
    pos = 0
    for route in result['routes']:
        shift_id = route['shift']['id']
        for loc in route['route']:
            node = loc['node']
            if node['type'] == 'depot':
                continue
            loc_id = node['value']['id']
            assert loc_id == planned_route[pos]['id']
            assert shift_id == planned_route[pos]['shift_id']
            pos += 1
    assert result['routes'][0]['shift']['start']['arrival_time_s'] == 39600


@pytest.mark.parametrize('start_at_garage', [True, False])
@pytest.mark.parametrize('planned_deliveries', [True, False])
def test_loaded_orders(start_at_garage, planned_deliveries):
    """
    Check work of loaded_orders options in planned route: make sure, that locations listed in
    loaded_orders lists are visited in corresponding runs, and other locations are visited after
    additional depot visit
    """
    request = tools.get_test_json('loaded_orders.json')
    vehicle = request["vehicles"][0]

    if not start_at_garage:
        vehicle["visit_depot_at_start"] = True
        vehicle.pop("start_at")
        vehicle["planned_route"]["locations"][0]["id"] = 999999999
    if not planned_deliveries:
        vehicle["planned_route"]["locations"] = [
            loc for loc in vehicle["planned_route"]["locations"] if loc["id"] in [0, 999999999]
        ]

    result = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 100000})

    assert result["metrics"]["number_of_routes"] == 3

    loaded_orders = []
    for loc in vehicle["planned_route"]["locations"]:
        if "loaded_orders" in loc:
            loaded_orders.append(set(loc["loaded_orders"]))

    for route_id in range(2):
        route_locations = set([
            loc["node"]["value"]["id"] for loc in result["routes"][route_id]["route"]
            if loc["node"]["value"].get("type", "") == "delivery"
        ])
        assert route_locations == loaded_orders[route_id]


def test_loaded_orders_one_run():
    """
    Check that vehicle is not allowed to visit depot, locations not listed in
    loaded_orders in garage are dropped or marked unfeasible if they are in planned_route
    """
    request = tools.get_test_json('loaded_orders.json')
    request["vehicles"][0]["max_runs"] = 1
    planned_route = request["vehicles"][0]["planned_route"]
    planned_route["locations"] = planned_route["locations"][:8] + [{
        "id": 67,
        "shift_id": "day"
    }]
    result = mvrp_checker.solve_and_check(
        json.dumps(request),
        expected_status="UNFEASIBLE",
        solver_arguments={'sa_iterations': 100000})
    assert result["metrics"]["number_of_routes"] == 1
    assert result["metrics"]["assigned_locations_count"] == len(planned_route["locations"]) - 1
    for loc in result["routes"][0]["route"]:
        if loc["node"]["value"]["id"] == 67:
            assert "unfeasible_reasons" in loc["node"]["value"]


@pytest.mark.skip()
def test_loaded_orders_empty_run():
    """
    Special case, when it is optimal to visit depot right after garage to take
    additional orders. Make sure, that this depot run won't be excluded because of empty run
    """
    request = tools.get_test_json('loaded_orders.json')
    planned_route = request["vehicles"][0]["planned_route"]
    request["depot"]["point"] = request["locations"][0]["point"]
    planned_route["locations"].pop(8)
    planned_route["locations"][0]["delivery_in_current_run"] = False
    planned_route["planned_runs_first"] = False
    result = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 100000})
    assert result["metrics"]["number_of_routes"] == 2
    assert result["routes"][0]["route"][0]["node"]["value"]["type"] == "garage"
    assert len(result["routes"][0]["route"]) == 2
    assert result["routes"][1]["route"][0]["node"]["type"] == "depot"


def test_multiple_middle_depots():
    """
    Checks that bug with multiple middle depots in planned_route,
    which crashed solver, is fixed now
    """
    request = tools.get_test_json('loaded_orders.json')
    request["vehicles"][0]["planned_route"]["locations"].append({
        "id": 999999999,
        "shift_id": "day"
    })
    mvrp_checker.solve_and_check(
        json.dumps(request),
        expected_status="PARTIAL_SOLVED",
        solver_arguments={'sa_iterations': 0})


@pytest.mark.parametrize("fixed", [True, False])
@pytest.mark.parametrize('optimize', [True, False])
def test_anchors(fixed, optimize):
    if fixed and optimize:
        return

    request = tools.get_test_json('planned_anchor.json')

    if fixed:
        request["vehicles"][0]["fixed_planned_route"] = True

    result = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000 if optimize else 0}
    )
    route = result["routes"][0]["route"]

    def contain(id, anchor_mode):
        for loc in route:
            if loc["node"]["value"]["id"] == id:
                if loc["node"]["value"]["anchor_mode"] == anchor_mode:
                    return True
        return False

    assert contain("anchor_2", "Decoupling")
    assert contain("anchor_2", "Rolling")
    assert contain("anchor_2", "Coupling")

    if not optimize:
        assert route[1]["node"]["value"]["id"] == "allow_trailer"
        assert route[2]["node"]["value"]["id"] == "anchor_2"
        assert route[3]["node"]["value"]["id"] == "forbid_trailer_1"
        assert route[4]["node"]["value"]["id"] == "anchor_2"
        assert route[5]["node"]["value"]["id"] == "forbid_trailer_2"
        assert route[6]["node"]["value"]["id"] == "anchor_2"
        assert route[2]["node"]["value"]["anchor_mode"] == "Decoupling"
        assert route[4]["node"]["value"]["anchor_mode"] == "Rolling"
        assert route[6]["node"]["value"]["anchor_mode"] == "Coupling"


@pytest.mark.parametrize('optimize', [True, False])
def test_crossdock(optimize):
    # При optimize == False тест проверяет, что решение представляет собой заданный маршрут
    # Есть решение с двумя машинами, но в planned_route указано использование машины, в которую нельзя поместить все заказы
    # При optimize == True тест проверяет, что в процессе оптимизации локации из planned_route остаются в исходной машине

    request = tools.get_test_json('planned_crossdock.json')

    result = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000 if optimize else 0},
        expected_status='SOLVED' if optimize else 'PARTIAL_SOLVED'
    )

    routes = result["routes"]

    if not optimize:
        assert len(routes) == 2
        route_from_depot_idx = 0 if routes[0]["vehicle_id"] == "vehicle_from_depot" else 1
        from_depot = routes[route_from_depot_idx]["route"]
        from_xdock = routes[1 - route_from_depot_idx]["route"]
        assert from_depot[1]["node"]["value"]["id"] == "xdock_1"
        assert from_depot[2]["node"]["value"]["id"] == "xdock_1"
        assert from_depot[1]["node"]["value"]["delivered_orders"] == ["planned_delivery"]
        assert from_depot[2]["node"]["value"]["picked_orders"] == ["planned_pickup"]

        from_xdock[1]["node"]["value"]["id"] == "planned_delivery"
        from_xdock[2]["node"]["value"]["id"] == "planned_pickup"

    if optimize:
        assert len(routes) == 3


def test_crossdock_fixed():
    # Проверяет что при fixed_planned_route: true солвер не падает

    request = tools.get_test_json('planned_crossdock.json')
    request["vehicles"][0]["fixed_planned_route"] = True
    request["vehicles"][1]["fixed_planned_route"] = True

    mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 0},
        expected_status='PARTIAL_SOLVED'
    )


def test_planned_parkings():
    request = tools.get_test_json('planned_route_parkings.json')

    result = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000},
        expected_status='SOLVED'
    )

    routes = result["routes"]
    # check that the first route still has parking nodes (the route is fixed)
    fixed_route_idx = 0 if routes[0]['vehicle_id'] == 1 else 1
    fixed_route = routes[fixed_route_idx]['route']
    assert len(fixed_route) == 7
    assert fixed_route[1]['node']['value']['type'] == "parking"
    assert fixed_route[4]['node']['value']['type'] == "parking"
