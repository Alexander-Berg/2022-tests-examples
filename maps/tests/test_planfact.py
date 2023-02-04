from typing import Callable

import pytest

from .conftest import AnalyticsBackend, auth_headers
from maps.b2bgeo.ya_courier.analytics_backend.test_lib.data.plan_fact_test_data import PLAN_FACT_TEST_DATA


def _add_status(status: str, item: dict):
    return {**item, 'version_status': status}


@pytest.mark.parametrize('endpoint', ['nodes', 'routes'])
def test_planfact(
    app: AnalyticsBackend,
    schema_validators: dict[str, Callable[[dict], None]],
    endpoint: str
):
    date = str(PLAN_FACT_TEST_DATA[endpoint][0]['db']['route_date'])

    root_path = f'/companies/1337/plan-fact/{endpoint}?start_date={date}&end_date={date}'
    response = app.request(root_path, headers=auth_headers('super'))
    assert response.status_code == 200, response.text

    response_json = response.json()
    schema_validators[endpoint](response_json)
    assert len(response_json['data']) > 0
    assert response_json['data'] == PLAN_FACT_TEST_DATA[endpoint][0]['response']['data']


@pytest.mark.parametrize('endpoint', ['nodes', 'routes'])
def test_planfact_versioned(app: AnalyticsBackend, schema_validators: dict[str, Callable[[dict], None]], endpoint: str):
    root_path = f'/companies/1337/plan-fact/versioned/{endpoint}?prev_version=0'
    response = app.request(root_path, headers=auth_headers('super'))
    assert response.status_code == 200, response.text

    response_json = response.json()
    schema_validators[f'{endpoint}_versioned'](response_json)
    assert len(response_json['data']) > 0
    assert response_json['version'] == PLAN_FACT_TEST_DATA[endpoint][0]['response']['version']
    assert response_json['data'] == list(
        map(lambda x: _add_status('added', x), PLAN_FACT_TEST_DATA[endpoint][0]['response']['data'])
    )


def test_planfact_versioned_deleted(app: AnalyticsBackend, schema_validators: dict[str, Callable[[dict], None]]):
    root_path = '/companies/1337/plan-fact/versioned/nodes?prev_version=1637264167568'
    response = app.request(root_path, headers=auth_headers('super'))
    assert response.status_code == 200, response.text

    response_json = response.json()
    schema_validators['nodes_versioned'](response_json)
    assert len(response_json['data']) > 0
    assert response_json['data'] == list(
        map(lambda x: _add_status('deleted', x), PLAN_FACT_TEST_DATA['nodes'][1]['response']['data'])
    )


def test_planfact_versioned_modified(app: AnalyticsBackend, schema_validators: dict[str, Callable[[dict], None]]):
    root_path = '/companies/1337/plan-fact/versioned/routes?prev_version=1638739181813'
    response = app.request(root_path, headers=auth_headers('super'))
    assert response.status_code == 200, response.text

    response_json = response.json()
    schema_validators['routes_versioned'](response_json)
    assert len(response_json['data']) > 0
    assert response_json['data'] == list(
        map(lambda x: _add_status('modified', x), PLAN_FACT_TEST_DATA['routes'][0]['response']['data'])
    )


@pytest.mark.parametrize('endpoint', ['nodes', 'routes'])
def test_invalid_dates(app: AnalyticsBackend, endpoint: str):
    root_path = f'/companies/1/plan-fact/{endpoint}'
    params = {'headers': auth_headers('1-admin')}

    assert app.request(f'{root_path}', **params).status_code == 400
    assert app.request(f'{root_path}?start_date=2021-01-01', **params).status_code == 400
    assert app.request(f'{root_path}?end_date=2021-01-01', **params).status_code == 400
    assert app.request(f'{root_path}?start_date=2021-01-01&end_date=2021-02-01', **params).status_code == 200
    assert app.request(f'{root_path}?start_date=2021-02-01&end_date=2021-01-01', **params).status_code == 422
    assert app.request(f'{root_path}?start_date=2021-01-01&end_date=2021-04-02', **params).status_code == 422


@pytest.mark.parametrize('endpoint', ['nodes', 'routes'])
def test_invalid_versions(app: AnalyticsBackend, endpoint: str):
    root_path = f'/companies/1/plan-fact/versioned/{endpoint}'
    params = {'headers': auth_headers('1-admin')}

    assert app.request(f'{root_path}?prev_version=0', **params).status_code == 200
    assert app.request(f'{root_path}?prev_version=10000', **params).status_code == 200
    assert app.request(f'{root_path}?prev_version=235235.5', **params).status_code == 400
    assert app.request(f'{root_path}?prev_version=abzh', **params).status_code == 400
    assert app.request(f'{root_path}?prev_version="2021-12-03"', **params).status_code == 400
    assert app.request(f'{root_path}?prev_version=2021-12-03', **params).status_code == 400
    assert app.request(f'{root_path}?prev_version=', **params).status_code == 400


@pytest.mark.parametrize('versioned', [True, False])
@pytest.mark.parametrize('endpoint', ['nodes', 'routes'])
def test_user_access(app: AnalyticsBackend, endpoint: str, versioned: bool):
    root = '/companies/6/plan-fact'

    if versioned:
        path = f'{root}/versioned/{endpoint}?prev_version=0'
    else:
        path = f'{root}/{endpoint}?start_date=2021-01-01&end_date=2021-02-01'

    assert app.request(path, headers=auth_headers('6-admin-retry')).status_code == 200
    assert app.request(path, headers=auth_headers('super')).status_code == 200
    assert app.request(path, headers=auth_headers('6-manager')).status_code == 403
    assert app.request(path, headers=auth_headers('1-admin')).status_code == 403


@pytest.mark.parametrize('versioned', [True, False])
@pytest.mark.parametrize('endpoint', ['nodes', 'routes'])
def test_pagination_limits(app: AnalyticsBackend, endpoint: str, versioned: bool):
    PER_PAGE_MIN = 10
    PER_PAGE_MAX = 1000

    root = '/companies/1/plan-fact'
    params = {'headers': auth_headers('1-admin')}

    if versioned:
        path = f'{root}/versioned/{endpoint}?prev_version=0'
    else:
        path = f'{root}/{endpoint}?start_date=2021-01-01&end_date=2021-02-01'

    assert app.request(f'{path}', **params).status_code == 200
    assert app.request(f'{path}&page=100', **params).status_code == 200
    assert app.request(f'{path}&per_page={PER_PAGE_MIN}', **params).status_code == 200
    assert app.request(f'{path}&page=100000&per_page={PER_PAGE_MAX}', **params).status_code == 200
    assert app.request(f'{path}&page=-1', **params).status_code == 400
    assert app.request(f'{path}&page=1&per_page={PER_PAGE_MAX + 1}', **params).status_code == 400
    assert app.request(f'{path}&per_page={PER_PAGE_MIN - 1}', **params).status_code == 400


def test_no_plan(app: AnalyticsBackend):
    path = '/companies/1/plan-fact/versioned/routes?prev_version=0'
    params = {'headers': auth_headers('1-admin')}

    response = app.request(path, **params).json()
    for route in response['data']:
        if route['plan'] is None:
            assert route['fact']['plan_violated'] is False
            break
    else:
        assert False, 'Route without plan not found'
