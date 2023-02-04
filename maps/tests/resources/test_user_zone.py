import uuid

from http import HTTPStatus
from datetime import timedelta

from maps.b2bgeo.reference_book.lib.models import UserZoneType

from maps.b2bgeo.identity.libs.payloads.py import User, UserCompany
from maps.b2bgeo.libs.py_auth.user_role import UserRole


MAXIMUM_USER_ZONE_REQUEST_SIZE = 100
TEST_USER_LOGIN = 'test_user_login'


def _create_zones(local_request, company_id: int, count=1, headers=None):
    path_zone = f'/api/v1/reference-book/companies/{company_id}/zones'
    zone_data = [
        {
            'number': f'zone {i}',
            'polygon': {'coordinates': [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]], 'type': 'Polygon'},
        }
        for i in range(count)
    ]
    local_request('POST', path_zone, headers=headers, data=zone_data)
    zones = []
    page = 1
    while True:
        zones_on_page = local_request('GET', f'{path_zone}?page={page}')
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


def test_user_zone_get_request_not_admin(local_request, audience, company_id, user_id, issuer):
    companies = [UserCompany(id=company_id, apikey=uuid.uuid4(), role=UserRole.manager)]
    user_payload = User(
        id=user_id,
        is_super=False,
        companies=companies,
    )
    user_jwt = issuer.issue(user_payload, audience, timedelta(minutes=15))

    zones = _create_zones(local_request, company_id, headers={'Authorization': f'bearer {user_jwt}'})
    zone_id = str(zones[0]['id'])
    zone_patch_data = {
        'number': 'new zone number',
        'color_fill': '#414141',
        'polygon': {'coordinates': [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]], 'type': 'Polygon'},
    }
    path_zone = f'/api/v1/reference-book/companies/{company_id}/zones/{zone_id}'

    local_request(
        'PATCH',
        path_zone,
        data=zone_patch_data,
        headers={'Authorization': f'bearer {user_jwt}'},
        expected_status=HTTPStatus.OK,
    )


def test_user_zone_get_request(local_request, company_id, user_id):
    zones = _create_zones(local_request, company_id)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)]
    path_user_zone = f'/api/v1/reference-book/companies/{company_id}/user-zones/{user_id}'

    local_request('POST', path_user_zone, data=user_zone_data, expected_status=HTTPStatus.OK)
    user_zone_permissions = local_request('GET', path_user_zone, expected_status=HTTPStatus.OK)
    assert user_zone_permissions == user_zone_data


def test_user_zone_for_public_zone(local_request, company_id, user_id):
    path_zone = '/api/v1/reference-book/public/zones'
    path_user_zone = f'/api/v1/reference-book/companies/{company_id}/user-zones/{user_id}'
    public_zones = local_request('GET', path_zone)
    assert public_zones
    public_zone_id = public_zones[0]['id']

    local_request(
        'POST', path_user_zone, data=[_format_edit_user_zone(public_zone_id)], expected_status=HTTPStatus.NOT_FOUND
    )


def test_user_zone_for_non_existent_zone(local_request, company_id, user_id):
    non_existent_zone = 41
    path_zone = f'/api/v1/reference-book/companies/{company_id}/zones/{non_existent_zone}'
    path_user_zone = f'/api/v1/reference-book/companies/{company_id}/user-zones/{user_id}'

    local_request('GET', path_zone, expected_status=HTTPStatus.NOT_FOUND)
    local_request(
        'POST',
        path_user_zone,
        data=[_format_edit_user_zone(non_existent_zone)],
        expected_status=HTTPStatus.NOT_FOUND,
    )


def test_user_zone_repeatable_zones(local_request, company_id, user_id):
    zones = _create_zones(local_request, company_id)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)] * 2
    path_user_zone = f'/api/v1/reference-book/companies/{company_id}/user-zones/{user_id}'

    local_request('POST', path_user_zone, data=user_zone_data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


def test_user_zone_too_many_zones(local_request, company_id, user_id):
    zones = _create_zones(local_request, company_id, MAXIMUM_USER_ZONE_REQUEST_SIZE + 1)
    user_zone_data = [_format_edit_user_zone(zone['id']) for zone in zones]
    path_user_zone = f'/api/v1/reference-book/companies/{company_id}/user-zones/{user_id}'

    local_request('POST', path_user_zone, data=user_zone_data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


def test_user_zone_pagination(local_request, company_id, user_id):
    per_page = 10
    zones = _create_zones(local_request, company_id, 2 * per_page - 1)
    user_zone_data = [_format_edit_user_zone(zone['id']) for zone in zones]
    path_user_zone = f'/api/v1/reference-book/companies/{company_id}/user-zones/{user_id}'

    local_request('POST', path_user_zone, data=user_zone_data, expected_status=HTTPStatus.OK)
    first_page_zones = local_request('GET', path_user_zone + f'?page=1&per_page={per_page}')
    second_page_zones = local_request('GET', path_user_zone + f'?page=2&per_page={per_page}')

    response_zones = first_page_zones + second_page_zones
    request_ids = set([zone['id'] for zone in zones])
    response_ids = set([zone['zone_id'] for zone in response_zones])

    assert len(first_page_zones) == per_page
    assert len(second_page_zones) == per_page - 1
    assert request_ids == response_ids


def test_user_zone_delete_non_existent_relation(local_request, company_id, user_id):
    zones = _create_zones(local_request, company_id)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)]
    path_user_zone = f'/api/v1/reference-book/companies/{company_id}/user-zones/{user_id}'

    assert local_request('GET', path_user_zone, expected_status=HTTPStatus.OK) == []
    local_request('DELETE', path_user_zone, data=user_zone_data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


def test_user_zone_post_non_existent_permission_type(local_request, company_id, user_id):
    zones = _create_zones(local_request, company_id)
    zone_id = str(zones[0]['id'])
    user_zone_data = [_format_edit_user_zone(zone_id)]
    user_zone_data[0]['type'] = 'non existent type'
    path_user_zone = f'/api/v1/reference-book/companies/{company_id}/user-zones/{user_id}'

    local_request('POST', path_user_zone, data=user_zone_data, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


def test_user_edit_created_zones(local_request, company_id, user_id):
    zones = _create_zones(local_request, company_id)
    zone_id = str(zones[0]['id'])
    zone_patch_data = {
        'number': 'new zone number',
        'color_fill': '#414141',
        'polygon': {'coordinates': [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]], 'type': 'Polygon'},
    }
    path_zone = f'/api/v1/reference-book/companies/{company_id}/zones/{zone_id}'

    local_request('PATCH', path_zone, data=zone_patch_data, expected_status=HTTPStatus.OK)
