#!/usr/bin/env python3

import sys
import datetime
import json
import logging
import os
import random
import re
import threading
import time
import itertools
from argparse import ArgumentParser
from retrying import retry
from enum import Enum
import functools
from multiprocessing import Pool
import requests
from requests.packages.urllib3.exceptions import InsecureRequestWarning, InsecurePlatformWarning, SNIMissingWarning
requests.packages.urllib3.disable_warnings((InsecureRequestWarning, InsecurePlatformWarning, SNIMissingWarning))

COMPANY_ID = 6
ROUTE_NUMBER = "LOAD_TESTING_ROUTE"
COURIER_NUMBER = "LOAD_TESTING_COURIER"
ORDER_NUMBER = "LOAD_TESTING_NUMBER"
SOLVER_TEST_APIKEY = os.environ.get('MVRP_TOKEN', '992044a6-d49b-4e69-a6a1-d5dc26600bf0')
COURIER_URLS = {
    "stable": "https://courier.yandex.ru",
    "dev": "https://test.courier.yandex.ru"
}
SYNC_SOLVER_URLS = {
    "stable": "http://b2bgeo-syncsolver.maps.yandex.net",
    "dev": "http://b2bgeo-syncsolver.testing.maps.yandex.net"
}
BATCH_SIZE = 100

stopevent = threading.Event()

yc_client = None
async_client = None
sync_client = None


class YaCourierRequest(Enum):
    GET_DEPOTS = 'companies/{company_id}/depots'
    GET_COURIERS = 'companies/{company_id}/couriers?page={}'
    GET_COURIER = 'companies/{company_id}/couriers?number={}'
    POST_ORDERS_BATCH = 'companies/{company_id}/orders-batch'
    PATCH_ORDERS = 'companies/{company_id}/orders/{}'
    GET_ORDERS = 'companies/{company_id}/orders/{}#get'
    POST_COURIERS = 'companies/{company_id}/couriers'
    DELETE_COURIERS = 'companies/{company_id}/couriers/{}'
    GET_ROUTES = 'companies/{company_id}/routes?page={}&date={}'
    POST_ROUTES = 'companies/{company_id}/routes'
    POST_ROUTES_BATCH = 'companies/{company_id}/routes-batch'
    GET_ROUTE_ORDERS = 'companies/{company_id}/orders?route_id={}'
    GET_ORDER_DETAILS = 'companies/{company_id}/order-details?date={}'
    DELETE_ORDERS = 'companies/{company_id}/orders/{}'
    POST_PUSH_POSITIONS = 'couriers/{}/routes/{}/push-positions'
    POST_PUSH_POSITIONS_IMEI = 'gps-trackers/{}/push-positions'


class AsyncSolverRequest(Enum):
    POST_MVRP = 'add/mvrp?apikey={apikey}'
    POST_SVRP = 'add/svrp?apikey={apikey}'
    GET_MVRP = 'result/{}'
    GET_SVRP = 'result/svrp/{}'


class SyncSolverRequest(Enum):
    POST_SOLVE = 'solve?apikey={apikey}'


class BaseClient:
    def __init__(self):
        self.session = requests.Session()
        self.url = None
        self.url_prefix = '/'
        self.headers = None

    def mount(self, *args):
        self.session.mount(*args)

    def request(self, what, *args, data=None, make_json=True, throw=True, **kwargs):
        method = what.name.split('_', 1)[0]
        url_pattern = what.value
        url = self.url + self.url_prefix + url_pattern.format(*args, **kwargs)
        logging.debug("request: %s %s", method, url)
        response = self.session.request(method, url, headers=self.headers, json=data, verify=False)
        if throw and response.status_code != requests.codes.ok:
            raise Exception("Failed to {} method={} {}: {}".format(what.name, method, url, response.content))
        return response.json() if make_json else response


class YaCourierClient(BaseClient):
    def __init__(self, environment, token):
        super().__init__()
        self.url = COURIER_URLS[environment]
        self.url_prefix = '/api/v1/'
        self.headers = {"Content-Type": "application/json", "Authorization": "OAuth " + token}

    def request(self, what, *args, data=None, make_json=True, throw=True):
        return super().request(what, *args, data=data, make_json=make_json, throw=throw, company_id=COMPANY_ID)


class AsyncSolverClient(BaseClient):
    def __init__(self, environment, apikey=SOLVER_TEST_APIKEY):
        super().__init__()
        self.url = COURIER_URLS[environment]
        self.url_prefix = '/vrs/api/v1/'
        self.apikey = apikey

    def request(self, what, *args, data=None, make_json=True, throw=True):
        return super().request(what, *args, data=data, make_json=make_json, throw=throw, apikey=self.apikey)


