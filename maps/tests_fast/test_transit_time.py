import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_transit_time():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('transit_time.json'),
        solver_arguments={'sa_iterations': 100000},
        expected_status="PARTIAL_SOLVED"
    )

    dropped_locs = set(map(lambda x: x['id'], result['dropped_locations']))
    expected_to_be_dropped = {4, 106, 107, 202}  # should fail hard transit time limit
    assert expected_to_be_dropped.issubset(dropped_locs)

    # check that this metric is calculated at all
    assert result['metrics']['transit_time_penalty'] > 0

    total_transit_distances = dict()
    for route in result['routes']:
        id = route['vehicle_id']
        total_transit_distances[id] = total_transit_distances.get(id, 0) + route['metrics']['total_transit_distance_m']

    # the routes in the test data are not optimal in terms of the transit distance
    # because they are heavily affected by transit time constraints
    # assume that it's impossible to find shorter routes which won't violate
    # the transit time constraints (unless the default speed is changed)
    # so if this fails, most likely some transit time hard limis are violated
    assert total_transit_distances[1] > 128000
    assert total_transit_distances[2] > 193000
    assert total_transit_distances[3] > 24000
    assert total_transit_distances[4] > 27000
    assert total_transit_distances[5] > 25000
