from http import HTTPStatus
from ya_courier_backend.models.user_zone import UserZoneType
import pytest

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import (
    local_delete,
    local_get,
    local_patch,
    local_post,
    Environment,
)
from maps.b2bgeo.ya_courier.backend.test_lib.util_db import add_user

from ya_courier_backend.models import UserRole


MAXIMUM_USER_ZONE_REQUEST_SIZE = 100
TEST_USER_LOGIN = 'test_user_login'


def _create_zones(env, count=1, headers=None):
    headers = headers or env.user_auth_headers
    path_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/zones'
    zone_data = [
        {
            'number': f'zone {i}',
            'polygon': {'coordinates': [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]], 'type': 'Polygon'},
        }
        for i in range(count)
    ]
    local_post(env.client, path_zone, headers=headers, data=zone_data)
    zones = []
    page = 1
    while True:
        zones_on_page = local_get(env.client, f'{path_zone}?page={page}', headers=env.user_auth_headers)
        page += 1
        if len(zones_on_page):
            zones += zones_on_page
        else:
            return zones


def _format_edit_user_zone(zone_id):
    return {
        'zone_id': str(zone_id),
        'type': UserZoneType.EDIT.value,
    }


@skip_if_remote
@pytest.mark.parametrize(
    argnames='user_role, status_before, status_after',
    argvalues=[
        (UserRole.admin, HTTPStatus.OK, HTTPStatus.OK),
        (UserRole.manager, HTTPStatus.FORBIDDEN, HTTPStatus.OK),
        (UserRole.dispatcher, HTTPStatus.FORBIDDEN, HTTPStatus.FORBIDDEN),
        (UserRole.app, HTTPStatus.FORBIDDEN, HTTPStatus.FORBIDDEN),
    ],
)
def test_user_zone_for_different_roles(env: Environment, user_role, status_before, status_after):
    user_id, user_auth = add_user(env, TEST_USER_LOGIN, user_role)
    zones = _create_zones(env)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)]
    path_user_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/user-zones/{user_id}'
    zone_patch_data = {'number': 'new zone number', 'color_fill': '#414141',
                       'polygon': {'coordinates': [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]], 'type': 'Polygon'}}
    zone_patch_path = f'/api/v1/reference-book/companies/{env.default_company.id}/zones/{zone_id}'

    local_patch(env.client, zone_patch_path, data=zone_patch_data, headers=user_auth, expected_status=status_before)

    local_post(env.client, path_user_zone, data=user_zone_data, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)
    local_patch(env.client, zone_patch_path, data=zone_patch_data, headers=user_auth, expected_status=status_after)

    local_delete(env.client, path_user_zone, data=user_zone_data, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)
    local_patch(env.client, zone_patch_path, data=zone_patch_data, headers=user_auth, expected_status=status_before)


@skip_if_remote
def test_user_zone_get_request(env: Environment):
    user_id, _ = add_user(env, TEST_USER_LOGIN, UserRole.manager)
    zones = _create_zones(env)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)]
    path_user_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/user-zones/{user_id}'

    local_post(env.client, path_user_zone, data=user_zone_data, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)
    user_zone_permissions = local_get(env.client, path_user_zone, headers=env.user_auth_headers, expected_status=HTTPStatus.OK)
    assert user_zone_permissions == user_zone_data


