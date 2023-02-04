import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


def test_pickup_and_delivery():
    mvrp_checker.solve_and_check(
        tools.get_test_data("pickup_and_delivery.json"), None,
        solver_arguments={'sa_iterations': 10000})


def test_pickup_from_any():
    mvrp_checker.solve_and_check(
        tools.get_test_data("pickup_from_any.json"), None,
        solver_arguments={'sa_iterations': 10000})


def test_pickup_group():
    """
    A simple test on which solver did not work correctly:
    3 locations: pickup and delivery pair and regular delivery,
    both deliveries are in the same location group.
    Solver could not serve pickup and delivery.
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data('pickup_group.json'),
        solver_arguments={'sa_iterations': 10000})


def test_multipickups():
    """
    Test with 3 pickups to the same delivery.
    """
    request = tools.get_test_json("multipickups.json")

    response = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 10000})

    route = response['routes'][0]['route']
    used_ids = set()
    unique = True
    for node in route:
        if node['node']['type'] == 'depot':
            continue
        loc = node['node']['value']
        if loc['id'] in used_ids:
            unique = False
        used_ids.add(loc['id'])
    assert unique


def test_multipickups_planned():
    """
    In planned route the delivery location is before the pickups, their order should be reversed
    but no locations should be dropped.
    """
    expected_metrics = {
        'assigned_locations_count': 4,
        'dropped_locations_count': 0,
        'total_unfeasibility_count': 0
    }

    mvrp_checker.solve_and_check(
        tools.get_test_data('multipickups_planned.json'),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics=expected_metrics)


@pytest.mark.skip(reason='Чекер ругается что заказ не доставлен, хотя мы и не могли. Надо аккуратно поправить чекер')
def test_multipickups_planned_dropped():
    """
    Тут происходит какой-то бред, но фронт порождает такие задачи при редактировании решений и надо как минимум не падать
    """
    mvrp_checker.solve_and_check(
        tools.get_test_data('dropped_multipickups.json'),
        solver_arguments={'sa_iterations': 10000},
        expected_status='PARTIAL_SOLVED')


@pytest.mark.parametrize('visited', [True, False])
@pytest.mark.parametrize('planned', [True, False])
def test_multipickups_visited_planned(visited, planned):
    task = tools.get_test_json("multipickups_visited_planned.json")

    vehicle = task['vehicles'][0]
    if not visited:
        del vehicle['visited_locations']
    if not planned:
        del vehicle['planned_route']

    expected_metrics = {
        'assigned_locations_count': 5,
        'dropped_locations_count': 0,
        'total_unfeasibility_count': 0
    }

    mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics=expected_metrics)


def test_dependent_pickups():
    """
    In this test it is impossible to fit all pickups to the same vehicle,
    and since they have the same delivery_to field they all should be dropped.
    """
    task = tools.get_test_json("multipickups.json")

    task['vehicles'][0]['capacity']['weight_kg'] = 9

    expected_metrics = {
        'assigned_locations_count': 0,
        'dropped_locations_count': 8,
        'total_unfeasibility_count': 0
    }

    mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics=expected_metrics,
        expected_status='PARTIAL_SOLVED')


def test_lifo():
    response = mvrp_checker.solve_and_check(
        tools.get_test_data("lifo.json"),
        solver_arguments={'sa_iterations': 10000})

    route = response['routes'][0]['route']
    delivery_to_stack = []
    for loc in route:
        if loc["node"]["type"] == "depot":
            continue

        if loc["node"]["value"]["type"] == "pickup":
            delivery_to = loc["node"]["value"].get("delivery_to", -1)
            delivery_to_stack.append(delivery_to)
        elif loc["node"]["value"]["type"] == "delivery":
            assert (len(delivery_to_stack) == 0) or (delivery_to_stack[-1] == loc["node"]["value"]["id"])
            delivery_to_stack = delivery_to_stack[:-1]
    assert delivery_to_stack == [-1]  # Delivery to depot
