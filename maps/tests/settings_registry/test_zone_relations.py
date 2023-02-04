import pytest
from http import HTTPStatus
from typing import List

from ya_courier_backend.models import db
from ya_courier_backend.models.zone_relation import ZoneRelation, ZoneRelationType

from maps.b2bgeo.ya_courier.backend.test_lib.conftest import skip_if_remote
from maps.b2bgeo.ya_courier.backend.test_lib.util_offline import local_get, local_post, local_delete


def _add_zones(env, zone_numbers):
    path_zone = f"/api/v1/reference-book/companies/{env.default_company.id}/zones"
    zone_data = [
        {
            "number": zone_number,
            "polygon": {
                "coordinates": [[[37, 55], [38, 55], [38, 56], [37, 56], [37, 55]]],
                "type": "Polygon"
            }
        }
        for zone_number in zone_numbers
    ]
    local_post(env.client, path_zone, headers=env.user_auth_headers, data=zone_data)

    res = []
    page_idx = 1
    while page := local_get(env.client, f'{path_zone}?page={page_idx}', headers=env.user_auth_headers):
        res += page
        page_idx += 1
    return res


def _post_zone_relations(env, relations_to_post, expected_status=HTTPStatus.OK):
    path_zone_relations = f'/api/v1/reference-book/companies/{env.default_company.id}/zone-relations'
    return local_post(env.client,
                      path_zone_relations,
                      headers=env.user_auth_headers,
                      data=relations_to_post,
                      expected_status=expected_status)


def _post_zone_relation(env, type, zone_ids, expected_status=HTTPStatus.OK):
    relations_to_post = [{
        'type': type,
        'zone_ids': zone_ids
    }]
    return _post_zone_relations(env, relations_to_post, expected_status)


def _post_incompatible_zones(env, zone_ids: List[List[str]], expected_status=HTTPStatus.OK):
    relations_to_post = [
        {'type': 'incompatible', 'zone_ids': x}
        for x in zone_ids
    ]
    return _post_zone_relations(env, relations_to_post, expected_status)


