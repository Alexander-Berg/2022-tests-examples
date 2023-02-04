import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_depot_extra_windows():
    result = mvrp_checker.solve_and_check(
        tools.get_test_data('depot_extra_windows.json'),
        solver_arguments={'sa_iterations': 10000},
        expected_status="SOLVED"
    )

    assert len(result['routes']) == 3

    start_window = '07:00:00-09:00:00'
    middle_window1 = '12:20:00-12:50:00'
    middle_window2 = '13:30:00-15:20:00'
    end_window = '17:00:00-19:00:00'

    assert result['routes'][0]['route'][0]['node']['used_time_window'] == start_window
    assert result['routes'][0]['route'][2]['node']['used_time_window'] == middle_window1
    assert result['routes'][1]['route'][0]['node']['used_time_window'] == middle_window1
    assert result['routes'][1]['route'][2]['node']['used_time_window'] == middle_window2
    assert result['routes'][2]['route'][0]['node']['used_time_window'] == middle_window2
    assert result['routes'][2]['route'][2]['node']['used_time_window'] == end_window
