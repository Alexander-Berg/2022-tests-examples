import uuid
from os import environ as env

import pytest
import requests

import maps.b2bgeo.test_lib.mock_pipedrive_gate as mock_pipedrive_gate
import maps.b2bgeo.test_lib.passport_uid_values as passport_uid_values
import maps.b2bgeo.test_lib.sender_values as sender_values
from maps.b2bgeo.libs.py_flask_utils.i18n import Keysets
from maps.b2bgeo.test_lib.mock_blackbox import NEOPHONISH_UID
from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote, is_auth_enabled, MOCK_APIKEYS_CONTEXT
from maps.b2bgeo.ya_courier.backend.test_lib.util import (
    api_path_with_company_id,
    cleanup_company,
    cleanup_user,
    create_route_env,
    create_tmp_company,
    create_user,
    entity_equal,
    env_get_request,
    env_patch_request,
    env_post_request,
    find_company,
    find_company_by_id,
    get_order,
    patch_order_by_order_id,
    set_partially_finished_status_enabled,
)
from ya_courier_backend.config.common import KNOWN_LOCALES, IGNORED_LOCALES
from ya_courier_backend.models import UserRole
from ya_courier_backend.resources.logistic_company import (
    FULL_SCENARIO_PARAMETERS,
    ServiceType,
    DEFAULT_STATUS_PRESETTLED_REASONS_KEYS
)
from ya_courier_backend.interservice.apikeys.yandex import APIKEYS_SERVICE_TOKEN

ENTITY = {
    'name': 'Flash Logistics',
}

NEW_ENTITY = {
    'name': 'Snail Logistics',
    'logo_url': 'https://avatars.mds.yandex.net/get-switch/41639/flash_1c27fd050b06e13818aad698f15735d8.gif/orig',
    'bg_color': '#bc0000',
    'sms_enabled': False,
}

WRONG_ENTITY_1 = {
    'logo_url': 'ftp://avatars.mds.yandex.net/get-switch/41639/flash_1c27fd050b06e13818aad698f15735d8.gif/orig',
}

WRONG_ENTITY_2 = {
    'bg_color': 'rred',
}

NEW_COMPANY = {
    'name': 'New Logistics Company',
    'logo_url': 'https://avatars.mds.yandex.net/get-switch/41639/flash_1c27fd050b06e13818aad698f15735d8.gif/orig',
    'bg_color': '#bc0000',
    'sms_enabled': False
}

NEW_COMPANY_BAD_VALUES = {
    "apikey": [["string"]],
    "bg_color": [False],
    "logo_url": [404],
    "mark_delivered_enabled": [1],
    "mark_delivered_radius": [None, 600, -5],
    "mark_delivered_service_time_coefficient": ['0', 0.0001, 1.2, -0.5],
    "name": [700],
    "optimal_order_sequence_enabled": [1],
    "services": [[
        {
            "enabled": True,
            "name": "service-1"
        },
        {
            "enabled": "disabled",
            "name": "service-2"
        }
    ]],
    "sms_enabled": [None],
    "sms_nearby_order_eta_s": [0.00001],
    "initial_login": [False],
    "messenger_enabled": [1],
    "route_violation_by_order_statuses": [1]
}

NEW_ADMIN_LOGIN = "company_new_admin"
NEW_ADMIN_UID = "company_new_admin_uid"
NEW_ADMIN_AUTH = "{}:{}".format(NEW_ADMIN_LOGIN, NEW_ADMIN_UID)

NEW_MANAGER_LOGIN = "company_new_manager"
NEW_MANAGER_UID = "company_new_manager_uid"
NEW_MANAGER_AUTH = "{}:{}".format(NEW_MANAGER_LOGIN, NEW_MANAGER_UID)

NEOPHONISH_AUTH = f":{NEOPHONISH_UID}"
EX_NEOPHONISH_AUTH = f"nephonish:{NEOPHONISH_UID}"


def _get_default_status_presettled_comments():
    Keysets.init(KNOWN_LOCALES, IGNORED_LOCALES)
    return [Keysets.get_status_presettled_comment(key, "ru_RU") for key in DEFAULT_STATUS_PRESETTLED_REASONS_KEYS]


