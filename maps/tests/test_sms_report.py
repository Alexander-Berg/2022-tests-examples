import requests

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    env_get_request,
)


def test_sms_report_missing_date(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(
            system_env_with_db,
            'sms-report'
        )
    )
    assert response.status_code == requests.codes.unprocessable
    assert "Missing data for required field" in response.json()['message']


def test_sms_report_invalid_date(system_env_with_db):
    for date in [None, '', 0, [], {}, 'abc', '24.06.2020']:
        response = env_get_request(
            system_env_with_db,
            api_path_with_company_id(
                system_env_with_db,
                f'sms-report?date={date}'
            )
        )
        assert response.status_code == requests.codes.unprocessable, date
        assert "Not a valid date " in response.json()['message']
