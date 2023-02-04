import http.client
import requests
import json
import os
import re
import time

import maps.b2bgeo.test_lib.apikey_values as apikey_values
from maps.b2bgeo.ya_courier.backend.test_lib.util import source_path
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_post_request, env_get_request, request, patch_company
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


def _get_task():
    data_root = source_path("maps/b2bgeo/ya_courier/backend")
    with open(os.path.join(data_root, "bin/tests/data/example-mvrp.json"), "r") as f:
        return json.load(f)


def _run_task(system_env_with_db, task=None, headers=None, params=None):
    if task is None:
        task = _get_task()
    return env_post_request(
        system_env_with_db,
        "vrs/add/mvrp",
        data=task,
        headers=headers,
        params=params
    )


def _get_children(system_env_with_db, parent_task_id):
    return env_get_request(
        system_env_with_db,
        "vrs/children",
        params={'parent_task_id': parent_task_id}
    )


def _set_apikey(system_env_with_db, apikey=apikey_values.ACTIVE):
    patch_company(
        system_env_with_db,
        {'apikey': apikey}
    )


def _test_solve_task(system_env_with_db, **run_task_kwargs):
    response = _run_task(system_env_with_db, **run_task_kwargs)
    response.raise_for_status()

    j = response.json()
    print(j)

    task_id = j['id']
    assert task_id

    status_url = response.headers['Location']
    print("Status URL: " + status_url)
    assert task_id in status_url
    assert status_url.startswith('http')

    tries = 30
    i = 0
    done = False
    while i < tries and not done:
        i += 1

        response = request(
            method='GET',
            url=status_url,
            verify=False,
            headers=system_env_with_db.get_headers()
        )
        j = response.json()
        print(response.status_code)
        print(j)

        response.raise_for_status()
        if response.status_code == requests.codes.ok:
            done = True
            assert "result" in j
        time.sleep(1)
    assert done

    response = env_get_request(
        system_env_with_db,
        "vrs/result/mvrp/{}".format(task_id)
    )
    j = response.json()

    assert response.status_code == requests.codes.ok
    assert "result" in j

    return j["result"]


class TestMVRP(object):
    def test_invalid_task_id(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        response = env_get_request(
            system_env_with_db,
            "vrs/result/mvrp/2222222"
        )
        j = response.json()
        print(j)

        assert response.status_code == requests.codes.gone
        assert j["error"]["message"] == "No task with this ID (task_id) was found."

    def test_invalid_schema(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        task = _get_task()
        del task['depot']['point']

        response = _run_task(system_env_with_db, task)
        print(response.text)
        assert response.status_code == requests.codes.bad_request

    @skip_if_remote
    def test_empty_apikey(self, system_env_with_db):
        try:
            _set_apikey(system_env_with_db, '')
            response = _run_task(system_env_with_db)
            print(response.text)
            assert response.status_code == requests.codes.unprocessable
        finally:
            _set_apikey(system_env_with_db)

    @skip_if_remote
    def test_wrong_apikey(self, system_env_with_db):
        try:
            _set_apikey(system_env_with_db, 'WRONG_APIKEY')
            response = _run_task(system_env_with_db)
            print(response.text)
            assert response.status_code == requests.codes.forbidden
        finally:
            _set_apikey(system_env_with_db)

    @skip_if_remote
    def test_post_task_protocol(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        response = _run_task(system_env_with_db, headers={'X-Forwarded-Proto': 'https'})
        response.raise_for_status()

        j = response.json()

        task_id = j['id']
        assert task_id

        status_url = response.headers['Location']
        assert task_id in status_url
        assert status_url.startswith('https')

    def test_post_task(self, system_env_with_db):
        _set_apikey(system_env_with_db)
        _test_solve_task(system_env_with_db)

    def test_request_task(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        task = _get_task()

        response = _run_task(system_env_with_db, task)
        response.raise_for_status()
        task_id = response.json()['id']

        response = env_get_request(
            system_env_with_db,
            "vrs/request/mvrp/{}".format(task_id)
        )
        assert response.status_code == requests.codes.ok

        j = response.json()
        for name in ['vehicles', 'locations', 'depot', 'options']:
            assert name in task
            assert name in j

    def test_children(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        response = _run_task(system_env_with_db)
        response.raise_for_status()

        parent_task_id = response.json()['id']
        assert parent_task_id

        response = _run_task(system_env_with_db, params={'parent_task_id': parent_task_id})
        response.raise_for_status()

        child_task_id = response.json()['id']
        assert child_task_id

        response = _get_children(system_env_with_db, parent_task_id)
        response.raise_for_status()
        assert child_task_id in [item['task_id'] for item in response.json()]

    def test_children_without_parent_task_id(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        response = _get_children(system_env_with_db, None)
        assert response.status_code == http.client.UNPROCESSABLE_ENTITY

    def test_taxi_task(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        task = _get_task()
        task['vehicles'].clear()  # use no own vehicles to force adding taxi

        solution = _test_solve_task(system_env_with_db, task=task, params={'taxi_types': 'van'})

        # Taxi vehicles should have been added to cover all locations
        assert len(solution['dropped_locations']) == 0
        assert len(solution['vehicles']) > 0

        # Frontend expects "ref" field of taxi vehicles to be in certain format.
        taxi_ref_format = "yandex:taxi-(van|lcv_l|lcv_m|express):.*"
        assert all(re.fullmatch(taxi_ref_format, v['ref']) for v in solution['vehicles'])

    def test_taxi_invalid_task(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        response = _run_task(system_env_with_db, params={'taxi_types': 'invalid_taxi_type'})

        assert response.status_code == requests.codes.unprocessable

    def test_zones_preprocess_logic_via_tags(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        task = _get_task()
        task["vehicles"][0]["tags"] = ["inside_mkad"]
        task["vehicles"][0]["excluded_tags"] = ["inside_ttk"]  # specify excluded tag to see them in locations
        task["locations"][0]["point"] = {"lat": 55.705788, "lon": 37.660347}  # point just inside of TTK

        solution = _test_solve_task(system_env_with_db, task=task)
        [route] = solution["routes"]

        dropped_location_required_tags = [location["required_tags"] for location in solution["dropped_locations"]]
        assert dropped_location_required_tags == [["inside_ttk"]]

        assert route["vehicle_id"] == 1
        location_required_tags = [node["node"]["value"].get("required_tags") for node in route["route"]]
        # All locations are inside of mkad, depots do not have required_tags field
        assert location_required_tags == [None] + [["inside_mkad"]] * (len(location_required_tags) - 2) + [None]

    def test_zones_preprocess_logic(self, system_env_with_db):
        _set_apikey(system_env_with_db)

        task = _get_task()
        task["vehicles"] = task["vehicles"][:1]
        task["vehicles"][0]["allowed_zones"] = ["public_inside_mkad"]
        task["vehicles"][0]["forbidden_zones"] = ["public_inside_ttk"]  # specify forbidden zone to see them in locations
        task["locations"][0]["point"] = {"lat": 55.705788, "lon": 37.660347}  # point just inside of TTK

        solution = _test_solve_task(system_env_with_db, task=task)
        [route] = solution["routes"]

        dropped_location_zones = [location["zones"] for location in solution["dropped_locations"]]
        assert dropped_location_zones == [["public_inside_ttk"]]

        assert route["vehicle_id"] == 1
        location_zones = [node["node"]["value"].get("zones") for node in route["route"]]
        assert location_zones == [["public_inside_mkad"]] * len(location_zones)