DEFAULT_STATUS_PRESETTLED_REASONS = _get_default_status_presettled_comments()


def _check_get_company(system_env_with_db, auth, expected, expected_services):
    response = env_get_request(
        system_env_with_db,
        "companies",
        auth=auth
    )
    j = response.json()
    print(j)

    assert response.status_code == requests.codes.ok
    assert len(j) == 1
    assert entity_equal(j[0], expected)

    assert {x['name'] for x in j[0]['services'] if x['enabled']} == set(expected_services)


def _create_user_with_specified_role(system_env_with_db, user_role):
    user_login = user_role.value
    user_auth = f'{user_login}:{user_login}_uid'
    company_id = system_env_with_db.company_id
    create_user(system_env_with_db, user_login, company_id, user_role, auth=system_env_with_db.auth_header_super)
    return user_auth


@skip_if_remote
def test_get(system_env_with_db):
    _check_get_company(system_env_with_db, None, ENTITY, ["courier"])


def test_get_by_id(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db)
    )
    j = response.json()
    print(j)

    assert response.status_code == requests.codes.ok
    assert entity_equal(j, ENTITY)


def test_patch_without_service_change(system_env_with_db):
    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data=NEW_ENTITY
    )

    j = response.json()
    print(j)

    assert response.status_code == requests.codes.ok
    assert entity_equal(j, NEW_ENTITY)


@pytest.mark.parametrize("wrong_entity, error_message", [(WRONG_ENTITY_1, "logo_url should be a valid encoded URL"),
                                                         (WRONG_ENTITY_2, "bg_color should be a valid HTML color"),
                                                         ([NEW_ENTITY], "is not of type 'object'"),
                                                         ({'mark_delivered_radius': 600}, "Failed validating 'maximum' in schema['properties']['mark_delivered_radius']")])
def test_wrong_patch(system_env_with_db, wrong_entity, error_message):
    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data=wrong_entity
    )
    j = response.json()

    assert response.status_code == requests.codes.unprocessable
    assert error_message in j['message']


def test_patch_apikey(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db)
    )
    assert response.status_code == requests.codes.ok
    apikey = response.json()['apikey']

    try:
        new_apikey = str(uuid.uuid4())
        response = env_patch_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db),
            data={'apikey': new_apikey}
        )
        assert response.status_code == requests.codes.ok
        expected = NEW_ENTITY.copy()
        expected['apikey'] = new_apikey
        assert entity_equal(response.json(), expected)
    finally:
        assert env_patch_request(
            system_env_with_db,
            api_path_with_company_id(system_env_with_db),
            data={'apikey': apikey}
        ).status_code == requests.codes.ok


@pytest.mark.parametrize(
    "comments_type, default_comments",
    [
        ("status_presettled_comments", DEFAULT_STATUS_PRESETTLED_REASONS),
        ("chat_courier_presettled_comments", None),
        ("chat_logistician_presettled_comments", None)
    ]
)
def test_patch_presettled_comments(system_env_with_db, comments_type, default_comments):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db)
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert j[comments_type] == default_comments

    presettled_comments = ["four", "five", "123"]
    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data={comments_type: presettled_comments}
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert j[comments_type] == presettled_comments

    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db)
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert j[comments_type] == presettled_comments


@pytest.mark.parametrize(
    "comments_type",
    [
        "status_presettled_comments",
        "chat_courier_presettled_comments",
        "chat_logistician_presettled_comments",
    ]
)
def test_patch_presettled_comments_empty(system_env_with_db, comments_type):
    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data={comments_type: []}
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert j[comments_type] == []

    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db)
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert j[comments_type] == []


@pytest.mark.parametrize(
    "comments_type, default_comments",
    [
        ("status_presettled_comments", DEFAULT_STATUS_PRESETTLED_REASONS),
        ("chat_courier_presettled_comments", None),
        ("chat_logistician_presettled_comments", None)
    ]
)
def test_patch_presettled_comments_null(system_env_with_db, comments_type, default_comments):
    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data={comments_type: None}
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert j[comments_type] == default_comments

    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db)
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert j[comments_type] == default_comments


