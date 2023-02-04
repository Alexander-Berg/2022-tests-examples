import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_locations_with_address():
    task = tools.get_test_json('locations_with_address.json')

    response = mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 1000},
        expected_status='SOLVED')

    routes = response['routes']
    assert len(routes) == 1
    route = routes[0]['route']
    assert len(route) == 5

    assert route[0]['node']['value']['address'] == "Depot address"
    assert route[1]['node']['value']['address'] == "Location " + str(route[1]['node']['value']['id']) + " address"
    assert route[2]['node']['value']['address'] == "Location " + str(route[2]['node']['value']['id']) + " address"
    assert route[3]['node']['value']['address'] == "Location " + str(route[3]['node']['value']['id']) + " address"
    assert route[4]['node']['value']['address'] == "Depot address"