class SyncSolverClient(BaseClient):
    def __init__(self, environment, apikey=SOLVER_TEST_APIKEY):
        super().__init__()
        self.url = SYNC_SOLVER_URLS[environment]
        self.apikey = apikey
        self.headers = {"Content-Type": "application/json"}

    def request(self, what, *args, data=None, make_json=True, throw=True):
        return super().request(what, *args, data=data, make_json=make_json, throw=throw, apikey=self.apikey)


def parse_args():
    parser = ArgumentParser(description="Load testing (MVRP/SVRP/PushPos)")
    parser.add_argument('-w', '--waves', required=True, nargs='+', help='JSON file with delivery wave(s) data')
    parser.add_argument('-v', '--verbose', action='store_true', help='Print more info')
    parser.add_argument('-e', '--environment', choices=COURIER_URLS.keys(), default="dev", help='Environment to use')
    parser.add_argument('-a', '--oauth-token', default=os.environ.get('YA_COURIER_TEST_TOKEN'),
                        help='Valid OAuth token for ya-courier-backend instance')
    parser.add_argument('-l', '--logfile', default='vrp_load_testing.log', help='Log filename')
    parser.add_argument('-t', '--duration', default=sys.maxsize, type=int,
                        help='Stop test after this time is passed (secs)')
    parser.add_argument('--use-imei', action='store_true', help='Use IMEI for push-positions')
    parser.add_argument('--clean-orders-on-start', action='store_true', default=False,
                        help='Delete orders before start')
    parser.add_argument('--threads-push-position', type=int, default=0,
                        help='Number of threads used to push positions')
    parser.add_argument('--number-processes', type=int, default=0,
                        help='Number of processes')
    parser.add_argument('--date', type=datetime.date.fromisoformat, default=datetime.date.today(),
                        help='Date of routes, if specified - tool can reuse already created routes / orders on the specified date; example: 2019-07-04')
    return parser.parse_args()


class AtomicValue:
    def __init__(self, value=0):
        self._value = value
        self._lock = threading.Lock()

    def inc(self):
        self.add(1)

    def add(self, value):
        with self._lock:
            self._value += value
            return self._value

    def value(self):
        return self._value


class AtomicDict:
    def __init__(self):
        self._dict = {}
        self._lock = threading.Lock()

    def inc(self, item):
        self.add(item, 1)

    def add(self, item, value):
        with self._lock:
            if item not in self._dict:
                self._dict[item] = 0
            self._dict[item] += value
            return self._dict[item]

    def values(self):
        with self._lock:
            return self._dict.copy()


thread_names = ['mvrp', 'svrp', 'push_pos', 'change_order_status']

thread_stats = {
    name: {
        'count': AtomicValue(),
        'started': AtomicValue(),
        'succeeded': AtomicValue(),
        'failed': AtomicValue(),
    } for name in thread_names
}

request_stats = {
    name: {
        'started': AtomicValue(),
        'succeeded': AtomicValue(),
        'response_time': AtomicValue(),
        'failed': AtomicValue(),
        'retries': AtomicValue(),
        'codes': AtomicDict()
    } for name in ['async_mvrp_add', 'async_mvrp_get', 'sync_svrp_get', 'push_pos', 'push_pos_imei', 'change_order_status']
}


def peek_vehicles(l, count):
    assert l
    if len(l) < count:
        unused_id = 1 + max(l, key=lambda x: x['id'])['id']
        old_count = len(l)
        new_list = random.sample(l, old_count)
        while len(new_list) < count:
            new_list.append(new_list[random.randint(0, old_count - 1)].copy())
            new_list[-1]['id'] = unused_id
            unused_id += 1
        return new_list
    else:
        return random.sample(l, count)


def peek_locations(locations, count):
    assert locations
    return random.choices(locations, k=count)


def interval_random(data):
    return random.uniform(data[0], data[1])


def make_log_exception_decorator(exception_criterion=lambda x: True):
    def log_exception(f):
        @functools.wraps(f)
        def wrapper(*args, **kwargs):
            try:
                return f(*args, **kwargs)
            except Exception as e:
                if exception_criterion(e):
                    logging.exception(str(e))
                raise

        return wrapper
    return log_exception


log_exceptions = make_log_exception_decorator()


def prepare_solver_options(wave, thread_name):
    options = wave[thread_name]['solver_options'].copy()
    for name in ['routing_mode', 'time_zone']:
        if name in options:
            raise Exception(
                '{} cannot be specified in {}:options,'
                ' it should be specified at the top level of wave'.format(name, thread_name))
        if name not in wave:
            raise Exception(
                '{} is missing in wave'.format(name))
        options[name] = wave[name]
    return options