@skip_if_remote
def test_user_zone_for_public_zone(env: Environment):
    user_id, _ = add_user(env, TEST_USER_LOGIN, UserRole.manager)
    path_zone = '/api/v1/reference-book/public/zones'
    path_user_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/user-zones/{user_id}'
    public_zones = local_get(env.client, path_zone, headers=env.user_auth_headers)
    assert public_zones
    public_zone_id = public_zones[0]['id']

    local_post(env.client, path_user_zone, data=[_format_edit_user_zone(public_zone_id)],
                headers=env.user_auth_headers, expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_user_zone_for_non_existent_zone(env: Environment):
    user_id, _ = add_user(env, TEST_USER_LOGIN, UserRole.manager)
    non_existent_zone = 41
    path_zone = f'/api/v1/reference-book/reference-book/companies/{env.default_company.id}/zones/{non_existent_zone}'
    path_user_zone = f'/api/v1/companies/{env.default_company.id}/user-zones/{user_id}'

    local_get(env.client, path_zone, headers=env.user_auth_headers, expected_status=HTTPStatus.NOT_FOUND)
    local_post(env.client, path_user_zone, data=[_format_edit_user_zone(non_existent_zone)],
               headers=env.user_auth_headers, expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_user_zone_repeatable_zones(env: Environment):
    user_id, _ = add_user(env, TEST_USER_LOGIN, UserRole.manager)
    zones = _create_zones(env)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)] * 2
    path_user_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/user-zones/{user_id}'

    local_post(env.client, path_user_zone, data=user_zone_data,
               headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_user_zone_too_many_zones(env: Environment):
    user_id, _ = add_user(env, TEST_USER_LOGIN, UserRole.manager)
    zones = _create_zones(env, MAXIMUM_USER_ZONE_REQUEST_SIZE + 1)
    user_zone_data = [_format_edit_user_zone(zone['id']) for zone in zones]
    path_user_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/user-zones/{user_id}'

    local_post(env.client, path_user_zone, data=user_zone_data,
               headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_user_zone_post_non_existent_user(env: Environment):
    non_existent_user_id = 41414141
    zones = _create_zones(env)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)]
    path_user_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/user-zones/{non_existent_user_id}'

    local_post(env.client, path_user_zone, data=user_zone_data,
               headers=env.user_auth_headers, expected_status=HTTPStatus.NOT_FOUND)


@skip_if_remote
def test_user_zone_pagination(env: Environment):
    user_id, _ = add_user(env, TEST_USER_LOGIN, UserRole.manager)
    per_page = 10
    zones = _create_zones(env, 2 * per_page - 1)
    user_zone_data = [_format_edit_user_zone(zone['id']) for zone in zones]
    path_user_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/user-zones/{user_id}'

    local_post(env.client, path_user_zone, data=user_zone_data,
               headers=env.user_auth_headers, expected_status=HTTPStatus.OK)
    first_page_zones = local_get(env.client, path_user_zone + f'?page=1&per_page={per_page}', headers=env.user_auth_headers)
    second_page_zones = local_get(env.client, path_user_zone + f'?page=2&per_page={per_page}', headers=env.user_auth_headers)

    response_zones = first_page_zones + second_page_zones
    request_ids = set([zone['id'] for zone in zones])
    response_ids = set([zone['zone_id'] for zone in response_zones])

    assert len(first_page_zones) == per_page
    assert len(second_page_zones) == per_page - 1
    assert request_ids == response_ids


@skip_if_remote
def test_user_zone_delete_non_existent_relation(env: Environment):
    user_id, _ = add_user(env, TEST_USER_LOGIN, UserRole.manager)
    zones = _create_zones(env)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)]
    path_user_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/user-zones/{user_id}'

    assert local_get(env.client, path_user_zone, headers=env.user_auth_headers, expected_status=HTTPStatus.OK) == []
    local_delete(env.client, path_user_zone, data=user_zone_data,
                 headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_user_zone_post_non_existent_permission_type(env: Environment):
    user_id, _ = add_user(env, TEST_USER_LOGIN, UserRole.manager)
    zones = _create_zones(env)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)]
    user_zone_data[0]['type'] = 'non existent type'
    path_user_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/user-zones/{user_id}'

    local_post(env.client, path_user_zone, data=user_zone_data,
               headers=env.user_auth_headers, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_user_edit_created_zones(env: Environment):
    user_id, headers = add_user(env, TEST_USER_LOGIN, UserRole.manager)
    zones = _create_zones(env, headers=headers)
    zone_id = str(zones[0]['id'])
    zone_patch_data = {
        'number': 'new zone number',
        'color_fill': '#414141',
        'polygon': {
            'coordinates': [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]],
            'type': 'Polygon'
        }
    }
    path_zone = f'/api/v1/reference-book/companies/{env.default_company.id}/zones/{zone_id}'

    local_patch(env.client, path_zone, data=zone_patch_data, headers=headers, expected_status=HTTPStatus.OK)
