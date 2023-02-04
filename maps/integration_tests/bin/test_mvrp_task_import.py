import contextlib
import datetime
import dateutil.tz
import logging
import requests
import time
import unittest
from dateutil.parser import parse

from .common import (
    DEFAULT_COMPANY_ID,
    DEFAULT_COMPANY_ID2,
    BACKEND_API_ENDPOINT,
    BACKEND_AUTH_TOKEN,
    get_backend_entity_by_number,
    backend_request,
)

from maps.b2bgeo.ya_courier.backend.test_lib.util import mvrp_task_verify_orders
from maps.b2bgeo.libs.time.py.time_utils import format_str_time_relative


logging.basicConfig(level=logging.INFO)

TIMEZONE = dateutil.tz.gettz("Europe/Moscow")

COURIER_DATA_BASE = {
    "name": "test_mvrp_task_import_courier_name",
    "number": "test_mvrp_task_import_courier_number",
    "sms_enabled": False,
}

DEPOT_DATA_BASE = {
    'number': "test_mvrp_task_import_depot",
    'name': 'Warehouse 1',
    'address': 'South-West',
    'time_interval': '0.00:00-23:59',
    'lat': 66.0884498633,
    'lon': 76.7006633776,
}

IMPORT_ROUTES_TASK_OPTIONS = {"solver_time_limit_s": 0, "thread_count": 1}


def _get_base_task(courier_numbers):
    now = datetime.datetime.now(tz=TIMEZONE)
    route_date = now.date() + datetime.timedelta(days=1)
    return {
        "vehicles": [
            {
                "id": courier_numbers[0],
                "tags": ["sheep"],
                "excluded_tags": ["wolf", "cabbage"],
                "capacity": {"weight_kg": 1000},
                "imei": 865905023851110,
            },
            {
                "id": courier_numbers[1],
                "tags": ["wolf"],
                "excluded_tags": ["sheep"],
                "capacity": {"weight_kg": 1000},
                "imei": 865905023851111,
                "routing_mode": "transit",
            },
            {
                "id": courier_numbers[2],
                "tags": ["cabbage"],
                "excluded_tags": ["sheep"],
                "capacity": {"weight_kg": 1000},
            },
        ],
        "options": {"quality": "low", "date": route_date.isoformat(), "time_zone": 3},
        "depot": {
            "time_window": "07:00-23:59",
            "id": DEPOT_DATA_BASE['number'],
            "point": {"lat": 55.799087, "lon": 37.729377},
        },
    }


def _get_two_routes_task(courier_numbers, set_planned_route=False):
    result = _get_base_task(courier_numbers)
    ts = datetime.datetime.now(tz=TIMEZONE).timestamp()
    result["locations"] = [
        {
            "required_tags": ["sheep"],
            "time_window": "10:00-11:00",
            "point": {"lat": 55.806085, "lon": 37.511084},
            "id": f"loc-{ts}-1",
            "phone": "optional phone number",
        },
        {
            "required_tags": ["wolf"],
            "time_window": "14:00-19:00",
            "point": {"lat": 55.757108, "lon": 37.587761},
            "id": f"loc-{ts}-2",
            "comments": "optional comments",
        },
        {
            "required_tags": ["sheep"],
            "time_window": "12:00-13:00",
            "point": {"lat": 55.727600, "lon": 37.609246},
            "id": f"loc-{ts}-3",
            "shared_with_company_ids": [DEFAULT_COMPANY_ID2],
        },
    ]
    if set_planned_route:
        result["vehicles"][0]["planned_route"] = {"locations": [{"id": f"loc-{ts}-1"}, {"id": f"loc-{ts}-3"}]}
        result["vehicles"][1]["planned_route"] = {"locations": [{"id": f"loc-{ts}-2"}]}
    return result


def _get_multiple_locations_task(courier_numbers, location_count, set_planned_route=False):
    result = _get_base_task(courier_numbers)
    ts = datetime.datetime.now(tz=TIMEZONE).timestamp()
    result["locations"] = [
        {
            "required_tags": ["sheep"],
            "time_window": "00:00-23:59",
            "point": {"lat": 55.806085, "lon": 37.511084},
            "id": f"loc-{ts}-{i}",
            "phone": "optional phone number",
            "comments": "optional comments",
        }
        for i in range(location_count)
    ]
    if set_planned_route:
        result["vehicles"][0]["planned_route"] = {"locations": [{"id": f"loc-{ts}-{i}"} for i in range(location_count)]}
    return result


def _setup_courier(courier_data):
    courier = get_backend_entity_by_number('couriers', courier_data['number'])
    if not courier:
        courier = backend_request("post", f"companies/{DEFAULT_COMPANY_ID}/couriers", json=courier_data).json()
    logging.info(f"Courier setup: {courier}")
    return courier


def _get_courier_id(courier_number):
    couriers = backend_request("get", f"companies/{DEFAULT_COMPANY_ID}/couriers?number={courier_number}").json()
    if couriers:
        return couriers[0]['id']
    return None


