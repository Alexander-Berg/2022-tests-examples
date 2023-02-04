import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


def node_type(node):
    return 'depot' if node['node']['type'] == 'depot' else \
        node['node']['value'].get('type', 'delivery')


def check_first_route_start(routes, start_at, visit_depot_at_start):
    first_route = routes['routes'][0]
    nodes = first_route['route']
    assert len(nodes) > 1
    index = 0
    if start_at:
        assert len(nodes) > 2
        assert node_type(nodes[index]) == 'garage'
        index += 1
    next_node_type = node_type(nodes[index])
    if visit_depot_at_start:
        assert next_node_type == 'depot'
    else:
        assert next_node_type != 'depot'


def check_last_route_end(routes, finish_at, return_to_depot):
    last_route = routes['routes'][-1]
    nodes = last_route['route']
    index = -1
    if finish_at:
        assert len(nodes) > 2
        assert node_type(nodes[index]) == 'garage'
        index -= 1
    prev_node_type = node_type(nodes[index])
    if return_to_depot:
        assert prev_node_type == 'depot'
    else:
        assert prev_node_type != 'depot'


@pytest.mark.parametrize('start_at', [True, False])
@pytest.mark.parametrize('finish_at', [True, False])
@pytest.mark.parametrize('visit_depot_at_start', [True, False])
@pytest.mark.parametrize('return_to_depot', [True, False])
@pytest.mark.parametrize('shifts', [True, False])
def test_start_finish_at(start_at, finish_at, visit_depot_at_start, return_to_depot, shifts):
    """
    This test check that vehicle starts the route from `start_at` location
    and returns to `finish_at` location if these fields are used.
    """
    if not start_at and not visit_depot_at_start:
        # this case is forbidden by solver, the first location is required
        return

    task = tools.get_test_json("start_finish_at.json")

    vehicle = task["vehicles"][0]
    if not start_at:
        del vehicle["start_at"]
    if not finish_at:
        del vehicle["finish_at"]
    if not shifts:
        del vehicle["shifts"]
    vehicle["visit_depot_at_start"] = visit_depot_at_start
    vehicle["return_to_depot"] = return_to_depot

    routes = mvrp_checker.solve_and_check(json.dumps(task), solver_arguments={'sa_iterations': 10000})

    assert len(routes['routes']) > 0

    check_first_route_start(routes, start_at, visit_depot_at_start)
    check_last_route_end(routes, finish_at, return_to_depot)
