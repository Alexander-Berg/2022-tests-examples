import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def seconds_from_time(time):
    h, m, s = map(int, time.split(':'))
    return (h * 60 + m) * 60 + s


def test_departure_time():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('departure_time.json'),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={'max_vehicle_runs': 2}
    )

    expected_departure_times = [seconds_from_time(time) for time in [
        '7:00:00',
        '7:10:00',
        '12:25:00',
        '12:25:00',
        '12:51:58',
        '13:01:58',
        '14:30:00',
        '14:57:00',
        '14:57:00'
    ]]

    departure_times = []
    for route_info in result['routes']:
        shift = route_info['shift']
        route = route_info['route']
        if 'start' in shift:
            departure_times.append(shift['start']['departure_time_s'])
        for node in route:
            departure_times.append(node['departure_time_s'])
        if 'end' in shift:
            departure_times.append(shift['end']['departure_time_s'])

    assert len(departure_times) == len(expected_departure_times)
    for i in range(len(departure_times)):
        assert abs(departure_times[i] - expected_departure_times[i]) < 1
