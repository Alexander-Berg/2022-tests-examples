import requests
import contextlib

from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    create_route_env,
    env_delete_request,
    env_get_request,
    env_patch_request,
    env_post_request,
    get_order,
    get_order_details,
)
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from ya_courier_backend.util.db_errors import CONSISTENCY_ERROR_MESSAGE

LOCATIONS = [
    {"lat": 55.733827, "lon": 37.588722},
    {"lat": 55.729299, "lon": 37.580116}
]

NON_EXISTING_COMPANY_ID = 99999999


@contextlib.contextmanager
def _create_company(env, name=None, initial_login="test_user"):
    company_id = None
    try:
        data = {
            'name': name,
            'logo_url': 'https://avatars.mds.yandex.net/get-switch/41639/flash_1c27fd050b06e13818aad698f15735d8.gif/orig',
            'bg_color': '#bc0000',
            'sms_enabled': False,
            'services': [
                {
                    "name": "courier",
                    "enabled": True
                },
                {
                    "name": "mvrp",
                    "enabled": False
                }
            ],
            'initial_login': initial_login
        }

        # Create company
        resp = env_post_request(
            env,
            "companies",
            data=data,
            auth=env.auth_header_super
        )
        resp.raise_for_status()
        company_id = resp.json()['id']
        yield company_id
    finally:
        if company_id:
            _delete_company(env, company_id)


def _delete_company(env, company_id):
    response = env_get_request(
        env,
        path=api_path_with_company_id(env, "users", company_id=company_id),
        auth=env.auth_header_super)
    assert response.status_code == requests.codes.ok
    users = response.json()
    for user in users:
        response = env_delete_request(
            env,
            api_path_with_company_id(env, "users", object_id=user["id"], company_id=company_id),
            auth=env.auth_header_super)
        assert response.status_code == requests.codes.ok

    response = env_delete_request(
        env,
        api_path_with_company_id(env, company_id=company_id),
        auth=env.auth_header_super
    )
    assert response.status_code == requests.codes.ok


def _get_order_by_number(env, order_number, company_id=None):
    response = env_get_request(
        env,
        api_path_with_company_id(env, "orders?number={}".format(order_number), company_id=company_id),
        auth=env.auth_header_super)
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert len(j) == 1
    return j[0]


@skip_if_remote
def test_order_sharing_creation(system_env_with_db):
    with create_route_env(
            system_env_with_db,
            "test_order_sharing_creation",
            route_date="2018.12.12",
            order_locations=LOCATIONS) as env:

        # get created order to use it as a template
        order = get_order(system_env_with_db, env["orders"][0]['id'])
        assert order["shared_with_company_id"] is None
        assert order["shared_with_company_ids"] == []
        assert order["company_id"] == system_env_with_db.company_id
        for field in ['id', 'confirmed_at', 'time_interval_secs', 'time_window', 'partner_id', 'shared_with_companies',
                      'delivered_at', 'notifications', 'company_id', 'history', 'status_log', 'order_status_comments']:
            order.pop(field, None)
        order['number'] += 'x'

        schema_validation_failed_msg = 'Json schema validation failed: OrderCreate:'
        schema_validation_batch_failed_msg = 'Json schema validation failed: OrdersBatch:'
        with _create_company(system_env_with_db, "test_order_sharing_creation_company", 'new_company_user') as new_company_id:
            for use_array, use_batch, set_shared_id, response_code, get_shared_id, response_substr in [
                    ([False, True], False, None, requests.codes.ok, None, None),
                    ([False, True], False, NON_EXISTING_COMPANY_ID, requests.codes.unprocessable_entity, None, None),
                    ([False, True], False, system_env_with_db.company_id, requests.codes.unprocessable_entity, None, None),
                    ([False, True], False, new_company_id, requests.codes.ok, new_company_id, None),
                    ([False, True], False, str(new_company_id), requests.codes.unprocessable_entity, None, schema_validation_failed_msg),
                    ([False, True], True, None, requests.codes.ok, None, None),
                    ([False, True], True, NON_EXISTING_COMPANY_ID, requests.codes.unprocessable_entity, None, None),
                    ([False, True], True, system_env_with_db.company_id, requests.codes.unprocessable_entity, None, None),
                    ([False, True], True, new_company_id, requests.codes.ok, new_company_id, None),
                    ([False, True], True, str(new_company_id), requests.codes.unprocessable_entity, None, schema_validation_batch_failed_msg)]:
                for pass_shared_id_as_array in use_array:
                    # create a new order
                    if pass_shared_id_as_array:
                        order['shared_with_company_ids'] = [] if set_shared_id is None else [set_shared_id]
                        order.pop('shared_with_company_id', None)
                    else:
                        order['shared_with_company_id'] = set_shared_id
                        order.pop('shared_with_company_ids', None)
                    response = env_post_request(
                        system_env_with_db,
                        path=api_path_with_company_id(system_env_with_db, "orders-batch" if use_batch else "orders"),
                        data=[order] if use_batch else order
                    )
                    assert response.status_code == response_code, response.text
                    # check that order creation worked as expected
                    if response_code != requests.codes.ok:
                        assert response_substr is None or response_substr in response.text
                    else:
                        order_tmp = _get_order_by_number(system_env_with_db, order['number'])
                        assert order_tmp['shared_with_company_id'] == get_shared_id
                        assert order_tmp['shared_with_company_ids'] == [] if get_shared_id is None else [get_shared_id]
                        assert order_tmp["shared_with_companies"] == []
                        assert env_delete_request(
                            system_env_with_db,
                            api_path_with_company_id(system_env_with_db, "orders/{}".format(order_tmp['id']))
                        ).status_code == requests.codes.ok


