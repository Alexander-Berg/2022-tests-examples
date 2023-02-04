import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_preliminary_service_duration():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('preliminary_service_duration.json'),
        solver_arguments={'sa_iterations': 10000}
    )
    depot = result['routes'][0]['route'][0]
    service_start = depot['arrival_time_s'] + depot['waiting_duration_s']
    assert service_start == mvrp_checker.parse_time_relative('13:30').total_seconds()
    assert depot['node']['value']['service_duration_s'] == 9000
