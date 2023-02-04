from random import randint, random
from math import pi
import pytest

from ya_courier_backend.util.position import Position
from ya_courier_backend.util.courier_idle import (
    CourierIdleResult,
    check_courier_idle,
    haversine_distance_m,
    CourierIdleStateType,
    DEFAULT_COURIER_IDLE_CONFIG as config
)


PARTITION_TIME = 5 * 60


def _generate_next_position(pos, dist, time, direction=1):
    earth_radius_m = 6371000
    time *= 60
    new_pos = Position(
        lon=pos.lon,
        lat=pos.lat + (direction * dist / earth_radius_m) / pi * 180,
        time=pos.time + time
    )
    return new_pos


def _get_test_data_results(partition):
    last_state = CourierIdleResult()
    results = []
    old_points = []
    for positions in partition:
        last_state = check_courier_idle(old_points, positions, last_state, config)
        results.append(last_state)
        old_points.extend(positions)
        old_points = list(filter(lambda pos: pos.time >= last_state.last_window_finish_timestamp - config['time_window'], old_points))
    return results


def _check_test(results, data_results):
    assert list(map(lambda x: x.type, results)) == data_results


def _split_data(data):
    partition = []
    pointer = 0
    while pointer < len(data):
        cur_part = []
        cur_part.append(data[pointer])
        pointer += 1
        while pointer < len(data) and data[pointer].time - cur_part[0].time < PARTITION_TIME:
            cur_part.append(data[pointer])
            pointer += 1
        partition.append(cur_part)
    return partition


def _route_normal(cur_position, data):
    """
        - normal route with 5 generated positions
    """
    for _ in range(5):
        cur_position = _generate_next_position(cur_position, 100, 10)
        data.append(cur_position)
    results = [CourierIdleStateType.no_change] * 6
    return data, results


def _route_normal_long(cur_position, data):
    """
        - normal route with 20 generated positions
    """
    for _ in range(20):
        cur_position = _generate_next_position(cur_position, 20, 2)
        data.append(cur_position)
    results = [CourierIdleStateType.no_change] * 7
    return data, results


def _route_idle(cur_position, data):
    """
        - generate 2 normal route positions
        - generate 3 positions with small distance between them
        - again generate 2 normal route positions
        - algorithm should change state to idle when it'll process close positions
    """
    for _ in range(2):
        cur_position = _generate_next_position(cur_position, 100, 10)
        data.append(cur_position)
    for _ in range(3):
        cur_position = _generate_next_position(cur_position, 5, 10)
        data.append(cur_position)
    for _ in range(2):
        cur_position = _generate_next_position(cur_position, 100, 10)
        data.append(cur_position)
    results = [CourierIdleStateType.no_change] * 4 + \
              [CourierIdleStateType.change] + \
              [CourierIdleStateType.no_change] * 2 + \
              [CourierIdleStateType.change]
    return data, results


def _route_gps_flapping(middle_position, data):
    """
        - fix some middle position
        - generate 8 positions on segment with distance no more than 50 meters
        - algorithm should change state to idle because all positions are close to each other
    """
    for i in range(8):
        data.append(_generate_next_position(middle_position, 50*random(), i*15, direction=2*randint(0, 1)-1))
    results = [CourierIdleStateType.no_change] * 2 + \
                [CourierIdleStateType.change] + \
                [CourierIdleStateType.no_change] * 5
    return data, results


def _route_broken_connection(cur_position, data):
    """
        - generate 3 normal route positions
        - generate 1 position with big difference in time and distance
        - again generate 3 normal route positions
        - algorithm shouldn't change state to idle
    """
    for _ in range(3):
        cur_position = _generate_next_position(cur_position, 100, 15)
        data.append(cur_position)
    cur_position = _generate_next_position(cur_position, 2000, 60)
    for _ in range(3):
        cur_position = _generate_next_position(cur_position, 100, 15)
        data.append(cur_position)
    results = [CourierIdleStateType.no_change] * 7
    return data, results


def _route_reverse(cur_position, data):
    """
        - generate 6 normal route positions
        - after that we should simulate courier's turn around
        - copy previous positions and add them in reversed order
    """
    for _ in range(6):
        cur_position = _generate_next_position(cur_position, 100, 6)
        data.append(cur_position)
    for pos in data[::-1]:
        data.append(Position(pos.lon, pos.lat, data[-1].time + 6 * 60))
    results = [CourierIdleStateType.no_change] * 14
    return data, results


def test_generate_next_position():
    position = Position(60, 60, 1e9+41)
    forward_position = _generate_next_position(position, 100, 10)
    backward_position = _generate_next_position(position, 200, 50, -1)

    assert position.time + 10 * 60 == forward_position.time
    assert position.time + 50 * 60 == backward_position.time

    EPS = 0.01
    assert abs(haversine_distance_m(position, forward_position) - 100) < EPS
    assert abs(haversine_distance_m(position, backward_position) - 200) < EPS


def test_result_data():
    data = [Position(60, 60, 1e9+41)]
    data.append(_generate_next_position(data[-1], 50, 15))
    data.append(_generate_next_position(data[-1], 50, 15))
    data.append(_generate_next_position(data[-1], 300, 15))
    old_points = []
    last_state = CourierIdleResult()

    last_state = check_courier_idle(old_points, [data[0]], last_state, config)
    old_points = data[:1]
    assert last_state.is_idle is False
    assert last_state.first_position is None
    assert not last_state.idle_position
    assert last_state.last_window_finish_timestamp == data[0].time
    assert last_state.type == CourierIdleStateType.no_change

    last_state = check_courier_idle(old_points, [data[1]], last_state, config)
    old_points = data[:2]
    assert last_state.is_idle is False
    assert last_state.first_position is None
    assert not last_state.idle_position
    assert last_state.last_window_finish_timestamp == data[1].time
    assert last_state.type == CourierIdleStateType.no_change

    last_state = check_courier_idle(old_points, [data[2]], last_state, config)
    old_points = data[:3]
    assert last_state.is_idle is True
    assert last_state.first_position == data[0]
    assert data[2] == last_state.idle_position
    assert last_state.last_window_finish_timestamp == data[2].time
    assert last_state.type == CourierIdleStateType.change

    last_state = check_courier_idle(old_points, [data[3]], last_state, config)
    old_points = data[1:4]
    assert last_state.is_idle is False
    assert last_state.first_position is None
    assert data[3] == last_state.idle_position
    assert last_state.last_window_finish_timestamp == data[3].time
    assert last_state.type == CourierIdleStateType.change


@pytest.mark.parametrize("gen_function", [_route_normal, _route_normal_long, _route_idle, _route_gps_flapping, _route_broken_connection, _route_reverse])
def test_algorithm(gen_function):
    start_position = Position(60, 60, 1e9+41)
    data, data_results = gen_function(start_position, [start_position])
    partition = _split_data(data)
    results = _get_test_data_results(partition)
    _check_test(results, data_results)
