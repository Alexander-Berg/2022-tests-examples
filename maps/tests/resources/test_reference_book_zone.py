import json
from copy import deepcopy
from http import HTTPStatus

import pytest

from maps.b2bgeo.reference_book.lib.models import (
    FORBIDDEN_COMPANY_ZONE_CHARACTERS,
    FORBIDDEN_COMPANY_ZONE_NUMBERS,
    PREFIX_PUBLIC_ZONE,
    ColorZoneException,
    CompanyZone,
    ForbiddenCharactersCompanyZoneException,
    GeoJsonTypeNotPolygonZoneException,
    InsideTtkMkadCompanyZoneException,
    PublicPrefixForCompanyZoneException,
    PublicZone,
    db,
)

ORIGINAL_ZONE = {
    'number': 'test_company_zone',
    'color_fill': '#010101',
    'color_edge': '#020202',
    'polygon': {
        'coordinates': [[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]],
        'type': 'Polygon',
    },
}


def _get_request_params(path: str, path_postfix: str):
    return {
        'path': path if not path_postfix else f'{path}{path_postfix}',
    }


def _get_params_company_zone(company_id: int, path_postfix: str = ''):
    path = f'/api/v1/reference-book/companies/{company_id}/zones'
    return _get_request_params(path=path, path_postfix=path_postfix)


def _get_params_public_zone(path_postfix: str = ''):
    path = '/api/v1/reference-book/public/zones'
    return _get_request_params(path=path, path_postfix=path_postfix)


def _generate_new_zone(zone, idx):
    new_zone = deepcopy(zone)
    new_zone['number'] = f'{new_zone["number"]}_{idx}'
    return new_zone


def test_company_zone_create(local_request, company_id):
    public_zones = local_request('GET', **_get_params_public_zone())
    local_request('POST', **_get_params_company_zone(company_id), data=[ORIGINAL_ZONE])  # post_batch_init

    company_after_zones = local_request('GET', **_get_params_company_zone(company_id))
    public_after_zones = local_request('GET', **_get_params_public_zone())
    assert len(company_after_zones) == 1
    assert len(public_zones) == len(public_after_zones)
    company_zone_id = company_after_zones[0].pop('id')
    assert company_after_zones[0] == ORIGINAL_ZONE

    resp_get_single = local_request('GET', **_get_params_company_zone(company_id, path_postfix=f'/{company_zone_id}'))
    assert resp_get_single.pop('id') == company_zone_id
    assert resp_get_single == ORIGINAL_ZONE


@pytest.mark.parametrize(
    argnames='forbidden_name',
    argvalues=FORBIDDEN_COMPANY_ZONE_NUMBERS,
)
def test_company_zone_inside_ttk_or_mkad(local_request, company_id, forbidden_name):
    company_zone_inside_ttk = deepcopy(ORIGINAL_ZONE)
    company_zone_inside_ttk['number'] = forbidden_name
    resp_post_batch = local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=[company_zone_inside_ttk],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert InsideTtkMkadCompanyZoneException.description in resp_post_batch['message']


def test_public_zone_add_new_one(local_request, company_id):
    public_zones = local_request('GET', **_get_params_public_zone())
    company_zones = local_request('GET', **_get_params_company_zone(company_id))

    new_test_public_zone = PublicZone(
        number='public_new',
        color_fill='red',
        polygon=json.dumps(ORIGINAL_ZONE['polygon']),
    )

    db.session.add(new_test_public_zone)
    db.session.commit()

    public_after_zones = local_request('GET', **_get_params_public_zone())
    company_after_zones = local_request('GET', **_get_params_company_zone(company_id))

    assert len(public_zones) + 1 == len(public_after_zones)
    assert len(company_zones) == len(company_after_zones)


def test_company_zone_create_with_bad_prefix_number(local_request, company_id):
    zone_bad_prefix = deepcopy(ORIGINAL_ZONE)
    zone_bad_prefix['number'] = f'{PREFIX_PUBLIC_ZONE}{zone_bad_prefix["number"]}'

    resp_post_batch = local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=[zone_bad_prefix],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert PublicPrefixForCompanyZoneException.description in resp_post_batch['message']


@pytest.mark.parametrize('character', list(FORBIDDEN_COMPANY_ZONE_CHARACTERS))
def test_company_zone_create_with_illegal_characters_number(local_request, company_id, character):
    zone_bad_prefix = deepcopy(ORIGINAL_ZONE)
    zone_bad_prefix['number'] = f'Sepa{character} rated'

    resp_post_batch = local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=[zone_bad_prefix],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert ForbiddenCharactersCompanyZoneException.description in resp_post_batch['message']


