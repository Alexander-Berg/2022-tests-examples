import json

from .conftest import API_KEY


def test_tasks(on_demand_app):
    route_base = f'/on-demand/companies/1/tasks{{}}?apikey={API_KEY}'

    locations = [
        {"type": "depots", "value": {"depots": [{"id": "1"}]}},
        {"type": "delivery", "value": {"position": {"lat": 55.735525, "lon": 37.642474}, "time": {"type": "asap"}}},
    ]

    tasks = {str(number): {"number": str(number), "locations": locations} for number in range(5)}
    payload = json.dumps({'tasks': list(tasks.values())})

    response = on_demand_app.request(route_base.format(''), method='post', data=payload)
    assert response.status_code == 200
    response_tasks = response.json()['tasks']
    assert len(response_tasks) == 5

    for response_task in response_tasks:
        tasks[response_task['number']]['id'] = response_task['id']

    for task in tasks.values():
        response = on_demand_app.request(route_base.format(f'/{task["id"]}'))
        assert response.status_code == 200
        assert response.json() == task


def test_non_existent_task(on_demand_app):
    route = f'/on-demand/companies/1/tasks/9001?apikey={API_KEY}'

    response = on_demand_app.request(route)
    assert response.status_code == 404