def _delete_courier(courier_number):
    courier_id = _get_courier_id(courier_number)
    if courier_id:
        logging.info(f"Deleting courier courier_number={courier_number}, id={courier_id}")
        backend_request("delete", f"companies/{DEFAULT_COMPANY_ID}/couriers/{courier_id}")
    else:
        logging.info(f"Courier courier_number={courier_number} does not exist")


def _try_solve_mvrp_task_once(task_data, wait=True):
    result = backend_request("post", url="vrs/add/mvrp", json=task_data).json()
    task_id = result['id']
    logging.info(f"Waiting for task_id: {task_id}:: {result}")

    result = backend_request("get", url=f"vrs/result/mvrp/{task_id}")
    while wait and result.status_code != 200:
        time.sleep(1)
        result = backend_request("get", url=f"vrs/result/mvrp/{task_id}")

    solution = result.json()
    assert ('completed' if wait else 'queued') in solution['status'], solution['status']
    return task_id, solution


def _solve_mvrp_task(task_data, wait=True, num_retries=3):
    """
    Sometimes the solver may fail due to router deployment in testing.
    """
    exception = None
    for i in range(num_retries):
        try:
            return _try_solve_mvrp_task_once(task_data, wait)
        except (AssertionError, Exception) as ex:
            logging.warning(f"Failed to solve a task: {ex}")
            exception = ex
        time.sleep(1)
    raise exception


@contextlib.contextmanager
def _cleanup_created_routes():
    route_ids = []
    try:
        yield route_ids
    finally:
        logging.info(f"Deleting routes: {route_ids}")
        start = time.time()
        backend_request("delete", f"companies/{DEFAULT_COMPANY_ID}/routes", json=route_ids)
        diff = time.time() - start
        assert diff < 60, f"{diff} < 60"
        logging.info(f"Routes deleted: {route_ids}")


def _import_mvrp_task(task_id, expected_status_code=requests.codes.ok, route_ids=None):
    url = f"{BACKEND_API_ENDPOINT}/companies/{DEFAULT_COMPANY_ID}/mvrp_task?task_id={task_id}"
    headers = {"Authorization": f"OAuth {BACKEND_AUTH_TOKEN}"}
    logging.info(f"Importing {url}")
    response = requests.request("post", url=url, headers=headers)

    result = response.json()
    if route_ids is not None and response.ok:
        for route in result:
            route_ids.append(route['id'])

    assert response.status_code == expected_status_code, response.text
    return result


def _import_routes(task_data, route_ids):
    url = f"{BACKEND_API_ENDPOINT}/companies/{DEFAULT_COMPANY_ID}/import-routes"
    headers = {"Authorization": f"OAuth {BACKEND_AUTH_TOKEN}"}
    response = requests.post(url, headers=headers, json=task_data)

    assert response.status_code == requests.codes.ok, response.text

    j = response.json()
    task_id = j["task_id"]
    imported_routes = j["routes"]
    for route in imported_routes:
        route_ids.append(route['id'])

    logging.info(f"Routes were imported with task {task_id}")

    return task_id, imported_routes


def _verify_routes(created_routes, solution_result, depot, courier_numbers, order_count_per_route):
    assert isinstance(created_routes, list)

    assert len(courier_numbers) == len(solution_result["vehicles"])
    courier_id_to_number = {_get_courier_id(courier_number): courier_number for courier_number in courier_numbers}
    vehicles_info = {
        str(vehicle['id']): {
            'imei': vehicle.get('imei'),
            'routing_mode': vehicle.get('routing_mode') or solution_result['options']['routing_mode'],
        }
        for vehicle in solution_result['vehicles']
    }
    solution_routes = solution_result['routes']
    assert len(created_routes) == len(solution_routes)

    assert len(order_count_per_route) == len(created_routes)
    expected_date = solution_result['options'].get('date')
    for idx, route in enumerate(created_routes):
        assert route['date'] == expected_date
        expected_route_number = (
            f"{solution_routes[idx]['vehicle_id']}-{solution_routes[idx]['run_number']}-{expected_date}"
        )
        assert route['number'] == expected_route_number
        assert route['company_id'] == DEFAULT_COMPANY_ID
        assert route['courier_id'] in courier_id_to_number.keys()
        assert courier_id_to_number[route['courier_id']] == str(solution_routes[idx]["vehicle_id"])
        assert route['depot_id'] == depot['id']
        assert route['route_start'] == format_str_time_relative(solution_routes[idx]["route"][0]['arrival_time_s'])
        assert (
            route['imei'] == vehicles_info[str(solution_routes[idx]['vehicle_id'])]['imei']
        ), f"{route['imei']} == {vehicles_info[str(solution_routes[idx]['vehicle_id'])]['imei']}"
        assert (
            route['routing_mode'] == vehicles_info[str(solution_routes[idx]['vehicle_id'])]['routing_mode']
        ), f"{route['routing_mode']} == {vehicles_info[str(solution_routes[idx]['vehicle_id'])]['routing_mode']}"
        orders = backend_request("get", f"companies/{DEFAULT_COMPANY_ID}/orders?route_id={route['id']}").json()
        assert len(orders) == order_count_per_route[idx], f"{len(orders)} == {order_count_per_route[idx]}"
        # "-2" - vehicle starts and returns to depot
        assert len(solution_routes[idx]["route"]) - 2 == len(
            orders
        ), f"{len(solution_routes[idx]['route']) - 2} == {len(orders)}"
        mvrp_task_verify_orders(
            orders,
            solution_routes[idx]["route"][1:-1],
            DEFAULT_COMPANY_ID,
            route["id"],
            parse(expected_date),
            'Europe/Moscow',
            expected_order_status='new',
        )