def test_company_zone_create_with_bad_geojson_type(local_request, company_id):
    zone_bad_geojson_type = deepcopy(ORIGINAL_ZONE)
    zone_bad_geojson_type['polygon'] = {
        'coordinates': [[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]],
        'type': 'LineString',
    }
    resp_post_batch = local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=[zone_bad_geojson_type],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert GeoJsonTypeNotPolygonZoneException.description in resp_post_batch['message']


def test_company_zone_create_with_bad_polygon(local_request, company_id):
    zone_self_intersecting = deepcopy(ORIGINAL_ZONE)
    ring = zone_self_intersecting['polygon']['coordinates'][0]
    ring[1], ring[2] = ring[2], ring[1]
    resp_post_batch = local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=[zone_self_intersecting],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert 'Geometry validation failed: Self-intersection at coordinate: 0.5 0.5' in resp_post_batch['message']


@pytest.mark.parametrize(
    argnames='bad_color_fill, msg',
    argvalues=[
        (True, 'Color for zone should be a valid HTML color'),
        ('#foobar', ColorZoneException.description),
    ],
)
def test_company_zone_create_with_bad_color_fill(local_request, company_id, bad_color_fill, msg):
    zone_bad_color_fill = deepcopy(ORIGINAL_ZONE)
    zone_bad_color_fill['color_fill'] = bad_color_fill
    resp_post_batch = local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=[zone_bad_color_fill],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert msg in resp_post_batch['message']


def test_company_zone_update_with_post(local_request, company_id):
    local_request(
        'POST',  # post_batch_init
        **_get_params_company_zone(company_id),
        data=[ORIGINAL_ZONE],
    )

    zone_update_by_post = deepcopy(ORIGINAL_ZONE)
    zone_update_by_post['color_fill'] = '#FFFFFF'

    zone_new_add_by_post = deepcopy(ORIGINAL_ZONE)
    zone_new_add_by_post['number'] = 'yet_another_zone'

    local_request(
        'POST',  # post_batch_with_update
        **_get_params_company_zone(company_id),
        data=[zone_update_by_post, zone_new_add_by_post],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )


def test_company_zone_update_with_post_bad_prefix(local_request, company_id):
    local_request(
        'POST',  # post_batch_init
        **_get_params_company_zone(company_id),
        data=[ORIGINAL_ZONE],
    )

    zone_update_by_post = deepcopy(ORIGINAL_ZONE)
    zone_update_by_post['color_fill'] = '#FFFFFF'
    zone_new_add_by_post = deepcopy(ORIGINAL_ZONE)
    zone_new_add_by_post['number'] = f'{PREFIX_PUBLIC_ZONE}yet_another_zone'
    local_request(
        'POST',  # post_batch_with_update
        **_get_params_company_zone(company_id),
        data=[zone_update_by_post, zone_new_add_by_post],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )

    resp_get_batch = local_request('GET', **_get_params_company_zone(company_id))
    assert len(resp_get_batch) == 1
    assert resp_get_batch[0]['color_fill'] == ORIGINAL_ZONE['color_fill']


def test_company_zone_patch_and_delete(local_request, company_id):
    local_request(
        'POST',  # post_batch_init
        **_get_params_company_zone(company_id),
        data=[ORIGINAL_ZONE],
    )
    resp_get_batch = local_request('GET', **_get_params_company_zone(company_id))
    assert len(resp_get_batch) == 1
    company_zone = resp_get_batch[0]

    zone_update_by_patch = deepcopy(ORIGINAL_ZONE)
    zone_update_by_patch['number'] = 'test_patched_zone'

    local_request(
        'PATCH',
        **_get_params_company_zone(company_id, path_postfix=f'/{company_zone["id"]}'),
        data=zone_update_by_patch,
    )
    resp_get_single = local_request(
        'GET', **_get_params_company_zone(company_id, path_postfix=f'/{company_zone["id"]}')
    )
    assert resp_get_single['number'] == zone_update_by_patch['number']
    assert resp_get_single['id'] == company_zone['id']

    local_request('DELETE', **_get_params_company_zone(company_id, path_postfix=f'/{company_zone["id"]}'))
    resp_get_batch = local_request('GET', **_get_params_company_zone(company_id))
    assert len(resp_get_batch) == 0


