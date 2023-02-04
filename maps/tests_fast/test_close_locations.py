import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json


def test_walking_close_locations():
    """
    Test checks that besides cost of walking is extremelly high we still have to walk
        because of "hard" close_locations restrictions.
    """
    result = mvrp_checker.solve_and_check(
        tools.get_test_data("close_locations.json"),
        tools.get_test_data("close_locations_distances.json"),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")

    assert result['metrics']['total_walking_distance_m'] > 0


def test_close_locations_():
    """
    Without `walking_courier` distance between location is way higher than `distance_till_service_m`
        value. Penalty value is also very high, so it's better to drop one of orders in terms of costs.
    After raising of distance_till_service_m both locations can be served without penalties.
    """
    request = tools.get_test_json("close_locations.json")
    del request['vehicles'][0]['walking_courier']
    del request['vehicles'][0]['close_locations'][0]['routing_mode']
    request['vehicles'][0]['close_locations'][0]['search_radius_m'] = 2000

    mvrp_checker.solve_and_check(
        json.dumps(request),
        tools.get_test_data("close_locations_distances.json"),
        solver_arguments={'sa_iterations': 10000},
        expected_status="PARTIAL_SOLVED")

    request['vehicles'][0]['close_locations'][0]['distance_till_service_m']['value'] = 2000
    mvrp_checker.solve_and_check(
        json.dumps(request),
        tools.get_test_data("close_locations_distances.json"),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED")
