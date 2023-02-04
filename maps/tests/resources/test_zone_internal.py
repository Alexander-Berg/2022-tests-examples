import json
from maps.b2bgeo.reference_book.lib.models import (
    PREFIX_PUBLIC_ZONE,
    PublicZone,
    db,
)


def _add_zones(local_request, company_id, zone_numbers):
    path_zone = f'/api/v1/reference-book/companies/{company_id}/zones'
    zone_data = [
        {
            'number': zone_number,
            'polygon': {'coordinates': [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]], 'type': 'Polygon'},
        }
        for zone_number in zone_numbers
    ]
    local_request('POST', path_zone, data=zone_data)
    return local_request('GET', f'{path_zone}')


def _add_public_zones(local_request, zone_numbers):
    zone_data = [
        PublicZone(
            number=f'{PREFIX_PUBLIC_ZONE}{zone_number}',
            polygon=json.dumps(
                {'coordinates': [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]], 'type': 'Polygon'}
            ),
        )
        for zone_number in zone_numbers
    ]
    for zone in zone_data:
        db.session.add(zone)
    db.session.commit()


def _post_zone_relations(local_request, company_id, relations_to_post):
    path_zone_relations = f'/api/v1/reference-book/companies/{company_id}/zone-relations'
    return local_request('POST', path_zone_relations, data=relations_to_post)


def _post_incompatible_zones(local_request, company_id, zone_ids: list[list[str]]):
    relations_to_post = [{'type': 'incompatible', 'zone_ids': x} for x in zone_ids]
    return _post_zone_relations(local_request, company_id, relations_to_post)


def test_zone_by_numbers(local_request, company_id):
    _add_zones(local_request, company_id, ['zone_1', 'zone_2', 'zone_3'])
    path = f'/api/v1/internal/reference-book/companies/{company_id}/zones?zone_numbers=zone_1,zone_3'
    response = local_request('GET', path)
    assert len(response) == 2
    numbers = set([item['number'] for item in response])
    assert 'zone_1' in numbers
    assert 'zone_3' in numbers


def test_zone_by_numbers_with_missing(local_request, company_id):
    _add_zones(local_request, company_id, ['zone_1', 'zone_2', 'zone_3'])
    path = f'/api/v1/internal/reference-book/companies/{company_id}/zones?zone_numbers=zone_1,zone_3,zone_4'
    response = local_request('GET', path)
    assert len(response) == 2
    numbers = set([item['number'] for item in response])
    assert 'zone_1' in numbers
    assert 'zone_3' in numbers


def test_zone_by_numbers_with_public(local_request, company_id):
    _add_zones(local_request, company_id, ['zone_1', 'zone_2', 'zone_3'])
    _add_public_zones(local_request, ['zone_4', 'zone_5', 'zone_6'])
    path = f'/api/v1/internal/reference-book/companies/{company_id}/zones?zone_numbers=zone_1,zone_3,{PREFIX_PUBLIC_ZONE}zone_5'
    response = local_request('GET', path)
    assert len(response) == 3
    numbers = set([item['number'] for item in response])
    assert 'zone_1' in numbers
    assert 'zone_3' in numbers
    assert f'{PREFIX_PUBLIC_ZONE}zone_5' in numbers


def test_zone_by_numbers_empty(local_request, company_id):
    path = f'/api/v1/internal/reference-book/companies/{company_id}/zones?zone_numbers=zone_1,zone_3,{PREFIX_PUBLIC_ZONE}zone_5'
    response = local_request('GET', path)
    assert len(response) == 0


def test_get_incompatible_zones(local_request, company_id):
    zones = _add_zones(local_request, company_id, ['zone_1', 'zone_2', 'zone_3', 'zone_4'])
    incompatible_zones = [[zones[0]['id'], zones[1]['id']], [zones[2]['id'], zones[3]['id']]]
    _post_incompatible_zones(local_request, company_id, incompatible_zones)
    path = f'/api/v1/internal/reference-book/companies/{company_id}/incompatible-zones'
    response = local_request('GET', path)
    assert len(response) == 2
    assert response == [[zones[0]['number'], zones[1]['number']], [zones[2]['number'], zones[3]['number']]]
