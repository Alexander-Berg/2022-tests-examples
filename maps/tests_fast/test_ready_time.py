import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


@pytest.mark.parametrize('use_ready_time', [True, False])
def test_ready_time(use_ready_time):
    task = tools.get_test_json("ready_time.json")
    expected_metrics = {
        'total_service_duration_s': 1200,
        'total_waiting_duration_s': 38424.66895981544
    }

    if not use_ready_time:
        expected_metrics['total_waiting_duration_s'] = 0
        for loc in task['locations']:
            if 'depot_ready_time' in loc:
                del loc['depot_ready_time']

    mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': 100000},
        expected_metrics=expected_metrics)


@pytest.mark.parametrize('visited', [True, False])
@pytest.mark.parametrize('visit_depot_at_start', [True, False])
def test_ready_time_start_at(visited, visit_depot_at_start):
    """
    This test checks that depot is visited before serving location with depot_ready_time.
    """
    task = tools.get_test_json("ready_time_start_at.json")

    vehicle = task['vehicles'][0]
    if not visited:
        del vehicle['visited_locations']
    vehicle['visit_depot_at_start'] = visit_depot_at_start

    expected_metrics = {
        'assigned_locations_count': 2,
        'max_vehicle_runs': 1 if not visited and visit_depot_at_start else 2
    }

    result = mvrp_checker.solve_and_check(
        json.dumps(task), None, solver_arguments={'sa_iterations': 100000},
        expected_metrics=expected_metrics)

    for route in result['routes']:
        was_depot = False
        for node in route['route']:
            if node['node']['type'] == 'depot':
                was_depot = True
            elif node['node']['value']['id'] == 2:
                assert was_depot


def test_ready_time_2_runs():
    """
    In this test route starts at 10:00 from depot(this time is fixed through visited_locations),
    but some locations have depot_ready_time after that. They should be served in the second runs.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('ready_time_2_runs.json'),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'number_of_routes': 2,
            'max_vehicle_runs': 2
        }
    )

    early_route = result['routes'][0]['route']
    assert early_route[0]['arrival_time_s'] == 36000
    for node in early_route:
        if node['node']['type'] != 'location':
            continue
        assert node['node']['value']['depot_ready_time'] <= "10:00:00"


def test_load_when_ready():
    """
    Test load_when_ready option. With this option we can start loading first order
    right after its depot_ready_time, not waiting for other orders, and save 10 minutes
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('load_when_ready.json'),
        solver_arguments={'sa_iterations': 10000},
    )

    route_info = result['routes'][0]
    depote_info = route_info['route'][0]
    assert depote_info['departure_time_s'] == 31200

    depot_duration = depote_info['departure_time_s'] - depote_info['arrival_time_s']
    service_and_waiting = depote_info['node']['value']['total_service_duration_s'] \
        + depote_info['waiting_duration_s']
    assert depot_duration == service_and_waiting
