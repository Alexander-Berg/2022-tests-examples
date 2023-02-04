import dateutil.tz
import pytest
import sys
import time
from threading import Thread, Event
from maps.b2bgeo.ya_courier.backend.test_lib import util
from datetime import datetime
from dateutil import parser
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote, system_env, EnvDB, get_passport_accounts
from maps.b2bgeo.ya_courier.backend.test_lib.env import bring_up_ya_courier, bring_up_periodic_tasks
from ya_courier_backend.models.order import get_order_history_event_records, OrderHistoryEvent
from maps.b2bgeo.libs.py_sqlalchemy_utils.mixins import (
    SIGNAL_CONCURRENT_UPDATE_FAILURE_COUNT
)

ORDER_LOC = [
    {
        'lat': 55.791928,
        'lon': 37.841492,
    },
    {
        'lat': 55.670455,
        'lon': 37.284573,
    },
    {
        'lat': 55.930423,
        'lon': 37.520571
    }
]

COURIER_START_POS = {
    'lat': 55.6447,
    'lon': 37.6727
}
TIME_ZONE = dateutil.tz.gettz('Europe/Moscow')
ROUTE_DATETIME = datetime.now(tz=TIME_ZONE)


start_barrier = Event()


def _timenow_to_ts(time_now):
    return parser.parse(str(ROUTE_DATETIME.date()) + 'T' + time_now).replace(tzinfo=TIME_ZONE).timestamp()


class UserEquipment:
    def __init__(self, env, idx, courier_id, route_id, track_template):
        self.env = env
        self.idx = idx
        self.courier_id = courier_id
        self.route_id = route_id
        self.track = [
            (
                location['lat'],
                location['lon'],
                _timenow_to_ts(location['time_now'].format(idx))
            )
            for location in track_template
        ]

    def __call__(self):
        while not start_barrier.is_set():
            continue
        util.push_positions(self.env, courier_id=self.courier_id, route_id=self.route_id, track=self.track)


class CockpitOperator:
    def __init__(self, env, idx, order):
        if idx % 2 == 0:
            env = env._replace(url=env.alternative_url)
        self.env = env
        self.idx = idx
        self.order = order

    def __call__(self):
        change = {
            "time_interval": "08:00 - 17:{:02d}".format(self.idx)
        }
        while not start_barrier.is_set():
            continue

        util.patch_order(self.env, self.order, change)


class RoutedOrdersJob:
    def __init__(self, env, courier_id, route_id, track_template):
        self.env = env._replace(url=env.alternative_url)
        self.courier_id = courier_id
        self.route_id = route_id
        start_pos = track_template[0]
        route_start_time_seconds = 31
        self.track = [
            (
                start_pos['lat'],
                start_pos['lon'],
                _timenow_to_ts(start_pos['time_now'].format(route_start_time_seconds))
            )
        ]
        for i in range(5):
            location = util.get_position_shifted_east(start_pos['lat'], start_pos['lon'], 10 * (i + 1))
            route_start_time_seconds += 1
            self.track.append(
                (
                    location[0],
                    location[1],
                    _timenow_to_ts(start_pos['time_now'].format(route_start_time_seconds))
                )
            )

    def __call__(self):
        now = datetime.now(tz=TIME_ZONE).time().strftime('%H:%M:%S')
        util.push_positions(self.env, self.courier_id, self.route_id, self.track)
        current_location = {
            'lat': self.track[-1][0],
            'lon': self.track[-1][0],
            'time_now': now
        }
        while not start_barrier.is_set():
            continue
        util.query_routed_orders(self.env, self.courier_id, self.route_id, point=current_location, strict=False)


def check_unistat(env):
    stats = util.get_unistat(env)
    assert len(stats) > 0
    signals = {(x[0].split(';')[1] if ';' in x[0] else x[0]): x[1] for x in stats}
    count_signal = SIGNAL_CONCURRENT_UPDATE_FAILURE_COUNT + '_summ'
    return signals.get(count_signal, 0) > 0


