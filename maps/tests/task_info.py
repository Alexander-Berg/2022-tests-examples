import requests
import pytz
import pytest
import urllib.parse
from datetime import datetime, timedelta
from jsonschema import validate
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import add_task_mvrp, API_KEY, LIMITED_API_KEY

TZ = pytz.timezone('Europe/Moscow')

SCHEMA = {
    "type": "array",
    "items": {
        "type": "object",
        "properties": {
            "task_id": {"type": "string"},
            "funnel_state": {"type": "string"},
            "status": {"type": "string"},
            "statuses": {"type": "array"},
            "task_type": {"type": "string"},
            "bucket": {"type": ["string", "null"]},
            "api_key": {"type": "string"},
            "message": {"type": "string"},

            "run_on_yt": {"type": "boolean"},
            "worker": {"type": "string"},
            "yt_operations": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "id": {"type": "string"},
                        "cluster": {"type": "string"},
                        "status": {"type": "string"}
                    }
                }
            },

            "matrix_statistics": {
                "type": "object",
                "properties": {
                    "requested_router": {"type": "string"},
                    "used_router": {"type": "string"},
                    "total_distances": {"type": "number"},
                    "geodesic_distances": {"type": "number"},
                    "slice_count": {"type": "number"},
                }
            },

            "solver_status": {"type": "string"},
            "locations": {"type": "number"},
            "vehicles": {"type": "number"},
            "threads": {"type": "number"},
            "tasks": {"type": "number"},
            "solver_time_limit_s": {"type": "number"},
            "depots": {
                "type": "array",
                "items": {
                    "type": "object",
                    "properties": {
                        "id": {
                            'oneOf': [
                                {
                                    'type': 'integer'
                                },
                                {
                                    'type': 'string'
                                }
                            ]
                        },
                        "ref": {"type": "string"},
                        "point": {
                            "type": "object",
                            "properties": {
                                "lat": {"type": "number"},
                                "lon": {"type": "number"}
                            }
                        }
                    }
                }
            },
            "used_vehicles": {"type": "number"},
            "dropped_locations_count": {"type": "number"},
            "quality": {"type": "string"},
            "date": {"type": "string"}
        },
        "additionalProperties": False
    }
}

TASK_WITH_DROPS = {
    "depot": {
        "point": {
            "lon": 37.729377,
            "lat": 55.799087
        },
        "time_window": "07:00-23:59"
    },
    "vehicles": [
        {
            "id": 1,
            "capacity": {
                "weight_kg": 5
            }
        }
    ],
    "options": {
        "minimize": "cost",
        "default_speed_km_h": 20,
        "time_zone": 3.0,
        "date": "2018-09-01",
        "quality": "low",
        "matrix_router": "geodesic"
    },
    "locations": [
        {
            "point": {
                "lon": 37.708392,
                "lat": 55.781803
            },
            "time_window": "12:00-18:00",
            "shipment_size": {
                "weight_kg": 4
            }
        },
        {
            "point": {
                "lon": 37.712129,
                "lat": 55.780426
            },
            "time_window": "2018-09-01T12:00:00+03/2018-09-01T18:00:00+03",
            "shipment_size": {
                "weight_kg": 4
            }
        }
    ]
}


def _iso_interval(start, end):
    return urllib.parse.quote('{}/{}'.format(start.isoformat(), end.isoformat()), safe='')


def _get(async_backend_url, query, token="TEST_AUTH"):
    headers = {}
    if token:
        headers["Authorization"] = "OAuth {}".format(token)

    res = requests.get(
        async_backend_url + query,
        headers=headers)

    return res


def _task_info_request(async_backend_url, time_interval_str):
    resp = _get(
        async_backend_url,
        '/stat/task_info?time_interval={}&status={}'.format(
            time_interval_str, 'completed'))
    assert resp.ok, resp.text
    assert isinstance(resp.json(), list)
    return resp.json()