@skip_if_remote
def test_order_sharing_modification(system_env_with_db):
    with create_route_env(
            system_env_with_db,
            "test_order_sharing_modification",
            route_date="2018.12.12",
            order_locations=LOCATIONS) as env:

        # get created order
        order = get_order(system_env_with_db, env["orders"][0]['id'])
        assert order["shared_with_company_id"] is None
        assert order["shared_with_company_ids"] == []
        assert order["shared_with_companies"] == []
        assert order["company_id"] == system_env_with_db.company_id

        with _create_company(system_env_with_db, "test_order_sharing_modification_company", 'new_company_user') as new_company_id:
            for use_batch, set_shared_id, response_code, get_shared_id, response_substr in [
                    (False, None, requests.codes.ok, None, None),
                    (False, NON_EXISTING_COMPANY_ID, requests.codes.unprocessable_entity, None, None),
                    (False, order["company_id"], requests.codes.unprocessable_entity, None, None),
                    (False, new_company_id, requests.codes.ok, new_company_id, None),
                    (False, str(new_company_id), requests.codes.unprocessable_entity, None, 'Json schema validation failed: OrderModify:'),
                    (True, NON_EXISTING_COMPANY_ID, requests.codes.unprocessable_entity, None, None),
                    (True, order["company_id"], requests.codes.unprocessable_entity, None, None),
                    (True, new_company_id, requests.codes.ok, new_company_id, None),
                    (True, str(new_company_id), requests.codes.unprocessable_entity, None, 'Json schema validation failed: OrdersBatch:'),
                    (True, None, requests.codes.ok, None, None)]:
                for pass_shared_id_as_array in [False, True]:
                    # modify the order
                    if pass_shared_id_as_array:
                        data = {'shared_with_company_ids': [] if set_shared_id is None else [set_shared_id]}
                    else:
                        data = {'shared_with_company_id': set_shared_id}
                    if use_batch:
                        data['number'] = order['number']
                        response = env_post_request(
                            system_env_with_db,
                            path=api_path_with_company_id(system_env_with_db, "orders-batch"),
                            data=[data]
                        )
                    else:
                        response = env_patch_request(
                            system_env_with_db,
                            path=api_path_with_company_id(system_env_with_db, "orders/{}".format(order['id'])),
                            data=data
                        )
                    assert response.status_code == response_code

                    # check that order modification worked as expected
                    if response_code != requests.codes.ok:
                        assert response_substr is None or response_substr in response.text
                    else:
                        order_tmp = _get_order_by_number(system_env_with_db, order['number'])
                        assert order_tmp['shared_with_company_id'] == get_shared_id
                        assert order_tmp['shared_with_company_ids'] == [] if get_shared_id is None else [get_shared_id]


class OrderSharingParameters:
    def __init__(self, patch_data, expected_response_code, expected_shared_ids, response_substr):
        self.patch_data = patch_data
        self.expected_response_code = expected_response_code
        self.expected_shared_ids = expected_shared_ids
        self.response_substr = response_substr


