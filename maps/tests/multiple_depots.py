import requests
import json
from maps.b2bgeo.mvrp_solver.backend.tests_lib.util import wait_task, API_KEY


_TASK = {
    'options': {
        'date': '2018-09-01',
        'time_zone': 3.0,
        'quality': 'low',
        'matrix_router': 'geodesic',
    },
    'depots': [
        {
            'id': 'depot_0',
            'point': {
                # Moscow, north-east
                'lat': 55.799004,
                'lon': 37.557313
            },
            'time_window': '12:00-23:59'
        },
        {
            'id': 'depot_1',
            'point': {
                # Moscow, north-west
                'lat': 55.799087,
                'lon': 37.729377
            },
            'time_window': '06:00-23:59'
        }
    ],
    'locations': [
        {
            'id': 'location_0',
            'required_tags': ['use_depot_0'],
            'point': {
                # Moscow, north-east, less than 1000 meters away from depot_0
                'lat': 55.798996,
                'lon': 37.545438
            },
            'time_window': '09:00-18:00'
        },
        {
            'id': 'location_1',
            'required_tags': ['use_depot_1'],
            'point': {
                # Moscow, north-west, less than 1000 meters away from depot_1
                'lat': 55.800054,
                'lon': 37.724734
            },
            'time_window': '09:00-18:00'
        }
    ],
    'vehicles': [
        {
            'id': 0,
            'depot_id': 'depot_0',
            'tags': ['use_depot_0'],
            'capacity': {
                'weight_kg': 5
            }
        },
        {
            'id': 1,
            'depot_id': 'depot_1',
            'tags': ['use_depot_1'],
            'capacity': {
                'weight_kg': 3
            }
        }
    ]
}


def test_multiple_depots(async_backend_url):
    response = requests.post(async_backend_url + '/add/mvrp?apikey={}'.format(API_KEY), json.dumps(_TASK))
    assert response.ok, response.text
    j = response.json()
    assert 'id' in j
    task_id = j['id']

    j = wait_task(async_backend_url, task_id)

    assert 'status' in j
    assert 'calculated' in j['status']
    assert j['message'] == 'Task successfully completed', j['message']

    assert 'matrix_statistics' in j
    matrix_statistics = j['matrix_statistics']
    assert len(matrix_statistics) == 1
    ms = matrix_statistics['driving']
    assert ms['total_distances'] == 16
    assert ms['requested_router'] == 'geodesic'
    assert ms['used_router'] == 'geodesic'
    assert ms['geodesic_distances'] == ms['total_distances']

    assert 'result' in j

    # Check that both vehicles are used in the routes
    assert len(j['result']['vehicles']) == 2
    assert len(j['result']['routes']) == 2
    assert {r['vehicle_id'] for r in j['result']['routes']} == {0, 1}

    for r in j['result']['routes']:

        # Check that the proper order is visited from the proper depot (location of an order
        # is less than 1000 meters away from the depot from which it should be delivered).
        # If this check fails then something might be wrong with the distance matrix.
        r['metrics']['total_transit_distance_m'] < 2000.0

        # Check that vehicle starts from the proper depot, delivers the proper order,
        # and returns to the same depot

        assert len(r['route']) == 3

        assert r['route'][0]['node']['type'] == 'depot'
        assert r['route'][1]['node']['type'] == 'location'
        assert r['route'][2]['node']['type'] == 'depot'

        id = r['vehicle_id']
        assert r['route'][0]['node']['value']['id'] == 'depot_' + str(id)
        assert r['route'][1]['node']['value']['id'] == 'location_' + str(id)
        assert r['route'][2]['node']['value']['id'] == 'depot_' + str(id)
