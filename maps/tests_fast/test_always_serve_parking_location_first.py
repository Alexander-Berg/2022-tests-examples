import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_always_serve_parking_location_first():
    """
    Checks that if test_always_serve_parking_location_first == true, then the location where we parked
    is actually served first. The task has 3 locations, because of the high parking_service_duration it's profitable
    to serve them all by walking. The only way to accomplish it is park at 0, then serve two other locations
    and serve 0 last (because of the time windows). But if the option is enabled, we forcefully serve 0 first
    and serve the two other with time window violations
    """

    task = tools.get_test_json('always_serve_parking_location_first.json')

    walking_route = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 1000},
        expected_status='SOLVED')

    route = walking_route['routes'][0]['route']
    assert len(route) == 7
    # check that we park/unpark at 0 and serve 0 first.
    assert route[1]['node']['value']['type'] == "parking"
    assert route[1]['node']['value']['id'] == 0
    assert route[5]['node']['value']['type'] == "parking"
    assert route[5]['node']['value']['id'] == 0
    assert route[2]['node']['value']['id'] == 0