@skip_if_remote
def test_order_sharing_with_multiple_companies(system_env_with_db):
    with create_route_env(
            system_env_with_db,
            "test_order_sharing_with_multiple_companies",
            route_date="2018.12.12",
            order_locations=LOCATIONS) as env:

        order = get_order(system_env_with_db, env["orders"][0]['id'])
        assert order["company_id"] == system_env_with_db.company_id

        with _create_company(
            system_env_with_db,
            "test_order_sharing_with_multiple_companies_company1",
            "company1_user"
        ) as company1_id:
            with _create_company(
                system_env_with_db,
                "test_order_sharing_with_multiple_companies_company2",
                "company2_user"
            ) as company2_id:

                order_sharing_parameters_list = [
                    OrderSharingParameters(
                        {
                            "shared_with_company_ids": [NON_EXISTING_COMPANY_ID]
                        },
                        requests.codes.unprocessable_entity,
                        None,
                        CONSISTENCY_ERROR_MESSAGE
                    ),
                    OrderSharingParameters(
                        {
                            "shared_with_company_id": company1_id,
                            "shared_with_company_ids": [company2_id]
                        },
                        requests.codes.ok,
                        [company2_id],
                        None
                    ),
                    OrderSharingParameters(
                        {
                            "shared_with_company_ids": [company1_id, company2_id]
                        },
                        requests.codes.ok,
                        [company1_id, company2_id],
                        None
                    ),
                    OrderSharingParameters(
                        {
                            "shared_with_company_ids": None
                        },
                        requests.codes.unprocessable_entity,
                        None,
                        "None is not of type \'array\'"
                    ),
                    OrderSharingParameters(
                        {
                            "shared_with_company_ids": [order["company_id"]]
                        },
                        requests.codes.unprocessable_entity,
                        None,
                        CONSISTENCY_ERROR_MESSAGE
                    ),
                    OrderSharingParameters(
                        {
                            "shared_with_company_ids": [company1_id, order["company_id"]]
                        },
                        requests.codes.unprocessable_entity,
                        None,
                        CONSISTENCY_ERROR_MESSAGE
                    ),
                    OrderSharingParameters(
                        {
                            "shared_with_company_ids": [company1_id]
                        },
                        requests.codes.ok,
                        [company1_id],
                        None
                    ),
                    OrderSharingParameters(
                        {
                            "shared_with_company_ids": []
                        },
                        requests.codes.ok,
                        [],
                        None
                    )
                ]

                # modify order's 'shared_with_company_ids', check response codes, return to initial state
                for use_batch in [False, True]:
                    for params in order_sharing_parameters_list:
                        if use_batch:
                            data = params.patch_data.copy()
                            data['number'] = order['number']
                            response = env_post_request(
                                system_env_with_db,
                                path=api_path_with_company_id(system_env_with_db, "orders-batch"),
                                data=[data]
                            )
                        else:
                            response = env_patch_request(
                                system_env_with_db,
                                path=api_path_with_company_id(system_env_with_db, "orders/{}".format(order['id'])),
                                data=params.patch_data
                            )
                        assert response.status_code == params.expected_response_code, response.text

                        if params.expected_response_code != requests.codes.ok:
                            assert params.expected_shared_ids is None
                            assert params.response_substr is not None
                            assert params.response_substr in response.text
                        else:
                            assert params.response_substr is None
                            assert len(set(params.expected_shared_ids)) == len(params.expected_shared_ids)

                            order_tmp = _get_order_by_number(system_env_with_db, order['number'])
                            if params.expected_shared_ids == []:
                                assert order_tmp['shared_with_company_id'] is None
                            else:
                                assert order_tmp['shared_with_company_id'] in params.expected_shared_ids
                            assert sorted(order_tmp['shared_with_company_ids']) == sorted(params.expected_shared_ids)

                            order_tmp = get_order_details(system_env_with_db, order['number'])
                            assert len(order_tmp['shared_with_companies']) == len(params.expected_shared_ids)
                            assert sorted([x['id'] for x in order_tmp['shared_with_companies']]) == sorted(params.expected_shared_ids)
                            assert len({x['name'] for x in order_tmp['shared_with_companies']}) == len(params.expected_shared_ids)