@skip_if_remote
def test_order_history_contention(system_env_with_db_many_instances):
    """
    The test checks that there are no lost updates due to a high load (many concurrent updates) on a given order.
    Particularly, it is important that routed-orders functionality (either directly being triggered by a user or
    as a background job) does not interfere with the mark delivery functionality.
    Moreover, we introduce additional source of contention by making frequent changes to the order's time interval.

    Note:
        - It is important to test on an environment with several backend instances running simultaneously, since we
          don't face any multithreading issues within one backend process.
        - Some of the workers' requests are expected to fail, but the workers must succeed eventually because of
          automatic retries.
    """
    system_env = system_env_with_db_many_instances
    delivery_order = ORDER_LOC[0]
    near_order = util.get_position_shifted_east(delivery_order['lat'], delivery_order['lon'], target_distance=20)
    track_template = [
        {
            'lat': COURIER_START_POS['lat'],
            'lon': COURIER_START_POS['lon'],
            'time_now': "09:00:{:02d}"
        },
        util.get_location_nearby(delivery_order, "09:15:{:02d}"),
        {
            'lat': delivery_order['lat'],
            'lon': delivery_order['lon'],
            'time_now': "09:30:{:02d}"
        },
        {
            'lat': near_order[0],
            'lon': near_order[1],
            'time_now': "09:45:{:02d}"
        },
        util.get_location_nearby(delivery_order, "09:59:{:02d}"),
    ]

    workers = []
    with util.create_route_env(system_env, "test_order_history_contention", order_locations=ORDER_LOC) as env:
        courier_id = env['courier']['id']
        route_id = env["route"]["id"]

        workers.append(Thread(target=RoutedOrdersJob(system_env, courier_id, route_id, track_template)))

        for i in range(5):
            workers.append(Thread(target=UserEquipment(system_env, i, courier_id, route_id, track_template)))

        num_operators = 3
        for i in range(num_operators):
            workers.append(Thread(target=CockpitOperator(system_env, i, env["orders"][0])))

        for worker in workers:
            worker.start()

        time.sleep(2.5)
        start_barrier.set()
        time.sleep(5)
        for worker in workers:
            worker.join()

        order = util.get_order(system_env, env["orders"][0]['id'])
        assert len(get_order_history_event_records(order, OrderHistoryEvent.arrival)) == 1
        assert len(get_order_history_event_records(order, OrderHistoryEvent.departure)) == 1
        assert len(get_order_history_event_records(order, OrderHistoryEvent.interval_update)) == num_operators
        assert len(get_order_history_event_records(order, OrderHistoryEvent.status_update)) == 1
        assert order['status'] == 'finished'

        check_unistat(system_env)
        check_unistat(system_env._replace(url=system_env.alternative_url))


@pytest.fixture(scope='module')
def system_env_with_db_many_instances():
    sys.stderr.write('system_env_with_db_many_instances() started.\n')
    sys.stderr.flush()

    with system_env(mvrp_solver_uri=None) as instance_env:
        env = EnvDB(
            url=instance_env['url'],
            mock_apikeys_url=instance_env['mock_apikeys_url'],
            mock_sender_url=instance_env['mock_sender_url'],
            mock_pipedrive_url=instance_env['mock_pipedrive_url'],
            mock_blackbox_url=instance_env['mock_blackbox_url'],
            company_id=None,
            auth_header="test_user:test_uid",
            auth_header_super="test_user_super:test_uid_super",
            passport_accounts=get_passport_accounts(),
            verify_ssl=False,
            existing=False,
            alternative_url=None
        )

        company_id = util.create_company(env, "test_user")

        with bring_up_periodic_tasks(instance_env['pg_settings']) as periodic_tasks_port,\
                bring_up_ya_courier(
                    apikeys_url=instance_env['mock_apikeys_url'],
                    sender_url=instance_env['mock_sender_url'],
                    pipedrive_url=instance_env['mock_pipedrive_url'],
                    blackbox_url=instance_env['mock_blackbox_url'],
                    graph_matcher_url=instance_env['mock_graph_matcher_url'],
                    pg_config=instance_env['pg_settings'],
                    periodic_tasks_port=periodic_tasks_port
                ) as (ya_courier_alternative_url, pid):
            assert env.url != ya_courier_alternative_url
            yield env._replace(alternative_url=ya_courier_alternative_url)._replace(company_id=company_id)