def prepare_vehicles_and_locations(wave):
    template_json = wave['out.template_json']
    if wave['location_count'] < wave['vehicle_count']:
        raise Exception(
            'location_count should be greater or equal than vehicle_count in delivery waves file')
    print('{} vehicles, {} locations in {}'.format(len(template_json['vehicles']), len(template_json['locations']),
                                                   wave['template']))
    template_json['vehicles'] = peek_vehicles(template_json['vehicles'], wave['vehicle_count'])
    for v in template_json['vehicles']:
        if 'visited_locations' in v:
            del v['visited_locations']
    template_json['locations'] = peek_locations(template_json['locations'], wave['location_count'])
    logging.info("prepared %d locations", len(template_json['locations']))


def split_list(plain_list, count, index=False):
    taken = 0
    for idx in range(count):
        take = (len(plain_list) - taken) // (count - idx)
        if index:
            yield idx, plain_list[taken:taken + take]
        else:
            yield plain_list[taken:taken + take]
        taken += take
    assert taken == len(plain_list)


def prepare_mvrp(wave):
    data = wave['out.template_json']
    data['options'] = prepare_solver_options(wave, 'mvrp')
    return data


def prepare_svrps(wave):
    data = []
    template_json = wave['out.template_json']
    vehicle_count = len(template_json['vehicles'])

    for vehicle_index, locations in split_list(template_json['locations'], vehicle_count, index=True):
        vehicle = template_json['vehicles'][vehicle_index]
        vehicle['visited_locations'] = [{"id": x['id']} for x in locations]
        data.append({
            'depot': template_json['depot'],
            'locations': locations,
            'options': prepare_solver_options(wave, 'svrp'),
            'vehicle': vehicle
        })

    return data


def sleep_retry():
    print('Retrying...')
    time.sleep(random.uniform(4, 6))


@retry(stop_max_attempt_number=5)
@log_exceptions
def prepare_depots():
    print('Loading depots...')
    depots = yc_client.request(YaCourierRequest.GET_DEPOTS)
    if len(depots) < 1:
        raise Exception("Failed to get depots: {}".format(depots))
    return depots


def load_couriers():
    print('Loading couriers...')
    couriers = []
    for page in range(100):
        page_couriers = yc_client.request(YaCourierRequest.GET_COURIERS, 1+page)
        if not page_couriers:
            return couriers
        couriers.extend([x for x in page_couriers if x['number'].startswith(COURIER_NUMBER)])
    raise Exception("Too many couriers")


@retry(stop_max_attempt_number=5)
@log_exceptions
def add_courier(courier_number):
    print('Adding courier {}'.format(courier_number))
    yc_client.request(YaCourierRequest.POST_COURIERS,
                      data={
                          'number': courier_number,
                          'name': 'Courier_{}'.format(courier_number),
                          'sms_enabled': False
                      })


def prepare_couriers(couriers_count):
    print('Prepare {} couriers...'.format(couriers_count))
    couriers = load_couriers()
    if len(couriers) >= couriers_count:
        print('Found couriers: {}'.format(len(couriers)))
        return couriers[:couriers_count]
    courier_numbers = {x['number'] for x in couriers}

    for idx in range(couriers_count):
        courier_number = make_courier_number(idx)
        if courier_number in courier_numbers:
            continue
        add_courier(courier_number)
    couriers = load_couriers()
    if len(couriers) < couriers_count:
        raise Exception("Unexpected number of couriers: actual: {}, expected: {}"
                        .format(len(couriers), couriers_count))

    print('Found couriers: {}'.format(len(couriers)))
    return couriers[:couriers_count]


@retry(stop_max_attempt_number=5)
@log_exceptions
def create_orders(data):
    return yc_client.request(YaCourierRequest.POST_ORDERS_BATCH, data=data)


@retry(stop_max_attempt_number=5)
@log_exceptions
def load_orders(date):
    result = yc_client.request(YaCourierRequest.GET_ORDER_DETAILS, date)
    result = {order_detail['order_id']: order_detail['order_number']
              for order_detail in result if is_order_number(order_detail['order_number'])}
    print('Found orders: {}'.format(len(result)))
    return result


