from http import HTTPStatus

import pytest

from maps.b2bgeo.reference_book.lib.models import ZoneRelation, ZoneRelationType, db


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

    res = []
    page_idx = 1
    while page := local_request('GET', f'{path_zone}?page={page_idx}'):
        res += page
        page_idx += 1
    return res


def _post_zone_relations(local_request, company_id, relations_to_post, expected_status=HTTPStatus.OK):
    path_zone_relations = f'/api/v1/reference-book/companies/{company_id}/zone-relations'
    return local_request('POST', path_zone_relations, data=relations_to_post, expected_status=expected_status)


def _post_zone_relation(local_request, company_id, type, zone_ids, expected_status=HTTPStatus.OK):
    relations_to_post = [{'type': type, 'zone_ids': zone_ids}]
    return _post_zone_relations(local_request, company_id, relations_to_post, expected_status)


def _post_incompatible_zones(local_request, company_id, zone_ids: list[list[str]], expected_status=HTTPStatus.OK):
    relations_to_post = [{'type': 'incompatible', 'zone_ids': x} for x in zone_ids]
    return _post_zone_relations(local_request, company_id, relations_to_post, expected_status)


def test_zone_relation_creation(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(local_request, company_id, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Post zone relation
    response = _post_incompatible_zones(local_request, company_id, [[zones[0]['id'], zones[1]['id']]])

    # 2. Check that relation is created
    zone_relation = db.session.query(ZoneRelation).one()
    assert len(response) == 1
    assert int(response[0]['id']) == zone_relation.id
    assert zone_relation.company_id == company_id
    assert zone_relation.type == ZoneRelationType.incompatible
    assert set(zone_relation.zone_ids) == {int(zone['id']) for zone in zones}


def test_multiple_zone_relation_creation(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(
        local_request, company_id, ['test_zone_relations_1', 'test_zone_relations_2', 'test_zone_relations_3']
    )

    # 1. Post zone relation
    response = _post_incompatible_zones(
        local_request, company_id, [[zones[0]['id'], zones[1]['id']], [zones[0]['id'], zones[2]['id']]]
    )

    # 2. Check that relation is created
    zone_relations = db.session.query(ZoneRelation).all()
    assert len(response) == 2
    assert len(zone_relations) == 2
    for response_relation, db_relation in zip(response, zone_relations):
        assert int(response_relation['id']) == db_relation.id


def test_descending_zone_ids_are_sorted_when_relation_is_created(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(local_request, company_id, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Post zone relation
    _post_incompatible_zones(
        local_request, company_id, [[max(zones[0]['id'], zones[1]['id']), min(zones[0]['id'], zones[1]['id'])]]
    )

    # 2. Check that relation is created
    zone_relation = db.session.query(ZoneRelation).one()
    assert str(zone_relation.zone_ids[0]) == min(zones[0]['id'], zones[1]['id'])
    assert str(zone_relation.zone_ids[1]) == max(zones[0]['id'], zones[1]['id'])


def test_relation_cant_be_created_for_single_zone(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(local_request, company_id, ['test_zone_relations_1'])

    # 1. Try to post self-related zone and get http 422
    response = _post_incompatible_zones(
        local_request, company_id, [[zones[0]['id'], zones[0]['id']]], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
    )
    assert 'Self related zones are not allowed' in response['message']


def test_post_same_relation_twice_fails(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(local_request, company_id, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Post zone relation
    _post_incompatible_zones(local_request, company_id, [[zones[0]['id'], zones[1]['id']]])

    # 2. Post same relation again;
    #    expect everything's fine and same relation_id is returned
    response = _post_incompatible_zones(
        local_request, company_id, [[zones[0]['id'], zones[1]['id']]], expected_status=HTTPStatus.UNPROCESSABLE_ENTITY
    )
    assert 'Zone relation already exists' in response['message']


def test_post_relation_of_more_than_two_zones_not_allowed(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(
        local_request,
        company_id,
        [
            'test_zone_relations_1',
            'test_zone_relations_2',
            'test_zone_relations_3',
        ],
    )

    # 1. Post relation of three zones; expected 422
    _post_incompatible_zones(
        local_request,
        company_id,
        [[zones[0]['id'], zones[1]['id'], zones[2]['id']]],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )


def test_post_invalid_relation_type_fails(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(local_request, company_id, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Try to post zone relation with invalid type; expected 422
    _post_zone_relation(
        local_request,
        company_id,
        'invalid_type',
        [zones[0]['id'], zones[1]['id']],
        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY,
    )


def test_post_relation_with_public_zone(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(local_request, company_id, ['test_zone_relations_1'])
    path_public_zones = '/api/v1/reference-book/public/zones'
    public_zones = local_request('GET', path_public_zones)

    # 1. Post zone relation
    _post_incompatible_zones(local_request, company_id, [[zones[0]['id'], public_zones[0]['id']]])

    # 2. Check that relation is created
    zone_relation = db.session.query(ZoneRelation).one()
    assert zone_relation.company_id == company_id
    assert set(zone_relation.zone_ids) == {int(zones[0]['id']), int(public_zones[0]['id'])}
    assert zone_relation.type == ZoneRelationType.incompatible


def test_delete_relation(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(local_request, company_id, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Post zone relation
    response = _post_incompatible_zones(local_request, company_id, [[zones[0]['id'], zones[1]['id']]])

    # 2. Delete zone relation
    path_zone_relation = f'/api/v1/reference-book/companies/{company_id}/zone-relations/{response[0]["id"]}'
    local_request('DELETE', path_zone_relation)

    # 3. Check that relation is deleted
    zone_relations = db.session.query(ZoneRelation).all()
    assert not zone_relations


def test_delete_non_existing_relation_fails(local_request, company_id):
    # 1. Trying to delete zone relation with non-existing id; expect 422
    fake_relation_id = 1234
    path_zone_relation = f'/api/v1/reference-book/companies/{company_id}/zone-relations/{fake_relation_id}'
    response = local_request('DELETE', path_zone_relation, expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert f'Relation with id \'{fake_relation_id}\' doesn\'t exist.' in response['message']


def test_relation_is_deleted_if_zone_is_deleted(local_request, company_id):
    # 0. Prepare zones and relations
    zones = _add_zones(
        local_request, company_id, ['test_zone_relations_1', 'test_zone_relations_2', 'test_zone_relations_3']
    )
    _post_incompatible_zones(
        local_request,
        company_id,
        [[zones[0]['id'], zones[1]['id']], [zones[0]['id'], zones[2]['id']], [zones[1]['id'], zones[2]['id']]],
    )

    # 1. Delete zone
    path_zones = f'/api/v1/reference-book/companies/{company_id}/zones/{zones[0]["id"]}'
    local_request('DELETE', path_zones)

    # 2. Check that corresponding relations are gone
    zone_relations = db.session.query(ZoneRelation).all()
    assert len(zone_relations) == 1


def test_get_all_relations(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(
        local_request, company_id, ['test_zone_relations_1', 'test_zone_relations_2', 'test_zone_relations_3']
    )

    # 1. Post zone relation
    _post_incompatible_zones(
        local_request,
        company_id,
        [[zones[0]['id'], zones[1]['id']], [zones[0]['id'], zones[2]['id']], [zones[1]['id'], zones[2]['id']]],
    )

    # 2. Get all relations
    path_zone_relations = f'/api/v1/reference-book/companies/{company_id}/zone-relations'
    response = local_request('GET', path_zone_relations)
    assert len(response) == 3


def test_get_relations_of_single_zone(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(
        local_request, company_id, ['test_zone_relations_1', 'test_zone_relations_2', 'test_zone_relations_3']
    )

    # 1. Post zone relation
    _post_incompatible_zones(
        local_request,
        company_id,
        [[zones[0]['id'], zones[1]['id']], [zones[0]['id'], zones[2]['id']], [zones[1]['id'], zones[2]['id']]],
    )

    # 2. Get relations for a given zone_id
    path_zone_relations = f'/api/v1/reference-book/companies/{company_id}/zone-relations?zone_ids={zones[0]["id"]}'
    response = local_request('GET', path_zone_relations)
    assert len(response) == 2


def test_get_a_lot_of_zone_relations(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(local_request, company_id, [f'test_zone_relations_{i}' for i in range(250)])

    # 1. Post zone relation
    _post_incompatible_zones(local_request, company_id, [[zones[0]['id'], zones[i + 1]['id']] for i in range(200)])
    _post_incompatible_zones(local_request, company_id, [[zones[1]['id'], zones[i + 2]['id']] for i in range(200)])
    _post_incompatible_zones(local_request, company_id, [[zones[2]['id'], zones[i + 3]['id']] for i in range(200)])

    # 2. Get relations for given zone_ids
    path_zone_relations = (
        f'/api/v1/reference-book/companies/{company_id}/zone-relations'
        f'?per_page=1000&zone_ids={zones[0]["id"]},{zones[1]["id"]},{zones[2]["id"]}'
    )
    response = local_request('GET', path_zone_relations)
    assert len(response) == 600


def test_get_relations_of_multiple_zones(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(
        local_request,
        company_id,
        ['test_zone_relations_1', 'test_zone_relations_2', 'test_zone_relations_3', 'test_zone_relations_4'],
    )

    # 1. Post zone relation
    _post_incompatible_zones(
        local_request,
        company_id,
        [
            [zones[0]['id'], zones[1]['id']],
            [zones[0]['id'], zones[2]['id']],
            [zones[1]['id'], zones[2]['id']],
            [zones[2]['id'], zones[3]['id']],
        ],
    )

    # 2. Get all relations
    path_zone_relations = (
        f'/api/v1/reference-book/companies/{company_id}/zone-relations?zone_ids={zones[0]["id"]},{zones[1]["id"]}'
    )
    response = local_request('GET', path_zone_relations)
    assert len(response) == 3


def test_get_relations_of_non_related_zone(local_request, company_id):
    # 0. Prepare zones
    zones = _add_zones(
        local_request,
        company_id,
        ['test_zone_relations_1', 'test_zone_relations_2', 'test_zone_relations_3', 'test_zone_relations_4'],
    )

    # 1. Post zone relation
    _post_incompatible_zones(
        local_request,
        company_id,
        [[zones[0]['id'], zones[1]['id']], [zones[0]['id'], zones[2]['id']], [zones[1]['id'], zones[2]['id']]],
    )

    # 2. Get all relations
    path_zone_relations = f'/api/v1/reference-book/companies/{company_id}/zone-relations?zone_ids={zones[3]["id"]}'
    local_request('GET', path_zone_relations, expected_status=HTTPStatus.OK)


@pytest.mark.parametrize('page', [1, 2])
def test_get_by_page(local_request, company_id, page):
    # 0. Prepare zones
    zones = _add_zones(
        local_request,
        company_id,
        ['test_zone_relations_1', 'test_zone_relations_2', 'test_zone_relations_3', 'test_zone_relations_4'],
    )

    # 1. Post zone relation
    relations_to_post = [
        [zones[0]['id'], zones[1]['id']],
        [zones[0]['id'], zones[2]['id']],
        [zones[1]['id'], zones[2]['id']],
        [zones[2]['id'], zones[3]['id']],
    ]
    _post_incompatible_zones(local_request, company_id, relations_to_post)

    # 2. Get all relations
    path_zone_relations = f'/api/v1/reference-book/companies/{company_id}/zone-relations?page={page}&per_page=2'
    response = local_request('GET', path_zone_relations)
    assert len(response) == 2

    reference_relations = relations_to_post[2 * (page - 1) : 2 * page]  # noqa: E203
    assert set(response[0]['zone_ids']) == set(reference_relations[0])
    assert set(response[1]['zone_ids']) == set(reference_relations[1])
