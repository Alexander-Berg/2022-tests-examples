import requests
import ssl
import contextlib
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    env_get_request, env_post_request, env_patch_request, env_delete_request,
    get_unistat, api_path_with_company_id, cleanup_courier,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote


def test_test_message(system_env_with_db):
    response = env_get_request(system_env_with_db, path='test')
    assert response.status_code == requests.codes.ok
    assert response.json() == {'message': 'OK'}


def test_ping_message(system_env_with_db):
    response = env_get_request(system_env_with_db, path='ping')
    assert response.status_code == requests.codes.ok
    assert response.json() == {'message': 'OK'}


def test_security_header(system_env_with_db):
    response = env_get_request(system_env_with_db, path='ping')
    assert response.status_code == requests.codes.ok
    assert response.headers['X-Content-Type'] == 'nosniff'
    assert response.headers['Cache-Control'] == 'no-store'

    response = env_get_request(system_env_with_db, path='non-existent-path')
    assert response.status_code == requests.codes.not_found
    assert response.headers['X-Content-Type'] == 'nosniff'
    assert response.headers['Cache-Control'] == 'no-store'


@pytest.mark.parametrize('handler', ['couriers', 'order-details?date=2020-03-11'])
def test_cache_control_header_get_method(system_env_with_db, handler):
    path = api_path_with_company_id(system_env_with_db, handler)
    response = env_get_request(system_env_with_db, path=path)
    assert response.status_code == requests.codes.ok
    assert response.headers['Cache-Control'] == 'no-store'


def test_cache_control_header(system_env_with_db):
    path = api_path_with_company_id(system_env_with_db, 'couriers')
    courier_number = 'test_cache_control_header_courier'

    cleanup_courier(system_env_with_db, courier_number, fail_on_error=False)

    response = env_post_request(system_env_with_db, path=path, data={'number': courier_number})
    assert response.status_code == requests.codes.ok
    assert 'Cache-Control' not in response.headers
    courier_id = response.json()['id']

    response = env_patch_request(system_env_with_db, path=path + '/' + str(courier_id), data={'phone': '+71231231234'})
    assert response.status_code == requests.codes.ok
    assert 'Cache-Control' not in response.headers

    response = env_delete_request(system_env_with_db, path=path + '/' + str(courier_id))
    assert response.status_code == requests.codes.ok
    assert 'Cache-Control' not in response.headers


def test_unistat_message(system_env_with_db):
    response = env_get_request(system_env_with_db, path='ping')
    assert response.status_code == requests.codes.ok

    j = get_unistat(system_env_with_db)
    assert len(j) > 0
    signals = {x[0]: x[1] for x in j}
    assert 'requests-pingresource_summ' in signals
    assert 'requests-GET-pingresource_summ' in signals
    assert 'statuses-pingresource-200_summ' in signals
    assert 'total_statuses-200_summ' in signals
    assert 'times-pingresource_dhhh' in signals
    assert 'times-GET-pingresource_dhhh' in signals


@skip_if_remote
def test_total_statuses_4xx(system_env_with_db):
    j = get_unistat(system_env_with_db)
    assert len(j) > 0
    signals = {x[0]: x[1] for x in j}

    # non-existent should not affect total_statuses signals
    statuses_4xx = signals.get('total_statuses-4xx_summ', 0)
    env_get_request(system_env_with_db, path='non-existent-path')
    j = get_unistat(system_env_with_db)
    signals = {x[0]: x[1] for x in j}
    assert statuses_4xx == signals.get('total_statuses-4xx_summ', 0)


# TODO: Fix this test
# Right now we can't stop database in tests
# But even if we can /unistat handler is making db queries too, we need to make it under try block or stop making
# queries there because now if database is down we can't send metrics and will not see errors in monitoring
'''
@skip_if_remote
def test_unistat_fail(system_env_without_db_and_process):
    env = system_env_without_db_and_process
    j = get_unistat(env)
    signals = {x[0]: x[1] for x in j}
    statuses_5xx = signals.get('total_statuses-5xx_summ', 0)

    response = requests.get(
        url=f"{env.url}/api/v1/companies/{env.company_id}/depots",
        headers=env.get_headers(),
        verify=env.verify_ssl
    )
    assert response.status_code >= 500

    j = get_unistat(env)
    signals = {x[0]: x[1] for x in j}
    assert statuses_5xx < signals.get('total_statuses-5xx_summ', 0)
'''


@contextlib.contextmanager
def _no_ssl_verification():
    old = ssl._create_default_https_context
    ssl._create_default_https_context = ssl._create_unverified_context
    yield
    ssl._create_default_https_context = old
