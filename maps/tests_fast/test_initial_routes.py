import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools

import json
from collections import defaultdict
import difflib
import pytest
from copy import deepcopy


def solve_with_initial_routes(request, old_response, sa_iterations, expected_status="SOLVED"):
    request["initial_routes"] = old_response["routes"]

    new_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        expected_status=expected_status,
        solver_arguments={'sa_iterations': sa_iterations}
    )

    return new_response


def get_vehicles_locations(response, loc_type=None, use_route_ids=False):
    vehicle_orders = defaultdict(set)
    for i, route in enumerate(response["routes"]):
        for loc in route["route"]:
            if loc_type is not None and loc["node"]["type"] == loc_type:
                vehicle_orders[i if use_route_ids else route["vehicle_id"]].add(loc["node"]["value"]["id"])
    return vehicle_orders


def check_responses_equal(old_response, new_response):
    old_response_lines = json.dumps(old_response, indent=4, sort_keys=True).split("\n")
    new_response_lines = json.dumps(new_response, indent=4, sort_keys=True).split("\n")
    tokens_to_ignore = [
        "+++", "---", "@", "operations_per_second",
        "optimization_steps", "iterations"
    ]
    diff_lines = []
    for line in difflib.unified_diff(old_response_lines, new_response_lines,
                                     fromfile='old', tofile='new', n=0):
        if all([token not in line for token in tokens_to_ignore]):
            diff_lines.append(line)
    assert len(diff_lines) == 0, "Old and new responses are different, diff:\n" + "\n".join(diff_lines)


def test_initial_routes():
    """
    Check that initial routes are loaded from the file correctly
    (i.e. response after 0 iterations will be the same)
    """
    request = tools.get_test_json('initial_routes.json')
    old_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 10000}
    )

    new_response = solve_with_initial_routes(request, old_response, 0)
    check_responses_equal(old_response, new_response)


def test_initial_routes_fixed_vehicle():
    """
    Check that all orders, for which fixed_vehicle option is set,
    will stay in the same vehicle
    """
    request = tools.get_test_json("initial_routes.json")
    old_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 10000}
    )

    for loc in old_response["routes"][0]["route"]:
        if loc["node"]["type"] == "location":
            loc["node"]["value"]["fixed_vehicle"] = True
    old_vehicles_orders = get_vehicles_locations(old_response, "location")

    new_response = solve_with_initial_routes(request, old_response, 10000)
    new_vehicle_orders = get_vehicles_locations(new_response, "location")
    assert old_vehicles_orders[1].issubset(new_vehicle_orders[1])


def test_initial_routes_keep_in_vehicle():
    """
    Check, that 5 orders, for which keep_in_vehicle option is set,
    still take up space in the vehicle even when they are dropped,
    and only 2 new orders can be added
    """
    request = tools.get_test_json("initial_routes.json")
    request["vehicles"] = request["vehicles"][:1]
    old_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 10000},
        expected_status="PARTIAL_SOLVED"
    )

    old_vehicles_orders = get_vehicles_locations(old_response, "location")
    request["vehicles"][0]["capacity"]["weight_kg"] = 7
    for loc in request["locations"]:
        if loc["id"] in old_vehicles_orders[1]:
            loc["penalty"] = {
                "drop": 0
            }

    for loc in old_response["routes"][0]["route"]:
        if loc["node"]["type"] == "location":
            loc["node"]["value"]["fixed_vehicle"] = True
            loc["node"]["value"]["keep_in_vehicle"] = True

    new_response = solve_with_initial_routes(
        request, old_response,
        10000, expected_status="PARTIAL_SOLVED")
    new_vehicle_orders = get_vehicles_locations(new_response, "location")
    assert len(set.intersection(old_vehicles_orders[1], new_vehicle_orders[1])) == 0
    assert new_response["metrics"]["assigned_locations_count"] == 2


def test_initial_routes_undroppable():
    """
    Check, that orders for which undroppable option is set
    aren't dropped even when it creates unfeasible solution
    """
    request = tools.get_test_json("initial_routes.json")
    old_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 10000}
    )

    for vehicle in request["vehicles"]:
        vehicle["capacity"]["weight_kg"] = 3
    for route in old_response["routes"]:
        for loc in route["route"]:
            if loc["node"]["type"] == "location":
                loc["node"]["value"]["undroppable"] = True

    new_response = solve_with_initial_routes(
        request, old_response, 10000, expected_status="UNFEASIBLE")
    assert new_response["metrics"]["dropped_locations_count"] == 0