@skip_if_remote
def test_zone_relation_creation(env):
    # 0. Prepare zones
    zones = _add_zones(env, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Post zone relation
    response = _post_incompatible_zones(env, [[zones[0]['id'], zones[1]['id']]])

    # 2. Check that relation is created
    with env.flask_app.app_context():
        zone_relation = db.session.query(ZoneRelation).one()
    assert len(response) == 1
    assert int(response[0]['id']) == zone_relation.id
    assert zone_relation.company_id == env.default_company.id
    assert zone_relation.type == ZoneRelationType.incompatible
    assert set(zone_relation.zone_ids) == {int(zone['id']) for zone in zones}


@skip_if_remote
def test_multiple_zone_relation_creation(env):
    # 0. Prepare zones
    zones = _add_zones(env, [
        'test_zone_relations_1',
        'test_zone_relations_2',
        'test_zone_relations_3'
    ])

    # 1. Post zone relation
    response = _post_incompatible_zones(env, [
        [zones[0]['id'], zones[1]['id']],
        [zones[0]['id'], zones[2]['id']]
    ])

    # 2. Check that relation is created
    with env.flask_app.app_context():
        zone_relations = db.session.query(ZoneRelation).all()
    assert len(response) == 2
    assert len(zone_relations) == 2
    for response_relation, db_relation in zip(response, zone_relations):
        assert int(response_relation['id']) == db_relation.id


@skip_if_remote
def test_descending_zone_ids_are_sorted_when_relation_is_created(env):
    # 0. Prepare zones
    zones = _add_zones(env, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Post zone relation
    _post_incompatible_zones(env, [[
        max(zones[0]['id'], zones[1]['id']),
        min(zones[0]['id'], zones[1]['id'])]])

    # 2. Check that relation is created
    with env.flask_app.app_context():
        zone_relation = db.session.query(ZoneRelation).one()
    assert str(zone_relation.zone_ids[0]) == min(zones[0]['id'], zones[1]['id'])
    assert str(zone_relation.zone_ids[1]) == max(zones[0]['id'], zones[1]['id'])


@skip_if_remote
def test_relation_cant_be_created_for_single_zone(env):
    # 0. Prepare zones
    zones = _add_zones(env, ['test_zone_relations_1'])

    # 1. Try to post self-related zone and get http 422
    response = _post_incompatible_zones(env,
                                        [[zones[0]['id'], zones[0]['id']]],
                                        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert "Self related zones are not allowed" in response['message']


@skip_if_remote
def test_post_same_relation_twice_fails(env):
    # 0. Prepare zones
    zones = _add_zones(env, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Post zone relation
    _post_incompatible_zones(env, [[zones[0]['id'], zones[1]['id']]])

    # 2. Post same relation again;
    #    expect everything's fine and same relation_id is returned
    response = _post_incompatible_zones(env,
                                        [[zones[0]['id'], zones[1]['id']]],
                                        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert 'Zone relation already exists' in response['message']


@skip_if_remote
def test_post_relation_of_more_than_two_zones_not_allowed(env):
    # 0. Prepare zones
    zones = _add_zones(env, [
        'test_zone_relations_1',
        'test_zone_relations_2',
        'test_zone_relations_3',
    ])

    # 1. Post relation of three zones; expected 422
    _post_incompatible_zones(env,
                             [[zones[0]['id'], zones[1]['id'], zones[2]['id']]],
                             expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_post_invalid_relation_type_fails(env):
    # 0. Prepare zones
    zones = _add_zones(env, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Try to post zone relation with invalid type; expected 422
    _post_zone_relation(env,
                        'invalid_type',
                        [zones[0]['id'], zones[1]['id']],
                        expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)


@skip_if_remote
def test_post_relation_with_public_zone(env):
    # 0. Prepare zones
    zones = _add_zones(env, ['test_zone_relations_1'])
    path_public_zones = '/api/v1/reference-book/public/zones'
    public_zones = local_get(env.client, path_public_zones, headers=env.user_auth_headers)

    # 1. Post zone relation
    _post_incompatible_zones(env, [[zones[0]['id'], public_zones[0]['id']]])

    # 2. Check that relation is created
    with env.flask_app.app_context():
        zone_relation = db.session.query(ZoneRelation).one()
    assert zone_relation.company_id == env.default_company.id
    assert set(zone_relation.zone_ids) == {int(zones[0]['id']), int(public_zones[0]['id'])}
    assert zone_relation.type == ZoneRelationType.incompatible


@skip_if_remote
def test_delete_relation(env):
    # 0. Prepare zones
    zones = _add_zones(env, ['test_zone_relations_1', 'test_zone_relations_2'])

    # 1. Post zone relation
    response = _post_incompatible_zones(env, [[zones[0]['id'], zones[1]['id']]])

    # 2. Delete zone relation
    path_zone_relation = f'/api/v1/reference-book/companies/{env.default_company.id}/zone-relations/{response[0]["id"]}'
    local_delete(env.client, path_zone_relation, headers=env.user_auth_headers)

    # 3. Check that relation is deleted
    with env.flask_app.app_context():
        zone_relations = db.session.query(ZoneRelation).all()
    assert not zone_relations


@skip_if_remote
def test_delete_non_existing_relation_fails(env):
    # 1. Trying to delete zone relation with non-existing id; expect 422
    fake_relation_id = 1234
    path_zone_relation = f'/api/v1/reference-book/companies/{env.default_company.id}/zone-relations/{fake_relation_id}'
    response = local_delete(env.client,
                            path_zone_relation,
                            headers=env.user_auth_headers,
                            expected_status=HTTPStatus.UNPROCESSABLE_ENTITY)
    assert f"Relation with id '{fake_relation_id}' doesn't exist." in response['message']


@skip_if_remote
def test_relation_is_deleted_if_zone_is_deleted(env):
    # 0. Prepare zones and relations
    zones = _add_zones(env, [
        'test_zone_relations_1',
        'test_zone_relations_2',
        'test_zone_relations_3'
    ])
    _post_incompatible_zones(env, [
        [zones[0]['id'], zones[1]['id']],
        [zones[0]['id'], zones[2]['id']],
        [zones[1]['id'], zones[2]['id']]
    ])

    # 1. Delete zone
    path_zones = f'/api/v1/reference-book/companies/{env.default_company.id}/zones/{zones[0]["id"]}'
    local_delete(env.client, path_zones, headers=env.user_auth_headers)

    # 2. Check that corresponding relations are gone
    with env.flask_app.app_context():
        zone_relations = db.session.query(ZoneRelation).all()
    assert len(zone_relations) == 1


@skip_if_remote
def test_get_all_relations(env):
    # 0. Prepare zones
    zones = _add_zones(env, [
        'test_zone_relations_1',
        'test_zone_relations_2',
        'test_zone_relations_3'
    ])

    # 1. Post zone relation
    _post_incompatible_zones(env, [
        [zones[0]['id'], zones[1]['id']],
        [zones[0]['id'], zones[2]['id']],
        [zones[1]['id'], zones[2]['id']]
    ])

    # 2. Get all relations
    path_zone_relations = f'/api/v1/reference-book/companies/{env.default_company.id}/zone-relations'
    response = local_get(env.client, path_zone_relations, headers=env.user_auth_headers)
    assert len(response) == 3


@skip_if_remote
def test_get_relations_of_single_zone(env):
    # 0. Prepare zones
    zones = _add_zones(env, [
        'test_zone_relations_1',
        'test_zone_relations_2',
        'test_zone_relations_3'
    ])

    # 1. Post zone relation
    _post_incompatible_zones(env, [
        [zones[0]['id'], zones[1]['id']],
        [zones[0]['id'], zones[2]['id']],
        [zones[1]['id'], zones[2]['id']]
    ])

    # 2. Get relations for a given zone_id
    path_zone_relations = f'/api/v1/reference-book/companies/{env.default_company.id}/zone-relations?zone_ids={zones[0]["id"]}'
    response = local_get(env.client,
                         path_zone_relations,
                         headers=env.user_auth_headers)
    assert len(response) == 2


@skip_if_remote
def test_get_a_lot_of_zone_relations(env):
    # 0. Prepare zones
    zones = _add_zones(env, [f'test_zone_relations_{i}' for i in range(250)])

    # 1. Post zone relation
    _post_incompatible_zones(env, [[zones[0]['id'], zones[i + 1]['id']] for i in range(200)])
    _post_incompatible_zones(env, [[zones[1]['id'], zones[i + 2]['id']] for i in range(200)])
    _post_incompatible_zones(env, [[zones[2]['id'], zones[i + 3]['id']] for i in range(200)])

    # 2. Get relations for given zone_ids
    path_zone_relations = (
        f'/api/v1/reference-book/companies/{env.default_company.id}/zone-relations'
        f'?per_page=1000&zone_ids={zones[0]["id"]},{zones[1]["id"]},{zones[2]["id"]}'
    )
    response = local_get(env.client, path_zone_relations, headers=env.user_auth_headers)
    assert len(response) == 600


@skip_if_remote
def test_get_relations_of_multiple_zones(env):
    # 0. Prepare zones
    zones = _add_zones(env, [
        'test_zone_relations_1',
        'test_zone_relations_2',
        'test_zone_relations_3',
        'test_zone_relations_4'
    ])

    # 1. Post zone relation
    _post_incompatible_zones(env, [
        [zones[0]['id'], zones[1]['id']],
        [zones[0]['id'], zones[2]['id']],
        [zones[1]['id'], zones[2]['id']],
        [zones[2]['id'], zones[3]['id']]
    ])

    # 2. Get all relations
    path_zone_relations = f'/api/v1/reference-book/companies/{env.default_company.id}/zone-relations?zone_ids={zones[0]["id"]},{zones[1]["id"]}'
    response = local_get(env.client,
                         path_zone_relations,
                         headers=env.user_auth_headers)
    assert len(response) == 3


@skip_if_remote
def test_get_relations_of_non_related_zone(env):
    # 0. Prepare zones
    zones = _add_zones(env, [
        'test_zone_relations_1',
        'test_zone_relations_2',
        'test_zone_relations_3',
        'test_zone_relations_4'
    ])

    # 1. Post zone relation
    _post_incompatible_zones(env, [
        [zones[0]['id'], zones[1]['id']],
        [zones[0]['id'], zones[2]['id']],
        [zones[1]['id'], zones[2]['id']]
    ])

    # 2. Get all relations
    path_zone_relations = f'/api/v1/reference-book/companies/{env.default_company.id}/zone-relations?zone_ids={zones[3]["id"]}'
    local_get(env.client,
              path_zone_relations,
              headers=env.user_auth_headers,
              expected_status=HTTPStatus.OK)


@skip_if_remote
@pytest.mark.parametrize("page", [1, 2])
def test_get_by_page(env, page):
    # 0. Prepare zones
    zones = _add_zones(env, [
        'test_zone_relations_1',
        'test_zone_relations_2',
        'test_zone_relations_3',
        'test_zone_relations_4'
    ])

    # 1. Post zone relation
    relations_to_post = [
        [zones[0]['id'], zones[1]['id']],
        [zones[0]['id'], zones[2]['id']],
        [zones[1]['id'], zones[2]['id']],
        [zones[2]['id'], zones[3]['id']],
    ]
    _post_incompatible_zones(env, relations_to_post)

    # 2. Get all relations
    path_zone_relations = f'/api/v1/reference-book/companies/{env.default_company.id}/zone-relations?page={page}&per_page=2'
    response = local_get(env.client,
                         path_zone_relations,
                         headers=env.user_auth_headers)
    assert len(response) == 2

    reference_relations = relations_to_post[2 * (page - 1) : 2 * page]
    assert set(response[0]['zone_ids']) == set(reference_relations[0])
    assert set(response[1]['zone_ids']) == set(reference_relations[1])
