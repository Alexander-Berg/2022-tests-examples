from copy import deepcopy
import json

import pytest

import _solver as solver
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools


def test_work_break_tuning():
    """
    Test tuning of work breaks between visited locations.
    """
    request = tools.get_test_json('visited_locations_work_breaks.json')
    del request['vehicles'][0]['fixed_work_breaks']

    mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 0},
        expected_metrics={
            "total_failed_time_window_count": 2
        })

    mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 20000},
        expected_metrics={
            "total_failed_time_window_count": 0
        })


@pytest.mark.parametrize('iterations', [0, 2000])
@pytest.mark.parametrize('remove_last', [False, True])
def test_fixed_work_breaks(iterations, remove_last):
    """
    Test that fixed work breaks are really fixed
    """
    request_good = tools.get_test_json('visited_locations_work_breaks.json')

    request_bad = deepcopy(request_good)
    request_bad['vehicles'][0]['fixed_work_breaks'] = [
        {"work_duration_s": 18000},
        {"work_duration_s": 10800}
    ]

    if remove_last:
        request_good['vehicles'][0]['fixed_work_breaks'].pop()
        request_bad['vehicles'][0]['fixed_work_breaks'].pop()

    mvrp_checker.solve_and_check(
        json.dumps(request_bad),
        solver_arguments={'sa_iterations': iterations},
        expected_metrics={
            "total_failed_time_window_count": 2
        })

    mvrp_checker.solve_and_check(
        json.dumps(request_good),
        solver_arguments={'sa_iterations': iterations},
        expected_metrics={
            "total_failed_time_window_count": 1 if remove_last and not iterations else 0
        })


@pytest.mark.parametrize('iterations', [0, 10000])
@pytest.mark.parametrize('remove_last', [False, True])
def test_planned_work_breaks(iterations, remove_last):
    """
    Test that planned work breaks are not fixed
    """
    request_good = tools.get_test_json('visited_locations_work_breaks.json')
    request_good['vehicles'][0]['planned_route'] = {
        'locations': deepcopy(request_good['vehicles'][0]['visited_locations']),
        'work_breaks': deepcopy(request_good['vehicles'][0]['fixed_work_breaks'])
    }
    del request_good['vehicles'][0]['fixed_work_breaks']

    request_bad = deepcopy(request_good)
    request_bad['vehicles'][0]['planned_route']['work_breaks'] = [
        {"work_duration_s": 18000},
        {"work_duration_s": 10800}
    ]

    if remove_last:
        request_good['vehicles'][0]['planned_route']['work_breaks'].pop()
        request_bad['vehicles'][0]['planned_route']['work_breaks'].pop()

    mvrp_checker.solve_and_check(
        json.dumps(request_bad),
        solver_arguments={'sa_iterations': iterations},
        expected_metrics={
            "total_failed_time_window_count": 2 if not iterations else 0
        })

    mvrp_checker.solve_and_check(
        json.dumps(request_good),
        solver_arguments={'sa_iterations': iterations},
        expected_metrics={
            "total_failed_time_window_count": 1 if remove_last and not iterations else 0
        })


@pytest.mark.parametrize('iterations', [0, 2000])
@pytest.mark.parametrize('remove_last', ['none', 'planned', 'fixed'])
def test_planned_and_fixed_work_breaks(iterations, remove_last):
    """
    Test that combination of planned and fixed breaks works correctly
    """
    request_good = tools.get_test_json('visited_locations_work_breaks.json')
    request_good['vehicles'][0]['planned_route'] = {
        'locations': deepcopy(request_good['vehicles'][0]['visited_locations']),
        'work_breaks': deepcopy(request_good['vehicles'][0]['fixed_work_breaks'])
    }

    request_bad = deepcopy(request_good)
    request_bad['vehicles'][0]['fixed_work_breaks'] = [
        {"work_duration_s": 18000},
        {"work_duration_s": 10800}
    ]
    request_bad['vehicles'][0]['planned_route']['work_breaks'] = [
        {"work_duration_s": 18000},
        {"work_duration_s": 10800}
    ]

    if remove_last == 'fixed':
        request_good['vehicles'][0]['fixed_work_breaks'].pop()
        request_bad['vehicles'][0]['fixed_work_breaks'].pop()

    if remove_last == 'planned':
        request_good['vehicles'][0]['planned_route']['work_breaks'].pop()
        request_bad['vehicles'][0]['planned_route']['work_breaks'].pop()

    mvrp_checker.solve_and_check(
        json.dumps(request_bad),
        solver_arguments={'sa_iterations': iterations},
        expected_metrics={
            "total_failed_time_window_count": 2
        })

    mvrp_checker.solve_and_check(
        json.dumps(request_good),
        solver_arguments={'sa_iterations': iterations},
        expected_metrics={
            "total_failed_time_window_count": 0
        })


def test_planned_and_fixed_work_breaks_error():
    """
    Test that solvers throws error on inconsistent fixed and planned work breaks
    """
    request = tools.get_test_json('visited_locations_work_breaks.json')
    request['vehicles'][0]['planned_route'] = {
        'locations': deepcopy(request['vehicles'][0]['visited_locations']),
        'work_breaks': [{"work_duration_s": 18000}]
    }
    with pytest.raises(solver.SolverLocalizedException):
        mvrp_checker.solve_and_check(json.dumps(request))