def test_initial_routes_loaded_orders():
    """
    Check loaded_orders option in initial routes
    (no orders are taken other than the ones which were loaded)
    """
    request = tools.get_test_json("initial_routes.json")
    request["vehicles"] = request["vehicles"][:1]
    old_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        expected_status="PARTIAL_SOLVED",
        solver_arguments={'sa_iterations': 10000}
    )

    request["vehicles"][0]["capacity"]["weight_kg"] = 10
    old_vehicles_orders = get_vehicles_locations(old_response, "location")
    loaded_orders = list(old_vehicles_orders[1])
    route = old_response["routes"][0]["route"]
    route[0]["node"]["value"]["loaded_orders"] = loaded_orders

    new_response = solve_with_initial_routes(
        request, old_response, 10000, expected_status="PARTIAL_SOLVED")
    new_vehicle_orders = get_vehicles_locations(new_response, "location")
    assert new_response["metrics"]["assigned_locations_count"] == len(loaded_orders)
    assert new_vehicle_orders[1] == set(loaded_orders)


def test_initial_routes_fixed_position():
    """
    Check that orders, for which positions are fixed, won't move anywhere
    """
    request = tools.get_test_json("initial_routes.json")
    old_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 10000}
    )

    old_vehicles_routes = defaultdict(list)
    for route in old_response["routes"]:
        for loc in route["route"][:3]:
            loc["node"]["value"]["fixed_position"] = True
            old_vehicles_routes[route["vehicle_id"]].append(loc["node"]["value"]["id"])

    new_response = solve_with_initial_routes(
        request, old_response, 10000, expected_status="SOLVED")

    for route in new_response["routes"]:
        for i, loc in enumerate(route["route"][:3]):
            assert loc["node"]["value"]["id"] == old_vehicles_routes[route["vehicle_id"]][i]


@pytest.mark.parametrize("level", ["all", "vehicle", "shift", "route"])
def test_initial_routes_route_fixation(level):
    request = tools.get_test_json("initial_routes_route_fixation.json")
    old_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 10000}
    )
    assert len(old_response["routes"]) == 8

    if level == "all":
        fixed_routes_ids = [0, 1, 2, 3, 4, 5, 6, 7]
        request["options"]["immutable"] = True
    elif level == "vehicle":
        fixed_routes_ids = [4, 5, 6, 7]
        request["vehicles"][1]["immutable"] = True
    elif level == "shift":
        fixed_routes_ids = [4, 5]
        request["vehicles"][1]["shifts"][0]["immutable"] = True
    elif level == "route":
        fixed_routes_ids = [2, 5]
        old_response["routes"][2]["immutable"] = True
        old_response["routes"][5]["immutable"] = True

    old_vehicles_routes = get_vehicles_locations(old_response, "location", use_route_ids=True)

    new_response = solve_with_initial_routes(
        request, old_response, 10000, expected_status="SOLVED")

    new_vehicles_routes = get_vehicles_locations(new_response, "location", use_route_ids=True)

    for route_id in fixed_routes_ids:
        assert old_vehicles_routes[route_id] == new_vehicles_routes[route_id]


@pytest.mark.parametrize("level", ["shift", "run"])
def test_initial_routes_order_run_fixation(level):
    request = tools.get_test_json("initial_routes_route_fixation.json")
    old_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 10000}
    )

    positions_to_fix = [
        (0, 2), (2, 1), (2, 2), (3, 1),
        (4, 1), (5, 1), (5, 2), (7, 1)
    ]

    routes = old_response["routes"]
    order_vehicles = {}
    order_runs = {}
    order_shifts = {}
    for run_idx, loc_idx in positions_to_fix:
        loc = routes[run_idx]["route"][loc_idx]["node"]["value"]
        order_id = loc["id"]
        order_vehicles[order_id] = routes[run_idx]["vehicle_id"]
        run_number = routes[run_idx]["run_number"]
        if level == "run":
            loc["fixed_run"] = True
            order_runs[order_id] = run_number
        else:
            loc["fixed_shift"] = True
            order_shifts[order_id] = "shift1" if run_number <= 2 else "shift2"

    new_response = solve_with_initial_routes(
        request, old_response, 10000, expected_status="SOLVED")

    for run_idx, route in enumerate(new_response["routes"]):
        for loc_idx, loc_info in enumerate(route["route"]):
            loc = route["route"][loc_idx]["node"]["value"]
            order_id = loc["id"]
            if order_id not in order_vehicles:
                continue
            assert route["vehicle_id"] == order_vehicles[order_id]

            run_number = route["run_number"]
            if level == "run":
                assert order_runs[order_id] == run_number
            elif level == "shift":
                assert order_shifts[order_id] == "shift1" if run_number <= 2 else "shift2"


