from datetime import timedelta
from dateutil import parser

from util import run_emulator, create_empty_task, add_courier, add_depot, add_order


def test_times_are_increasing():
    # Send courier just before he's arriving at depot (by teleporting)
    task = create_empty_task()
    task['options']['iteration_period'] = 300
    add_depot(task)
    add_courier(task)
    # Orders are distanced such that transit duration is > 3min
    add_order(task, created_at=0, lat=55.730725, lon=37.628855, time_window_end=300)
    add_order(task, created_at=800, lat=55.730725, lon=37.628855, time_window_end=900)
    add_order(task, created_at=1100, lat=55.730725, lon=37.628855, time_window_end=1200)
    result = run_emulator(task)
    for routes in result['routes']:
        times = [loc['time'] for loc in routes['locations']]
        for prev, next in zip(times, times[1:]):
            time_diff = parser.parse(next) - parser.parse(prev)
            assert time_diff > timedelta(
                minutes=3
            ), f'Courier {routes["courier"]} visited locations are incorrect: {prev, next}'