def test_public_zone_patch_and_delete_by_endpoints_of_company_zone(local_request, company_id):
    public_zones = local_request('GET', **_get_params_public_zone())

    zone_for_patch = deepcopy(public_zones[0])
    zone_for_patch['number'] = 'try_to_patch_public_zone'

    local_request(
        'PATCH',
        **_get_params_company_zone(company_id, path_postfix=f'/{zone_for_patch["id"]}'),
        data=zone_for_patch,
        expected_status=HTTPStatus.NOT_FOUND,
    )

    local_request(
        'DELETE',
        **_get_params_company_zone(company_id, path_postfix=f'/{public_zones[0]["id"]}'),
        expected_status=HTTPStatus.NOT_FOUND,
    )


def test_company_zone_patch_bad_prefix(local_request, company_id):
    local_request(
        'POST',  # post_batch_init
        **_get_params_company_zone(company_id),
        data=[ORIGINAL_ZONE],
    )
    resp_get_batch = local_request('GET', **_get_params_company_zone(company_id))
    assert len(resp_get_batch) == 1
    company_zone = resp_get_batch[0]

    zone_update_by_patch_bad_prefix = deepcopy(ORIGINAL_ZONE)
    zone_update_by_patch_bad_prefix['number'] = f'{PREFIX_PUBLIC_ZONE}test_patched_zone'

    local_request(
        'PATCH',
        **_get_params_company_zone(company_id, path_postfix=f'/{company_zone["id"]}'),
        data=zone_update_by_patch_bad_prefix,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    resp_get_single = local_request(
        'GET', **_get_params_company_zone(company_id, path_postfix=f'/{company_zone["id"]}')
    )
    assert resp_get_single['number'] == ORIGINAL_ZONE['number']


def test_company_zone_updated_at(local_request, company_id):
    local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=[ORIGINAL_ZONE],
    )
    zone_created = db.session.query(CompanyZone).one()
    db.session.expunge(zone_created)

    zone_update_by_post = deepcopy(ORIGINAL_ZONE)
    zone_update_by_post['color_fill'] = '#121212'
    local_request(
        'PATCH',
        **_get_params_company_zone(company_id, path_postfix=f'/{zone_created.id}'),
        data=zone_update_by_post,
    )
    zone_updated = db.session.query(CompanyZone).one()

    assert zone_updated.updated_at > zone_created.updated_at


def test_company_zone_count_limit(local_request, company_id):
    for idx in range(3):
        zones = [_generate_new_zone(ORIGINAL_ZONE, idx) for idx in range(idx * 100, (idx + 1) * 100)]
        local_request('POST', **_get_params_company_zone(company_id), data=zones)

    zones = [_generate_new_zone(ORIGINAL_ZONE, 300)]
    resp = local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=zones,
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert 'Exceeded limit on object count' in resp['message'], resp['message']


@pytest.mark.parametrize(
    argnames='zone_patch_data',
    argvalues=[
        {'color_fill': '#010101'},
        {'color_edge': '#020202'},
        {'number': 'new patched zone number'},
        {
            'polygon': {
                'coordinates': [[[0, 0], [0, 1], [1, 1], [1, 0], [0, 0]]],
                'type': 'Polygon',
            }
        },
        {},
    ],
)
def test_company_zone_patch_different_fields(local_request, company_id, zone_patch_data):
    local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=[ORIGINAL_ZONE],
    )
    resp_get_batch = local_request('GET', **_get_params_company_zone(company_id))
    assert len(resp_get_batch) == 1
    company_zone = resp_get_batch[0]

    local_request(
        'PATCH',
        **_get_params_company_zone(company_id, path_postfix=f'/{company_zone["id"]}'),
        data=zone_patch_data,
        expected_status=HTTPStatus.OK,
    )
    resp_get_single = local_request(
        'GET', **_get_params_company_zone(company_id, path_postfix=f'/{company_zone["id"]}')
    )
    for key, value in zone_patch_data.items():
        assert resp_get_single[key] == value


def test_company_zone_create_with_too_many_points(local_request, company_id):
    zone_too_many_points = deepcopy(ORIGINAL_ZONE)
    lat = 0
    lon = 0
    d_lat = 0.00001
    d_lon = 0.00002
    coordinates = []
    for _ in range(1000):
        coordinates.append([lat, lon])
        lat += d_lat
        lon += d_lon
    coordinates.append([lat, 0])
    coordinates.append([0, 0])

    zone_too_many_points['polygon'] = {
        'coordinates': [coordinates],
        'type': 'Polygon',
    }
    resp_post_batch = local_request(
        'POST',
        **_get_params_company_zone(company_id),
        data=[zone_too_many_points],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )
    assert 'Following zones have too many points ' in resp_post_batch['message']