def test_cleanup(system_env_with_db):
    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data=ENTITY
    )
    j = response.json()
    print(j)

    assert response.status_code == requests.codes.ok
    assert entity_equal(j, ENTITY)


def test_create_company_schema(system_env_with_db):
    def try_create_company(data):
        response = env_post_request(system_env_with_db, "companies", data=data,
                                    auth=system_env_with_db.auth_header_super)
        assert response.status_code == requests.codes.unprocessable
        return response.json()['message']

    data_valid = NEW_COMPANY.copy()
    data_valid['services'] = [
        {
            'name': 'courier',
            'enabled': True
        },
        {
            'name': 'mvrp',
            'enabled': False
        }
    ]
    data_valid["initial_login"] = "initial_login"

    for field, bad_values in NEW_COMPANY_BAD_VALUES.items():
        for bad_value in bad_values:
            data = data_valid.copy()
            data[field] = bad_value
            message = try_create_company(data)
            assert 'schema validation failed' in message
            assert field in message
            if not isinstance(bad_value, list):
                assert str(bad_value) in message


@skip_if_remote
def test_create_company_by_superuser(system_env_with_db):
    data = NEW_COMPANY.copy()
    data['initial_login'] = NEW_ADMIN_LOGIN

    # Should fail to create company if 'services' is missing in payload
    response = env_post_request(
        system_env_with_db,
        "companies",
        data=data,
        auth=system_env_with_db.auth_header_super
    )
    assert response.status_code == requests.codes.unprocessable
    assert "'services' is a required property" in response.json()['message']

    # Create company
    data['services'] = [
        {
            "name": "courier",
            "enabled": True
        },
        {
            "name": "mvrp",
            "enabled": False
        }
    ]
    response = env_post_request(
        system_env_with_db,
        "companies",
        data=data,
        auth=system_env_with_db.auth_header_super
    )
    assert response.status_code == requests.codes.ok
    company = response.json()
    company_id = company['id']
    assert company["mark_delivered_enabled"] is True
    assert company["mark_delivered_radius"] == 500
    assert company["mark_delivered_service_time_coefficient"] == 0.5
    assert company["sms_nearby_order_eta_s"] == 1800
    assert company["optimal_order_sequence_enabled"] is True
    assert company["apikey"] is None
    assert company["messenger_enabled"] is True
    assert company["route_violation_by_order_statuses"] is True

    # Get it with the new user
    _check_get_company(system_env_with_db, NEW_ADMIN_AUTH, NEW_COMPANY, ["courier"])

    # Create duplicate company
    new_data = data.copy()
    new_data["initial_login"] = "another_admin_login"
    response = env_post_request(
        system_env_with_db,
        "companies",
        data=new_data,
        auth=system_env_with_db.auth_header_super
    )
    assert response.ok
    assert response.json()["id"] != company_id

    # Check the new user
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, company_id=company_id, path='users'),
        auth=NEW_ADMIN_AUTH
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert len(j) == 1
    assert j[0]['login'] == NEW_ADMIN_LOGIN
    assert j[0]['role'] == 'admin'

    # Changing services by manager should fail
    data = {'services': [{"name": "mvrp", "enabled": True}]}
    create_user(system_env_with_db, NEW_MANAGER_LOGIN, company_id, UserRole.manager, auth=system_env_with_db.auth_header_super)
    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, company_id=company_id),
        data=data,
        auth=NEW_MANAGER_AUTH
    )
    cleanup_user(system_env_with_db, NEW_MANAGER_LOGIN, company_id, auth=system_env_with_db.auth_header_super)
    assert response.status_code == requests.codes.forbidden
    assert "Your role doesn't allow to make this request." in response.json()['message']

    # Changing services by admin should work
    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, company_id=company_id),
        data=data,
        auth=NEW_ADMIN_AUTH
    )
    assert response.status_code == requests.codes.ok

    _check_get_company(system_env_with_db, NEW_ADMIN_AUTH, NEW_COMPANY, ["courier", "mvrp"])

    data = {'services': [{"name": "mvrp", "enabled": False}, {"name": "courier", "enabled": True}]}

    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, company_id=company_id),
        data=data,
        auth=system_env_with_db.auth_header_super
    )
    assert response.status_code == requests.codes.ok

    _check_get_company(system_env_with_db, NEW_ADMIN_AUTH, NEW_COMPANY, ["courier"])

    cleanup_company(system_env_with_db, company_id)