@pytest.mark.parametrize("token", [None, "WRONG_TOKEN"])
def test_wrong_auth(async_backend_url_module_scope, token):
    now = datetime.now(TZ)
    resp = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}&status={}'.format(
            _iso_interval(now - timedelta(days=1), now), 'completed'),
        token=token)
    assert resp.status_code == 403, resp.text
    response = resp.json()
    assert "message" in response, response
    assert "AUTHORIZATION" in response["message"], response["message"]


@pytest.mark.parametrize("parameters", [(4.0, True), (4.01, False)])
def test_time_limits(async_backend_url_module_scope, parameters):
    now = datetime.now(TZ)
    past = now - timedelta(hours=parameters[0])
    resp = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}'.format(
            _iso_interval(past, now)))
    if parameters[1]:
        assert resp.ok, resp.text
        response = resp.json()
        assert isinstance(response, list)
    else:
        assert resp.status_code == 400, resp.text
        response = resp.json()
        assert "error" in response, response
        assert "message" in response["error"], response
        assert "is too long" in response["error"]["message"], response


def test_task_info_logic(async_backend_url_module_scope):
    start = datetime.now(TZ)
    add_task_mvrp(async_backend_url_module_scope, API_KEY)

    middle = datetime.now(TZ)
    add_task_mvrp(async_backend_url_module_scope, API_KEY)
    add_task_mvrp(async_backend_url_module_scope, LIMITED_API_KEY)
    add_task_mvrp(async_backend_url_module_scope, API_KEY, task_value=TASK_WITH_DROPS)

    end = datetime.now(TZ)

    # Ensure that with wide window and no other filters will get all the tasks
    stat = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}'.format(_iso_interval(start, end))).json()
    assert len(stat) == 4

    # Check the format
    validate(stat, SCHEMA)

    # Check that filter by time works
    stat = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}'.format(_iso_interval(start, middle))).json()
    assert len(stat) == 1

    # Check that filter by api_key works
    stat = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}&api_key={}'.format(_iso_interval(start, end), LIMITED_API_KEY)).json()
    assert len(stat) == 1

    # check that filter by funnel state works
    stat = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}&funnel_state={}'.format(_iso_interval(start, end), "solved_without_drops")).json()
    assert len(stat) == 3

    # check that filter by task status works
    long_task = TASK_WITH_DROPS.copy()
    long_task["options"]["solver_time_limit_s"] = 3600
    requests.post(
        async_backend_url_module_scope + "/add/mvrp?apikey={}".format(API_KEY),
        json=long_task)

    end = datetime.now(TZ)

    stat = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}'.format(_iso_interval(start, end))).json()
    assert len(stat) == 5

    stat = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}&status=completed'.format(_iso_interval(start, end))).json()
    assert len(stat) == 4


def test_task_info_meta_info(async_backend_url_module_scope):
    start = datetime.now(TZ)
    add_task_mvrp(async_backend_url_module_scope, API_KEY)
    middle = datetime.now(TZ)

    # Check meta info fields values

    stat = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}'.format(_iso_interval(start, middle))).json()
    assert len(stat) == 1

    expected_meta_info = {
        "depots": [
            {
                "id": -1,
                "ref": "Depot ref",
                "point": {
                    "lat": 55.799087,
                    "lon": 37.729377
                }
            }
        ],
        "used_vehicles": 1,
        "dropped_locations_count": 0,
        "date": "2018-09-01",
        "quality": "low"
    }

    for field in expected_meta_info:
        assert stat[0][field] == expected_meta_info[field]

    # Check meta info fields absence for not completed tasks
    long_task = TASK_WITH_DROPS.copy()
    long_task["options"]["solver_time_limit_s"] = 3600
    requests.post(
        async_backend_url_module_scope + "/add/mvrp?apikey={}".format(API_KEY),
        json=long_task)

    end = datetime.now(TZ)

    stat = _get(
        async_backend_url_module_scope,
        '/stat/task_info?time_interval={}'.format(_iso_interval(middle, end))).json()
    assert len(stat) == 1

    for field in expected_meta_info:
        assert field not in stat[0]
