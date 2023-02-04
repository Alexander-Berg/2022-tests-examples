import requests
import json

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_local
from maps.b2bgeo.ya_courier.backend.test_lib.util import get_auth_header, api_path_with_company_id, env_get_request
from os import environ as env

USER = {
    'company_id': 6,
    'courier_id': 161,
    'auth_token': env.get('YA_COURIER_TEST_TOKEN')
}

WRONG_USER = {
    'company_id': 7,
    'courier_id': 6,
    'auth_token': 'xxxxxxxx'
}

MOBILE_USER = {
    'company_id': 6,
    'auth_token': env.get('YA_COURIER_TEST_MOBILE_TOKEN')
}


def get_couriers(env, token, courier_id, company_id):
    path = api_path_with_company_id(env,
                                    path="couriers",
                                    object_id=courier_id,
                                    company_id=company_id)
    return env_get_request(env,
                           path=path,
                           headers=get_auth_header(token))


def get_courier_routes(env, token, courier_id):
    return env_get_request(env,
                           path="couriers/{}/routes?date=2017-05-12".format(courier_id),
                           headers=get_auth_header(token))


@skip_if_local
def test_correct_token(system_env_with_db):
    response = env_get_request(system_env_with_db, path='test',
                               headers=get_auth_header(USER['auth_token']))
    response.raise_for_status()
    j = response.json()
    print(json.dumps(j))

    assert response.status_code == requests.codes.ok
    assert response.json() == {'message': 'OK'}


@skip_if_local
def test_incorrect_token(system_env_with_db):
    response = env_get_request(system_env_with_db,
                               path='test',
                               headers=get_auth_header(WRONG_USER['auth_token']))
    j = response.json()
    print(json.dumps(j))

    assert response.status_code == requests.codes.unauthorized
    assert j['error'] == 'InvalidToken'


@skip_if_local
def test_get_courier(system_env_with_db):
    response = get_couriers(system_env_with_db,
                            token=USER['auth_token'],
                            courier_id=USER['courier_id'],
                            company_id=USER['company_id'])
    j = response.json()
    print(json.dumps(j))

    assert response.status_code == requests.codes.ok


@skip_if_local
def test_get_wrong_courier(system_env_with_db):
    response = get_couriers(system_env_with_db,
                            token=USER['auth_token'],
                            courier_id=WRONG_USER['courier_id'],
                            company_id=USER['company_id'])
    j = response.json()
    print(json.dumps(j))

    assert response.status_code == requests.codes.unprocessable


@skip_if_local
def test_orders_by_courier(system_env_with_db):
    response = get_courier_routes(system_env_with_db,
                                  token=USER['auth_token'],
                                  courier_id=USER['courier_id'])
    j = response.json()
    print(json.dumps(j))

    assert response.status_code == requests.codes.ok


@skip_if_local
def test_orders_by_wrong_courier(system_env_with_db):
    response = get_courier_routes(system_env_with_db,
                                  token=USER['auth_token'],
                                  courier_id=WRONG_USER['courier_id'])
    j = response.json()
    print(json.dumps(j))

    assert response.status_code == requests.codes.forbidden


@skip_if_local
def test_mobile_auth(system_env_with_db):
    response = env_get_request(system_env_with_db,
                               path='test',
                               headers=get_auth_header(MOBILE_USER['auth_token']))
    j = response.json()
    print(json.dumps(j))

    assert response.status_code == requests.codes.ok