def _post_create_company(env, data, auth=None, expected_status_code=requests.codes.ok, expected_error_message=None, utm=None):
    utm_args = '?' + '&'.join([f'{k}={v}' for k, v in utm.items()]) if utm else ''
    response = env_post_request(
        env,
        'create-company' + utm_args,
        data=data,
        auth=env.auth_header_super if auth is None else auth
    )
    assert response.status_code == expected_status_code, response.text
    assert expected_error_message is None or expected_error_message in response.json()['message']
    return response.json()


@skip_if_remote
def test_create_company_by_passport_user(system_env_with_db):
    data = NEW_COMPANY.copy()
    services = [
        {
            'name': 'courier',
            'enabled': True
        },
        {
            'name': 'mvrp',
            'enabled': False
        }
    ]

    # Should fail to create company if the user is already registered in the database (even if he is superuser)
    data['services'] = services
    _post_create_company(system_env_with_db, data, expected_status_code=requests.codes.unprocessable,
                         expected_error_message="'{}' is already registered".format(
                             system_env_with_db.auth_header_super.split(':')[0]))

    # Should fail to create company if 'initial_login' is present in payload
    data['initial_login'] = NEW_ADMIN_LOGIN
    error_msg = "Additional properties are not allowed ('initial_login' was unexpected)"
    _post_create_company(system_env_with_db, data, auth=NEW_ADMIN_AUTH,
                         expected_status_code=requests.codes.unprocessable, expected_error_message=error_msg)
    del data['initial_login']

    # Should fail to create company if 'services' is missing in payload
    del data['services']
    error_msg = "'services' is a required property"
    _post_create_company(system_env_with_db, data, auth=NEW_ADMIN_AUTH,
                         expected_status_code=requests.codes.unprocessable, expected_error_message=error_msg)
    data['services'] = services

    # Create company
    j = _post_create_company(system_env_with_db, data, auth=NEW_ADMIN_AUTH)
    assert j['mark_delivered_enabled'] is True
    assert j['mark_delivered_radius'] == 500
    assert j['mark_delivered_service_time_coefficient'] == 0.5
    assert j['sms_nearby_order_eta_s'] == 1800
    assert j['optimal_order_sequence_enabled'] is True
    assert j['services'] == data['services']
    assert j['messenger_enabled'] is True
    assert j['route_violation_by_order_statuses'] is True
    company_id = j['id']

    # Get company info by the new user
    _check_get_company(system_env_with_db, NEW_ADMIN_AUTH, NEW_COMPANY, ['courier'])

    # Check the new user
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, company_id=company_id, path='users'),
        auth=NEW_ADMIN_AUTH
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert len(j) == 1
    assert j[0]['login'] == NEW_ADMIN_LOGIN
    assert j[0]['role'] == 'admin'

    # Should fail to create company if the user is already registered in the database
    data['name'] = NEW_COMPANY['name'] + '_xxx'
    error_msg = "'{}' is already registered".format(NEW_ADMIN_AUTH.split(':')[0])
    _post_create_company(system_env_with_db, data, auth=NEW_ADMIN_AUTH,
                         expected_status_code=requests.codes.unprocessable, expected_error_message=error_msg)
    data['name'] = NEW_COMPANY['name']

    # Delete user
    cleanup_user(system_env_with_db, NEW_ADMIN_LOGIN, company_id, auth=system_env_with_db.auth_header_super)

    cleanup_company(system_env_with_db, company_id)

    # Able to create company again if the company with the same name was deleted
    company_id = _post_create_company(system_env_with_db, data, auth=NEW_ADMIN_AUTH)['id']

    cleanup_company(system_env_with_db, company_id)


