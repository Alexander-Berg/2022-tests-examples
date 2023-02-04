from util import run_emulator, create_empty_task, add_courier, add_depot, add_order


def test_time_heuristic():
    #  We should move for first order because we are already late
    task = create_empty_task()
    add_depot(task)
    add_courier(task)
    add_order(task, lat=59.9, time_window_end=20000)
    add_order(task, created_at=900, time_window_end=20000)
    result = run_emulator(task)
    assert result['metrics']['courier_metrics']['delivery_count']['minimum'] == 2
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['average'] == 1


def test_distance_heuristic():
    #  First order is too close, so we can wait for demand trend drop and deliver it
    task = create_empty_task()
    add_depot(task)
    add_courier(task, capacity={'units': 10})
    add_order(task, shipment_size={'units': 9})
    add_order(task, created_at=1000, lat=59.9)
    result = run_emulator(task)
    assert result['metrics']['courier_metrics']['delivery_count']['minimum'] == 2
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['average'] == 1


def test_demand_heuristic():
    #  There comes two orders which we can deliver, but demand trend is high
    #  So we wait until third order created, then again waiting for demand trend drop
    task = create_empty_task()
    add_depot(task)
    add_courier(task)
    add_order(task, lat=57.9)
    add_order(task, lat=57.9)
    add_order(task, created_at=600, lat=57.9)
    result = run_emulator(task)
    assert result['metrics']['courier_metrics']['delivery_count']['minimum'] == 3
    assert result['metrics']['delivery_metrics']['delivery_count_per_route']['count'] == 1
