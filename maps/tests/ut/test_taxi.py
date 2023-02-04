import copy
import json
import os
import pytest
from datetime import datetime, timezone

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util import source_path
from ya_courier_backend.util import taxi
from ya_courier_backend.util.taxi_tracking import filter_new_positions
from ya_courier_backend.util.tracking import ZERO_POSITION_EPSILON


def _get_task():
    data_root = source_path("maps/b2bgeo/ya_courier/backend")
    with open(os.path.join(data_root, "bin/tests/data/example-mvrp.json"), "r") as f:
        return json.load(f)


def _get_task_result():
    result_file = source_path("maps/b2bgeo/ya_courier/backend/bin/tests/data/sample-mvrp-result.json")
    with open(result_file) as f:
        return json.load(f)


@skip_if_remote
def test_taxi_preprocess_no_taxi():
    f = taxi.TaxiTaskFilter()
    task = _get_task()

    old_task = copy.deepcopy(task)
    f.preprocess_mvrp_task(task)

    assert old_task == task


@skip_if_remote
@pytest.mark.parametrize('taxi_types_list', [['van'], ['lcv_m'], ['lcv_l'], ['express'], ['courier'], ['express', 'van'],
                         ['lcv_m', 'lcv_l'], ['van', 'lcv_m', 'lcv_l'], ['express', 'lcv_m', 'lcv_l'],
                         ['express', 'van', 'lcv_m', 'lcv_l'], ['express', 'van', 'lcv_m', 'lcv_l', 'courier']])
def test_taxi_preprocess(taxi_types_list):
    f = taxi.TaxiTaskFilter()
    task = _get_task()

    f.preprocess_mvrp_task(task, taxi_types=taxi_types_list)

    taxi_vehicles = [v for v in task['vehicles'] if taxi.is_taxi_vehicle(v)]

    assert len(taxi_vehicles) > 0

    used_taxi_types = set(map(taxi.get_taxi_vehicle_type, taxi_vehicles))

    assert sorted(used_taxi_types) == sorted(taxi_types_list)


@skip_if_remote
def test_taxi_preprocess_invalid_taxi_type():
    f = taxi.TaxiTaskFilter()
    task = _get_task()

    exception = None

    try:
        f.preprocess_mvrp_task(task, taxi_types='invalid_taxi_type')
    except Exception as e:
        exception = e

    assert exception is not None


@skip_if_remote
def test_taxi_preprocess_parameters():
    f = taxi.TaxiTaskFilter()
    task = _get_task()

    f.preprocess_mvrp_task(task, taxi_types=['van', 'lcv_m', 'lcv_l', 'express', 'courier'])

    def has_common_parameters(vehicle):
        return (vehicle['max_runs'] == 1 and
                vehicle['visit_depot_at_start'] is True and
                vehicle['return_to_depot'] is False)

    taxi_vehicles = filter(taxi.is_taxi_vehicle, task['vehicles'])
    assert all(map(has_common_parameters, taxi_vehicles))


@skip_if_remote
def test_taxi_postprocess_no_taxi():
    f = taxi.TaxiTaskFilter()
    task_result = _get_task_result()

    old_result = copy.deepcopy(task_result)
    f.postprocess_mvrp_task(task_result)

    assert old_result == task_result


@skip_if_remote
def test_taxi_postprocess_filters_unused_taxi():
    f = taxi.TaxiTaskFilter()
    task_result = _get_task_result()
    task_vehicles = task_result['result']['vehicles']

    sample_vehicle = copy.copy(task_vehicles[0])

    # Mark some of the used vehicles as taxi
    task_vehicles[0]['ref'] = taxi.build_taxi_ref('TAXI')

    vehicle_refs = [v['ref'] for v in task_vehicles]

    # Add some unused taxi
    for taxi_type, taxi_id in [('van', 1), ('lcv_m', 0), ('express', 2)]:
        vehicle = copy.copy(sample_vehicle)
        vehicle['id'] = taxi_id
        vehicle['ref'] = taxi.build_taxi_ref(taxi_type)
        task_vehicles.append(vehicle)

    # Add unused non-taxi vehicle, it should not be removed by this filter
    vehicle = copy.copy(sample_vehicle)
    vehicle['id'] = 2
    vehicle['ref'] = 'sample_vehicle_ref'
    task_vehicles.append(vehicle)
    vehicle_refs.append(vehicle['ref'])

    f.postprocess_mvrp_task(task_result)

    postprocessed_vehicle_refs = [v['ref'] for v in task_result['result']['vehicles']]
    assert postprocessed_vehicle_refs == vehicle_refs


@skip_if_remote
def test_positions_are_filtered_based_on_route_time_window_and_last_pos_time():
    times = datetime.fromtimestamp(100, tz=timezone.utc), None, datetime.fromtimestamp(200, tz=timezone.utc)
    positions = [
        {"timestamp": 99, "latitude": 55, "longitude": 37},
        {"timestamp": 100, "latitude": 55, "longitude": 37},
        {"timestamp": 150, "latitude": 55, "longitude": 37},
        {"timestamp": 151, "latitude": 55, "longitude": 37},
        {"timestamp": 200, "latitude": 55, "longitude": 37},
        {"timestamp": 201, "latitude": 55, "longitude": 37},
    ]
    expected_positions = [
        {"timestamp": 100, "latitude": 55, "longitude": 37},
        {"timestamp": 150, "latitude": 55, "longitude": 37},
        {"timestamp": 151, "latitude": 55, "longitude": 37},
        {"timestamp": 200, "latitude": 55, "longitude": 37},
    ]
    assert filter_new_positions(positions, None, times) == expected_positions

    expected_positions = [
        {"timestamp": 151, "latitude": 55, "longitude": 37},
        {"timestamp": 200, "latitude": 55, "longitude": 37},
    ]
    assert filter_new_positions(positions, 150, times) == expected_positions


def test_positions_from_forbidden_are_filtered_out():
    times = datetime.fromtimestamp(100, tz=timezone.utc), None, datetime.fromtimestamp(200, tz=timezone.utc)
    positions = [
        {"timestamp": 150, "latitude": 55.971, "longitude": 37.41},
        {"timestamp": 151, "latitude": ZERO_POSITION_EPSILON * 0.9, "longitude": ZERO_POSITION_EPSILON * 0.9},
    ]
    assert filter_new_positions(positions, None, times) == []