def test_create_company_by_passport_user_with_oauth(system_env_with_db):
    data = NEW_COMPANY.copy()
    data['services'] = [
        {
            'name': 'courier',
            'enabled': True
        },
        {
            'name': 'mvrp',
            'enabled': False
        }
    ]

    # Cleanup (needed in case a previous run of the test has failed)
    _clean_up_test_company(system_env_with_db, data['name'])

    # Should fail to create company if the user is already registered in the database (even if he is superuser)
    _post_create_company(system_env_with_db, data, expected_status_code=requests.codes.unprocessable,
                         expected_error_message="' is already registered")

    # Create company
    if is_auth_enabled():
        auth_token = env.get('YA_COURIER_TEST_TOKEN_UNREGISTERED')
        assert auth_token is not None
        auth = f"Auth {auth_token}"
    else:
        auth = "unregistered_user:1234"
    company_id = _post_create_company(system_env_with_db, data, auth=auth)['id']

    # Get company info by the new user
    _check_get_company(system_env_with_db, auth, NEW_COMPANY, ['courier'])

    # Check the new user
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, company_id=company_id, path='users'),
        auth=auth
    )
    assert response.status_code == requests.codes.ok
    j = response.json()
    assert len(j) == 1
    assert j[0]['role'] == 'admin'

    cleanup_company(system_env_with_db, company_id)


@skip_if_remote
def test_create_company_by_neophonish(system_env_with_db):
    data = NEW_COMPANY.copy()
    data['name'] = 'Neophonish Logistic Company'
    services = [
        {
            'name': 'courier',
            'enabled': True
        },
        {
            'name': 'mvrp',
            'enabled': False
        }
    ]
    data['services'] = services
    _clean_up_test_company(system_env_with_db, data['name'])

    j = _post_create_company(system_env_with_db, data, auth=NEOPHONISH_AUTH)
    company_id = j['id']
    _check_get_company(system_env_with_db, NEOPHONISH_AUTH, data, ["courier"])

    _check_get_company(system_env_with_db, EX_NEOPHONISH_AUTH, data, ["courier"])

    cleanup_company(system_env_with_db, company_id)


def _clean_up_test_company(system_env_with_db, name=NEW_COMPANY['name']):
    # Cleanup (needed in case a previous run of the test has failed)
    company = find_company(system_env_with_db, name, auth=system_env_with_db.auth_header_super)
    if company:
        cleanup_company(system_env_with_db, company['id'])


def _get_mock_active_apikeys():
    return MOCK_APIKEYS_CONTEXT[APIKEYS_SERVICE_TOKEN]['active_apikeys']


SELF_REGISTRATION_MANAGER_INFO = {
    'manager_name': 'Dummy name',
    'manager_position': 'Dummy position',
    'manager_email': 'Dummy email',
    'manager_phone': 'Dummy phone',
    'vehicle_park_size': '1 - 100500',
    'dadata': {
        'value': 'Dummy sample of dadata',
        'data': {
            'inn': '1234567890',
        },
    },
}

NEW_ADMIN_VALID_APIKEY_AUTH = f"{NEW_ADMIN_LOGIN}:{passport_uid_values.VALID}"


@skip_if_remote
def test_create_company_full_scenario_good_cases(system_env_with_db):
    _clean_up_test_company(system_env_with_db)
    num_active_apikeys_before = len(_get_mock_active_apikeys())

    data = {**NEW_COMPANY, **SELF_REGISTRATION_MANAGER_INFO}
    utm = {
        'utm_source': 'value_utm_source',
        'utm_term': 'value_utm_term'
    }

    data.pop('manager_phone', None)
    # Should fail to create company if some of FULL_SCENARIO_PARAMETERS are present and some are not
    error_msg = f"Either all fields from {sorted(list(FULL_SCENARIO_PARAMETERS))} must be present or none of them"
    _post_create_company(system_env_with_db, data, auth=NEW_ADMIN_VALID_APIKEY_AUTH,
                         expected_status_code=requests.codes.unprocessable, expected_error_message=error_msg)

    # Add all required for full scenario fields to payload, create company and
    # check that all services are enabled while services field isn't specified in payload
    data["manager_phone"] = "Dummy phone"

    def check_created_company(payload, expected_num_active_apikeys):
        j = _post_create_company(system_env_with_db, payload, auth=NEW_ADMIN_VALID_APIKEY_AUTH, utm=utm)

        expected_services = [{'name': s.value, 'enabled': True} for s in ServiceType]
        assert j["services"] == expected_services
        mock_apikey_context = _get_mock_active_apikeys()
        assert j['apikey'] == mock_apikey_context[-1]
        assert len(mock_apikey_context) == expected_num_active_apikeys

        cleanup_company(system_env_with_db, j["id"])

    check_created_company(data, num_active_apikeys_before + 1)

    # Specify services in payload: "courier" is enabled and "mvrp" is disabled
    # Create company and check that all services are enabled while "mvrp" is disabled in payload
    # Provided apikey must be used
    payload_services = [
        {
            "name": "courier",
            "enabled": True
        },
        {
            "name": "mvrp",
            "enabled": False
        }
    ]
    data["services"] = payload_services
    data['apikey'] = 'cool-new-apikey'
    _get_mock_active_apikeys().append(data['apikey'])

    check_created_company(data, num_active_apikeys_before + 2)


