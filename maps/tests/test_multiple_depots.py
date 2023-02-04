import copy

from util import run_emulator


DEFAULT_COURIERS = [
    {
        'id': '1',
        'position': {
            'lat': 1.000,
            'lon': 1.000,
        },
    },
    {
        'id': '2',
        'position': {
            'lat': 1.005,
            'lon': 1.005,
        },
    },
]

DEFAULT_DEPOTS = [
    {
        'id': '1',
        'position': {
            'lat': 1.001,
            'lon': 1.001,
        },
        'service_duration_s': 5,
    },
    {
        'id': '2',
        'position': {
            'lat': 1.003,
            'lon': 1.003,
        },
        'service_duration_s': 5,
    },
]

DEFAULT_ORDERS = [
    {
        'id': '1',
        'position': {
            'lat': 1.002,
            'lon': 1.002,
        },
        'created_at': 1,
        'service_duration_s': 10,
        'depot_id': '1',
    },
    {
        'id': '2',
        'position': {
            'lat': 1.004,
            'lon': 1.004,
        },
        'created_at': 1,
        'service_duration_s': 10,
        'depot_id': '2',
    },
]


def build_task(couriers, depots, orders):
    return copy.deepcopy(
        {
            'couriers': couriers,
            'start_at': 1,
            'depots': depots,
            'orders': orders,
            'options': {
                'matrix_router': 'geodesic',
                'solver_time_limit_s': 1,
                'thread_count': 1,
            },
        }
    )


def test_one_courier_deliver_all_orders_from_all_depots():
    task = build_task(DEFAULT_COURIERS[:1], DEFAULT_DEPOTS, DEFAULT_ORDERS)
    result = run_emulator(task)

    assert result['metrics']['courier_metrics']['delivery_count']['count'] == 1
    assert result['metrics']['courier_metrics']['delivery_count']['minimum'] == 2
    assert result['metrics']['courier_metrics']['delivery_count']['maximum'] == 2
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['count'] == 2
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['maximum'] == 1
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['minimum'] == 1


def test_two_couriers_deliver_all_orders_from_all_depots():
    task = build_task(DEFAULT_COURIERS, DEFAULT_DEPOTS, DEFAULT_ORDERS)
    result = run_emulator(task)

    assert result['metrics']['courier_metrics']['delivery_count']['count'] == 2
    assert result['metrics']['courier_metrics']['delivery_count']['minimum'] == 1
    assert result['metrics']['courier_metrics']['delivery_count']['maximum'] == 1
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['count'] == 2
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['maximum'] == 1
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['minimum'] == 1


def test_two_couriers_deliver_all_orders_from_one_depot():
    task = build_task(DEFAULT_COURIERS, DEFAULT_DEPOTS[:1], DEFAULT_ORDERS)
    task['orders'][1]['depot_id'] = DEFAULT_DEPOTS[0]['id']
    result = run_emulator(task)

    assert result['metrics']['courier_metrics']['delivery_count']['count'] == 2
    assert result['metrics']['courier_metrics']['delivery_count']['average'] == 1
