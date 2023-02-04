import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


HOUR_S = 3600


def test_wait_if_early():
    """
    This test checks that wait_if_early parameter of visited locations
    works as expected.
    """

    r = mvrp_checker.solve_and_check(
        tools.get_test_data('visited_wait_if_early.json'),
        solver_arguments={'sa_iterations': 1},
        expected_metrics={
            "total_failed_time_window_count": 1,
            "failed_time_window_depot_count": 0,
            "failed_time_window_locations_count": 1
        }
    )

    assert len(r["routes"]) == 1
    for elem in r["routes"][0]["route"]:
        node = elem["node"]
        if node["type"] == "location":
            loc_id = node["value"]["id"]
            if loc_id in ["1", "3"]:
                assert elem["waiting_duration_s"] > 0
            elif loc_id == "2":
                assert elem["waiting_duration_s"] == 0


def check_visit_times(result):
    needed_times = {}
    visited_locs = result['vehicles'][0]['visited_locations']
    for loc in visited_locs:
        if 'time' in loc:
            needed_times[loc['id']] = mvrp_checker.parse_time_relative(loc['time']).total_seconds()

    route = result['routes'][0]['route']
    last_departure_time = 0
    for node in route:
        loc_id = node['node']['value']['id']
        if loc_id in needed_times:
            assert node['departure_time_s'] == needed_times[loc_id]
            del needed_times[loc_id]
        assert last_departure_time <= node['arrival_time_s']
        assert node['arrival_time_s'] <= node['departure_time_s']
        last_departure_time = node['departure_time_s']


def test_visited_locations_time():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('visited_locations_time.json'),
        solver_arguments={'sa_iterations': 0},
    )
    check_visit_times(result)


def test_visited_locations_service():
    """
    Checks that departure time == arrival time + service time and
    service times are adjusted to keep arrival times nondecreasing
    """

    result = mvrp_checker.solve_and_check(
        tools.get_test_data('visited_locations_service.json'),
        solver_arguments={'sa_iterations': 0}
    )
    check_visit_times(result)


def test_visited_locations_hard_shift_window():
    """
    In case a vehicle shift has a hard time window, visited locations
    should be serviced after the start of that window.
    """

    result = mvrp_checker.solve_and_check(
        tools.get_test_data('visited_locations_hard_shift_window.json'),
        solver_arguments={'sa_iterations': 0}
    )

    route = result['routes'][0]['route']
    for node in route:
        loc = node['node']['value']
        if loc.get('type') == 'delivery':
            assert node['arrival_time_s'] == 15 * HOUR_S


def test_visited_locations_monotone():
    """
    Visited locations are far away and we need a lot of time to serve them.
    But time is set for 2 locations and times for other locations should be adjusted
    so that they are nondecreasing.
    """

    result = mvrp_checker.solve_and_check(
        tools.get_test_data('visited_locations_monotone.json'),
        solver_arguments={'sa_iterations': 0}
    )
    check_visit_times(result)


def test_visited_ready_time():
    """
    This test checks that depots without fixed visit time are visited after
    depot ready times for locations that are served after them.
    """

    result = mvrp_checker.solve_and_check(
        tools.get_test_data('visited_ready_time.json'),
        solver_arguments={'sa_iterations': 0},
        expected_metrics={
            "number_of_routes": 2
        }
    )

    correct_start_times = [46800, 54000]

    for route_id, route in enumerate(result['routes']):
        start_time = route['route'][0]['arrival_time_s']
        assert start_time == correct_start_times[route_id]


def test_visited_locations_preliminary_service():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('visited_locations_preliminary_service.json'),
        solver_arguments={'sa_iterations': 0},
    )
    check_visit_times(result)
