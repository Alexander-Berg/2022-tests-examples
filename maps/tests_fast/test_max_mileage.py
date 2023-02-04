import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.mvrp_checker as mvrp_checker
import maps.b2bgeo.mvrp_solver.annealing_mvrp.tests_lib.tools as tools
import json
import pytest


FullRouteLengthKm = 384.6573417565196


def mileage_penalty(route_length_km, shift):
    penalty = shift['penalty']['max_mileage']
    excess_km = route_length_km - shift['max_mileage_km']
    return 0 if excess_km <= 0 else excess_km * penalty['km'] + penalty['fixed']


@pytest.mark.parametrize('max_mileage_km', [0, 100, 384, 385, 1e300])
def test_max_mileage(max_mileage_km):
    """
    There are 3 locations in this test and the route
    is actually fixed due to their time windows
    """

    data = tools.get_test_json("max_mileage.json")

    shift = data["vehicles"][0]['shifts'][0]
    shift["max_mileage_km"] = max_mileage_km

    mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'total_transit_distance_m': 1e3*FullRouteLengthKm,
            'total_mileage_penalty': mileage_penalty(FullRouteLengthKm, shift)
        })


@pytest.mark.parametrize('max_mileages_km', [
    [19, 27, 87],   # no mileage failures
    [18, 26, 86],   # all mileage limits are failed
    [15, 30, 90],   # only 1st is failed
    [20, 20, 100],  # only 2nd is failed
    [19, 50, 80],   # only 3rd is failed
    [10, 10, 100],  # 1st and 2nd are failed
    [10, 50, 10],   # 1st and 3rd are failed
    [30, 50, 100],  # 2nd and 3rd are failed
])
def test_shift_max_mileage(max_mileages_km):
    """
    There are 3 locations in this test and the route is fixed due to shift time
    windows and location time windows. Each location is supposed to be served
    in a separate shift, so we should get 3 different routes with the following
    milages: 18.6km, 26.5km, 86.5km.
    """

    route_lengths_km = [18.6, 26.5, 86.5]

    data = tools.get_test_json("max_mileage_shifts.json")

    expected_mileage_penalty = 0
    for shift_idx, shift in enumerate(data["vehicles"][0]['shifts']):
        shift["max_mileage_km"] = max_mileages_km[shift_idx]
        expected_mileage_penalty += mileage_penalty(route_lengths_km[shift_idx], shift)

    mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'total_transit_distance_m': 1e3*sum(route_lengths_km),
            'total_mileage_penalty': expected_mileage_penalty
        })


@pytest.mark.parametrize('drop_penalties', [
    [1e5, 1e5, 1e5],
    [1e3, 1e5, 1e5],
    [1e5, 1e3, 1e5],
    [1e5, 1e5, 1e3]
])
def test_max_mileage_drop_optimization(drop_penalties):
    """
    There are 3 locations in this test and the whole route is
    about 384.6km long, while max_mileage=300km so it is always failed
    if all locations are assigned. So we set a small drop penalty for
    one location only and the route should become less than 300km long
    whatever location we choose.
    """

    max_mileage_km = 300

    data = tools.get_test_json("max_mileage_optimization.json")

    for loc_idx, loc in enumerate(data['locations']):
        loc['penalty']['drop'] = drop_penalties[loc_idx]

    shift = data["vehicles"][0]['shifts'][0]
    shift["max_mileage_km"] = max_mileage_km

    if min(drop_penalties) >= 1e4:
        expected_status = 'SOLVED'
        expected_drops = 0
        expected_mileage_penalty = mileage_penalty(FullRouteLengthKm, shift)
    else:
        expected_status = 'PARTIAL_SOLVED'
        expected_drops = 1
        expected_mileage_penalty = 0

    mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 10000},
        expected_status=expected_status,
        expected_metrics={
            'total_mileage_penalty': expected_mileage_penalty,
            'dropped_locations_count': expected_drops
        })


def test_max_mileage_late_optimization():
    """
    There are 3 locations in this test and the whole route without
    time window failures is ~384.65 km long, while it's possible to
    shorten it to 349.425 km with 1 failed time window.
    So we check here, that solver shortens the route when penalty
    for time window failure is less than penalty for mileage limit failure.
    """

    route_length_km = 349.425
    penalty = {"fixed": 2000, "km": 300}

    data = tools.get_test_json("max_mileage.json")

    shift = data["vehicles"][0]['shifts'][0]
    shift["max_mileage_km"] = 0
    shift.setdefault('penalty', {})['max_mileage'] = penalty

    mvrp_checker.solve_and_check(
        json.dumps(data),
        solver_arguments={'sa_iterations': 10000},
        expected_metrics={
            'total_failed_time_window_count': 1,
            'total_transit_distance_m': 1e3*route_length_km,
            'total_mileage_penalty': mileage_penalty(route_length_km, shift)
        })