def _check_import_routes_task_options(solution):
    for param in IMPORT_ROUTES_TASK_OPTIONS:
        assert solution["result"]["options"][param] == IMPORT_ROUTES_TASK_OPTIONS[param]

    driving_statistics = solution["matrix_statistics"]["driving"]
    assert driving_statistics["requested_router"] == "geodesic"
    assert driving_statistics["used_router"] == "geodesic"
    assert driving_statistics["slice_count"] == 1

    assert len(solution["result"]["metrics"]["_tasks_summary"]) == 1


class MvrpTaskImportTest(unittest.TestCase):
    def setUp(self):
        self.courier_numbers = []
        self.depot = None
        logging.info("Setting up couriers and depots")
        now = datetime.datetime.now(tz=TIMEZONE)
        for i in range(3):
            courier_number = f"test_car_{now.timestamp()}_{i}"
            self.courier_numbers.append(courier_number)
            if i == 0:
                _setup_courier({**COURIER_DATA_BASE, **{"number": courier_number}})

        self.depot = get_backend_entity_by_number('depots', DEPOT_DATA_BASE["number"])
        if not self.depot:
            self.depot = backend_request(
                "post", "companies/{}/depots".format(DEFAULT_COMPANY_ID), json=DEPOT_DATA_BASE
            ).json()

    def tearDown(self):
        logging.info("Removing created couriers")
        for courier_number in self.courier_numbers:
            _delete_courier(courier_number)

    def test_import_generic_task(self):
        task = _get_two_routes_task(self.courier_numbers)
        logging.info(f"Task data: {task}")
        task_id, solution = _solve_mvrp_task(task)
        assert len(solution['result']['routes']) == 2

        with _cleanup_created_routes() as route_ids:
            routes = _import_mvrp_task(task_id, route_ids=route_ids)

            _verify_routes(
                routes,
                solution['result'],
                self.depot,
                courier_numbers=self.courier_numbers,
                order_count_per_route=[2, 1],
            )

    def test_import_500_locations_task(self):
        location_count = 500
        task = _get_multiple_locations_task(self.courier_numbers, location_count)
        logging.info("Start solver")
        task_id, solution = _solve_mvrp_task(task)
        assert len(solution['result']['routes']) == 1

        with _cleanup_created_routes() as route_ids:
            start = time.time()
            routes = _import_mvrp_task(task_id, route_ids=route_ids)
            diff = time.time() - start
            assert diff < 60, f"{diff} < 60"

            _verify_routes(
                routes,
                solution['result'],
                self.depot,
                courier_numbers=self.courier_numbers,
                order_count_per_route=[location_count],
            )

    def test_import_non_existent_task(self):
        response = _import_mvrp_task(task_id='some-non-existent-task-id', expected_status_code=requests.codes.not_found)
        assert response['message'] == 'No task with this ID (task_id) was found.'

    def test_fetching_solution_while_not_finished_computing(self):
        task = _get_two_routes_task(self.courier_numbers)
        task['options']['quality'] = 'normal'
        task['options']['solver_time_limit_s'] = 10
        task_id, solution = _solve_mvrp_task(task, wait=False)

        with _cleanup_created_routes() as route_ids:
            response = _import_mvrp_task(
                task_id, expected_status_code=requests.codes.unprocessable, route_ids=route_ids
            )

            assert response['message'] == 'Cannot retrieve MVRP task result'

    def test_import_routes_generic_task(self):
        task = _get_two_routes_task(self.courier_numbers, set_planned_route=True)

        # Set 'solver_time_limit_s' to 60 seconds in order to check that it's overwritten
        task['options']['solver_time_limit_s'] = 60

        logging.info(f"/import-routes with task data: {task}")

        with _cleanup_created_routes() as route_ids:
            task_id, routes = _import_routes(task, route_ids)

            solution = backend_request("get", url=f"vrs/result/mvrp/{task_id}").json()
            _check_import_routes_task_options(solution)

            _verify_routes(
                routes,
                solution['result'],
                self.depot,
                courier_numbers=self.courier_numbers,
                order_count_per_route=[2, 1],
            )

    def test_import_routes_500_locations_task(self):
        location_count = 500
        task = _get_multiple_locations_task(self.courier_numbers, location_count, set_planned_route=True)

        logging.info("/import-routes with 500 locations")

        with _cleanup_created_routes() as route_ids:
            task_id, routes = _import_routes(task, route_ids)

            solution = backend_request("get", url=f"vrs/result/mvrp/{task_id}").json()
            _check_import_routes_task_options(solution)

            _verify_routes(
                routes,
                solution['result'],
                self.depot,
                courier_numbers=self.courier_numbers,
                order_count_per_route=[location_count],
            )
