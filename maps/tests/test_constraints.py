from util import run_emulator, read_task


def test_simple_example_requires_one_courier():
    result = run_emulator(read_task('example.json'))
    # 1 route is enough
    assert result['metrics']['courier_metrics']['delivery_count']['minimum'] == 0
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['count'] == 1
    assert result['metrics']['delivery_metrics']['delivery_duration']['maximum'] < 3600


def test_extra_weight_requires_two_couriers():
    task = read_task('example.json')
    task['couriers'][0]['capacity'] = {'weight_kg': 100}
    task['couriers'][1]['capacity'] = {'weight_kg': 1}
    task['orders'][0]['shipment_size'] = {'weight_kg': 0.5}
    task['orders'][1]['shipment_size'] = {'weight_kg': 50}
    task['orders'][2]['shipment_size'] = {'weight_kg': 50}
    result = run_emulator(task)
    # 2 routes are needed
    assert result['metrics']['courier_metrics']['delivery_count']['minimum'] == 1
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['count'] == 2


def test_time_window():
    task = read_task('example.json')
    task['orders'][-1]['time'] = {'type': 'window', 'value': {'start': 1640884528, 'end': 1640884828}}
    # It's important here, because system time (now) incrementation is first operation in emulator loop.
    task['options']['iteration_period'] = 60.0
    result = run_emulator(task)
    # 1 route is enough, but more time is needed
    assert result['metrics']['courier_metrics']['delivery_count']['minimum'] == 0
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['count'] == 1
    assert result['metrics']['delivery_metrics']['delivery_duration']['maximum'] > 500