def test_initial_routes_actual_time():
    """
    Check actual_arrival_time_s and actual_departure_time_s options
    """
    request = tools.get_test_json("initial_routes.json")
    old_response = mvrp_checker.solve_and_check(
        json.dumps(request),
        None,
        solver_arguments={'sa_iterations': 10000}
    )

    old_vehicles_orders = get_vehicles_locations(old_response, "location")
    arrival_times = []
    departure_times = []
    for loc in old_response["routes"][0]["route"]:
        loc["node"]["value"]["fixed_position"] = True
        loc["actual_arrival_time_s"] = loc["arrival_time_s"] + 3600
        loc["actual_departure_time_s"] = loc["departure_time_s"] + 3600
        arrival_times.append(loc["actual_arrival_time_s"])
        departure_times.append(loc["actual_departure_time_s"])

    new_response = solve_with_initial_routes(
        request, old_response, 10000, expected_status="SOLVED")

    route = new_response["routes"][0]["route"]
    for i, loc in enumerate(route[:len(old_vehicles_orders[1])]):
        assert loc["arrival_time_s"] == arrival_times[i]
        assert loc["departure_time_s"] == departure_times[i]


@pytest.mark.parametrize('actual_time', [False, True])
def test_initial_routes_breaks(actual_time):
    """
    Check check loading initial routes with breaks and actual time
    """

    requests_to_check = [
        "work_break_chains.json",
        "work_breaks_travel.json",
        "work_breaks_continuous.json"
    ]

    for request_name in requests_to_check:
        request = tools.get_test_json(request_name)
        old_response = mvrp_checker.solve_and_check(
            json.dumps(request),
            None,
            solver_arguments={'sa_iterations': 100000}
        )

        old_vehicles_locations = get_vehicles_locations(old_response)
        old_response_copy = deepcopy(old_response)
        for route in old_response_copy["routes"]:
            for loc in route["route"]:
                if loc["node"]["type"] == "break" and "penalty" in loc["node"]["value"]:
                    loc["node"]["value"]["penalty"].pop("out_of_time", None)
                    loc["node"]["value"].pop("shipment_size", None)
                if actual_time:
                    loc["actual_arrival_time_s"] = loc["arrival_time_s"]
                    loc["actual_departure_time_s"] = loc["departure_time_s"]
                    loc["node"]["value"]["fixed_position"] = True

        new_response = solve_with_initial_routes(request, old_response_copy, 0)
        new_vehicles_locations = get_vehicles_locations(new_response)
        assert old_vehicles_locations == new_vehicles_locations


def filter_keys(dict, keys_to_keep):
    return {key: dict[key] for key in dict if key in keys_to_keep}


def test_initial_routes_min_description():
    """
    Check loading initial routes with minimal description provided
    """
    requests_to_check = [
        "drop_off.json",
        "walking_courier.json",
        "trailer_and_anchor.json",
        "crossdock.json"
    ]

    for i, request_name in enumerate(requests_to_check):
        request = tools.get_test_json(request_name)
        status = "SOLVED" if i < 3 else "PARTIAL_SOLVED"
        old_response = mvrp_checker.solve_and_check(
            json.dumps(request),
            None,
            solver_arguments={'sa_iterations': 10000},
            expected_status=status
        )

        old_vehicles_locations = get_vehicles_locations(old_response)
        routes = old_response["routes"]
        for i in range(len(routes)):
            routes[i] = filter_keys(routes[i], ["route", "vehicle_id", "shift"])
            route = routes[i]["route"]
            for j in range(len(route)):
                route[j] = filter_keys(route[j], ["node"])
                route[j]["node"] = filter_keys(route[j]["node"], ["type", "value"])

                loc_type = route[j]["node"]["value"].get("type", "")
                keys_to_keep = ["id"]
                if route[j]["node"]["value"].get("trailer_used", False):
                    keys_to_keep.append("trailer_used")
                if loc_type == "parking":
                    keys_to_keep += ["type", "parking_type"]
                elif loc_type == "anchor":
                    keys_to_keep += ["anchor_mode"]
                elif loc_type == "crossdock":
                    keys_to_keep += ["type", "delivered_orders", "picked_orders"]
                route[j]["node"]["value"] = filter_keys(
                    route[j]["node"]["value"], keys_to_keep)

        new_response = solve_with_initial_routes(
            request, old_response, 0, expected_status=status)
        new_vehicles_locations = get_vehicles_locations(new_response)

        assert old_vehicles_locations == new_vehicles_locations
