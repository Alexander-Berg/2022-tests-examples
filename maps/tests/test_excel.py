from os import path
from typing import Callable

from yandex.maps.test_utils.common import wait_until

from .conftest import AnalyticsBackend, auth_headers


def _task_is_completed(resp):
    return resp.json()['status'] == 'completed'


def _get_unistat_value(unistat_response: dict[list], key: str) -> float:
    for k, v in unistat_response:
        if k == key:
            return v
    return 0.0


def test_normal_excel_report(app: AnalyticsBackend, schema_validators: dict[str, Callable[[dict], None]]):
    root_path = '/companies/1/plan-fact/excel-report'
    post_path = f'{root_path}?start_date=2021-01-01&end_date=2021-02-01'
    response = app.request(post_path, method='post', headers=auth_headers('1-admin'), read_timeout=2)
    assert response.status_code == 202, response.text
    schema_validators['excel_accepted'](response.json())
    assert response.json()['status'] == 'queued'
    assert response.json()['message'] == 'Task is queued'

    id = response.json()['id']
    assert wait_until(lambda: _task_is_completed(app.request(f'{root_path}/{id}', headers=auth_headers('1-admin'))))

    response = app.request(f'{root_path}/{id}', headers=auth_headers('1-admin'))
    schema_validators['excel_status'](response.json())
    assert path.isfile(response.json()['result'])

    response = app.request('/unistat')
    assert response.status_code == 200, response.text
    assert _get_unistat_value(response.json(), 'analytics_report_completed_summ') >= 1.


def test_invalid_dates_are_rejected_with_bad_request(app: AnalyticsBackend):
    root_path = '/companies/1/plan-fact/excel-report'
    basic_args = {'method': 'post', 'headers': auth_headers('1-admin')}
    assert app.request(f'{root_path}?start_date=2021-01-01', **basic_args).status_code == 400
    assert app.request(f'{root_path}?end_date=2021-01-01', **basic_args).status_code == 400
    assert app.request(f'{root_path}?start_date=2021-01-01&end_date=2021-01', **basic_args).status_code == 400
    assert app.request(f'{root_path}?start_date=2021-02-01&end_date=2021-01-01', **basic_args).status_code == 422
    assert app.request(f'{root_path}?start_date=2021-01-01&end_date=2021-04-01', **basic_args).status_code == 422


def test_invalid_depot_ids_are_rejected_with_bad_request(app: AnalyticsBackend):
    root_path = '/companies/1/plan-fact/excel-report?start_date=2021-01-01&end_date=2021-02-01'
    basic_args = {'method': 'post', 'headers': auth_headers('1-admin')}
    assert app.request(f'{root_path}&depot_ids=1,2', **basic_args).status_code == 202
    assert app.request(f'{root_path}&depot_ids=1,1', **basic_args).status_code == 400
    assert app.request(f'{root_path}&depot_ids=a', **basic_args).status_code == 400
    assert app.request(f'{root_path}&depot_ids=', **basic_args).status_code == 400
    assert app.request(f'{root_path}&depot_ids={",".join(map(str, range(101)))}', **basic_args).status_code == 422


def test_user_access(app: AnalyticsBackend):
    root_path = '/companies/6/plan-fact/excel-report'
    post_path = f'{root_path}?start_date=2021-01-01&end_date=2021-02-01'

    response = app.request(post_path, method='post', headers=auth_headers('6-admin-retry'))
    assert response.status_code == 202, response.text
    id = response.json()['id']

    get_path = f'{root_path}/{id}'
    assert app.request(get_path, headers=auth_headers('6-admin-retry')).status_code == 200
    assert app.request(get_path, headers=auth_headers('super')).status_code == 200
    assert app.request(get_path, headers=auth_headers('6-manager')).status_code == 403
    assert app.request(get_path, headers=auth_headers('1-admin')).status_code == 403

    assert app.request(post_path, method='post', headers=auth_headers('super')).status_code == 202
    assert app.request(post_path, method='post', headers=auth_headers('6-manager')).status_code == 403
    assert app.request(post_path, method='post', headers=auth_headers('1-admin')).status_code == 403