def prepare_orders(args, routes, locations):
    """
    Make order from each location.
    Distribute orders between routes uniformly.
    """
    print('Prepare orders for {} routes, {} locations...'.format(len(routes), len(locations)))
    assert len(routes) <= len(locations)
    result = {'inserted': 0, 'updated': 0}

    orders = load_orders(args.date.strftime("%F"))
    if len(orders) >= len(locations):
        return result

    orders = list(enumerate(locations))
    orders_count = 0
    for orders_chunk in split_list(orders, 1 + len(orders) // BATCH_SIZE):
        print('prepare {} orders...'.format(len(orders_chunk)))
        orders_data = []
        for order_idx, location in orders_chunk:
            route_idx = int(order_idx * len(routes) / len(locations))
            route = routes[route_idx]
            location = location['point']
            orders_data.append({
                "address": "Leo Tolstoy Str, 16",
                "amount": 1,
                "comments": "Do not call",
                "customer_name": "golovasteek",
                "description": "Test order",
                "lat": location['lat'],
                "lon": location['lon'],
                "number": make_order_number(route['id'], order_idx),
                "payment_type": "cash",
                "phone": "+79161111111",
                "route_id": route['id'],
                "service_duration_s": 300,
                "time_interval": "00:00-1.01:00",
                "volume": 1,
                "weight": 1,
                "status": "new",
            })

            new_orders = create_orders(orders_data)
            for key in result.keys():
                result[key] += new_orders[key]
            orders_count += len(orders_data)
            orders_data = []

    print('Prepared orders {}: {}'.format(orders_count, result))
    return result


@retry(stop_max_attempt_number=2)
@log_exceptions
def delete_order(order_id):
    yc_client.request(YaCourierRequest.DELETE_ORDERS, order_id)


def clean_orders(routes):
    print('Cleaning orders...')
    for route in routes:
        orders = yc_client.request(YaCourierRequest.GET_ROUTE_ORDERS, route['id'])
        for order in orders:
            delete_order(order['id'])


def is_route_number(number, date):
    return number.startswith('{}/{}/'.format(ROUTE_NUMBER, date))


def make_route_number(idx, date):
    return "{}/{}/{}".format(ROUTE_NUMBER, date, idx)


def make_order_number(route_id, idx):
    return "{}/{}/{}".format(ORDER_NUMBER, route_id, idx)


def make_courier_number(idx):
    return '{}/{}'.format(COURIER_NUMBER, idx)


def is_order_number(number):
    return number.startswith('{}/'.format(ORDER_NUMBER))


@retry(stop_max_attempt_number=5)
@log_exceptions
def load_routes(date, use_imei=None):
    print('Loading routes...')
    items = []
    for page in range(100):
        page_items = yc_client.request(YaCourierRequest.GET_ROUTES, 1+page, date if date else "")
        if not page_items:
            return items
        page_items = [x for x in page_items if is_route_number(x['number'], date)]
        if use_imei is not None:
            page_items = [x for x in page_items if (use_imei and x['imei']) or (not use_imei and x['imei'] is None)]
        if date:
            page_items = [x for x in page_items if x['date'] == date]
        items.extend(page_items)
    raise Exception("Too many routes")


@retry(stop_max_attempt_number=5)
@log_exceptions
def add_routes(routes_data):
    print('Creating/updating {} routes...'.format(len(routes_data)))
    yc_client.request(YaCourierRequest.POST_ROUTES_BATCH, data=routes_data)


def prepare_routes(args, depots, couriers, first_route_index, route_count):
    print('Creating/updating routes...')
    route_date = args.date.isoformat()
    routes = load_routes(route_date, args.use_imei)
    if len(routes) >= route_count:
        print('Found routes: {}'.format(len(routes)))
        return routes[:route_count]
    route_numbers = {x['number'] for x in routes}

    assert route_count > 0
    random_imei = random.randint(0, sys.maxsize - 1 - route_count)

    for routes_chunk in split_list(range(route_count), 1 + route_count // BATCH_SIZE):
        routes_data = []
        for idx in routes_chunk:
            route_number = make_route_number(idx, route_date)
            if route_number not in route_numbers:
                routes_data.append({
                    "courier_id": couriers[idx % len(couriers)]['id'],
                    "date": route_date,
                    "depot_id": depots[0]['id'],
                    "number": make_route_number(first_route_index + idx, route_date),
                    "imei": random_imei + idx if args.use_imei else None
                })
        if routes_data:
            add_routes(routes_data)

    routes = load_routes(route_date, args.use_imei)
    print('Found routes: {}'.format(len(routes)))
    return routes[:route_count]


def async_add_task(send_json, solver_type):
    if solver_type == 'mvrp':
        request_type = AsyncSolverRequest.POST_MVRP
    elif solver_type == 'svrp':
        request_type = AsyncSolverRequest.POST_SVRP
    else:
        raise Exception("Unknown solver type {}".format(solver_type))

    name = 'async_{}_add'.format(solver_type)
    logging.debug("{}: {}".format(name, request_type.value))

    stats = request_stats[name]
    stats['started'].inc()

    retry_count = 0

    while True:
        try:
            start_time = time.time()
            response = async_client.request(request_type, data=send_json, make_json=False, throw=False)

            logging.debug("Response {0}: {1}".format(name, response.status_code))

            if response.status_code == 202:
                parsed = response.json()
                response_id = parsed['id']
                stats['response_time'].add(time.time() - start_time)
                stats['succeeded'].inc()
                return response_id
            else:
                stats['codes'].inc(response.status_code)
                if response.ok:
                    logging.error("{0} Unexpected response: {1}".format(name, response.status_code))
                    break
                else:
                    logging.warning("{0} Failure response: {1}, {2}".format(name, response.status_code, response.text))

        except Exception:
            logging.exception("{} exception, url: {}".format(name, response.url))

        if retry_count >= 3:
            break
        retry_count += 1
        stats['retries'].inc()
        sleep_retry()

    stats['failed'].inc()
    return None


def async_get_task(resp_id, solver_type):
    if solver_type == 'mvrp':
        request_type = AsyncSolverRequest.GET_MVRP
    elif solver_type == 'svrp':
        request_type = AsyncSolverRequest.GET_SVRP
    else:
        raise Exception("Unknwn solver type {}".format(solver_type))

    name = 'async_{}_get'.format(solver_type)
    logging.debug("get {0} {2}: {1}".format(name, request_type.value, resp_id))

    stats = request_stats[name]
    stats['started'].inc()

    retry_count = 0

    while True:
        try:
            start_time = time.time()
            response = async_client.request(request_type, resp_id, make_json=False, throw=False)

            if response.status_code == 200:
                parsed = response.json()
                if 'result' in parsed and parsed['result']['solver_status'] in ['SOLVED', 'PARTIAL_SOLVED']:
                    logging.debug("Solved " + name)
                    stats['response_time'].add(time.time() - start_time)
                    stats['succeeded'].inc()
                    return True
                logging.warning("Not solved {} {}".format(resp_id, parsed))
                break
            elif response.status_code in [201, 202]:
                logging.debug("{} waiting for task {}: {} {}".format(name, resp_id, response.status_code,
                                                                     response.text))
                time.sleep(random.uniform(4, 6))
                continue
            else:
                stats['codes'].inc(response.status_code)
                if response.ok:
                    logging.error("{} Unexpected response for task {}: {}".format(name, resp_id, response.status_code))
                    break
                else:
                    logging.warning("{} Failure response for task {}: {} {}".format(name, resp_id, response.status_code,
                                                                                    response.text))

        except Exception:
            logging.exception("{} exception, url: {}".format(name, response.url))

        if retry_count >= 3:
            break
        retry_count += 1
        stats['retries'].inc()
        sleep_retry()

    stats['failed'].inc()
    return False


def async_mvrp_requester(wave):
    stats = thread_stats['mvrp']

    stats['count'].inc()

    next_task_at = time.time() + interval_random(wave['mvrp']['warming_up_s'])

    while True:
        stopevent.wait(max(next_task_at - time.time(), 0))
        if stopevent.is_set():
            break

        next_task_at = time.time() + interval_random(wave['mvrp']['period_s'])

        stats['started'].inc()

        resp_id = async_add_task(wave['out.mvrp'], 'mvrp')

        if resp_id is not None and async_get_task(resp_id, 'mvrp'):
            stats['succeeded'].inc()
            logging.debug("mvrp solved")
        else:
            stats['failed'].inc()


def sync_svrp_get_task(send_json):
    name = 'sync_svrp_get'
    request_type = SyncSolverRequest.POST_SOLVE
    logging.debug("{0} vehicle {2}: {1}".format(name, request_type.value, send_json['vehicle']['id']))

    stats = request_stats[name]
    stats['started'].inc()

    # TODO: Change order of vehicles's visited_locations

    retry_count = 0

    while True:
        try:
            start_time = time.time()
            response = sync_client.request(request_type, data=send_json, make_json=False, throw=False)

            logging.debug("Response {}: {}".format(name, response.status_code))
            print("{} Response {}: {}".format(sync_client.url, name, response.status_code))

            if response.status_code == 200:
                assert "id" in response.json()
                stats['response_time'].add(time.time() - start_time)
                stats['succeeded'].inc()
                return True
            else:
                stats['codes'].inc(response.status_code)
                if response.ok:
                    logging.error("{0} Unexpected response: {1}".format(name, response.status_code))
                    break
                else:
                    logging.warning("{0} Failure response: {1} {2}".format(name, response.status_code, response.text))

        except Exception:
            logging.exception("{} exception, url: {}".format(name, response.url))

        if retry_count >= 3:
            break
        retry_count += 1
        stats['retries'].inc()
        sleep_retry()

    stats['failed'].inc()
    return False


def sync_svrp_requester(wave, courier_idx):
    send_json = wave['out.svrps'][courier_idx]
    stats = thread_stats['svrp']

    stats['count'].inc()

    next_task_at = time.time() + interval_random(wave['svrp']['warming_up_s'])

    while True:
        stopevent.wait(max(next_task_at - time.time(), 0))
        if stopevent.is_set():
            break

        next_task_at = time.time() + interval_random(wave['svrp']['period_s'])

        stats['started'].inc()

        if sync_svrp_get_task(send_json):
            stats['succeeded'].inc()
            logging.debug("svrp solved")
        else:
            stats['failed'].inc()


def push_pos_send(send_json, courier_id, route_id, imei):
    if imei:
        request_type = YaCourierRequest.POST_PUSH_POSITIONS_IMEI
        params = imei,
        name = "push_pos_imei"
    else:
        request_type = YaCourierRequest.POST_PUSH_POSITIONS
        params = courier_id, route_id
        name = "push_pos"
    logging.debug("{0}: {1}".format(name, request_type.value))

    stats = request_stats[name]
    stats['started'].inc()

    try:
        start_time = time.time()
        logging.debug("%s: courier_id=%d route_id=%d imei=%s", name, courier_id, route_id, imei)
        response = yc_client.request(request_type, *params, make_json=False, throw=False, data=send_json)
        logging.debug("Response {0}: {1}".format(name, response.status_code))

        if response.status_code == 200:
            stats['response_time'].add(time.time() - start_time)
            stats['succeeded'].inc()
            return True
        else:
            stats['codes'].inc(response.status_code)
            if response.ok:
                logging.error("{0} Unexpected response: {1}".format(name, response.status_code))
            else:
                logging.warning("{0} Failure response: {1} {2}".format(name, response.status_code, response.text.strip()))

    except Exception as error:
        stats['codes'].inc(str(error))
        logging.exception("{} exception, url: {}".format(name, request_type.value))

    stats['failed'].inc()
    return False


class BaseThreadLoop:
    def __init__(self, wave, stats, payload):
        self.wave = wave
        self.params = self.wave.get(payload.name)
        self.payload = payload
        self.stats = stats[payload.name]
        self.stats['count'].inc()

    def __call__(self):
        if not self.params:
            return
        next_task_at = time.time() + interval_random(self.params['warming_up_s'])

        while True:
            stopevent.wait(max(next_task_at - time.time(), 0))
            if stopevent.is_set():
                break

            time_passed = interval_random(self.params['period_s'])
            next_task_at = time.time() + time_passed

            self.payload(self.stats, self.params, time_passed)


class PushPositionPayload:
    name = 'push_pos'

    def __init__(self, wave, courier_idxs, date):
        self.date = date
        self.tracks = []
        for courier_idx in courier_idxs:
            svrp = wave['out.svrps'][courier_idx]
            self.tracks.append({
                'svrp': svrp,
                'courier_id': wave['out.routes'][courier_idx]['courier_id'],
                'route_id': wave['out.routes'][courier_idx]['id'],
                'imei': wave['out.routes'][courier_idx]['imei'],
                'lat': svrp['locations'][0]['point']['lat'],
                'lon': svrp['locations'][0]['point']['lon'],
            })

    def __call__(self, stats, params, time_passed):
        for track in self.tracks:
            # 0.00008 ~= 20 km/h
            track['lat'] += (0.00008 * time_passed) * random.uniform(-1, 1)
            track['lon'] += (0.00008 * time_passed) * random.uniform(-1, 1)

            position = {
                "latitude": track['lat'],
                "longitude": track['lon'],
                "time": datetime.datetime.combine(self.date, datetime.datetime.now(datetime.timezone.utc).time(),
                                                  tzinfo=datetime.timezone.utc).isoformat()
            }
            if not track["imei"]:
                position["accuracy"] = 5
            send_json = {"positions": [position]}

            stats['started'].inc()
            if push_pos_send(send_json, track['courier_id'], track['route_id'], track['imei']):
                stats['succeeded'].inc()
            else:
                stats['failed'].inc()


class ChangeOrderStatusPayload:
    name = 'change_order_status'

    def __init__(self, orders):
        self.order_ids = list(orders.keys())

    def __call__(self, stats, params, time_passed):
        stats['started'].inc()
        for _ in range(5):
            order_id = self.order_ids[random.randint(0, len(self.order_ids) - 1)]
            logging.debug(f"check order: {order_id}")
            response = yc_client.request(YaCourierRequest.GET_ORDERS, order_id, make_json=True, throw=True)
            new_status = params.get('new_status', 'confirmed')
            if response['status'] == new_status:
                continue
            response = yc_client.request(YaCourierRequest.PATCH_ORDERS, order_id, data={'status': new_status},
                                         make_json=False, throw=False)
            if response.status_code == requests.codes.ok:
                stats['succeeded'].inc()
            else:
                stats['failed'].inc()
            return
        stats['failed'].inc()


def parse_timing(val):
    val = ''.join(val.split())
    pattern = r'([0-9]+\.?[0-9]*)([smhd]?)-([0-9]+\.?[0-9]*)([smhd])$'
    match = re.match(pattern, val, re.IGNORECASE)
    if not match:
        raise Exception("Bad format of time interval '{}'. Expected: '{}'".format(val, pattern))
    suffixes = [match.group(4) if not match.group(2) else match.group(2), match.group(4)]
    convert = {
        's': 1,
        'm': 60,
        'h': 60 * 60,
        'd': 24 * 60 * 60
    }
    return sorted([float(match.group(1 + 2 * idx)) * convert[suffixes[idx]] for idx in range(2)])


def stats_printer(args):
    start_time = time.time()
    while time.time() - start_time < args.duration:
        time_passed = datetime.timedelta(seconds=int(time.time() - start_time))
        print('\n{}; time passed: {}'.format(datetime.datetime.now().strftime("%F %T"), time_passed))
        print("PID: {}".format(os.getpid()))
        print("Threads:")
        for name, stats in sorted(thread_stats.items()):
            print("  {} [{}]: started: {}, succeeded: {}, failed: {}".format(
                name,
                stats['count'].value(),
                stats['started'].value(),
                stats['succeeded'].value(),
                stats['failed'].value()))
        print("Requests:")
        for name, stats in sorted(request_stats.items()):
            print("  {}: started: {}, succeeded: {}, time: {:.0f}ms, failed: {}, retries: {}, fail codes: {}".format(
                name,
                stats['started'].value(),
                stats['succeeded'].value(),
                stats['response_time'].value() / stats['succeeded'].value() * 1000 if stats['succeeded'].value() else 0,
                stats['failed'].value(),
                stats['retries'].value(),
                stats['codes'].values()))
        time.sleep(10)
    stopevent.set()


def get_threads_push_position(svrps_count, num_threads):
    threads_push_position = min(svrps_count, num_threads) \
        if num_threads > 0 else svrps_count
    return threads_push_position


def main():
    global yc_client
    global async_client
    global sync_client

    random.seed()

    args = parse_args()
    if not args.oauth_token:
        raise Exception('OAuth token is missing')

    logging.basicConfig(filename=args.logfile, filemode='w', format='%(asctime)s - %(name)s - %(levelname)s - %(message)s')
    logger = logging.getLogger()
    logger.setLevel(level=logging.DEBUG if args.verbose else logging.INFO)
    log_handler = logging.StreamHandler(sys.stdout)
    log_handler.setLevel(level=logging.DEBUG)
    log_handler.setFormatter(logging.Formatter('%(asctime)s\t%(process)d\t%(threadName)s\t%(message)s'))
    logger.addHandler(log_handler)

    yc_client = YaCourierClient(args.environment, args.oauth_token)
    async_client = AsyncSolverClient(args.environment)
    sync_client = SyncSolverClient(args.environment)

    print('-'*60)
    waves = []
    for filename in args.waves:
        print('Loading waves file {}'.format(filename))
        with open(filename, "r") as f:
            ops = json.loads(f.read())
            for op in ops:
                for name in thread_names:
                    if name in op:
                        for param in ['warming_up', 'period']:
                            op[name][param+'_s'] = parse_timing(op[name][param])
                            del op[name][param]
                for _ in range(op['count']):
                    waves.append(op)
                    waves[-1]['__filename__'] = filename

    print('-'*60)
    print('Preparing couriers and depots data')

    depots = prepare_depots()
    couriers_count = sum([w['vehicle_count'] for w in waves])
    couriers = prepare_couriers(couriers_count)

    print('-'*60)
    print('Preparing delivery waves')

    first_route_index = 0
    first_courier_index = 0
    for i, wave in enumerate(waves):
        print('----- Preparing delivery wave {}'.format(i))
        with open(wave['template'], 'r') as f:
            wave['out.template_json'] = json.loads(f.read())
        prepare_vehicles_and_locations(wave)
        wave['out.mvrp'] = None
        wave['out.svrps'] = []
        wave['out.routes'] = []
        if 'mvrp' in wave:
            wave['out.mvrp'] = prepare_mvrp(wave)
        if 'svrp' in wave:
            wave['out.svrps'] = prepare_svrps(wave)

            if args.clean_orders_on_start:
                clean_orders(load_routes(args.date.isoformat()))

            wave_couriers = couriers[first_courier_index:first_courier_index + wave['vehicle_count']]
            wave['out.routes'] = prepare_routes(args, depots, wave_couriers, first_route_index, len(wave['out.svrps']))
            wave['out.orders'] = prepare_orders(args, wave['out.routes'], wave['out.template_json']['locations'])
            first_route_index += len(wave['out.svrps'])
            first_courier_index += wave['vehicle_count']

    print('-'*60)
    print('Configuring connection pools')

    svrps_count = sum([len(w['out.svrps']) for w in waves])

    # 1 - for stats_printer, <= 1 for async_mvrp_requester,
    # <= svrps_count for sync_svrp_requester or push_pos_sender
    thread_count = 1 + 1 + svrps_count
    logging.info('maximum threads per process: %d', thread_count)
    assert thread_count >= 1

    yc_client.mount('https://', requests.adapters.HTTPAdapter(pool_connections=thread_count, pool_maxsize=thread_count))
    async_client.mount('https://', requests.adapters.HTTPAdapter(pool_connections=thread_count,
                                                                 pool_maxsize=thread_count))
    sync_client.mount('https://', requests.adapters.HTTPAdapter(pool_connections=thread_count,
                                                                pool_maxsize=thread_count))

    print('-'*60)
    print('Starting delivery waves')

    svrp_tuples = []
    threads = []
    for wave in waves:
        if wave['out.mvrp'] is not None:
            thread = threading.Thread(target=async_mvrp_requester, args=[wave])
            thread.daemon = True
            thread.start()
            threads.append(thread)

        for courier_idx in range(len(wave['out.svrps'])):
            svrp_tuples.append((wave, courier_idx))

    threads.append(threading.Thread(target=stats_printer, args=[args]))
    threads[-1].start()

    print('-'*60)

    threads_push_position = get_threads_push_position(len(svrp_tuples), args.threads_push_position)

    if args.number_processes > 0:
        start_waves_f = functools.partial(start_waves, args=args,
                                          threads_push_position=threads_push_position // args.number_processes + 1)
        with Pool(args.number_processes) as pool:
            logging.info('splitting %d tasks into %d processes', len(svrp_tuples), args.number_processes)
            pool.map(start_waves_f, split_list(svrp_tuples, args.number_processes))
    else:
        svrp_threads = start_waves(svrp_tuples, args=args, threads_push_position=threads_push_position,
                                   join_threads=False)
        threads.extend(svrp_threads)

    for thread in threads:
        thread.join()

    print('-'*60)
    print('Threads finished')

    for i, wave in enumerate(waves):
        print('Cleaning wave {}'.format(i))


def start_waves(wave_tuples, args=None, threads_push_position=None, join_threads=True):
    print('Starting delivery waves in {}'.format(os.getpid()))
    threads = []
    orders = load_orders(args.date.strftime("%F"))
    for wave, courier_idx in wave_tuples:
        thread = threading.Thread(target=sync_svrp_requester, args=[wave, courier_idx])
        thread.start()
        threads.append(thread)
        target = BaseThreadLoop(wave, thread_stats, ChangeOrderStatusPayload(orders))
        thread2 = threading.Thread(target=target)
        thread2.start()
        threads.append(thread2)

    threads_push_position = min(len(wave_tuples), threads_push_position) if threads_push_position else len(wave_tuples)
    logging.info('set threads_push_position %d', threads_push_position)
    for part_tuples in split_list(wave_tuples, threads_push_position):
        # split different waves to different threads
        for wave, iter_tuples in itertools.groupby(part_tuples, key=lambda x: x[0]):
            courier_idxs = [t[1] for t in iter_tuples]
            logging.info('starting thread for wave: %s couriers: %s', wave['__filename__'], str(courier_idxs))
            target = BaseThreadLoop(wave, thread_stats, PushPositionPayload(wave, courier_idxs, args.date))
            thread = threading.Thread(target=target)
            thread.start()
            threads.append(thread)

    threads.append(threading.Thread(target=stats_printer, args=[args]))
    threads[-1].start()
    logging.info('started %d threads', len(threads))

    if not join_threads:
        return threads

    print('-'*60)
    for thread in threads:
        thread.join()
    print('-'*60)
    print('Threads finished in {}'.format(os.getpid()))


if __name__ == "__main__":
    main()