@skip_if_remote
def test_create_company_full_scenario_bad_cases_apikey_service(system_env_with_db):
    _clean_up_test_company(system_env_with_db)
    num_active_apikeys_before = len(_get_mock_active_apikeys())

    data = {**NEW_COMPANY, **SELF_REGISTRATION_MANAGER_INFO}
    new_admin_error_apikey = f"{NEW_ADMIN_LOGIN}:{passport_uid_values.SIMULATE_INTERNAL_ERROR}"

    error_msg = "Server is unable to create new apikey due to an internal error"
    _post_create_company(system_env_with_db, data, auth=new_admin_error_apikey,
                         expected_status_code=requests.codes.internal_server_error, expected_error_message=error_msg)
    assert num_active_apikeys_before == len(_get_mock_active_apikeys())

    new_admin_error_apikey = f"{NEW_ADMIN_LOGIN}:{passport_uid_values.INVALID_FOR_APIKEYS}"

    error_msg = "Server is unable to create new apikey due to an internal error"
    _post_create_company(system_env_with_db, data, auth=new_admin_error_apikey,
                         expected_status_code=requests.codes.internal_server_error, expected_error_message=error_msg)
    assert num_active_apikeys_before == len(_get_mock_active_apikeys())


@skip_if_remote
def test_create_company_full_scenario_bad_cases_auth_service(system_env_with_db):
    _clean_up_test_company(system_env_with_db)

    data = {**NEW_COMPANY, **SELF_REGISTRATION_MANAGER_INFO}
    new_admin_error_apikey = f"{NEW_ADMIN_LOGIN}:{passport_uid_values.INVALID}"

    error_msg = "Invalid token"
    _post_create_company(system_env_with_db, data, auth=new_admin_error_apikey,
                         expected_status_code=requests.codes.unauthorized, expected_error_message=error_msg)


@skip_if_remote
def test_create_company_full_scenario_bad_cases_secondary_services(system_env_with_db):
    """
    In this scenario one or several services /create-company depends upon will return errors.
    """
    _clean_up_test_company(system_env_with_db)

    data = {**NEW_COMPANY, **SELF_REGISTRATION_MANAGER_INFO,
            **{'manager_phone': mock_pipedrive_gate.VALUE_TRIGGERING_INTERNAL_ERROR}}
    j = _post_create_company(system_env_with_db, data, auth=NEW_ADMIN_VALID_APIKEY_AUTH)
    cleanup_company(system_env_with_db, j["id"])

    data['manager_phone'] = 'Dummy phone'
    data['manager_email'] = sender_values.EMAIL_SIMULATE_INTERNAL_ERROR
    j = _post_create_company(system_env_with_db, data, auth=NEW_ADMIN_VALID_APIKEY_AUTH)
    cleanup_company(system_env_with_db, j["id"])


