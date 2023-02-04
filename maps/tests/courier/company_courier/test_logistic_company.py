import pytest
from http import HTTPStatus

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    Environment,
    local_get,
    local_delete,
    local_patch,
    local_post,
    create_test_order_data,
)


@skip_if_remote
def test_delete_logistic_company_with_deleted_couriers(env: Environment):
    route_path = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}'
    local_delete(env.client, route_path, headers=env.user_auth_headers)

    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    local_delete(env.client, depot_path, headers=env.user_auth_headers)

    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_second_depot.id}'
    local_delete(env.client, depot_path, headers=env.user_auth_headers)

    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    couriers_path = f'/api/v1/companies/{env.default_company.id}/couriers'
    resp = local_get(env.client, couriers_path, headers=env.user_auth_headers)
    assert resp == []

    user_path = f'/api/v1/companies/{env.default_company.id}/users/{env.default_user.id}'
    local_delete(env.client, user_path, headers=env.superuser_auth_headers)

    company_path = f'/api/v1/companies/{env.default_company.id}'
    local_delete(env.client, company_path, headers=env.superuser_auth_headers, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_delete_logistic_company_with_deleted_depots(env: Environment):
    route_path = f'/api/v1/companies/{env.default_company.id}/routes/{env.default_route.id}'
    local_delete(env.client, route_path, headers=env.user_auth_headers)

    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_second_depot.id}'
    local_delete(env.client, depot_path, headers=env.user_auth_headers)

    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_delete(env.client, courier_path, headers=env.user_auth_headers)

    depots_path = f'/api/v1/companies/{env.default_company.id}/depots'
    resp = local_get(env.client, depots_path, headers=env.user_auth_headers)
    assert resp == []

    user_path = f'/api/v1/companies/{env.default_company.id}/users/{env.default_user.id}'
    local_delete(env.client, user_path, headers=env.superuser_auth_headers)

    company_path = f'/api/v1/companies/{env.default_company.id}'
    local_delete(env.client, company_path, headers=env.superuser_auth_headers, expected_status=HTTPStatus.OK)


