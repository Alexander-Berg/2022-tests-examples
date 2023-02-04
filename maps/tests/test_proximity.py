import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


def test_proximity():
    """
    This test includes 200 location from the original lamoda request,
    where proximity gave about 28% of better stability.
    Solver was worse than logist in terms of stability on +11.7%.
    However with proximity_factor=0.6 solver became better and won logist on 16.1% by stability.
    See https://st.yandex-team.ru/BBGEO-1702#5c7fad73cde9dd001ff7e330 for more info.

    Main metrics to control in this test are:
        * proximity - the less, the more stable solution is;
        * total_proximity_distance_m - the less, the more stable solution is;
        * total_proximity_duration_s - the less, the more stable solution is;
        * total_proximity_penalty - the less, the more stable solution is
          (this metrics includes additional vehicle costs for proximity distance and duration);
        * total_cost_with_penalty - it includes total_proximity_penalty, so it's not correct
          to compare this metric in solutions which are got using different values of proximity_factor.

    Solver deviation results (30 runs):
    log2_iterations  cost_best  cost_avr   cost_2std(%)  cost_99err(%)
    1000000.000      27923.591  31105.343  9.640         22.150
    """

    with open(tools.data_path('data/lamoda_request.json')) as f_in:
        request = json.load(f_in)

    route = mvrp_checker.solve_and_check(
        json.dumps(request),
        solver_arguments={'sa_iterations': 1000000},
        runs_count=10)

    expected_metrics = {
        "assigned_locations_count": 200.0,
        "global_proximity": 5.747,
        "max_vehicle_runs": 1.0,
        "number_of_routes": 8.02,
        "objective_minimum": 1939359.326,
        "operations_per_second": 19185.029,
        "optimization_steps": 1000000.0,
        "proximity": 2.939,
        "total_cost": 28239.224,
        "total_cost_with_penalty": 31105.343,
        "total_proximity_distance_m": 620372.089,
        "total_proximity_duration_s": 111666.976,
        "total_proximity_penalty": 2866.119,
        "total_transit_distance_cost": 3778.224,
        "total_transit_distance_m": 490678.447,
        "total_transit_duration_s": 88322.121,
        "used_vehicles": 8.02
    }

    rel_accuracies = {
        "global_proximity": 0.0817,
        "number_of_routes": 0.06415,
        "objective_minimum": 0.0462,
        "operations_per_second": 0.12205,
        "proximity": 0.0387,
        "total_cost": 0.054299999999999994,
        "total_cost_with_penalty": 0.0482,
        "total_proximity_distance_m": 0.034249999999999996,
        "total_proximity_duration_s": 0.034249999999999996,
        "total_proximity_penalty": 0.034249999999999996,
        "total_transit_distance_cost": 0.026099999999999998,
        "total_transit_distance_m": 0.026099999999999998,
        "total_transit_duration_s": 0.026099999999999998,
        "used_vehicles": 0.06415
    }

    tools.check_metrics_are_close(route["metrics"], expected_metrics, rel_accuracies)


@pytest.mark.parametrize('use_in_proximity', [True, False])
@pytest.mark.parametrize('proximity', [True, False])
@pytest.mark.parametrize('global_proximity', [True, False])
def test_use_in_proximity(use_in_proximity, proximity, global_proximity):
    """
    In this test there are 2 locations: one close to depot and one far away.
    The second location also has small drop penalty.
    Proximity and global proximity are set to 10, so it is optimal to drop the second location.
    When use_in_proximity is set to false, we should be able to serve it.
    """

    task = tools.get_test_json("use_in_proximity.json")

    farLocation = task['locations'][1]
    farLocation['use_in_proximity'] = use_in_proximity

    if not proximity:
        del task['options']['proximity_factor']
    if not global_proximity:
        del task['options']['global_proximity_factor']

    drop = (proximity or global_proximity) and use_in_proximity

    mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
        expected_status='PARTIAL_SOLVED' if drop else 'SOLVED',
        expected_metrics={
            'assigned_locations_count': 1 if drop else 2
        }
    )


def test_global_proximity_attraction_point():
    task = tools.get_test_json("global_proximity_attraction_point.json")
    mvrp_checker.solve_and_check(
        json.dumps(task),
        solver_arguments={'sa_iterations': 10000},
    )
