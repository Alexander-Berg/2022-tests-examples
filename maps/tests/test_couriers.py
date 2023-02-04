import json

from .conftest import API_KEY


def test_one_courier(on_demand_app):
    route = f'/on-demand/companies/1/couriers/1?apikey={API_KEY}'

    response = on_demand_app.request(route, method='patch', data=json.dumps({'status': 'offline'}))
    assert response.status_code == 200

    response = on_demand_app.request(route)
    assert response.status_code == 200
    assert response.json() == {'status': 'offline'}

    response = on_demand_app.request(route, method='patch', data=json.dumps({'status': 'online'}))
    assert response.status_code == 200

    response = on_demand_app.request(route)
    assert response.status_code == 200
    assert response.json() == {'status': 'online'}


def test_multiple_couriers(on_demand_app):
    route_base = f'/on-demand/companies/1/couriers{{}}?apikey={API_KEY}'

    online_ids = set(range(1, 4, 2))
    offline_ids = set(range(2, 4, 2))

    payload = json.dumps(
        {'couriers': [{'id': str(id), 'status': 'online' if id in online_ids else 'offline'} for id in range(1, 4)]}
    )

    response = on_demand_app.request(route_base.format(''), method='post', data=payload)
    assert response.status_code == 200

    response = on_demand_app.request(route_base.format('/online'))
    assert response.status_code == 200

    response_online_ids = set(int(courier['id']) for courier in response.json()['couriers'])

    assert response_online_ids | online_ids == response_online_ids
    assert len(response_online_ids & offline_ids) == 0

    response = on_demand_app.request(route_base.format('/1'), method='patch', data=json.dumps({'status': 'offline'}))
    assert response.status_code == 200

    response = on_demand_app.request(route_base.format('/online'))
    assert response.status_code == 200
    assert len(response.json()['couriers']) == len(response_online_ids) - 1


def test_non_existent_couriers(on_demand_app):
    route_base = f'/on-demand/companies/1/couriers{{}}?apikey={API_KEY}'

    payload = json.dumps(
        {
            'couriers': [
                {'id': '2', 'status': 'online'},
                {'id': '5', 'status': 'online'},
                {'id': '10', 'status': 'online'},
            ]
        }
    )

    response = on_demand_app.request(route_base.format(''), method='post', data=payload)
    assert response.status_code == 404
    message = response.json()["error"]["message"]
    assert '5' in message and '10' in message and '2' not in message

    response = on_demand_app.request(route_base.format('/1338'))
    assert response.status_code == 404

    response = on_demand_app.request(route_base.format('/1339'), method='patch', data=json.dumps({'status': 'online'}))
    assert response.status_code == 404


def test_duplicate_couriers(on_demand_app):
    route_base = f'/on-demand/companies/1/couriers{{}}?apikey={API_KEY}'

    payload = json.dumps(
        {
            'couriers': [
                {'id': '1', 'status': 'online'},
                {'id': '1337', 'status': 'online'},
                {'id': '1', 'status': 'online'},
            ]
        }
    )

    response = on_demand_app.request(route_base.format(''), method='post', data=payload)
    assert response.status_code == 422
    assert 'Duplicating courier ids' in response.text