@skip_if_remote
def test_delete_logistic_company_with_deleted_depots_and_couriers_but_routes(env: Environment):
    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_depot.id}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    depot_path = f'/api/v1/companies/{env.default_company.id}/depots/{env.default_second_depot.id}'
    local_delete(env.client, depot_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    courier_path = f'/api/v1/companies/{env.default_company.id}/couriers/{env.default_courier.id}'
    local_delete(env.client, courier_path, query={'shallow_delete': True}, headers=env.user_auth_headers)

    user_path = f'/api/v1/companies/{env.default_company.id}/users/{env.default_user.id}'
    local_delete(env.client, user_path, headers=env.superuser_auth_headers)

    company_path = f'/api/v1/companies/{env.default_company.id}'
    local_delete(env.client, company_path, headers=env.superuser_auth_headers,
                 expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
@pytest.mark.parametrize('lang', ('ru_RU', 'es_CL', 'tr_TR'))
def test_status_presettled_comments(env: Environment, lang):
    path = f'/api/v1/companies/{env.default_company.id}?lang=en_US'
    company_en = local_get(env.client, path, headers=env.user_auth_headers)

    path = f'/api/v1/companies/{env.default_company.id}?lang={lang}'
    company_local = local_get(env.client, path, headers=env.user_auth_headers)

    for comment_en, comment_local in zip(company_en['status_presettled_comments'],
                                         company_local['status_presettled_comments']):
        assert comment_en != comment_local


@skip_if_remote
def test_status_presettled_comments_fallback(env: Environment):
    path = f'/api/v1/companies/{env.default_company.id}?lang=ru_RU'
    company_ru = local_get(env.client, path, headers=env.user_auth_headers)

    path = f'/api/v1/companies/{env.default_company.id}?lang=uk_UA'
    company_uk = local_get(env.client, path, headers=env.user_auth_headers)

    assert company_ru['status_presettled_comments'] == company_uk['status_presettled_comments']


@skip_if_remote
def test_status_presettled_comments_company_locale(env: Environment):
    path = f'/api/v1/companies/{env.default_company.id}'
    company_ru = local_get(env.client, path, headers=env.user_auth_headers)

    local_patch(env.client, path, headers=env.user_auth_headers, data={'locale': 'en_US'})
    company_en = local_get(env.client, path, headers=env.user_auth_headers)

    assert company_ru['status_presettled_comments'] != company_en['status_presettled_comments']


@skip_if_remote
@pytest.mark.parametrize('param, valid, invalid', [
    ('shared_task_expiration_delay_h', 1, 0),
    ('shared_task_expiration_delay_h', 429240, 429241),
    ('shared_task_expiration_delay_h', 100, None),
    ('node_idle_type', 'service_duration', 'invalid'),
    ('node_idle_type', 'time_window_end', None),
    ('courier_can_patch_time_interval', True, None),
    ('courier_can_patch_time_interval', False, 'invalid'),
    ('push_call_reminder_s', 0, -1),
    ('push_call_reminder_s', 1000, None),
    ('courier_connection_loss_s', 60, 59),
    ('courier_connection_loss_s', 1000, None),
    ('idle_time_window_s', 900, 899),
    ('idle_time_window_s', 1800, None),
    ('enabled_push_notifications', ['assignment_courier', 'call_client'], ['Assignment_courier', 'call_client']),
    ('enabled_push_notifications', [], None),
    ('allowed_courier_position_sources', [], None),
    ('allowed_courier_position_sources', ['gps_tracker', 's2s_api'], ['APP']),
    ('enabled_logistician_notifications', ['courier_idle'], None),
    ('enabled_logistician_notifications', [], None),
    ('max_route_finish_prolongation_h', 0, -1),
    ('max_route_finish_prolongation_h', 168, 169),
    ('max_route_finish_prolongation_h', 5, None),
    ('locale', 'ru_RU', 'aa_BB'),
    ('locale', 'ru_RU', None),
])
def test_patch_logistic_company_params(env: Environment, param, valid, invalid):
    path = f'/api/v1/companies/{env.default_company.id}'

    auth = env.user_auth_headers
    company_before = local_get(env.client, path, headers=auth)

    local_patch(env.client, path, headers=auth, data={param: invalid}, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    local_patch(env.client, path, headers=auth, data={param: valid}, expected_status=HTTPStatus.OK)

    # Patch another field and check that this does not affect the result
    local_patch(env.client, path, headers=auth, data={'apikey': 'test_apikey'})

    assert {**company_before, 'apikey': 'test_apikey', param: valid} == local_get(env.client, path, headers=auth)


@skip_if_remote
def test_company_long_name_post(env: Environment):
    path = '/api/v1/companies'

    long_name = 'c' * 100
    data = {
        'name': long_name,
        'initial_login': 'ini_login',
        'services':
        [
            {
                'name': 'courier',
                'enabled': True
            }
        ]
    }

    invalid_post_response = local_post(env.client, path, headers=env.superuser_auth_headers,
                                       data=data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    assert "Failed validating 'maxLength' in schema['properties']['name']" in invalid_post_response['message']


@skip_if_remote
def test_company_long_name_patch(env: Environment):
    patch_path = f'/api/v1/companies/{env.default_company.id}'
    long_name_dict = {'name': 'c' * 100}

    invalid_patch_response = local_patch(env.client, patch_path, headers=env.superuser_auth_headers,
                                         data=long_name_dict, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)

    assert "Failed validating 'maxLength' in schema['properties']['name']" in invalid_patch_response['message']


@skip_if_remote
@pytest.mark.parametrize('param, valid, invalid', [
    ('router_category', 'main', 'another_main'),
    ('router_category', 'auto', 'another_auto'),
    ('router_category', 'global', 'another_global'),
    ('contract_sms_enabled', False, 'non-bool'),
    ('contract_sms_enabled', True, 5),
    ('support_chat_enabled', False, None),
    ('support_chat_enabled', True, 5),
])
def test_patch_company_internals(env: Environment, param, valid, invalid):
    path = f'/api/v1/internal/companies/{env.default_company.id}/settings'

    auth = env.superuser_auth_headers

    local_patch(env.client, path, headers=auth, data={param: invalid}, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    local_patch(env.client, path, headers=auth, data={param: valid}, expected_status=HTTPStatus.OK)

    auth = env.user_auth_headers
    path = f'/api/v1/companies/{env.default_company.id}'
    assert local_get(env.client, path, headers=auth)['settings'][param] == valid


@skip_if_remote
def test_access_patch_company_internals(env: Environment):
    path = f'/api/v1/internal/companies/{env.default_company.id}/settings'

    auth = env.user_auth_headers
    local_patch(env.client, path, headers=auth, data={}, expected_status=HTTPStatus.FORBIDDEN)


@skip_if_remote
@pytest.mark.parametrize('param, value', [
    ('router_category', 'main'),
    ('contract_sms_enabled', False),
    ('send_navi_deeplinks', True),
    ('support_chat_enabled', True),
])
def test_patch_company_internals_wrong_route(env: Environment, param, value):
    path = f'/api/v1/companies/{env.default_company.id}'
    auth = env.superuser_auth_headers

    param_prev_value = local_get(env.client, path, headers=auth)['settings'][param]
    local_patch(env.client, path, headers=auth, data={param: value})
    assert local_get(env.client, path, headers=auth)['settings'][param] == param_prev_value


@skip_if_remote
def test_internal_company_access_works_for_superusers_only(env: Environment):
    path = f'/api/v1/internal/companies/{env.default_company.id}/access'
    local_get(env.client, path, headers=env.user_auth_headers, expected_status=HTTPStatus.FORBIDDEN)
    res = local_get(env.client, path, headers=env.superuser_auth_headers)
    assert res == {
        'company_id': env.default_company.id,
        'granted_access_to': [],
        'granted_access_by': [],
    }


@skip_if_remote
def test_internal_company_returns_not_found_for_non_existent_company(env: Environment):
    path = '/api/v1/internal/companies/123123/access'
    local_get(env.client, path, headers=env.superuser_auth_headers, expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_internal_company_access_shows_companies_given_access_via_order_sharing(env: Environment):
    path_orders = f'/api/v1/companies/{env.default_company.id}/orders'
    data = {**create_test_order_data(env.default_route.id), 'shared_with_company_ids': [env.default_shared_company.id]}
    local_post(env.client, path_orders, headers=env.user_auth_headers, data=data)

    path = f'/api/v1/internal/companies/{env.default_company.id}/access'
    res = local_get(env.client, path, headers=env.superuser_auth_headers)
    assert res == {
        'company_id': env.default_company.id,
        'granted_access_to': [env.default_shared_company.id],
        'granted_access_by': [],
    }

    path = f'/api/v1/internal/companies/{env.default_shared_company.id}/access'
    res = local_get(env.client, path, headers=env.superuser_auth_headers)
    assert res == {
        'company_id': env.default_shared_company.id,
        'granted_access_to': [],
        'granted_access_by': [env.default_company.id],
    }