def test_partially_finished_status_enabled(system_env_with_db):
    with create_tmp_company(
        system_env_with_db,
        "Test company test_partially_finished_status_enabled"
    ) as company_id:

        assert find_company_by_id(system_env_with_db, company_id, auth=system_env_with_db.auth_header_super)['partially_finished_status_enabled'] is False

        with create_route_env(
            system_env_with_db,
            "test_partially_finished_status_enabled",
            company_id=company_id,
            auth=system_env_with_db.auth_header_super
        ) as route_env:

            order_id = route_env['orders'][0]['id']

            status_code, j = patch_order_by_order_id(system_env_with_db, order_id, {'status': 'partially_finished'}, company_id=company_id, auth=system_env_with_db.auth_header_super, strict=False)
            assert status_code == requests.codes.unprocessable
            assert "not allowed to set partially_finished status" in j['message']

            set_partially_finished_status_enabled(system_env_with_db, True, company_id)

            patch_order_by_order_id(system_env_with_db, order_id, {'status': 'partially_finished'}, company_id=company_id, auth=system_env_with_db.auth_header_super)

            updated_order = get_order(system_env_with_db, order_id, company_id, auth=system_env_with_db.auth_header_super)
            assert updated_order['status'] == 'partially_finished'


def test_patch_mark_delivered_service_time_coefficient(system_env_with_db):
    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data={"mark_delivered_service_time_coefficient": 0.0001}
    )
    assert response.status_code == requests.codes.unprocessable
    assert "0.0001 is less than the minimum of 0.001" in response.json()['message']
    assert "Failed validating 'minimum' in schema['properties']['mark_delivered_service_time_coefficient']" in response.json()['message']

    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data={"mark_delivered_service_time_coefficient": 1.1}
    )
    assert response.status_code == requests.codes.unprocessable
    assert "1.1 is greater than the maximum of 1" in response.json()['message']
    assert "Failed validating 'maximum' in schema['properties']['mark_delivered_service_time_coefficient']" in response.json()['message']

    response = env_patch_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db),
        data={"mark_delivered_service_time_coefficient": 0.5}
    )
    assert response.status_code == requests.codes.ok


def test_get_company_account_info(system_env_with_db):
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, 'account-info')
    )
    j = response.json()

    assert response.status_code == requests.codes.ok
    assert {'tariff', 'balance', 'ban_info', 'banned'} == j.keys()
    assert isinstance(j['tariff'], dict)
    assert {'id', 'name', 'description', 'limits',
            'activation_date', 'expiration_date', 'next_debit'} == j['tariff'].keys()
    assert j['tariff']['name']
    assert isinstance(j['tariff']['limits'], list)
    assert j['tariff']['limits'][0]['name']

    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, 'account-info'),
        params={'lang': 'en_EN'}
    )
    assert response.status_code == requests.codes.ok
    j_en = response.json()
    assert j_en['tariff']['id'] == j['tariff']['id']
    assert j_en['tariff']['name'] != j['tariff']['name']
    assert j_en['tariff']['description'] != j['tariff']['description']

    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, 'account-info'),
        params={'lang': 'xx'}
    )
    assert response.status_code == requests.codes.ok
    j_xx = response.json()
    assert j_xx['tariff']['id'] == j['tariff']['id']
    assert j_xx['tariff']['name'] == j_en['tariff']['name']
    assert j_xx['tariff']['description'] == j_en['tariff']['description']


@skip_if_remote
@pytest.mark.parametrize('user_role', [UserRole.admin, UserRole.manager, UserRole.dispatcher])
def test_account_info_ban_fields_access(system_env_with_db, user_role):
    user_auth = _create_user_with_specified_role(system_env_with_db, user_role)
    response = env_get_request(
        system_env_with_db,
        api_path_with_company_id(system_env_with_db, 'account-info'),
        auth=user_auth
    )
    j = response.json()
    assert 'tariff' in j.keys()
    assert 'banned' in j.keys()
    if user_role == UserRole.admin:
        assert 'ban_info' in j.keys()
        assert 'next_debit' in j['tariff'].keys()
    if user_role == UserRole.manager:
        assert 'ban_info' not in j.keys()
        assert 'next_debit' in j['tariff'].keys()
    if user_role == UserRole.dispatcher:
        assert 'ban_info' not in j.keys()
        assert 'next_debit' not in j['tariff'].keys()
